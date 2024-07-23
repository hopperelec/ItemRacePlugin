package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.Random;

public class AutoDepositListener implements Listener {
    private final ItemRacePlugin plugin;
    private final Random soundPitchRandomizer = new Random();

    public AutoDepositListener(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean canAutoDeposit(@NotNull Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    @EventHandler
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && canAutoDeposit(player)) {
            plugin.tryDeposit(player, event.getItem().getItemStack());
            event.setCancelled(true);

            // Recreate part of the event without the player receiving the picked-up item
            event.getItem().remove();
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, .2f, soundPitchRandomizer.nextFloat() * 1.4f + 2f);
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (!canAutoDeposit(event.getPlayer())) return;
        for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
            if (itemStack != null)
                plugin.clearAndTryDeposit(event.getPlayer(), itemStack);
        }
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player && canAutoDeposit(player))) return;
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            final ItemStack movedItem = event.getCurrentItem();
            if (movedItem != null)
                plugin.clearAndTryDeposit(player, movedItem);
        } else if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            final ItemStack itemMovedToHotbar = event.getCurrentItem();
            if (itemMovedToHotbar != null)
                plugin.clearAndTryDeposit(player, itemMovedToHotbar);
        } else if (event.getClickedInventory() instanceof PlayerInventory) {
            if (
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME ||
                    event.getAction() == InventoryAction.PLACE_ALL
            ) plugin.clearAndTryDeposit(player, event.getCursor());
        }
    }
}
