package uk.co.hopperelec.mc.itemrace;

import org.bukkit.scoreboard.DisplaySlot;
import uk.co.hopperelec.mc.itemrace.pointshandling.PointsAwardMode;

public record ItemRaceConfig(
        // Points
        PointsAwardMode pointsAwardMode,
        int itemsPerPointGrowthRate,
        boolean awardPointForFirstItem,
        int maxPointsPerItemType, // TODO: Prevent players from manually depositing more than this
        boolean allowDamagedTools,

        // Inventory
        boolean persistDepositedItems,
        int autosaveFrequencyTicks,

        // Scoreboard
        boolean defaultScoreboardState, // 'on' => true, 'off' => false
        DisplaySlot scoreboardDisplaySlot,

        // Denylist
        ItemType[] denylistItems,
        boolean treatDenylistAsWhitelist
) {}
