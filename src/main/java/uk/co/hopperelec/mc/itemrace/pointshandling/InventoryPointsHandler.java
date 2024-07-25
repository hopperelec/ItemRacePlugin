package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.HashMap;
import java.util.Map;

public class InventoryPointsHandler extends PointsHandler {
    public InventoryPointsHandler(@NotNull ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    public @NotNull Map<Material, Integer> getItems(@NotNull OfflinePlayer offlinePlayer) {
        if (!(offlinePlayer instanceof Player player))
            throw new IllegalStateException("Tried to get items of an offline player when the server is using a points award mode that requires a player to be online");
        final Map<Material, Integer> items = new HashMap<>();
        addInventoryToItemMap(player.getInventory(), items);
        return items;
    }
}
