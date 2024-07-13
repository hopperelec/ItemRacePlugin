package uk.co.hopperelec.mc.itemrace;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public record ItemType(Material material) {
    public ItemType(@NotNull Material material) {
        if (!material.isItem()) throw new IllegalArgumentException("Material must be an item");
        this.material = material;
    }
    public ItemType(@NotNull String materialName) {
        this(Material.valueOf(materialName.toUpperCase()));
    }

    public boolean is(@NotNull Material material) {
        return this.material == material;
    }
}
