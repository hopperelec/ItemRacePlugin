package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

public abstract class ItemRaceGUI implements InventoryHolder {
    protected final @NotNull ItemRacePlugin plugin;
    private final @NotNull Inventory inventory;
    protected final @NotNull Player viewer;

    public ItemRaceGUI(@NotNull ItemRacePlugin plugin, @NotNull Player viewer, int slots, @NotNull Component title) {
        this.plugin = plugin;
        this.viewer = viewer;
        inventory = plugin.getServer().createInventory(this, slots, title);
        viewer.openInventory(inventory);
        plugin.guiListener.registerGUI(this);
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void onClose(@NotNull InventoryCloseEvent event) {}
    public void onClick(@NotNull InventoryClickEvent event) {}
    public void onMoveInto(@NotNull InventoryClickEvent event) {}
}
