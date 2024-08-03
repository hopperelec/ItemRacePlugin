package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.gui.ItemRaceGUI;

import java.util.HashSet;
import java.util.Set;

public class GUIListener implements Listener {
    private final ItemRacePlugin plugin;
    private final Set<ItemRaceGUI> guis = new HashSet<>();

    public GUIListener(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerGUI(@NotNull ItemRaceGUI gui) {
        if (guis.isEmpty()) plugin.getServer().getPluginManager().registerEvents(this, plugin);
        guis.add(gui);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ItemRaceGUI itemRaceGUI) {
            if (event.getInventory().getViewers().size() <= 1) {
                guis.remove(itemRaceGUI);
                if (guis.isEmpty()) {
                    InventoryClickEvent.getHandlerList().unregister(this);
                    InventoryCloseEvent.getHandlerList().unregister(this);
                }
            }
            itemRaceGUI.onClose(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() != null) {
            if (event.getClickedInventory().getHolder() instanceof ItemRaceGUI itemRaceGUI)
                itemRaceGUI.onClick(event);
            else if (
                    event.getInventory().getHolder() instanceof ItemRaceGUI itemRaceGUI &&
                    event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
            )
                itemRaceGUI.onMoveInto(event);
        }
    }

    public void refreshDepositedItems(@NotNull OfflinePlayer player) {
        guis.forEach(gui -> gui.onRefreshDepositedItems(player));
    }

    public void addEligiblePlayer(@NotNull OfflinePlayer player) {
        guis.forEach(gui -> gui.onAddEligiblePlayer(player));
    }

    public void removeEligiblePlayer(@NotNull OfflinePlayer player) {
        guis.forEach(gui -> gui.onRemoveEligiblePlayer(player));
    }
}
