package uk.co.hopperelec.mc.itemrace;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.itemrace.pointshandling.PointsAwardMode;

import java.util.Arrays;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.floorLog;
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;

public class ItemRaceConfig {
    // Points
    public final @NotNull PointsAwardMode pointsAwardMode;
    public final int itemsPerPointGrowthRate;
    public final boolean awardPointForFirstItem;
    public final int maxPointsPerItemType;
    public final int maxItemsAwardedPoints; // Calculated
    // Deposited items
    public final boolean persistDepositedItems;
    public final int autosaveFrequencyTicks;
    public final boolean splitItemsIntoStacks;
    // Scoreboard
    public final boolean defaultScoreboardState;
    public final @NotNull DisplaySlot scoreboardDisplaySlot;
    // Denylist
    public final @NotNull ItemType @NotNull [] denylistItems;
    public final boolean treatDenylistAsWhitelist;
    public final boolean allowDamagedTools;

    public ItemRaceConfig(
            @NotNull PointsAwardMode pointsAwardMode,
            int itemsPerPointGrowthRate,
            boolean awardPointForFirstItem,
            int maxPointsPerItemType,
            boolean persistDepositedItems,
            int autosaveFrequencyTicks,
            boolean splitItemsIntoStacks,
            boolean defaultScoreboardState,
            @NotNull DisplaySlot scoreboardDisplaySlot,
            @NotNull ItemType @NotNull [] denylistItems,
            boolean treatDenylistAsWhitelist,
            boolean allowDamagedTools
    ) {
        this.pointsAwardMode = pointsAwardMode;
        if (itemsPerPointGrowthRate < 1) throw new IllegalArgumentException("itemsPerPointGrowthRate must be at least 1");
        this.itemsPerPointGrowthRate = itemsPerPointGrowthRate;
        this.awardPointForFirstItem = awardPointForFirstItem;
        this.maxPointsPerItemType = maxPointsPerItemType;
        this.persistDepositedItems = persistDepositedItems;
        this.autosaveFrequencyTicks = autosaveFrequencyTicks;
        this.splitItemsIntoStacks = splitItemsIntoStacks;
        this.defaultScoreboardState = defaultScoreboardState;
        this.scoreboardDisplaySlot = scoreboardDisplaySlot;
        this.denylistItems = denylistItems;
        this.treatDenylistAsWhitelist = treatDenylistAsWhitelist;
        this.allowDamagedTools = allowDamagedTools;

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
    }

    public int pointsForAmount(int amount) {
        if (amount <= 0) return 0;
        int score = floorLog(itemsPerPointGrowthRate, amount);
        if (awardPointForFirstItem && itemsPerPointGrowthRate > 1) score += 1;
        if (maxPointsPerItemType >= 0) score = Math.min(score, maxPointsPerItemType);
        return score;
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
