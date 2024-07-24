package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.listeners.PlayerInventoryListener;

import java.util.Random;

public class AutoDepositor extends DepositedItems implements PlayerInventoryListener {
    private final Random soundPitchRandomizer = new Random();

    public AutoDepositor(ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean listeningFor(@NotNull Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    public void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
        clearAndTryDeposit(player, itemStack);
    }

    public void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event) {
        tryDeposit(player, event.getItem().getItemStack());
        event.setCancelled(true);

        // Recreate part of the event without the player receiving the picked-up item
        event.getItem().remove();
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, .2f, soundPitchRandomizer.nextFloat() * 1.4f + 2f);
    }
}
