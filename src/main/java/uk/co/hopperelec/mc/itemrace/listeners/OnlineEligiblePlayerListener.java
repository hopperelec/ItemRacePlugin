package uk.co.hopperelec.mc.itemrace.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class OnlineEligiblePlayerListener implements Listener {
    private final GUIListener guiListener;

    public OnlineEligiblePlayerListener(@NotNull GUIListener guiListener) {
        this.guiListener = guiListener;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        guiListener.addEligiblePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        guiListener.removeEligiblePlayer(event.getPlayer());
    }
}
