package uk.co.hopperelec.mc.itemrace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ItemRaceUtils {
    public static @NotNull String serializeTranslatable(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(component, locale)
        );
    }

    public static boolean isDamaged(@NotNull ItemStack item) {
        return item.getItemMeta() instanceof Damageable itemMeta && itemMeta.hasDamage();
    }

    // Returns true if it couldn't translate the item name
    public static boolean itemNameContains(@NotNull Material itemType, @NotNull String query, @NotNull Locale locale) {
        final String translationKey = itemType.getItemTranslationKey();
        if (translationKey == null) throw new IllegalArgumentException("Item type " + itemType + " has no translation key");
        return serializeTranslatable(
                Component.translatable(translationKey), locale
        ).toLowerCase(locale).contains(query.toLowerCase(locale));
    }
}
