package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fr.minuskube.inv.InventoryManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.listeners.DepositGUIListener;
import uk.co.hopperelec.mc.itemrace.pointshandling.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.*;

public final class ItemRacePlugin extends JavaPlugin {
    public final InventoryManager inventoryManager = new InventoryManager(this);
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
        final JsonNode configFile = new YAMLMapper().readTree(new File(getDataFolder(), "config.yml"));
        config = new ItemRaceConfig(
                PointsAwardMode.valueOf(configFile.get("points").get("award_mode").asText().toUpperCase()),
                configFile.get("points").get("items_per_point_growth_rate").asInt(),
                configFile.get("points").get("award_for_first_item").asBoolean(),
                configFile.get("points").get("max_per_item_type").asInt(),
                configFile.get("deposited_items").get("persist").asBoolean(),
                (int)(configFile.get("deposited_items").get("autosave_frequency").asDouble() * 60 * 20),
                configFile.get("scoreboard").get("default_state").asBoolean(),
                switch (configFile.get("scoreboard").get("display_slot").asText().toUpperCase()) {
                    case "SIDEBAR" -> DisplaySlot.SIDEBAR;
                    case "BELOW_NAME" -> DisplaySlot.BELOW_NAME;
                    case "PLAYER_LIST" -> DisplaySlot.PLAYER_LIST;
                    default -> throw new IllegalStateException("scoreboard.display_slot must be one of SIDEBAR, BELOW_NAME or PLAYER_LIST");
                },
                configFile.get("inventory_gui").get("split_into_stacks").asBoolean(),
                StreamSupport.stream(configFile.get("denylist").get("items").spliterator(), false)
                        .map(JsonNode::asText)
                        .map(ItemType::new)
                        .toArray(ItemType[]::new),
                configFile.get("denylist").get("treat_as_allowlist").asBoolean(),
                configFile.get("denylist").get("allow_damaged_tools").asBoolean()
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
        // Initialize inventory for `/itemrace inventory`
        inventoryManager.init();
        // Listen for interactions with the deposit GUI
        if (pointsHandler instanceof DepositedItems depositedItems) {
            getServer().getPluginManager().registerEvents(new DepositGUIListener(config, depositedItems), this);
        }
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
    
    public void openDepositGUI(@NotNull Player player) {
        player.openInventory(new DepositGUI(this).getInventory());
    }
}
