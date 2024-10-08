package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.gui.AutoDepositItemsGUI;
import uk.co.hopperelec.mc.itemrace.gui.DepositGUI;
import uk.co.hopperelec.mc.itemrace.gui.DepositedItemsGUI;
import uk.co.hopperelec.mc.itemrace.pointshandling.DepositedItems;
import uk.co.hopperelec.mc.itemrace.pointshandling.ManualDepositHandler;
import uk.co.hopperelec.mc.itemrace.pointshandling.PointsAwardMode;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import static org.bukkit.Bukkit.getScoreboardManager;
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;

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
    @CommandPermission("itemrace.reset")
    public void onReset(@NotNull CommandSender sender, @Optional @Name("player") OfflinePlayer player) {
        if (!(plugin.pointsHandler instanceof DepositedItems depositedItems)) {
            sender.sendMessage(Component.translatable("command.reset.unsupported"));
            return;
        }
        if (player == null) {
            depositedItems.reset();
            sender.sendMessage(Component.translatable("command.reset.all"));
        } else {
            depositedItems.reset(player);
            if (player.getName() != null) {
                sender.sendMessage(Component.translatable("command.reset.player",
                        Component.text(player.getName())
                ));
            }
        }
    }
    
    protected void onDepositAll(@NotNull Player player, @NotNull DepositedItems depositedItems, @NotNull Material material) {
        // Check if the player has already been awarded the maximum number of points for the given material
        if (depositedItems.isMaxed(player, material)) {
            player.sendMessage(Component.translatable("command.deposit.full",
                    Component.text(plugin.config.maxPointsPerItemType),
                    Component.translatable(material.translationKey())
            ));
            return;
        }
        // Check if the given material can be deposited
        if (!plugin.config.awardPointsFor(material)) {
            player.sendMessage(Component.translatable("command.deposit.itemtype.denied",
                    Component.translatable(material)
            ));
            return;
        }
        // Remove and count all items of that type in their inventory
        int amountInt = 0;
        int numberOfItemsLeftToMax = depositedItems.numberOfItemsLeftToMax(player, material);
        for (ItemStack item : player.getInventory().all(material).values()) {
            if (!plugin.config.allowDamagedTools && isDamaged(item)) continue;
            final int amount = item.getAmount();
            item.setAmount(amount - numberOfItemsLeftToMax);
            if (amount > numberOfItemsLeftToMax) {
                amountInt += numberOfItemsLeftToMax;
                break;
            }
            amountInt += amount;
            numberOfItemsLeftToMax -= item.getAmount();
        }
        if (amountInt == 0) {
            player.sendMessage(Component.translatable("command.deposit.all.none",
                    Component.translatable(material.translationKey())
            ));
            return;
        }
        // Items have been successfully taken from their inventory
        depositedItems.deposit(player, material, amountInt);
        player.sendMessage(Component.translatable("command.deposit.all.success",
                Component.text(amountInt),
                Component.translatable(material.translationKey())
        ));
    }
    
    protected void onDepositInventory(@NotNull Player player, @NotNull DepositedItems depositedItems) {
        depositedItems.tryDeposit(player, player.getInventory());
        player.sendMessage(Component.translatable("command.deposit.inventory.success"));
    }

    // Returns the number of items which couldn't be removed
    private int removeItems(@NotNull Player player, @NotNull Material material, int amount) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() != material) continue;
            if (!plugin.config.allowDamagedTools && isDamaged(itemStack)) continue;
            if (itemStack.getAmount() >= amount) {
                itemStack.setAmount(itemStack.getAmount() - amount);
                return 0;
            }
            amount -= itemStack.getAmount();
            itemStack.setAmount(0);
        }
        return amount;
    }
    
    protected void onDeposit(@NotNull Player player, @NotNull DepositedItems depositedItems, int amount, @NotNull Material material, boolean mainHand) {
        // Check if the player has already been awarded the maximum number of points for the given material
        if (depositedItems.isMaxed(player, material)) {
            player.sendMessage(Component.translatable("command.deposit.full",
                    Component.text(plugin.config.maxPointsPerItemType),
                    Component.translatable(material.translationKey())
            ));
            return;
        }
        // Check if the amount is valid
        if (amount <= 0) {
            player.sendMessage(Component.translatable("command.deposit.amount.zero"));
            return;
        }
        // Check if the given material can be deposited
        if (!plugin.config.awardPointsFor(material)) {
            player.sendMessage(Component.translatable("command.deposit.itemtype.denied",
                    Component.translatable(material)
            ));
            return;
        }
        // Clamp amount to the maximum number of items that can be awarded points
        amount = Math.min(amount, depositedItems.numberOfItemsLeftToMax(player, material));
        // Remove items from inventory
        if (mainHand) {
            final ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            itemInMainHand.setAmount(itemInMainHand.getAmount() - amount);
        } else {
            final int amountRemaining = removeItems(player, material, amount);
            if (amountRemaining == amount) {
                player.sendMessage(Component.translatable("command.deposit.amount.insufficient",
                        Component.text(amount),
                        Component.text(material.name())
                ));
                return;
            }
            amount -= amountRemaining;
        }
        // Items have been successfully taken from their inventory
        depositedItems.deposit(player, material, amount);
        player.sendMessage(Component.translatable("command.deposit.specific.success",
                Component.text(amount),
                Component.translatable(material.translationKey())
        ));
    }

    @Subcommand("deposit|dep")
    @CommandCompletion("all|inventory @depItemType")
    @CommandPermission("itemrace.deposit")
    public void onDeposit(
        @NotNull Player player,
        @Optional @Name("amount") String amountStr, @Optional @Name("item") ItemType itemType
    ) {
        if (!(plugin.pointsHandler instanceof ManualDepositHandler depositedItems)) {
            player.sendMessage(Component.translatable("command.deposit.disabled"));
            return;
        }
        if (Objects.equals(amountStr, "all")) {
            onDepositAll(player, depositedItems, itemType == null ? player.getInventory().getItemInMainHand().getType() : itemType.material());
            
        } else if (Objects.equals(amountStr, "inventory")) {
            if (itemType != null) {
                player.sendMessage(Component.translatable("command.deposit.inventory.itemtype"));
                return;
            }
            onDepositInventory(player, depositedItems);
            
        } else if (plugin.config.pointsAwardMode == PointsAwardMode.DEPOSIT_GUI && amountStr == null) {
            new DepositGUI(plugin, player);
            
        } else {
            final Material material;
            if (itemType == null) {
                if (!plugin.config.allowDamagedTools && isDamaged(player.getInventory().getItemInMainHand())) {
                    player.sendMessage(Component.translatable("command.deposit.specific.damaged"));
                    return;
                }
                material = player.getInventory().getItemInMainHand().getType();
            } else {
                material = itemType.material();
            }

            final int amountInt;
            if (amountStr == null) {
                amountInt = player.getInventory().getItemInMainHand().getAmount();
            } else {
                try {
                    amountInt = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.translatable("command.deposit.amount.invalid",
                            Component.text(amountStr))
                    );
                    return;
                }
            }

            onDeposit(player, depositedItems, amountInt, material, itemType == null && amountStr == null);
        }
    }

    @Subcommand("autodeposit|autodep")
    @CommandCompletion("add|remove @autoDepItemType")
    @CommandPermission("itemrace.autodeposit")
    public void onAutoDeposit(@NotNull Player player, @Optional String addRemove, @Optional @Name("item") ItemType itemType) {
        if (!(plugin.pointsHandler instanceof ManualDepositHandler manualDepositHandler)) {
            player.sendMessage(Component.translatable("command.deposit.disabled"));
            return;
        }
        if (addRemove == null) {
            new AutoDepositItemsGUI(plugin, player);
            return;
        }

        final boolean add;
        if (addRemove.equals("add")) {
            add = true;
        } else if (addRemove.equals("remove")) {
            add = false;
        } else {
            player.sendMessage(Component.translatable("command.autodeposit.invalid_addremove",
                    Component.text(addRemove)
            ));
            return;
        }

        if (itemType == null) {
            player.sendMessage(Component.translatable("command.autodeposit."+(add ? "add" : "remove")+".missing_itemtype"));
            return;
        }

        final boolean success;
        if (add) success = manualDepositHandler.addAutoDepositItem(player, itemType.material());
        else success = manualDepositHandler.removeAutoDepositItem(player, itemType.material());
        player.sendMessage(Component.translatable(
                "command.autodeposit."+(add ? "add" : "remove")+"."+(success ? "success" : "failure"),
                Component.translatable(itemType.material().translationKey())
        ));
    }

    @Subcommand("inventory|inv")
    @CommandCompletion("@players")
    @CommandPermission("itemrace.inventory.self")
    public void onInventory(@NotNull Player sender, @Optional @Name("player") OfflinePlayer player) {
        if (player == null) player = sender;
        final Component playerNameComponent = Component.text(Objects.requireNonNull(player.getName()));
        if (player == sender || sender.hasPermission("itemrace.inventory"))
            new DepositedItemsGUI(plugin, sender, player);
        else
            sender.sendMessage(Component.translatable("command.inventory.others.denied", playerNameComponent));

    }

    @Subcommand("leaderboard|lb")
    @CommandPermission("itemrace.leaderboard")
    public void onLeaderboard(@NotNull CommandSender sender) {
        sender.sendMessage(Component.translatable("command.leaderboard.header"));
        final Map<OfflinePlayer, Integer> scores = plugin.pointsHandler.calculateScores();
        if (scores.isEmpty()) sender.sendMessage(Component.translatable("command.leaderboard.empty"));
        else {
            int positionI = 1;
            int ownPosition = -1;
            for (
                    Map.Entry<OfflinePlayer, Integer> score :
                    scores.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10).toList()
            ) {
                if (score.getKey() == sender) ownPosition = positionI;
                sender.sendMessage(
                        Component.text(positionI++)
                                .append(Component.text(". "))
                                .append(Component.text(Objects.requireNonNull(score.getKey().getName())))
                                .append(Component.text(" - "))
                                .append(Component.text(score.getValue()))
                );
            }
            if (ownPosition > 0) {
                sender.sendMessage("");
                sender.sendMessage(Component.translatable("command.leaderboard.position",
                        Component.text(ownPosition)
                ));
            }
        }
        sender.sendMessage(Component.translatable("command.leaderboard.footer"));
    }

    @Subcommand("togglescoreboard|tsb")
    @CommandPermission("itemrace.togglescoreboard")
    public void onToggleScoreboard(@NotNull Player player) {
        if (plugin.pointsHandler instanceof DepositedItems depositedItems) {
            if (player.getScoreboard() == depositedItems.scoreboard)
                player.setScoreboard(getScoreboardManager().getMainScoreboard());
            else
                player.setScoreboard(depositedItems.scoreboard);
        } else player.sendMessage(Component.translatable("command.togglescoreboard.unsupported"));
    }
}
