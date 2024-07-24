package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.Arrays;

public class MaxInventoryListener extends PlayerInventoryListener {
    private final ItemRacePlugin plugin;

    public MaxInventoryListener(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    private void countItems(@NotNull Player player, @NotNull Material material) {
        final int count = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && item.getType() == material)
                .map(ItemStack::getAmount)
                .reduce(0, Integer::sum);
        if (plugin.getAmountDeposited(player, material) < count)
            plugin.setAmountDeposited(player, material, count);
    }

    @Override
    protected void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
        plugin.runInstantly(() -> countItems(player, itemStack.getType()));
    }

    @Override
    protected void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event) {
        plugin.runInstantly(() -> countItems(player, event.getItem().getItemStack().getType()));
    }
}
