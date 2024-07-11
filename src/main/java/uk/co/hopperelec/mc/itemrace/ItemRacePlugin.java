package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.PaperCommandManager;
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

import java.util.*;

public final class ItemRacePlugin extends JavaPlugin {
    private final Map<OfflinePlayer, Map<Material, Integer>> depositedItems = new HashMap<>();
    public Scoreboard scoreboard;
    public Objective scoreboardObjective;
    public final InventoryManager inventoryManager = new InventoryManager(this);

    public ItemRacePlugin() {
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
    }

    @Override
    public void onEnable() {
        // Initialize inventory for `/itemrace inventory`
        inventoryManager.init();

        // Load commands
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ItemRaceCommand(this));
        commandManager.enableUnstableAPI("help");
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

        // Create scoreboard
        scoreboard = getServer().getScoreboardManager().getNewScoreboard();
        scoreboardObjective = scoreboard.registerNewObjective(
                "score",
                Criteria.DUMMY,
                Component.translatable("scoreboard.title", Style.style(TextDecoration.BOLD))
        );
        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private int calculateScore(@NotNull OfflinePlayer player) {
        if (!depositedItems.containsKey(player)) return 0;
        int score = 0;
        for (int amount : depositedItems.get(player).values()) {
            score += 32 - Integer.numberOfLeadingZeros(amount); // log2(amount)
        }
        return score;
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
