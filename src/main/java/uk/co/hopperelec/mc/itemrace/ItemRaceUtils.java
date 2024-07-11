package uk.co.hopperelec.mc.itemrace;

import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ItemRaceUtils {
    public static String serializeTranslatable(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(component, locale)
        );
    }
}
