package me.sunmc.smite.api.ability;

import me.sunmc.smite.Smite;
import me.sunmc.smite.api.event.AbilityActivateEvent;
import me.sunmc.smite.api.event.AbilityEndEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for ability activation, deactivation, and state tracking.
 *
 * <p>This manager handles the complete lifecycle of abilities including validation,
 * activation, duration tracking, and cleanup. It integrates with the cooldown
 * system and fires appropriate events for ability state changes.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Automatic cooldown management</li>
 *   <li>Active ability tracking</li>
 *   <li>Event firing for ability lifecycle</li>
 *   <li>Thread-safe state management</li>
 * </ul>
 *
 * @author SunMC
 * @version 1.0.0
 * @see Ability
 * @see AbilityContext
 */
public final class AbilityManager {

    private final Smite plugin;
    private final AbilityCooldownManager cooldownManager;
    private final Map<UUID, Set<String>> activeAbilities;

    /**
     * Constructs a new ability manager.
     *
     * @param plugin          the plugin instance
     * @param cooldownManager the cooldown manager
     */
    public AbilityManager(@NotNull Smite plugin, @NotNull AbilityCooldownManager cooldownManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.activeAbilities = new ConcurrentHashMap<>();
    }

    /**
     * Attempts to activate an ability with the given context.
     *
     * <p>This method performs the following checks in order:</p>
     * <ol>
     *   <li>Ability is enabled in configuration</li>
     *   <li>Ability is not on cooldown</li>
     *   <li>Ability's custom activation conditions are met</li>
     * </ol>
     *
     * <p>If all checks pass, the ability is activated, an {@link AbilityActivateEvent}
     * is fired, and the cooldown timer is started.</p>
     *
     * @param ability the ability to activate
     * @param context the execution context
     * @return true if the ability was successfully activated, false otherwise
     */
    public boolean activateAbility(@NotNull Ability ability, @NotNull AbilityContext context) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        Player player = context.player();
        String abilityName = ability.getName();

        // Check if ability is enabled
        if (!ability.isEnabled()) {
            sendMessage(player, "ability-disabled", ability.getDisplayName());
            return false;
        }

        // Check cooldown
        if (ability.isOnCooldown(player.getUniqueId())) {
            int remaining = cooldownManager.getRemainingCooldownSeconds(
                    player.getUniqueId(),
                    abilityName
            );
            sendMessage(player, "ability-on-cooldown", ability.getDisplayName(), remaining);
            return false;
        }

        // Check custom activation conditions
        if (!ability.canActivate(context)) {
            sendMessage(player, "ability-cannot-activate", ability.getDisplayName());
            return false;
        }

        // Fire pre-activation event
        AbilityActivateEvent event = new AbilityActivateEvent(ability, context);
        plugin.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Activate the ability
        try {
            ability.activate(context);

            // Mark as active
            activeAbilities.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                    .add(abilityName.toLowerCase());

            // Start cooldown
            ability.startCooldown(player.getUniqueId());

            // Send success message
            sendMessage(player, "ability-activated", ability.getDisplayName());

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error activating ability " + abilityName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deactivates an ability for a specific player.
     *
     * <p>This method removes the ability from the active set and calls its
     * {@link Ability#deactivate(AbilityContext)} method for cleanup.</p>
     *
     * @param ability the ability to deactivate
     * @param player  the player to deactivate for
     */
    public void deactivateAbility(@NotNull Ability ability, @NotNull Player player) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");

        String abilityName = ability.getName();
        UUID playerId = player.getUniqueId();

        Set<String> playerAbilities = activeAbilities.get(playerId);
        if (playerAbilities == null || !playerAbilities.contains(abilityName.toLowerCase())) {
            return;
        }

        // Create deactivation context
        AbilityContext context = new AbilityContext.Builder(player)
                .trigger(AbilityTrigger.MANUAL)
                .build();

        try {
            ability.deactivate(context);
            playerAbilities.remove(abilityName.toLowerCase());

            // Fire deactivation event
            AbilityEndEvent event = new AbilityEndEvent(ability, context);
            plugin.getServer().getPluginManager().callEvent(event);

        } catch (Exception e) {
            plugin.getLogger().severe("Error deactivating ability " + abilityName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if an ability is currently active for a player.
     *
     * @param playerId    the UUID of the player
     * @param abilityName the name of the ability
     * @return true if the ability is active, false otherwise
     */
    public boolean isAbilityActive(@NotNull UUID playerId, @NotNull String abilityName) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(abilityName, "Ability name cannot be null");

        Set<String> playerAbilities = activeAbilities.get(playerId);
        return playerAbilities != null && playerAbilities.contains(abilityName.toLowerCase());
    }

    /**
     * Gets all active abilities for a specific player.
     *
     * @param playerId the UUID of the player
     * @return an unmodifiable set of active ability names
     */
    @NotNull
    public Set<String> getActiveAbilities(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Set<String> playerAbilities = activeAbilities.get(playerId);
        if (playerAbilities == null) {
            return Collections.emptySet();
        }

        return Set.copyOf(playerAbilities);
    }

    /**
     * Clears all active abilities for a specific player.
     * This method calls deactivate on each ability.
     *
     * @param player the player to clear abilities for
     */
    public void clearPlayerAbilities(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        UUID playerId = player.getUniqueId();
        Set<String> playerAbilities = activeAbilities.get(playerId);

        if (playerAbilities == null || playerAbilities.isEmpty()) {
            return;
        }

        // Create a copy to avoid concurrent modification
        Set<String> abilitiesToClear = new HashSet<>(playerAbilities);

        for (String abilityName : abilitiesToClear) {
            plugin.getAbilityRegistry().getAbility(abilityName).ifPresent(ability ->
                    deactivateAbility(ability, player)
            );
        }
    }

    /**
     * Clears all active abilities for all players.
     * This should be called during plugin shutdown.
     */
    public void clearAllActiveAbilities() {
        activeAbilities.clear();
    }

    /**
     * Schedules an ability to automatically deactivate after a duration.
     *
     * @param ability       the ability to schedule deactivation for
     * @param player        the player the ability is active for
     * @param durationTicks the duration in ticks before deactivation
     */
    public void scheduleDeactivation(@NotNull Ability ability, @NotNull Player player, long durationTicks) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (player.isOnline() && isAbilityActive(player.getUniqueId(), ability.getName())) {
                        deactivateAbility(ability, player);
                        sendMessage(player, "ability-ended", ability.getDisplayName());
                    }
                },
                durationTicks
        );
    }

    /**
     * Sends a formatted message to a player from the messages' configuration.
     *
     * @param player       the player to send the message to
     * @param messageKey   the configuration key for the message
     * @param replacements the replacement values for placeholders
     */
    private void sendMessage(@NotNull Player player, @NotNull String messageKey, Object... replacements) {
        String message = plugin.getConfigManager().getMessage(messageKey, replacements);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }
}