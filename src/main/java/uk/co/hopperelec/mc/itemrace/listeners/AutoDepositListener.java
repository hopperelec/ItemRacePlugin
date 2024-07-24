package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.Random;

public class AutoDepositListener extends PlayerInventoryListener {
    private final ItemRacePlugin plugin;
    private final Random soundPitchRandomizer = new Random();

    public AutoDepositListener(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected boolean listeningFor(@NotNull Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    @Override
    protected void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
        plugin.clearAndTryDeposit(player, itemStack);
    }

    @Override
    protected void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event) {
        plugin.tryDeposit(player, event.getItem().getItemStack());
        event.setCancelled(true);

        // Recreate part of the event without the player receiving the picked-up item
        event.getItem().remove();
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, .2f, soundPitchRandomizer.nextFloat() * 1.4f + 2f);
    }
}
