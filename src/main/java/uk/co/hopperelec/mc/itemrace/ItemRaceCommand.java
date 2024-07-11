package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import fr.minuskube.inv.SmartInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import static org.bukkit.Bukkit.getScoreboardManager;

@CommandAlias("itemrace")
public class ItemRaceCommand extends BaseCommand {
    private final ItemRacePlugin plugin;

    public ItemRaceCommand(@NotNull ItemRacePlugin plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    public void doHelp(@NotNull CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reset")
    @CommandCompletion("@player")
    public void onReset(@NotNull CommandSender sender, @Optional @Name("player") OfflinePlayer player) {
        if (player == null) {
            plugin.resetDepositedItems();
            sender.sendMessage("Cleared all deposited items");
        } else {
            plugin.resetDepositedItems(player);
            sender.sendMessage(
                    Component.text("Cleared all items deposited by ")
                            .append(Component.text(Objects.requireNonNull(player.getName())))
            );
        }
    }

    @Subcommand("deposit|dep")
    @CommandCompletion("all|inventory @itemType")
    public void onDeposit(@NotNull Player player, @Optional @Name("amount") String amountStr, @Optional @Name("item") Material itemType) {
        if (Objects.equals(amountStr, "all")) {
            if (itemType == null) itemType = player.getInventory().getItemInMainHand().getType();
            if (itemType.isAir()) {
                player.sendMessage("You cannot deposit air!");
                return;
            }
            final int amountInt = player.getInventory().all(itemType).values().stream()
                    .map(ItemStack::getAmount)
                    .reduce(0, Integer::sum);
            if (amountInt == 0) {
                player.sendMessage("You do not have any "+ItemRaceUtils.getMaterialDisplayName(itemType)+" to deposit!");
                return;
            }
            player.getInventory().remove(itemType);
            plugin.depositItems(player, itemType, amountInt);
            player.sendMessage("Deposited all ("+amountInt+") "+ItemRaceUtils.getMaterialDisplayName(itemType)+" in your inventory");
        } else if (Objects.equals(amountStr, "inventory")) {
            if (itemType != null) {
                player.sendMessage("You cannot specify an item type when depositing your inventory");
                return;
            }
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (item != null && !item.getType().isAir()) {
                    plugin.depositItems(player, item.getType(), item.getAmount());
                    item.setAmount(0);
                }
            }
            player.sendMessage("Deposited all items in your inventory");
        } else {
            int amountInt = -1;
            boolean removedItems = false;
            if (itemType == null) {
                itemType = player.getInventory().getItemInMainHand().getType();
                if (amountStr == null) {
                    amountInt = player.getInventory().getItemInMainHand().getAmount();
                    player.getInventory().setItemInMainHand(null);
                    removedItems = true;
                }
            }
            if (itemType.isAir()) {
                player.sendMessage("You cannot deposit air!");
                return;
            }
            if (amountInt == -1) {
                try {
                    amountInt = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    player.sendMessage("Amount must be either 'all', 'inventory' or a number, not "+amountStr);
                    return;
                }
            }
            if (amountInt <= 0) {
                player.sendMessage("You cannot deposit 0 items!");
                return;
            }
            if (!removedItems) {
                Map<Integer, ItemStack> itemsNotRemoved = player.getInventory().removeItem(new ItemStack(itemType, amountInt));
                if (!itemsNotRemoved.isEmpty()) {
                    int amountNotRemoved = itemsNotRemoved.get(0).getAmount();
                    if (amountInt == amountNotRemoved) {
                        player.sendMessage("You do not have "+amountInt+" "+itemType.name()+" to deposit!");
                        return;
                    }
                    amountInt -= amountNotRemoved;
                }
            }
            plugin.depositItems(player, itemType, amountInt);
            player.sendMessage("Deposited "+amountInt+" "+ItemRaceUtils.getMaterialDisplayName(itemType));
        }
    }

    @Subcommand("inventory|inv")
    public void onInventory(@NotNull Player sender, @Optional @Name("player") OfflinePlayer player) {
        if (player == null) player = sender;
        final ItemRaceInventoryProvider inventoryProvider = new ItemRaceInventoryProvider(plugin, player);
        final SmartInventory inventory = SmartInventory.builder()
                .provider(inventoryProvider)
                .manager(plugin.inventoryManager)
                .title(player.getName()+"'s ItemRace inventory")
                .build();
        inventoryProvider.setInventory(inventory);
        inventory.open(sender);
    }

    @Subcommand("leaderboard|lb")
    public void onLeaderboard(@NotNull CommandSender sender) {
        sender.sendMessage("=== ItemRace Leaderboard ===");
        if (plugin.hasDepositedItems()) {
            final Map<OfflinePlayer, Integer> scores = plugin.calculateScores();
            int position = 1;
            for (
                    Map.Entry<OfflinePlayer, Integer> score :
                    scores.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(10).toList()
            ) {
                sender.sendMessage(
                        Component.text(position++)
                                .append(Component.text(". "))
                                .append(Component.text(Objects.requireNonNull(score.getKey().getName())))
                                .append(Component.text(" - "))
                                .append(Component.text(score.getValue()))
                );
            }
            if (sender instanceof Player) {
                sender.sendMessage("");
                sender.sendMessage("You are position "+scores.get(sender));
            }
        } else {
            sender.sendMessage("Nobody has deposited any items yet");
        }
        sender.sendMessage("==========================");
    }

    @Subcommand("togglescoreboard|tsb")
    public void onToggleScoreboard(@NotNull Player player) {
        if (player.getScoreboard() == plugin.scoreboard) {
            player.setScoreboard(getScoreboardManager().getMainScoreboard());
        } else {
            player.setScoreboard(plugin.scoreboard);
        }
    }
}
