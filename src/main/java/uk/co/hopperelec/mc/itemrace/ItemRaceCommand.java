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
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.serializeTranslatable;

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
    @CommandCompletion("@players")
    public void onReset(@NotNull CommandSender sender, @Optional @Name("player") OfflinePlayer player) {
        if (player == null) {
            plugin.resetDepositedItemsInventory();
            sender.sendMessage(Component.translatable("command.reset.all"));
        } else {
            plugin.resetDepositedItemsInventory(player);
            if (player.getName() != null) {
                sender.sendMessage(Component.translatable(
                        "command.reset.player",
                        Component.text(player.getName())
                ));
            }
        }
    }

    @Subcommand("deposit|dep")
    @CommandCompletion("all|inventory @itemType")
    public void onDeposit(
        @NotNull Player player,
        @Optional @Name("amount") String amountStr, @Optional @Name("item") ItemType itemType
    ) {
        if (plugin.config.pointsAwardMode() != PointsAwardMode.MANUAL_DEPOSIT) {
            player.sendMessage(Component.translatable("command.deposit.disabled"));
            return;
        }
        final Material material;
        if (Objects.equals(amountStr, "all")) {
            material = itemType == null ? player.getInventory().getItemInMainHand().getType() : itemType.material();
            if (!plugin.canDeposit(material)) {
                player.sendMessage(
                        Component.translatable(
                                "command.deposit.itemtype.denied",
                                Component.translatable(material)
                        )
                );
                return;
            }
            int amountInt = 0;
            for (ItemStack item : player.getInventory().all(material).values()) {
                if (!plugin.config.allowDamagedTools() && isDamaged(item)) continue;
                amountInt += item.getAmount();
                item.setAmount(0);
            }
            if (amountInt == 0) {
                player.sendMessage(Component.translatable(
                        "command.deposit.all.none",
                        Component.translatable(material.translationKey())
                ));
                return;
            }
            plugin.depositItems(player, material, amountInt);
            player.sendMessage(
                    Component.translatable(
                            "command.deposit.all.success",
                            Component.text(amountInt),
                            Component.translatable(material.translationKey())
                    )
            );

        } else if (Objects.equals(amountStr, "inventory")) {
            if (itemType != null) {
                player.sendMessage(Component.translatable("command.deposit.inventory.itemtype"));
                return;
            }
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (item == null || !plugin.canDeposit(item.getType())) continue;
                if (!plugin.config.allowDamagedTools() && isDamaged(item)) continue;
                plugin.depositItems(player, item.getType(), item.getAmount());
                item.setAmount(0);
            }
            player.sendMessage(Component.translatable("command.deposit.inventory.success"));

        } else {
            int amountInt = -1;
            if (itemType == null) {
                if (!plugin.config.allowDamagedTools() && isDamaged(player.getInventory().getItemInMainHand())) {
                    player.sendMessage(Component.translatable("command.deposit.specific.damaged"));
                    return;
                }
                material = player.getInventory().getItemInMainHand().getType();
                if (amountStr == null) {
                    amountInt = player.getInventory().getItemInMainHand().getAmount();
                }
            } else {
                material = itemType.material();
            }
            if (!plugin.canDeposit(material)) {
                player.sendMessage(
                        Component.translatable(
                                "command.deposit.itemtype.denied",
                                Component.translatable(material)
                        )
                );
                return;
            }
            if (amountInt == -1) {
                try {
                    amountInt = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.translatable(
                            "command.deposit.amount.invalid",
                            Component.text(amountInt))
                    );
                    return;
                }
            }
            if (amountInt <= 0) {
                player.sendMessage(Component.translatable("command.deposit.amount.zero"));
                return;
            }
            if (itemType == null && amountStr == null) {
                player.getInventory().setItemInMainHand(null);
            } else {
                int amountRemoved = 0;
                int amountToRemove = amountInt;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item == null || item.getType() != material) continue;
                    if (!plugin.config.allowDamagedTools() && isDamaged(item)) continue;
                    if (item.getAmount() >= amountToRemove) {
                        amountRemoved += item.getAmount();
                        amountToRemove -= item.getAmount();
                        item.setAmount(0);
                    } else {
                        amountRemoved = amountInt;
                        item.setAmount(item.getAmount() - amountToRemove);
                        break;
                    }
                }
                if (amountRemoved == 0) {
                    player.sendMessage(Component.translatable(
                            "command.deposit.amount.insufficient",
                            Component.text(amountInt),
                            Component.text(material.name())
                    ));
                    return;
                }
            }
            plugin.depositItems(player, material, amountInt);
            player.sendMessage(Component.translatable("command.deposit.specific.success",
                    Component.text(amountInt),
                    Component.translatable(material.translationKey())
            ));
        }
    }

    @Subcommand("inventory|inv")
    @CommandCompletion("@players")
    public void onInventory(@NotNull Player sender, @Optional @Name("player") OfflinePlayer player) {
        if (player == null) player = sender;
        final ItemRaceInventoryProvider inventoryProvider = new ItemRaceInventoryProvider(plugin, player);
        final SmartInventory inventory = SmartInventory.builder()
                .provider(inventoryProvider)
                .manager(plugin.inventoryManager)
                .title(
                        // SmartInventories doesn't support Adventure components
                        serializeTranslatable(
                                Component.translatable(
                                        "inventory.title",
                                        Component.text(Objects.requireNonNull(player.getName()))
                                ), sender.locale()
                        )
                ).build();
        inventoryProvider.setInventory(inventory);
        inventory.open(sender);
    }

    @Subcommand("leaderboard|lb")
    public void onLeaderboard(@NotNull CommandSender sender) {
        sender.sendMessage(Component.translatable("command.leaderboard.header"));
        if (plugin.hasDepositedItemsInventory()) {
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
                sender.sendMessage(Component.translatable(
                        "command.leaderboard.position",
                        Component.text(scores.get(sender)))
                );
            }
        } else {
            sender.sendMessage(Component.translatable("command.leaderboard.empty"));
        }
        sender.sendMessage(Component.translatable("command.leaderboard.footer"));
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
