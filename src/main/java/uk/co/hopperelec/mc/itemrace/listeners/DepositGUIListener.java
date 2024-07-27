package uk.co.hopperelec.mc.itemrace.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.DepositGUI;
import uk.co.hopperelec.mc.itemrace.ItemRaceConfig;
import uk.co.hopperelec.mc.itemrace.pointshandling.DepositedItems;

public class DepositGUIListener implements Listener {
    private final ItemRaceConfig config;
    private final DepositedItems depositedItems;

    public DepositGUIListener(@NotNull ItemRaceConfig config, @NotNull DepositedItems depositedItems) {
        this.config = config;
        this.depositedItems = depositedItems;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getHolder() instanceof DepositGUI) {
            final ItemStack itemBeingAdded = switch (event.getAction()) {
                case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
                case HOTBAR_SWAP -> event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                default -> null;
            };
            if (itemBeingAdded != null && !config.awardPointsFor(itemBeingAdded))
                event.setCancelled(true);
        } else if (
                event.getInventory().getHolder() instanceof DepositGUI &&
                event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                !config.awardPointsFor(event.getCurrentItem())
        ) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DepositGUI)) return;
        depositedItems.tryDeposit((OfflinePlayer) event.getPlayer(), event.getInventory());
        if (!event.getInventory().isEmpty()) {
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null) event.getPlayer().getInventory().addItem(item);
            }
            event.getPlayer().sendMessage(Component.translatable("depositgui.returned"));
        }
    }
}
