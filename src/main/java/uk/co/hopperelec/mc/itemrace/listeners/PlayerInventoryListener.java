package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public interface PlayerInventoryListener extends Listener {
    default boolean listeningFor(@NotNull Player player) { return true; }
    void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack);
    void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && listeningFor(player)) {
            onPlayerPickupItem(player, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    default void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (!listeningFor(event.getPlayer())) return;
        for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
            if (itemStack != null)
                onMoveToInventory(event.getPlayer(), itemStack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    default void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player && listeningFor(player))) return;
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            final ItemStack movedItem = event.getCurrentItem();
            if (movedItem != null)
                onMoveToInventory(player, movedItem);
        } else if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            final ItemStack itemMovedToHotbar = event.getCurrentItem();
            if (itemMovedToHotbar != null)
                onMoveToInventory(player, itemMovedToHotbar);
        } else if (event.getClickedInventory() instanceof PlayerInventory) {
            if (
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME ||
                    event.getAction() == InventoryAction.PLACE_ALL
            ) onMoveToInventory(player, event.getCursor());
        }
    }
}
