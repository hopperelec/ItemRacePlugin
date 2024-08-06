package uk.co.hopperelec.mc.itemrace.pointshandling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
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
    private final Map<UUID, Map<Material, Integer>> items = new HashMap<>();
    private final String DATA_FILE_NAME = "data.yml";
    private final File DATA_FILE = new File(plugin.getDataFolder(), DATA_FILE_NAME);
    
    public DepositedItems(@NotNull ItemRacePlugin plugin) {
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
        scoreboardObjective.setDisplaySlot(config().scoreboardDisplaySlot);
        if (config().defaultScoreboardState) {
            plugin.getServer().getPluginManager().registerEvents(new ShowScoreboardListener(scoreboard), plugin);
        }

        if (config().persistDepositedItems && DATA_FILE.exists()) {
            // Load persisted deposited items
            deserializeData(YamlConfiguration.loadConfiguration(DATA_FILE));

            // Start auto-saving deposited items
            if (config().autosaveFrequencyTicks > 0)
                plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
                        plugin, this::save,
                        config().autosaveFrequencyTicks,
                        config().autosaveFrequencyTicks
                );
        }
    }

    @Override
    public void onDisable() {
        if (config().persistDepositedItems) save();
    }

    @Override
    public @NotNull Collection<? extends OfflinePlayer> getEligiblePlayers() {
        return items.keySet().stream().map(plugin.getServer()::getOfflinePlayer).toList();
    }

    @Override
    public @NotNull Map<Material, Integer> getItems(@NotNull OfflinePlayer player) {
        return items.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    public void save() {
        final YamlConfiguration serializedFile = new YamlConfiguration();
        serializeData(serializedFile);
        try {
            serializedFile.save(DATA_FILE);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save "+ DATA_FILE_NAME +": "+e.getMessage());
        }
    }

    protected void serializeData(@NotNull YamlConfiguration data) {
        final ConfigurationSection serializedDepositedItems = data.createSection("deposited_items");
        items.forEach((uuid, depositedItems) -> {
            final ConfigurationSection serializedInventory = serializedDepositedItems.createSection(uuid.toString());
            depositedItems.forEach((material, amount) -> serializedInventory.set(material.toString(), amount));
        });
    }

    protected void deserializeData(@NotNull YamlConfiguration data) {
        final ConfigurationSection serializedDepositedItems = data.getConfigurationSection("deposited_items");
        if (serializedDepositedItems == null) return;
        serializedDepositedItems.getValues(false).forEach((uuidString, serializedInventory) -> {
            final UUID uuid = UUID.fromString(uuidString);
            ((ConfigurationSection) serializedInventory).getValues(false).forEach(
                    (material, amount) -> trySetAmount(
                            uuid,
                            Material.valueOf(material),
                            (int) amount
                    )
            );
            refreshScoreboard(plugin.getServer().getOfflinePlayer(uuid));
        });
    }

    public void deposit(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        final UUID uuid = player.getUniqueId();
        if (items.containsKey(uuid))
            amount += items.get(uuid).getOrDefault(itemType, 0);
        setAmount(player, itemType, amount);
    }

    public void tryDeposit(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        try { deposit(player, itemType, amount); }
        catch (IllegalArgumentException ignored) {}
    }
    public void tryDeposit(@NotNull OfflinePlayer player, @NotNull ItemStack itemStack) {
        tryDeposit(player, itemStack.getType(), itemStack.getAmount());
    }
    // Will leave any items that couldn't be deposited in the inventory
    public void tryDeposit(@NotNull OfflinePlayer player, @NotNull Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && config().awardPointsFor(itemStack))  {
                final int amount = Math.min(itemStack.getAmount(), numberOfItemsLeftToMax(player, itemStack.getType()));
                if (amount == 0) continue;
                deposit(player, itemStack.getType(), amount);
                itemStack.setAmount(itemStack.getAmount() - amount);
            }
        }
    }

    public int getAmount(@NotNull OfflinePlayer player, @NotNull Material itemType) {
        final UUID uuid = player.getUniqueId();
        return items.containsKey(uuid) ? items.get(uuid).getOrDefault(itemType, 0) : 0;
    }

    protected void setAmount(@NotNull UUID uuid, @NotNull Material itemType, int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("Amount cannot be negative");
        if (!config().awardPointsFor(itemType))
            throw new IllegalArgumentException("Tried to set the amount of an item type which is not awarded points");
        if (!items.containsKey(uuid))
            items.put(uuid, new HashMap<>());
        else if (amount == 0)
            items.get(uuid).remove(itemType);
        items.get(uuid).put(itemType, Math.min(amount, config().maxItemsAwardedPoints));
    }
    public void setAmount(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        boolean isNew = !items.containsKey(player.getUniqueId());
        setAmount(player.getUniqueId(), itemType, amount);
        refreshScoreboard(player);
        if (isNew) plugin.guiListener.addEligiblePlayer(player);
        else plugin.guiListener.refreshDepositedItems(player);
    }


    public void trySetAmount(@NotNull UUID uuid, @NotNull Material itemType, int amount) {
        try { setAmount(uuid, itemType, amount); }
        catch (IllegalArgumentException ignored) {}
    }
    public void trySetAmount(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        try { setAmount(player, itemType, amount); }
        catch (IllegalArgumentException ignored) {}
    }

    protected void refreshScoreboard(@NotNull OfflinePlayer player) {
        scoreboardObjective.getScore(player).setScore(calculateScore(player));
    }

    public boolean isMaxed(@NotNull OfflinePlayer player, @NotNull Material itemType) {
        return getAmount(player, itemType) >= config().maxItemsAwardedPoints;
    }

    public int numberOfItemsLeftToMax(@NotNull OfflinePlayer player, @NotNull Material itemType) {
        return config().maxItemsAwardedPoints - getAmount(player, itemType);
    }

    public void reset() {
        getEligiblePlayers().forEach(this::reset);
    }

    public void reset(@NotNull OfflinePlayer player) {
        items.remove(player.getUniqueId());
        scoreboard.resetScores(player);
        plugin.guiListener.removeEligiblePlayer(player);
    }
}
