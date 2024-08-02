package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.List;

public abstract class PaginatedGUI extends ItemRaceGUI {
    public final static int SLOTS = 54;
    public final static int NUM_ITEMS_PER_PAGE = 45;
    public final static int PREVIOUS_BUTTON_SLOT = 48;
    public final static int NEXT_BUTTON_SLOT = 50;

    private int page = 1;
    private int numPages = 1;
    private List<ItemStack> items;

    public PaginatedGUI(@NotNull ItemRacePlugin plugin, @NotNull Player viewer, @NotNull Component title) {
        super(plugin, viewer, SLOTS, title);
    }

    public void setItems(@NotNull List<ItemStack> items) {
        this.items = items;
        numPages = Math.max(1, Math.ceilDiv(items.size(), NUM_ITEMS_PER_PAGE));
        if (page > numPages) page = numPages;
        draw();
    }

    private void draw() {
        // Add paginated items
        final int startOfPage = (page - 1) * NUM_ITEMS_PER_PAGE;
        int pagedIndex = 0;
        while (pagedIndex < NUM_ITEMS_PER_PAGE) {
            final int itemsIndex = startOfPage + pagedIndex;
            getInventory().setItem(pagedIndex++, items.get(itemsIndex));
            if (itemsIndex == items.size() - 1) break;
        }
        while (pagedIndex < NUM_ITEMS_PER_PAGE) {
            getInventory().clear(pagedIndex++);
        }

        // Add buttons
        if (page == 1) getInventory().clear(PREVIOUS_BUTTON_SLOT);
        else getInventory().setItem(PREVIOUS_BUTTON_SLOT, createArrow("gui.button.previous"));
        if (page == numPages) getInventory().clear(NEXT_BUTTON_SLOT);
        else getInventory().setItem(NEXT_BUTTON_SLOT, createArrow("gui.button.next"));
    }

    protected @NotNull ItemStack createArrow(@NotNull String nameKey) {
        final ItemStack itemStack = new ItemStack(Material.ARROW);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(Component.translatable(nameKey), viewer.locale()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        if (event.getClick().isMouseClick()) {
            if (event.getSlot() == PREVIOUS_BUTTON_SLOT) {
                if (page != 1) {
                    page--;
                    draw();
                }
            } else if (event.getSlot() == NEXT_BUTTON_SLOT) {
                if (page != numPages) {
                    page++;
                    draw();
                }
            }
        }
        event.setCancelled(true);
    }

    @Override
    public void onMoveInto(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }
}