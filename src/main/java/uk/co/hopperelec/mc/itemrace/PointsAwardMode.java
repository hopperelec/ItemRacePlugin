package uk.co.hopperelec.mc.itemrace;

public enum PointsAwardMode {
    AUTO_DEPOSIT(true),
    MANUAL_DEPOSIT(true),
    MAX_INVENTORY(true),
    INVENTORY,
    ENDER_CHEST;

    public final boolean hasOwnInventory;
    PointsAwardMode(boolean hasOwnInventory) {
        this.hasOwnInventory = hasOwnInventory;
    }
    PointsAwardMode() {
        this(false);
    }
}
