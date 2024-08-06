package uk.co.hopperelec.mc.itemrace.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.itemrace.ItemType;
import uk.co.hopperelec.mc.itemrace.pointshandling.PointsAwardMode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;

public class ItemRaceConfig {
    // Points
    public final @NotNull PointsAwardMode pointsAwardMode;
    public final int itemsPerPointGrowthRate;
    public final boolean awardPointForFirstItem;
    public final int maxPointsPerItemType;
    public final int maxItemsAwardedPoints; // Calculated
    public final @NotNull PointsModifier @NotNull [] modifiers;
    // Deposited items
    public final boolean persistDepositedItems;
    public final int autosaveFrequencyTicks;
    public final boolean splitItemsIntoStacks;
    public final @NotNull Set<Material> defaultAutoDeposit;
    // Scoreboard
    public final boolean defaultScoreboardState;
    public final @NotNull DisplaySlot scoreboardDisplaySlot;
    // Denylist
    public final @NotNull ItemType @NotNull [] denylistItems;
    public final boolean treatDenylistAsWhitelist;
    public final boolean allowDamagedTools;

    public ItemRaceConfig(@NotNull FileConfiguration configFile) {
        // Points
        final String pointsAwardModeStr = configFile.getString("points.award_mode");
        if (pointsAwardModeStr == null) throw new IllegalStateException("points.award_mode must be set");
        pointsAwardMode = PointsAwardMode.valueOf(pointsAwardModeStr.toUpperCase());

        itemsPerPointGrowthRate = configFile.getInt("points.items_per_point_growth_rate");
        awardPointForFirstItem = configFile.getBoolean("points.award_for_first_item");
        maxPointsPerItemType = configFile.getInt("points.max_per_item_type");
        maxItemsAwardedPoints =
                maxPointsPerItemType < 0 ? Integer.MAX_VALUE : (
                        maxPointsPerItemType == 0 ? 0 :
                                Double.valueOf(
                                        Math.pow(
                                                itemsPerPointGrowthRate,
                                                maxPointsPerItemType - (awardPointForFirstItem ? 1 : 0)
                                        )
                                ).intValue()
                );

        final List<?> modifiersSerialized = configFile.getList("points.modifiers");
        if (modifiersSerialized == null) modifiers = new PointsModifier[0];
        else modifiers = modifiersSerialized.stream()
                .map(modifierObj -> {
                    final Map<?, ?> modifierMap = (Map<?, ?>) modifierObj;
                    final List<?> changesSerialized = (List<?>) modifierMap.get("changes");
                    if (changesSerialized == null) throw new IllegalStateException("Each points.modifiers must contain `changes`");
                    return new PointsModifier(
                            ((List<?>) modifierMap.get("items")).stream()
                                    .map(itemObj -> new ItemType((String) itemObj))
                                    .collect(Collectors.toSet()),
                            changesSerialized.stream()
                                    .map(changeObj -> {
                                        final Map<?, ?> changeMap = (Map<?, ?>) changeObj;
                                        final String typeStr = (String) changeMap.get("type");
                                        final Integer value = (Integer) changeMap.get("value");
                                        if (typeStr == null || value == null)
                                            throw new IllegalStateException("Each points.modifiers.changes must contain the fields `type` and `value`");
                                        final Integer minItems = (Integer) changeMap.get("min_items");
                                        final Integer maxItems = (Integer) changeMap.get("max_items");
                                        return new PointsChange(
                                                PointsChangeType.valueOf(typeStr.toUpperCase()),
                                                value,
                                                minItems == null ? 1 : minItems,
                                                maxItems == null ? Integer.MAX_VALUE : maxItems
                                        );
                                    }).toList()
                    );
                }).toArray(PointsModifier[]::new);

        // Deposited items
        persistDepositedItems = configFile.getBoolean("deposited_items.persist");
        autosaveFrequencyTicks = (int)(configFile.getDouble("deposited_items.autosave_frequency") * 60 * 20);
        splitItemsIntoStacks = configFile.getBoolean("deposited_items.gui.split_into_stacks");
        defaultAutoDeposit = configFile.getStringList("deposited_items.default_auto_deposit").stream()
                .map(Material::valueOf)
                .filter(Material::isItem)
                .collect(Collectors.toUnmodifiableSet());

        // Scoreboard
        defaultScoreboardState = configFile.getBoolean("scoreboard.default_state");

        final String scoreboardDisplaySlotStr = configFile.getString("scoreboard.display_slot");
        if (scoreboardDisplaySlotStr == null) throw new IllegalStateException("scoreboard.display_slot must be set");
        scoreboardDisplaySlot = switch (scoreboardDisplaySlotStr.toUpperCase()) {
            case "SIDEBAR" -> DisplaySlot.SIDEBAR;
            case "BELOW_NAME" -> DisplaySlot.BELOW_NAME;
            case "PLAYER_LIST" -> DisplaySlot.PLAYER_LIST;
            default -> throw new IllegalStateException("scoreboard.display_slot must be one of SIDEBAR, BELOW_NAME or PLAYER_LIST");
        };

        // Denylist
        denylistItems = configFile.getStringList("denylist.items").stream()
                .map(ItemType::new)
                .toArray(ItemType[]::new);
        treatDenylistAsWhitelist = configFile.getBoolean("denylist.treat_as_allowlist");
        allowDamagedTools = configFile.getBoolean("denylist.allow_damaged_tools");
    }

    public int calculatePointsFor(int amount, @NotNull Material material) {
        final ItemType itemType = new ItemType(material);
        int points = 0;
        int minItems = awardPointForFirstItem ? 1 : 2;
        while (minItems <= amount) {
            int pointsToAdd = 1;
            for (PointsModifier modifier : modifiers) {
                pointsToAdd = modifier.apply(itemType, minItems, pointsToAdd);
            }
            points += pointsToAdd;
            if (itemsPerPointGrowthRate == 1) minItems++;
            else minItems *= itemsPerPointGrowthRate;
        }
        if (maxPointsPerItemType >= 0) points = Math.min(points, maxPointsPerItemType);
        return points;
    }

    public boolean awardPointsFor(@Nullable Material material) {
        if (material == null || material.isAir()) return false;
        return treatDenylistAsWhitelist ?
                Arrays.stream(denylistItems).anyMatch(itemType -> itemType.is(material)) :
                Arrays.stream(denylistItems).noneMatch(itemType -> itemType.is(material));
    }
    public boolean awardPointsFor(@Nullable ItemStack itemStack) {
        return itemStack != null && (allowDamagedTools || !isDamaged(itemStack)) && awardPointsFor(itemStack.getType());
    }
}
