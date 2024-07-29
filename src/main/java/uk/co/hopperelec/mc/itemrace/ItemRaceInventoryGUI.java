package uk.co.hopperelec.mc.itemrace;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryManager;
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
import uk.co.hopperelec.mc.itemrace.pointshandling.PointsHandler;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.serializeTranslatable;

public class ItemRaceInventoryGUI implements InventoryProvider {
    private final InventoryManager inventoryManager;
    private final PointsHandler pointsHandler;
    public final @NotNull Player viewer;
    public @Nullable OfflinePlayer player;
    private SmartInventory inventory;

    public ItemRaceInventoryGUI(
            @NotNull InventoryManager inventoryManager,
            @NotNull PointsHandler pointsHandler,
            @NotNull Player viewer,
            @NotNull OfflinePlayer player
    ) {
        this.inventoryManager = inventoryManager;
        this.pointsHandler = pointsHandler;
        this.viewer = viewer;
        setPlayer(player);
    }

    @NotNull
    private ClickableItem createArrow(@NotNull String nameKey, @NotNull Consumer<InventoryClickEvent> action) {
        final ItemStack itemStack = new ItemStack(Material.ARROW);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(Component.translatable(nameKey), viewer.locale()));
        itemStack.setItemMeta(itemMeta);
        return ClickableItem.of(itemStack, action);
    }

    private void setPlayer(@Nullable OfflinePlayer player) {
        this.player = player;
        inventory = SmartInventory.builder()
                .provider(this)
                .manager(inventoryManager)
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
        if (player == null) {
            // Main menu
            pagination.setItems(
                    pointsHandler.getEligiblePlayers().stream()
                            .map(player -> {
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
            pagination.setItems(
                    pointsHandler.getItems(player).entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Integer::intValue).reversed()))
                            .map(entry -> {
                                final ItemStack itemStack = new ItemStack(entry.getKey(), entry.getValue());
                                final ItemMeta itemMeta = itemStack.getItemMeta();
                                itemMeta.itemName(
                                        GlobalTranslator.render(
                                                Component.translatable("inventorygui.inventory.itemname",
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
