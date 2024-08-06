package uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.listeners.PlayerInventoryListener;
import uk.co.hopperelec.mc.itemrace.pointshandling.DepositedItems;

import java.util.Random;

public abstract class AutoDepositor extends DepositedItems implements PlayerInventoryListener {
    private final Random soundPitchRandomizer = new Random();

    public AutoDepositor(@NotNull ItemRacePlugin plugin) {
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

    // Returns the amount deposited
    protected abstract int autoDeposit(@NotNull Player player, @NotNull ItemStack itemStack);

    public void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
        itemStack.setAmount(itemStack.getAmount() - autoDeposit(player, itemStack));
    }

    public void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event) {
        final ItemStack itemStack = event.getItem().getItemStack();
        final int amountDeposited = autoDeposit(player, itemStack);
        if (amountDeposited == itemStack.getAmount()) {
            event.setCancelled(true);
            // Recreate part of the event without the player receiving the picked-up item
            event.getItem().remove();
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, .2f, soundPitchRandomizer.nextFloat() * 1.4f + 2f);
        } else if (amountDeposited != 0) {
            itemStack.setAmount(itemStack.getAmount() - amountDeposited);
            event.getItem().setItemStack(itemStack);
        }
    }
}
