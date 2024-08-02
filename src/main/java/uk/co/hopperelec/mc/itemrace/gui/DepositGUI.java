package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.pointshandling.DepositedItems;

public class DepositGUI extends ItemRaceGUI {
    private final DepositedItems depositedItems;

    public DepositGUI(@NotNull ItemRacePlugin plugin, @NotNull Player viewer) {
        super(plugin, viewer, 27, Component.translatable("gui.deposit.title"));
        depositedItems = (DepositedItems) plugin.pointsHandler;
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        depositedItems.tryDeposit((OfflinePlayer) event.getPlayer(), event.getInventory());
        if (!event.getInventory().isEmpty()) {
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null) event.getPlayer().getInventory().addItem(item);
            }
            event.getPlayer().sendMessage(Component.translatable("gui.deposit.returned"));
        }
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        final ItemStack itemBeingAdded = switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP -> event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            default -> null;
        };
        if (itemBeingAdded != null && !plugin.config.awardPointsFor(itemBeingAdded))
            event.setCancelled(true);
    }

    @Override
    public void onMoveInto(@NotNull InventoryClickEvent event) {
        if (!plugin.config.awardPointsFor(event.getCurrentItem()))
            event.setCancelled(true);
    }
}
