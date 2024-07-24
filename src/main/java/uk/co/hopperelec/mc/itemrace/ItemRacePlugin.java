package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.minuskube.inv.InventoryManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.listeners.AutoDepositListener;
import uk.co.hopperelec.mc.itemrace.listeners.MaxInventoryListener;
import uk.co.hopperelec.mc.itemrace.listeners.ShowScoreboardListener;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.*;

public final class ItemRacePlugin extends JavaPlugin {
    private final File DEPOSITED_ITEMS_FILE = new File(getDataFolder(), "deposited_items.yml");
    private final Map<OfflinePlayer, Map<Material, Integer>> depositedItems = new HashMap<>();
    public Scoreboard scoreboard;
    public Objective scoreboardObjective;
    public final InventoryManager inventoryManager = new InventoryManager(this);
    public final ItemRaceConfig config;

    public ItemRacePlugin() throws IOException {
        // Load translations
        final TranslationRegistry registry = TranslationRegistry.create(Key.key("namespace:value"));
        registry.registerAll(
                Locale.US,
                ResourceBundle.getBundle(
                        "uk.co.hopperelec.mc.itemrace.en_US",
                        Locale.US, UTF8ResourceBundleControl.get()
                ),
                true
        );
        GlobalTranslator.translator().addSource(registry);

        // Load config
        saveDefaultConfig();
        final JsonNode configFile = new YAMLMapper().readTree(new File(getDataFolder(), "config.yml"));
        config = new ItemRaceConfig(
                PointsAwardMode.valueOf(configFile.get("points").get("award_mode").asText().toUpperCase()),
                configFile.get("points").get("items_per_point_growth_rate").asInt(),
                configFile.get("points").get("award_for_first_item").asBoolean(),
                configFile.get("points").get("max_per_item_type").asInt(),
                configFile.get("points").get("allow_damaged_tools").asBoolean(),
                configFile.get("deposited_items").get("persist").asBoolean(),
                (int)(configFile.get("deposited_items").get("autosave_frequency").asDouble() * 60 * 20),
                configFile.get("scoreboard").get("default_state").asBoolean(),
                switch (configFile.get("scoreboard").get("display_slot").asText().toUpperCase()) {
                    case "SIDEBAR" -> DisplaySlot.SIDEBAR;
                    case "BELOW_NAME" -> DisplaySlot.BELOW_NAME;
                    case "PLAYER_LIST" -> DisplaySlot.PLAYER_LIST;
                    default -> throw new IllegalStateException("scoreboard.display_slot must be one of SIDEBAR, BELOW_NAME or PLAYER_LIST");
                },
                StreamSupport.stream(configFile.get("denylist").get("items").spliterator(), false)
                        .map(JsonNode::asText)
                        .map(ItemType::new)
                        .toArray(ItemType[]::new),
                configFile.get("denylist").get("treat_as_allowlist").asBoolean()
        );

        // TODO: Remove once implemented
        if (
                config.pointsAwardMode() != PointsAwardMode.MANUAL_DEPOSIT &&
                config.pointsAwardMode() != PointsAwardMode.AUTO_DEPOSIT
        ) {
            getLogger().warning(config.pointsAwardMode().name()+" has not been implemented yet!");
        }
    }

    @Override
    public void onEnable() {
        // Create scoreboard
        scoreboard = getServer().getScoreboardManager().getNewScoreboard();
        scoreboardObjective = scoreboard.registerNewObjective(
                "score",
                Criteria.DUMMY,
                Component.translatable("scoreboard.title", Style.style(TextDecoration.BOLD))
        );
        scoreboardObjective.setDisplaySlot(config.scoreboardDisplaySlot());
        if (config.defaultScoreboardState()) {
            getServer().getPluginManager().registerEvents(new ShowScoreboardListener(scoreboard), this);
        }

        // Load persisted deposited items
        persistItems: if (isPersistingDepositedItems() && DEPOSITED_ITEMS_FILE.exists()) {
            final JsonNode fileTree;
            try {
                fileTree = new YAMLMapper().readTree(DEPOSITED_ITEMS_FILE);
            } catch (IOException e) {
                getLogger().warning("Failed to load deposited_items.yml: "+e.getMessage());
                break persistItems;
            }
            fileTree.fields().forEachRemaining(inventory -> {
                final OfflinePlayer player = getServer().getOfflinePlayer(UUID.fromString(inventory.getKey()));
                inventory.getValue().fields().forEachRemaining(
                        depositedItem -> setAmountDeposited(
                                player,
                                Material.valueOf(depositedItem.getKey()),
                                depositedItem.getValue().asInt()
                        )
                );
            });
        }

        // Initialize inventory for `/itemrace inventory`
        inventoryManager.init();

        // Start auto-saving deposited items
        if (isPersistingDepositedItems() && config.autosaveFrequencyTicks() > 0)
            getServer().getScheduler().scheduleSyncRepeatingTask(
                    this, this::saveDepositedItems,
                    config.autosaveFrequencyTicks(),
                    config.autosaveFrequencyTicks()
            );

        // Start listening for inventory changes if needed for configured pointsAwardMode
        switch (config.pointsAwardMode()) {
            case AUTO_DEPOSIT:
                getServer().getPluginManager().registerEvents(new AutoDepositListener(this), this);
                break;
            case MAX_INVENTORY:
                getServer().getPluginManager().registerEvents(new MaxInventoryListener(this), this);
        }

        // Load commands
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion(
                "itemType",
                c -> Arrays.stream(c.getPlayer().getInventory().getStorageContents())
                        .filter(Objects::nonNull)
                        .map(ItemStack::getType)
                        .distinct()
                        .filter(this::canDeposit)
                        .map(Material::name)
                        .toList()
        );
        commandManager.getCommandContexts().registerContext(ItemType.class, (c) -> {
            final String name = c.popFirstArg();
            try {
                return new ItemType(name);
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandArgument(
                        // There might be a way to let the client handle this, but I can't figure it out
                        serializeTranslatable(
                                Component.translatable(
                                        "command-context.itemtype.invalid",
                                        Component.text(name)
                                ),
                                c.getPlayer().locale()
                        )
                );
            }
        });
        commandManager.registerCommand(new ItemRaceCommand(this));
        commandManager.enableUnstableAPI("help");
    }

    @Override
    public void onDisable() {
        if (config.persistDepositedItems()) saveDepositedItems();
    }

    public void runInstantly(@NotNull Runnable runnable) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, runnable);
    }

    public boolean isPersistingDepositedItems() {
        return config.persistDepositedItems() && config.pointsAwardMode().hasOwnInventory;
    }

    public void saveDepositedItems() {
        final Map<UUID, Map<String, Integer>> serializedDepositedItems = new HashMap<>();
        depositedItems.forEach((player, items) -> {
            final Map<String, Integer> serializedItems = new HashMap<>();
            items.forEach((material, amount) -> serializedItems.put(material.name(), amount));
            serializedDepositedItems.put(player.getUniqueId(), serializedItems);
        });
        try {
            new YAMLMapper().writeValue(DEPOSITED_ITEMS_FILE, serializedDepositedItems);
        } catch (IOException e) {
            getLogger().warning("Failed to save deposited_items.yml: "+e.getMessage());
        }
    }

    public boolean canDeposit(@NotNull Material material) {
        if (material.isAir()) return false;
        if (config.treatDenylistAsWhitelist())
            return Arrays.stream(config.denylistItems()).anyMatch(itemType -> itemType.is(material));
        return Arrays.stream(config.denylistItems()).noneMatch(itemType -> itemType.is(material));
    }

    public boolean canDeposit(@NotNull ItemStack itemStack) {
        if (config.allowDamagedTools() && isDamaged(itemStack)) return false;
        return canDeposit(itemStack.getType());
    }

    public void tryDeposit(@NotNull Player player, @NotNull Material itemType, int amount) {
        if (canDeposit(itemType)) depositItems(player, itemType, amount);
    }

    public void tryDeposit(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (canDeposit(itemStack)) depositItems(player, itemStack.getType(), itemStack.getAmount());
    }

    public void clearAndTryDeposit(@NotNull Player player, @NotNull ItemStack itemStack) {
        tryDeposit(player, itemStack);
        itemStack.setAmount(0);
    }

    public int scoreForAmount(int amount) {
        if (amount == 0) return 0;
        int score = floorLog(config.itemsPerPointGrowthRate(), amount);
        if (config.maxPointsPerItemType() > 0) score = Math.min(score, config.maxPointsPerItemType());
        if (config.awardPointForFirstItem() && config.itemsPerPointGrowthRate() > 1) return score + 1;
        return score;
    }

    private int calculateScore(@NotNull OfflinePlayer player) {
        if (!depositedItems.containsKey(player)) return 0;
        return depositedItems.get(player).values().stream()
                .map(this::scoreForAmount)
                .reduce(0, Integer::sum);
    }

    @NotNull
    public Map<OfflinePlayer,Integer> calculateScores() {
        return depositedItems.entrySet().stream().collect(
                HashMap::new,
                (map, entry) -> map.put(entry.getKey(), calculateScore(entry.getKey())),
                HashMap::putAll
        );
    }

    private void throwIfNotHasOwnInventory() {
        if (!config.pointsAwardMode().hasOwnInventory)
            throw new IllegalStateException("This server is using a points award mode that does not have a deposited items inventory");
    }

    public int getAmountDeposited(@NotNull OfflinePlayer player, @NotNull Material itemType) {
        throwIfNotHasOwnInventory();
        return depositedItems.containsKey(player) ? depositedItems.get(player).getOrDefault(itemType, 0) : 0;
    }

    public void setAmountDeposited(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        throwIfNotHasOwnInventory();
        if (!depositedItems.containsKey(player)) {
            depositedItems.put(player, new HashMap<>());
        }
        depositedItems.get(player).put(itemType, amount);
        scoreboardObjective.getScore(player).setScore(calculateScore(player));
    }

    public void depositItems(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        if (depositedItems.containsKey(player)) {
            amount += depositedItems.get(player).getOrDefault(itemType, 0);
        }
        setAmountDeposited(player, itemType, amount);
    }

    public void resetDepositedItemsInventory() {
        throwIfNotHasOwnInventory();
        depositedItems.clear();
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    public void resetDepositedItemsInventory(@NotNull OfflinePlayer player) {
        throwIfNotHasOwnInventory();
        depositedItems.remove(player);
        scoreboard.resetScores(player);
    }

    @NotNull
    public Map<Material,Integer> getDepositedItems(@NotNull OfflinePlayer player) {
        return depositedItems.getOrDefault(player, new HashMap<>());
    }

    public boolean hasDepositedItemsInventory() {
        return !depositedItems.isEmpty();
    }

    public boolean hasDepositedItemsInventory(@NotNull OfflinePlayer player) {
        return depositedItems.containsKey(player);
    }
}
