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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.floorLog;
import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.serializeTranslatable;

public final class ItemRacePlugin extends JavaPlugin {
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
                configFile.get("inventory").get("persist_inventory").asBoolean(),
                configFile.get("inventory").get("autosave_frequency").asInt(),
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
    }

    @Override
    public void onEnable() {
        // Initialize inventory for `/itemrace inventory`
        inventoryManager.init();

        // Load commands
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion(
                "itemType",
                c -> Arrays.stream(c.getPlayer().getInventory().getStorageContents())
                        .filter(Objects::nonNull)
                        .map(ItemStack::getType)
                        .distinct()
                        .filter(itemType -> !itemType.isAir())
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

    public void depositItems(@NotNull OfflinePlayer player, @NotNull Material itemType, int amount) {
        int currentAmountDeposited = 0;
        if (!depositedItems.containsKey(player)) {
            depositedItems.put(player, new HashMap<>());
        } else {
            currentAmountDeposited = depositedItems.get(player).getOrDefault(itemType, 0);
        }
        depositedItems.get(player).put(itemType, currentAmountDeposited + amount);
        scoreboardObjective.getScore(player).setScore(calculateScore(player));
    }

    public void resetDepositedItems() {
        depositedItems.clear();
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    public void resetDepositedItems(@NotNull OfflinePlayer player) {
        depositedItems.remove(player);
        scoreboard.resetScores(player);
    }

    @NotNull
    public Map<Material,Integer> getDepositedItems(@NotNull OfflinePlayer player) {
        return depositedItems.getOrDefault(player, new HashMap<>());
    }

    public boolean hasDepositedItems() {
        return !depositedItems.isEmpty();
    }

    public boolean hasDepositedItems(@NotNull OfflinePlayer player) {
        return depositedItems.containsKey(player);
    }
}
