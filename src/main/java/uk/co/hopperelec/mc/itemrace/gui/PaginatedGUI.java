package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PaginatedGUI extends ItemRaceGUI {
    public final static int SLOTS = 54;
    public final static int NUM_ITEMS_PER_PAGE = 45;
    public final static int PREVIOUS_BUTTON_SLOT = 48;
    public final static int NEXT_BUTTON_SLOT = 50;
    public final static int SEARCH_BUTTON_SLOT = 53;
    private @Nullable String searchQuery;
    public final @NotNull Component searchTitle;

    private int page = 1;
    private int numPages = 1;
    private List<ItemStack> unfilteredItems;
    private List<ItemStack> filteredItems;

    public PaginatedGUI(@NotNull ItemRacePlugin plugin, @NotNull Player viewer, @NotNull Component title, @NotNull Component searchTitle) {
        super(plugin, viewer, SLOTS, title);
        this.searchTitle = searchTitle;
    }

    public int getCurrentPage() {
        return page;
    }

    public int getNumPages() {
        return numPages;
    }

    public @NotNull List<ItemStack> getItems() {
        return new ArrayList<>(unfilteredItems);
    }

    public @NotNull List<ItemStack> getItemsShown() {
        return new ArrayList<>(filteredItems);
    }

    public int getTotalNumItems() {
        return unfilteredItems.size();
    }

    public int getNumItemsShown() {
        return filteredItems.size();
    }

    private void refreshItems() {
        filteredItems = searchQuery == null ? unfilteredItems : unfilteredItems.stream()
                .filter(itemStack -> filter(itemStack, searchQuery))
                .toList();
        numPages = Math.max(1, Math.ceilDiv(filteredItems.size(), NUM_ITEMS_PER_PAGE));
        if (page > numPages) page = numPages;
        draw();
    }

    public void setItems(@NotNull Collection<ItemStack> items) {
        this.unfilteredItems = new ArrayList<>(items);
        refreshItems();
    }

    public void addItem(@NotNull ItemStack item) {
        unfilteredItems.add(item);
        if (searchQuery == null || filter(item, searchQuery)) {
            filteredItems.add(item);
            if (filteredItems.size() != 1 && numPages % NUM_ITEMS_PER_PAGE == 1) {
                if (page == numPages++) drawNextArrow();
            } else if (page == numPages) getInventory().addItem(item);
        }
    }

    public void replaceItem(int index, @NotNull ItemStack item) {
        final ItemStack oldItem = unfilteredItems.set(index, item);
        for (int j = 0; j < filteredItems.size(); j++) {
            if (filteredItems.get(j).equals(oldItem)) {
                filteredItems.set(j, item);
                if (j >= (page - 1) * NUM_ITEMS_PER_PAGE && j < page * NUM_ITEMS_PER_PAGE)
                    getInventory().setItem(j % NUM_ITEMS_PER_PAGE, item);
                return;
            }
        }
    }

    public void draw() {
        // Add paginated items
        final int startOfPage = (page - 1) * NUM_ITEMS_PER_PAGE;
        int pagedIndex = 0;
        int itemsIndex = startOfPage + pagedIndex;
        while (pagedIndex < NUM_ITEMS_PER_PAGE && itemsIndex < filteredItems.size()) {
            getInventory().setItem(pagedIndex++, filteredItems.get(itemsIndex++));
        }
        while (pagedIndex < NUM_ITEMS_PER_PAGE) {
            getInventory().clear(pagedIndex++);
        }

        // Add buttons
        if (page == 1) getInventory().clear(PREVIOUS_BUTTON_SLOT);
        else getInventory().setItem(PREVIOUS_BUTTON_SLOT, createArrow("gui.button.previous"));
        if (page == numPages) getInventory().clear(NEXT_BUTTON_SLOT);
        else drawNextArrow();

        final ItemStack searchButton = new ItemStack(Material.ARROW);
        final ItemMeta searchButtonMeta = searchButton.getItemMeta();
        searchButtonMeta.itemName(
                GlobalTranslator.render(
                        Component.translatable("gui.button.search"),
                        viewer.locale()
                )
        );
        if (searchQuery != null)
            searchButtonMeta.lore(List.of(
                    GlobalTranslator.render(
                            Component.translatable("gui.button.search.lore", Component.text(searchQuery)),
                            viewer.locale()
                    )
            ));
        searchButton.setItemMeta(searchButtonMeta);
        getInventory().setItem(SEARCH_BUTTON_SLOT, searchButton);
    }

    private void drawNextArrow() {
        getInventory().setItem(NEXT_BUTTON_SLOT, createArrow("gui.button.next"));
    }

    protected @NotNull ItemStack createArrow(@NotNull String nameKey) {
        final ItemStack itemStack = new ItemStack(Material.ARROW);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(Component.translatable(nameKey), viewer.locale()));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public @Nullable String getSearchQuery() {
        return searchQuery;
    }

    public void search(@Nullable String query) {
        page = 1;
        searchQuery = query == null || query.isEmpty() ? null : query.toLowerCase();
        refreshItems();
    }

    public abstract boolean filter(@NotNull ItemStack itemStack, @NotNull String query);

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
            } else if (event.getSlot() == SEARCH_BUTTON_SLOT) {
                final ItemStack barrier = new ItemStack(Material.BARRIER);
                final ItemMeta barrierMeta = barrier.getItemMeta();
                barrierMeta.displayName(Component.empty());
                barrier.setItemMeta(barrierMeta);
                new AnvilGUI.Builder()
                        .plugin(plugin)
                        .jsonTitle(GsonComponentSerializer.gson().serialize(searchTitle))
                        .itemLeft(barrier)
                        .itemRight(barrier)
                        .itemOutput(barrier)
                        .onClick((slot, stateSnapshot) -> List.of())
                        .onClose(stateSnapshot -> {
                            search(stateSnapshot.getText());
                            // Not sure why but, if I don't delay this, then GUIListener isn't able to register itself
                            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this::show);
                        })
                        .open(viewer);
            }
        }
        event.setCancelled(true);
    }

    @Override
    public void onMoveInto(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
