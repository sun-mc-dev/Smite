package me.sunmc.smite.listener;

import me.sunmc.smite.Smite;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player join/quit events.
 */
public class PlayerListener implements Listener {

    private final Smite plugin;

    public PlayerListener(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        plugin.getAbilityManager().getPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        plugin.getAbilityManager().removePlayerData(event.getPlayer());
        plugin.getAbilityManager().getCooldownManager().clearCooldowns(event.getPlayer());
    }
}