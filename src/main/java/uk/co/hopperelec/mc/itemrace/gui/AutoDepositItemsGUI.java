package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.pointshandling.ManualDepositHandler;

import java.util.Arrays;
import java.util.Set;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.itemNameContains;

public class AutoDepositItemsGUI extends PaginatedGUI {
    private final @NotNull ManualDepositHandler manualDepositHandler;

    public AutoDepositItemsGUI(
            @NotNull ItemRacePlugin plugin,
            @NotNull Player viewer
    ) {
        super(
                plugin, viewer,
                Component.translatable("gui.auto_deposit_items.title"),
                Component.translatable("gui.items.search.title")
        );
        if (!(plugin.pointsHandler instanceof ManualDepositHandler))
            throw new IllegalStateException("Auto-deposit items GUI is only available when using a manual points award mode");
        this.manualDepositHandler = (ManualDepositHandler) plugin.pointsHandler;

        final Set<Material> autoDepositItems = manualDepositHandler.getAutoDepositItems(viewer);
        setItems(
                Arrays.stream(Material.values())
                        .filter(material -> material.isItem() && !material.isLegacy() && !material.isAir())
                        .map(material -> {
                            final ItemStack itemStack = new ItemStack(material);
                            if (autoDepositItems.contains(material)) {
                                final ItemMeta itemMeta = itemStack.getItemMeta();
                                itemMeta.setEnchantmentGlintOverride(true);
                                itemStack.setItemMeta(itemMeta);
                            }
                            return itemStack;
                        }).toList()
        );
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        super.onClick(event);
        if (event.getSlot() < NUM_ITEMS_PER_PAGE && event.getClick().isMouseClick()) {
            final ItemStack itemStack = event.getCurrentItem();
            if (itemStack != null) {
                final ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setEnchantmentGlintOverride(
                        manualDepositHandler.toggleAutoDepositItem(viewer, itemStack.getType())
                );
                itemStack.setItemMeta(itemMeta);
            }
        }
    }

    @Override
    public boolean filter(@NotNull ItemStack itemStack, @NotNull String query) {
        return itemNameContains(itemStack.getType(), query, viewer.locale());
    }
}
