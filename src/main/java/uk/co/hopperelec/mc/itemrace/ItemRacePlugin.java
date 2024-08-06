package uk.co.hopperelec.mc.itemrace;

import co.aikar.commands.BukkitCommandCompletionContext;
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
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.config.ItemRaceConfig;
import uk.co.hopperelec.mc.itemrace.listeners.GUIListener;
import uk.co.hopperelec.mc.itemrace.listeners.OnlineEligiblePlayerListener;
import uk.co.hopperelec.mc.itemrace.pointshandling.*;
import uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit.AutoDepositAllHandler;
import uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit.AutoDepositHandler;

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
        config = new ItemRaceConfig(getConfig());
        pointsHandler = switch (config.pointsAwardMode) {
            case AUTO_DEPOSIT_ALL -> new AutoDepositAllHandler(this);
            case AUTO_DEPOSIT -> new AutoDepositHandler(this);
            case DEPOSIT_COMMAND, DEPOSIT_GUI -> new ManualDepositHandler(this);
            case MAX_INVENTORY -> new MaxInventoryPointsHandler(this);
            case INVENTORY -> new InventoryPointsHandler(this);
            case ENDER_CHEST -> new EnderChestPointsHandler(this);
        };
    }

    private @NotNull List<String> getDepositableItemNames(@NotNull BukkitCommandCompletionContext context) {
        return Arrays.stream(context.getPlayer().getInventory().getContents())
                .filter(Objects::nonNull)
                .map(ItemStack::getType)
                .distinct()
                .filter(config::awardPointsFor)
                .map(Material::name)
                .toList();
    }

    @Override
    public void onEnable() {
        pointsHandler.onEnable();
        if (pointsHandler instanceof InventoryPointsHandler)
            getServer().getPluginManager().registerEvents(new OnlineEligiblePlayerListener(guiListener), this);
        // Load commands
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion("depItemType", this::getDepositableItemNames);
        commandManager.getCommandCompletions().registerCompletion(
                "autoDepItemType",
                context -> {
                    final String addRemove = context.getContextValue(String.class, 1);
                    if (addRemove.equals("add"))
                        return getDepositableItemNames(context);
                    if (addRemove.equals("remove") && pointsHandler instanceof ManualDepositHandler manualDepositHandler)
                        return manualDepositHandler.getAutoDepositItems(context.getPlayer()).stream()
                                .map(Material::name)
                                .toList();
                    return List.of();
                }
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
