package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.DepositGUI;
import uk.co.hopperelec.mc.itemrace.pointshandling.DepositedItems;

public class DepositGUIListener implements Listener {
    private final DepositedItems depositedItems;

    public DepositGUIListener(@NotNull DepositedItems depositedItems) {
        this.depositedItems = depositedItems;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (
                event.getClickedInventory() != null &&
                event.getClickedInventory().getHolder() instanceof DepositGUI &&
                !depositedItems.canAwardPointsFor(event.getCursor())
        ) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DepositGUI) {
            depositedItems.deposit((OfflinePlayer) event.getPlayer(), event.getInventory());
        }
    }
}
