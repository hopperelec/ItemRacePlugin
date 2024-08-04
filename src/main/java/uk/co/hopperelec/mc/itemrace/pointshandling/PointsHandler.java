package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.config.ItemRaceConfig;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class PointsHandler {
    protected final @NotNull ItemRacePlugin plugin;

    public PointsHandler(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    protected ItemRaceConfig config() { return plugin.config; }

    public abstract @NotNull Map<Material, Integer> getItems(@NotNull OfflinePlayer offlinePlayer);
    
    public @NotNull Collection<? extends OfflinePlayer> getEligiblePlayers() {
        return plugin.getServer().getOnlinePlayers();
    }

    public void onEnable() {}
    public void onDisable() {}

    public int calculateScore(@NotNull OfflinePlayer player) {
        return getItems(player).entrySet().stream()
                .map(entry -> config().calculatePointsFor(entry.getValue(), entry.getKey()))
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

    protected void addInventoryToItemMap(@NotNull Inventory inventory, @NotNull Map<Material, Integer> items) {
        for (ItemStack itemStack : inventory) {
            if (config().awardPointsFor(itemStack))
                items.put(itemStack.getType(), items.getOrDefault(itemStack.getType(), 0) + itemStack.getAmount());
        }
    }
}
