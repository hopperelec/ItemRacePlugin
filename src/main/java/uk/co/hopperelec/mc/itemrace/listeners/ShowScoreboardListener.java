package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

public class ShowScoreboardListener implements Listener {
    private final Scoreboard scoreboard;

    public ShowScoreboardListener(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(scoreboard);
    }
}
