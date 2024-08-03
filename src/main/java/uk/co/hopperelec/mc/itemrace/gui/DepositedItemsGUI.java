package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.*;

public class DepositedItemsGUI extends PaginatedGUI {
    public final @NotNull OfflinePlayer player;
    public final static int PLAYERS_BUTTON_SLOT = 45;

    public DepositedItemsGUI(
            @NotNull ItemRacePlugin plugin,
            @NotNull Player viewer,
            @NotNull OfflinePlayer player
    ) {
        super(plugin, viewer, Component.translatable("gui.deposited_items.title",
                Component.text(Objects.requireNonNull(player.getName()))
        ));
        this.player = player;

        final boolean canViewOtherInventories = viewer.hasPermission("itemrace.inventory");
        if (!canViewOtherInventories && player != viewer)
            throw new IllegalStateException("Player " + viewer.getName() + " does not have permission to view other player's inventories");
        if (canViewOtherInventories)
            getInventory().setItem(PLAYERS_BUTTON_SLOT, createArrow("gui.deposited_items.players_button"));
        setItems();
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        super.onClick(event);
        if (
                event.getSlot() == PLAYERS_BUTTON_SLOT && event.getClick().isMouseClick() &&
                event.getWhoClicked().hasPermission("itemrace.inventory")
        ) new PlayersGUI(plugin, viewer);
    }

    @Override
    public void onRefreshDepositedItems(@NotNull OfflinePlayer player) {
        if (this.player == player) setItems();
    }

    private void setItems() {
        final List<ItemStack> items = new ArrayList<>();
        plugin.pointsHandler.getItems(player).entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> {
                    if (plugin.config.splitItemsIntoStacks)
                        for (
                                int stackSize = entry.getValue();
                                stackSize > 0 ;
                                stackSize -= entry.getKey().getMaxStackSize()
                        ) items.add(createDepositedItem(entry.getKey(), stackSize, entry.getValue(), false));
                    else items.add(createDepositedItem(entry.getKey(), entry.getValue(), entry.getValue(), true));
                });
        setItems(items);
    }

    private @NotNull ItemStack createDepositedItem(@NotNull Material material, int stackSize, int amountShown, boolean setMaxStackSize) {
        final ItemStack itemStack = new ItemStack(material, stackSize);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (setMaxStackSize) {
            itemMeta.setMaxStackSize(99);
            if (stackSize > 99) itemMeta.setEnchantmentGlintOverride(true);
        }
        itemMeta.itemName(
                GlobalTranslator.render(
                        Component.translatable("gui.deposited_items.item_name",
                                Component.translatable(itemStack.getType().translationKey()),
                                Component.text(amountShown)
                        ), viewer.locale()
                )
        );
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
