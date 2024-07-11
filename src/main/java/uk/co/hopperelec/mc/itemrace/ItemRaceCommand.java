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
    public void onDeposit(@NotNull Player player, @Optional @Name("amount") Integer amount, @Optional @Name("item") Material itemType) {
        boolean itemsRemoved = false;
        if (itemType == null) {
            itemType = player.getInventory().getItemInMainHand().getType();
            if (amount == null) {
                amount = player.getInventory().getItemInMainHand().getAmount();
                player.getInventory().setItemInMainHand(null);
                itemsRemoved = true;
            }
        }
        if (itemType == Material.AIR) {
            player.sendMessage("You cannot deposit air!");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("You cannot deposit 0 items!");
            return;
        }
        if (!itemsRemoved) {
            Map<Integer, ItemStack> itemsNotRemoved = player.getInventory().removeItem(new ItemStack(itemType, amount));
            if (!itemsNotRemoved.isEmpty()) {
                int amountNotRemoved = itemsNotRemoved.get(0).getAmount();
                if (amount == amountNotRemoved) {
                    player.sendMessage("You do not have "+amount+" "+itemType.name()+" to deposit!");
                    return;
                }
                amount -= amountNotRemoved;
            }
        }
        plugin.depositItems(player, itemType, amount);
        player.sendMessage("Deposited "+amount+" "+ItemRaceUtils.getMaterialDisplayName(itemType));
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
            sender.sendMessage("Nobody has deposited any items yet");
        } else {
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
