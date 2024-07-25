package uk.co.hopperelec.mc.itemrace;

import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class DepositGUI implements InventoryHolder {
    private final Inventory inventory;

    public DepositGUI(@NotNull ItemRacePlugin plugin) {
        inventory = plugin.getServer().createInventory(this, InventoryType.CHEST, Component.translatable("depositgui.title"));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
