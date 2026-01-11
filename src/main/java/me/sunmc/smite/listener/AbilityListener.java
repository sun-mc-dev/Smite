package me.sunmc.smite.listener;

import me.sunmc.smite.Smite;
import me.sunmc.smite.api.event.AbilityActivateEvent;
import me.sunmc.smite.api.event.AbilityEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to ability-specific events for logging and integration.
 *
 * <p>This listener provides hooks for external plugins to interact with
 * the ability system through the event API.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class AbilityListener implements Listener {

    private final Smite plugin;

    /**
     * Constructs a new ability listener.
     *
     * @param plugin the plugin instance
     */
    public AbilityListener(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    /**
     * Logs ability activations if debug mode is enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbilityActivate(AbilityActivateEvent event) {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info(String.format(
                    "Player %s activated ability %s (trigger: %s)",
                    event.getPlayer().getName(),
                    event.getAbility().getName(),
                    event.getContext().trigger().name()
            ));
        }
    }

    /**
     * Logs ability deactivations if debug mode is enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbilityEnd(AbilityEndEvent event) {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info(String.format(
                    "Ability %s ended for player %s",
                    event.getAbility().getName(),
                    event.getPlayer().getName()
            ));
        }
    }
}