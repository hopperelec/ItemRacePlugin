package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
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
        this.inventory = plugin.getServer().createInventory(this, slots, title);
        show();
    }

    public void show() {
        // Bukkit warns against opening an inventory from InventoryClickEvent without using `runTask`.
        // (see https://github.com/Bukkit/Bukkit/blob/f210234e59275330f83b994e199c76f6abd41ee7/src/main/java/org/bukkit/event/inventory/InventoryClickEvent.java#L17-L33)
        // `runTask` also seems necessary for re-opening a GUI from the AnvilGUI's onClose.
        // (see https://github.com/WesJD/AnvilGUI/issues/348)
        // Rather than using `runTask` when constructing each GUI, I've just used it here to avoid future headache.
        plugin.getServer().getScheduler().runTask(plugin, () -> viewer.openInventory(inventory));
        plugin.guiListener.registerGUI(this);
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void onClose(@NotNull InventoryCloseEvent event) {}
    public void onClick(@NotNull InventoryClickEvent event) {}
    public void onMoveInto(@NotNull InventoryClickEvent event) {}
    public void onRefreshDepositedItems(@NotNull OfflinePlayer player) {}
    public void onAddEligiblePlayer(@NotNull OfflinePlayer player) {}
    public void onRemoveEligiblePlayer(@NotNull OfflinePlayer player) {}
}
