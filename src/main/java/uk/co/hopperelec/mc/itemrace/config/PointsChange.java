package uk.co.hopperelec.mc.itemrace.config;

import org.jetbrains.annotations.NotNull;

public record PointsChange(@NotNull PointsChangeType type, int value, int minItems, int maxItems) {
    public int apply(int numItems, int currentPoints) {
        if (numItems < minItems || numItems > maxItems) return currentPoints;
        return switch (type) {
            case FIXED -> value;
            case ADD -> currentPoints + value;
            case MULTIPLY -> currentPoints * value;
        };
    }
}
