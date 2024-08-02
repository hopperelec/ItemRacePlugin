package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.listeners.GUIListener;
import uk.co.hopperelec.mc.itemrace.pointshandling.*;

import java.io.IOException;
import java.util.*;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.*;

public final class ItemRacePlugin extends JavaPlugin {
    public final GUIListener guiListener = new GUIListener(this);
    public final ItemRaceConfig config;
    public final PointsHandler pointsHandler;

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
        config = new ItemRaceConfig(
                PointsAwardMode.valueOf(Objects.requireNonNull(getConfig().getString("points.award_mode")).toUpperCase()),
                getConfig().getInt("points.items_per_point_growth_rate"),
                getConfig().getBoolean("points.award_for_first_item"),
                getConfig().getInt("points.max_per_item_type"),
                getConfig().getBoolean("deposited_items.persist"),
                (int)(getConfig().getDouble("deposited_items.autosave_frequency") * 60 * 20),
                getConfig().getBoolean("scoreboard.default_state"),
                switch (Objects.requireNonNull(getConfig().getString("scoreboard.display_slot")).toUpperCase()) {
                    case "SIDEBAR" -> DisplaySlot.SIDEBAR;
                    case "BELOW_NAME" -> DisplaySlot.BELOW_NAME;
                    case "PLAYER_LIST" -> DisplaySlot.PLAYER_LIST;
                    default -> throw new IllegalStateException("scoreboard.display_slot must be one of SIDEBAR, BELOW_NAME or PLAYER_LIST");
                },
                getConfig().getBoolean("deposited_items_gui.split_into_stacks"),
                getConfig().getStringList("denylist.items").stream()
                        .map(ItemType::new)
                        .toArray(ItemType[]::new),
                getConfig().getBoolean("denylist.treat_as_allowlist"),
                getConfig().getBoolean("denylist.allow_damaged_tools")
        );

        pointsHandler = switch (config.pointsAwardMode) {
            case AUTO_DEPOSIT_ALL, AUTO_DEPOSIT -> new AutoDepositor(this);
            case DEPOSIT_COMMAND, DEPOSIT_GUI -> new DepositedItems(this, true);
            case MAX_INVENTORY -> new MaxInventoryPointsHandler(this);
            case INVENTORY -> new InventoryPointsHandler(this);
            case ENDER_CHEST -> new EnderChestPointsHandler(this);
        };
    }

    @Override
    public void onEnable() {
        pointsHandler.onEnable();
        // Load commands
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion(
                "itemType",
                c -> Arrays.stream(c.getPlayer().getInventory().getContents())
                        .filter(Objects::nonNull)
                        .map(ItemStack::getType)
                        .distinct()
                        .filter(config::awardPointsFor)
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
                                Component.translatable("command-context.itemtype.invalid",
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
        pointsHandler.onDisable();
    }

    public void runInstantly(@NotNull Runnable runnable) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, runnable);
    }
}
