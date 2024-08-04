package uk.co.hopperelec.mc.itemrace.config;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemType;

import java.util.List;
import java.util.Set;

public record PointsModifier(@NotNull Set<ItemType> items, @NotNull List<PointsChange> changes) {
    public PointsModifier(@NotNull Set<ItemType> items, @NotNull List<PointsChange> changes) {
        this.items = Set.copyOf(items);
        this.changes = List.copyOf(changes);
    }

    public int apply(@NotNull ItemType itemType, int numItems, int currentPoints) {
        if (items.contains(itemType)) {
            for (PointsChange change : changes) {
                currentPoints = change.apply(numItems, currentPoints);
            }
        }
        return currentPoints;
    }
}
