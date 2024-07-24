package uk.co.hopperelec.mc.itemrace.pointshandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.listeners.ShowScoreboardListener;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DepositedItems extends PointsHandler {
    public Scoreboard scoreboard;
    public Objective scoreboardObjective;
    private final Map<OfflinePlayer, Map<Material, Integer>> items = new HashMap<>();
    private final File FILE = new File(plugin.getDataFolder(), "deposited_items.yml");
    
    public DepositedItems(ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        // Create scoreboard
        scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
        scoreboardObjective = scoreboard.registerNewObjective(
                "score",
                Criteria.DUMMY,
                Component.translatable("scoreboard.title", Style.style(TextDecoration.BOLD))
        );
        scoreboardObjective.setDisplaySlot(config().scoreboardDisplaySlot());
        if (config().defaultScoreboardState()) {
            plugin.getServer().getPluginManager().registerEvents(new ShowScoreboardListener(scoreboard), plugin);
        }

        persistItems: if (config().persistDepositedItems() && FILE.exists()) {
            // Load persisted deposited items
            final JsonNode fileTree;
            try {
                fileTree = new YAMLMapper().readTree(FILE);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load deposited_items.yml: "+e.getMessage());
                break persistItems;
            }
            fileTree.fields().forEachRemaining(inventory -> {
                final OfflinePlayer player = plugin.getServer().getOfflinePlayer(UUID.fromString(inventory.getKey()));
                inventory.getValue().fields().forEachRemaining(
                        depositedItem -> setAmount(
                                player,
                                Material.valueOf(depositedItem.getKey()),
                                depositedItem.getValue().asInt()
                        )
                );
            });

            // Start auto-saving deposited items
            if (config().autosaveFrequencyTicks() > 0)
                plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
                        plugin, this::save,
                        config().autosaveFrequencyTicks(),
                        config().autosaveFrequencyTicks()
                );
        }
    }

    @Override
    public void onDisable() {
        if (config().persistDepositedItems()) save();
    }

    @Override
    public Collection<? extends OfflinePlayer> getEligiblePlayers() {
        return items.keySet();
    }

    @Override
    public @NotNull Map<Material, Integer> getItems(@NotNull OfflinePlayer player) {
        return items.getOrDefault(player, new HashMap<>());
    }

    public void save() {
        final Map<UUID, Map<String, Integer>> serializedDepositedItems = new HashMap<>();
        items.forEach((player, items) -> {
            final Map<String, Integer> serializedItems = new HashMap<>();
            items.forEach((material, amount) -> serializedItems.put(material.name(), amount));
            serializedDepositedItems.put(player.getUniqueId(), serializedItems);
        });
        try {
            new YAMLMapper().writeValue(FILE, serializedDepositedItems);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save deposited_items.yml: "+e.getMessage());
        }
    }

    public void tryDeposit(@NotNull Player player, @NotNull Material itemType, int amount) {
        if (canAwardPointsFor(itemType)) deposit(player, itemType, amount);
    }
    public void tryDeposit(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (canAwardPointsFor(itemStack)) deposit(player, itemStack.getType(), itemStack.getAmount());
    }
    public void clearAndTryDeposit(@NotNull Player player, @NotNull ItemStack itemStack) {
        tryDeposit(player, itemStack);
        itemStack.setAmount(0);
    }

    public int getAmount(@NotNull OfflinePlayer player, @NotNull Material itemType) {
        return items.containsKey(player) ? items.get(player).getOrDefault(itemType, 0) : 0;
    }

    public void setAmount(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        if (!items.containsKey(player))
            items.put(player, new HashMap<>());
        items.get(player).put(itemType, amount);
        scoreboardObjective.getScore(player).setScore(calculateScore(player));
    }

    public void deposit(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        if (items.containsKey(player)) {
            amount += items.get(player).getOrDefault(itemType, 0);
        }
        setAmount(player, itemType, amount);
    }

    public void reset() {
        items.clear();
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    public void reset(@NotNull OfflinePlayer player) {
        items.remove(player);
        scoreboard.resetScores(player);
    }
}
