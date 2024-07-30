package uk.co.hopperelec.mc.itemrace;

import com.destroystokyo.paper.profile.PlayerProfile;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.serializeTranslatable;

public class ItemRaceInventoryGUI implements InventoryProvider {
    private final @NotNull ItemRacePlugin plugin;
    public final @NotNull Player viewer;
    public @Nullable OfflinePlayer player;
    private SmartInventory inventory;

    public ItemRaceInventoryGUI(
            @NotNull ItemRacePlugin plugin,
            @NotNull Player viewer,
            @NotNull OfflinePlayer player
    ) {
        this.plugin = plugin;
        this.viewer = viewer;
        setPlayer(player);
    }

    private @NotNull ClickableItem createArrow(@NotNull String nameKey, @NotNull Consumer<InventoryClickEvent> action) {
        final ItemStack itemStack = new ItemStack(Material.ARROW);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(Component.translatable(nameKey), viewer.locale()));
        itemStack.setItemMeta(itemMeta);
        return ClickableItem.of(itemStack, action);
    }

    private @NotNull ClickableItem createDepositedItem(@NotNull Material material, int stackSize, int amountShown) {
        final ItemStack itemStack = new ItemStack(material, stackSize);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(
                GlobalTranslator.render(
                        Component.translatable("inventorygui.inventory.itemname",
                                Component.translatable(itemStack.getType().translationKey()),
                                Component.text(amountShown)
                        ), viewer.locale()
                )
        );
        itemStack.setItemMeta(itemMeta);
        return ClickableItem.empty(itemStack);
    }

    private void setPlayer(@Nullable OfflinePlayer player) {
        this.player = player;
        inventory = SmartInventory.builder()
                .provider(this)
                .manager(plugin.inventoryManager)
                .title(
                        // SmartInventories doesn't support Adventure components
                        serializeTranslatable(
                                player == null ?
                                        Component.translatable("inventorygui.mainmenu.title") :
                                        Component.translatable("inventorygui.inventory.title", Component.text(Objects.requireNonNull(player.getName()))),
                                viewer.locale())
                )
                .build();
        inventory.open(viewer);
    }

    @Override
    public void init(@NotNull Player viewer, @NotNull InventoryContents contents) {
        final Pagination pagination = contents.pagination();
        pagination.setItemsPerPage(45);

        final boolean canViewOtherInventories = viewer.hasPermission("itemrace.inventory");
        if (!canViewOtherInventories && player != viewer)
            throw new IllegalStateException("Player " + viewer.getName() + " does not have permission to view other player's inventories");

        if (player == null) {
            // Main menu
            pagination.setItems(
                    plugin.pointsHandler.getEligiblePlayers().stream()
                            .sorted(Comparator.comparing(
                                    player -> Objects.requireNonNull(player.getName()),
                                    String.CASE_INSENSITIVE_ORDER
                            )).map(player -> {
                                final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
                                final SkullMeta itemMeta = (SkullMeta) itemStack.getItemMeta();
                                final PlayerProfile playerProfile = player.getPlayerProfile();
                                playerProfile.complete(); // Blocking call, TODO: complete profile async
                                itemMeta.itemName(GlobalTranslator.render(
                                        Component.translatable("inventorygui.mainmenu.skull",
                                            Component.text(Objects.requireNonNull(playerProfile.getName()))
                                        ),
                                        viewer.locale()
                                ));
                                itemMeta.setPlayerProfile(playerProfile);
                                itemStack.setItemMeta(itemMeta);
                                return ClickableItem.of(itemStack, e -> setPlayer(player));
                            })
                            .toArray(ClickableItem[]::new)
            );
        } else {
            // Inventory menu
            final List<ClickableItem> items = new ArrayList<>();
            plugin.pointsHandler.getItems(player).entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> {
                        if (plugin.config.splitItemsIntoStacks)
                            for (
                                    int stackSize = entry.getValue();
                                    stackSize > 0 ;
                                    stackSize -= entry.getKey().getMaxStackSize()
                            ) items.add(createDepositedItem(entry.getKey(), stackSize, entry.getValue()));
                        else items.add(createDepositedItem(entry.getKey(), entry.getValue(), entry.getValue()));
                    });
            pagination.setItems(items.toArray(ClickableItem[]::new));
            if (canViewOtherInventories)
                contents.set(5, 0, createArrow("inventorygui.button.mainmenu", e -> setPlayer(null)));
        }

        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));
        if (!pagination.isFirst())
            contents.set(5, 3, createArrow("inventorygui.button.previous",
                    e -> inventory.open(viewer, pagination.previous().getPage()))
            );
        if (!pagination.isLast())
            contents.set(5, 5, createArrow("inventorygui.button.next",
                    e -> inventory.open(viewer, pagination.next().getPage()))
            );
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {}
}
