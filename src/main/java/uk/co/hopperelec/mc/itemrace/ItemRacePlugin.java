package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.PaperCommandManager;
import fr.minuskube.inv.InventoryManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class ItemRacePlugin extends JavaPlugin {
    public final Map<OfflinePlayer, Map<Material, Integer>> depositedItems = new HashMap<>();
    public final InventoryManager inventoryManager = new InventoryManager(this);

    @Override
    public void onEnable() {
        inventoryManager.init();
        final PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new ItemRaceCommand(this));
        manager.enableUnstableAPI("help");
    }

    public int calculateScore(@NotNull OfflinePlayer player) {
        if (!depositedItems.containsKey(player)) return 0;
        int score = 0;
        for (int amount : depositedItems.get(player).values()) {
            score += 32 - Integer.numberOfLeadingZeros(amount); // log2(amount)
        }
        return score;
    }

    public Map<OfflinePlayer,Integer> calculateScores() {
        return depositedItems.entrySet().stream().collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), calculateScore(entry.getKey())),
                HashMap::putAll
        );
    }
}
