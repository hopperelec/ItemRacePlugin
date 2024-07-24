package uk.co.hopperelec.mc.itemrace;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class ItemRaceInventoryProvider implements InventoryProvider {
    private final ItemRacePlugin plugin;
    private final OfflinePlayer player;
    private SmartInventory inventory;

    @NotNull
    private static ItemStack createArrow(@NotNull String nameKey, @NotNull Locale locale) {
        final ItemStack itemStack = new ItemStack(Material.ARROW);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(Component.translatable(nameKey), locale));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemRaceInventoryProvider(@NotNull ItemRacePlugin plugin, @NotNull OfflinePlayer player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void setInventory(@NotNull SmartInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void init(@NotNull Player viewer, @NotNull InventoryContents contents) {
        final Pagination pagination = contents.pagination();
        pagination.setItems(
            plugin.getItems(player).entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Integer::intValue).reversed()))
                    .map(entry -> {
                        final ItemStack itemStack = new ItemStack(entry.getKey(), entry.getValue());
                        final ItemMeta itemMeta = itemStack.getItemMeta();
                        itemMeta.itemName(
                                GlobalTranslator.render(
                                        Component.translatable(
                                                "inventory.itemname",
                                                Component.translatable(itemStack.getType().translationKey()),
                                                Component.text(entry.getValue())
                                        ), viewer.locale()
                                )
                        );
                        itemStack.setItemMeta(itemMeta);
                        return ClickableItem.empty(itemStack);
                    })
                    .toArray(ClickableItem[]::new)
        );
        pagination.setItemsPerPage(45);
        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));
        if (!pagination.isFirst())
            contents.set(5, 3, ClickableItem.of(
                    createArrow("inventory.previous", viewer.locale()),
                    e -> inventory.open(viewer, pagination.previous().getPage()))
            );
        if (!pagination.isLast())
            contents.set(5, 5, ClickableItem.of(
                    createArrow("inventory.next", viewer.locale()),
                    e -> inventory.open(viewer, pagination.next().getPage()))
            );
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {}
}
