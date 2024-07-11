package uk.co.hopperelec.mc.itemrace;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class ItemRaceUtils {
    public static String getMaterialDisplayName(@NotNull Material material) {
        return WordUtils.capitalizeFully(material.name().replace("_", " "));
    }
}
