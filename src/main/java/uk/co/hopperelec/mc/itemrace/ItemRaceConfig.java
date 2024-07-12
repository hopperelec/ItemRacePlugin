package uk.co.hopperelec.mc.itemrace;

import org.bukkit.scoreboard.DisplaySlot;

public record ItemRaceConfig(
    // Points
    PointsAwardMode pointsAwardMode,
    int itemsPerPointGrowthRate,
    boolean awardPointForFirstItem,
    int maxPointsPerItemType,

    // Inventory
    boolean persistInventory,
    int autosaveFrequency,

    // Scoreboard
    boolean defaultState, // 'on' => true, 'off' => false
    DisplaySlot displaySlot,

    // Denylist
    ItemType[] denylistItems,
    boolean treatDenylistAsWhitelist
) {}
