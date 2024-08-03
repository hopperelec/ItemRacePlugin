package uk.co.hopperelec.mc.itemrace.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayersGUI extends PaginatedGUI {
    public PlayersGUI(@NotNull ItemRacePlugin plugin, @NotNull Player viewer) {
        super(plugin, viewer, Component.translatable("gui.players.title"));
        final AtomicInteger index = new AtomicInteger();
        setItems(
                plugin.pointsHandler.getEligiblePlayers().stream()
                        .sorted(Comparator.comparing(
                                player -> Objects.requireNonNull(player.getName()),
                                String.CASE_INSENSITIVE_ORDER
                        )).map(player -> createSkull(player, index.getAndIncrement())).toList()
        );
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        super.onClick(event);
        if (event.getSlot() < 45 && event.getClick().isMouseClick()) {
            final ItemStack itemStack = event.getCurrentItem();
            if (itemStack != null && itemStack.getItemMeta() instanceof SkullMeta skullMeta)
                new DepositedItemsGUI(plugin, viewer, Objects.requireNonNull(skullMeta.getOwningPlayer()));
        }
    }

    @Override
    public void onAddEligiblePlayer(@NotNull OfflinePlayer player) {
        addItem(createSkull(player, getNumItems()));
    }

    @Override
    public void onRemoveEligiblePlayer(@NotNull OfflinePlayer player) {
        // Remove existing skull
        final List<ItemStack> newItems = getItems();
        for (int i = 0; i < newItems.size(); i++) {
            if (newItems.get(i).getItemMeta() instanceof SkullMeta skullMeta){
                if (skullMeta.getOwningPlayer() == player) {
                    newItems.set(i, newItems.get(i).withType(Material.BARRIER));
                    break;
                }
            }
        }
        setItems(newItems);
    }

    private @NotNull ItemStack createSkull(@NotNull OfflinePlayer player, int index) {
        final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta itemMeta = (SkullMeta) itemStack.getItemMeta();
        itemMeta.itemName(GlobalTranslator.render(
                Component.translatable("gui.players.skull",
                        Component.text(Objects.requireNonNull(player.getName()))
                ),
                viewer.locale()
        ));
        itemMeta.setOwningPlayer(player); // In case the player's profile is already up to date
        itemStack.setItemMeta(itemMeta);
        player.getPlayerProfile().update().thenAcceptAsync(
                updatedProfile -> {
                    itemMeta.setPlayerProfile(updatedProfile);
                    itemStack.setItemMeta(itemMeta);
                    replaceItem(index, itemStack);
                }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin)
        );
        return itemStack;
    }
}
