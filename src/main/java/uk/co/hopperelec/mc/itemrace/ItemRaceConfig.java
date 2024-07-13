package uk.co.hopperelec.mc.itemrace;

import org.bukkit.scoreboard.DisplaySlot;

public record ItemRaceConfig(
    // Points
    PointsAwardMode pointsAwardMode, // TODO: Implement pointsAwardMode
    int itemsPerPointGrowthRate,
    boolean awardPointForFirstItem,
    int maxPointsPerItemType,
    boolean allowDamagedTools,

    // Inventory
    boolean persistInventory, // TODO: Implement ItemRace inventory saving
    int autosaveFrequency,

    // Scoreboard
    boolean defaultScoreboardState, // 'on' => true, 'off' => false
    DisplaySlot scoreboardDisplaySlot,

    // Denylist
    // TODO: Implement denylist/allowlist
    ItemType[] denylistItems,
    boolean treatDenylistAsWhitelist
) {}
