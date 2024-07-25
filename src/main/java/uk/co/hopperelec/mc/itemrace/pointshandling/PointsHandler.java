package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.itemrace.ItemRaceConfig;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.floorLog;
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;

public abstract class PointsHandler {
    protected ItemRacePlugin plugin;

    public PointsHandler(ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    public ItemRaceConfig config() { return plugin.config; }

    @NotNull
    public abstract Map<Material, Integer> getItems(@NotNull OfflinePlayer offlinePlayer);
    
    public Collection<? extends OfflinePlayer> getEligiblePlayers() {
        return plugin.getServer().getOnlinePlayers();
    }

    public void onEnable() {}
    public void onDisable() {}

    public int pointsForAmount(int amount) {
        if (amount == 0) return 0;
        int score = floorLog(config().itemsPerPointGrowthRate(), amount);
        if (config().maxPointsPerItemType() > 0) score = Math.min(score, config().maxPointsPerItemType());
        if (config().awardPointForFirstItem() && config().itemsPerPointGrowthRate() > 1) return score + 1;
        return score;
    }

    public int calculateScore(@NotNull OfflinePlayer player) {
        return getItems(player).values().stream()
                .map(this::pointsForAmount)
                .reduce(0, Integer::sum);
    }
    
    public @NotNull Map<OfflinePlayer,Integer> calculateScores() {
        return getEligiblePlayers().stream()
                .map(player -> Map.entry(player, calculateScore(player)))
                .filter(entry -> entry.getValue() > 0)
                .collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll
                );
    }
    
    public boolean canAwardPointsFor(@Nullable Material material) {
        if (material == null || material.isAir()) return false;
        if (config().treatDenylistAsWhitelist())
            return Arrays.stream(config().denylistItems()).anyMatch(itemType -> itemType.is(material));
        return Arrays.stream(config().denylistItems()).noneMatch(itemType -> itemType.is(material));
    }

    public boolean canAwardPointsFor(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        if (config().allowDamagedTools() && isDamaged(itemStack)) return false;
        return canAwardPointsFor(itemStack.getType());
    }

    protected void addInventoryToItemMap(@NotNull Inventory inventory, @NotNull Map<Material, Integer> items) {
        for (ItemStack itemStack : inventory) {
            if (canAwardPointsFor(itemStack)) {
                items.put(itemStack.getType(), items.getOrDefault(itemStack.getType(), 0) + itemStack.getAmount());
            }
        }
    }
}
