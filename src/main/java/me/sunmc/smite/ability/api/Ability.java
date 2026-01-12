package me.sunmc.smite.ability.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Core interface for all abilities in the Smite plugin.
 * Provides a flexible, asynchronous API for creating custom abilities.
 */
public interface Ability {

    /**
     * Gets the unique identifier for this ability.
     *
     * @return The ability ID
     */
    @NotNull
    String getId();

    /**
     * Gets the display name of this ability.
     *
     * @return The display name
     */
    @NotNull
    String getDisplayName();

    /**
     * Gets the cooldown duration in seconds.
     *
     * @return Cooldown in seconds
     */
    int getCooldown();

    /**
     * Gets the description of this ability.
     *
     * @return The ability description
     */
    @NotNull
    String getDescription();

    /**
     * Checks if the ability can be activated by the player.
     *
     * @param player  The player attempting to activate
     * @param context The activation context
     * @return true if the ability can be activated
     */
    boolean canActivate(@NotNull Player player, @NotNull ActivationContext context);

    /**
     * Activates the ability for the player.
     * This method is called asynchronously to prevent blocking the main thread.
     *
     * @param player  The player activating the ability
     * @param context The activation context
     * @return A CompletableFuture that completes when the ability execution finishes
     */
    @NotNull
    CompletableFuture<ActivationResult> activate(@NotNull Player player, @NotNull ActivationContext context);

    /**
     * Called when the ability is deactivated or ends.
     *
     * @param player The player whose ability is ending
     */
    void onDeactivate(@NotNull Player player);

    /**
     * Gets the ability type.
     *
     * @return The ability type
     */
    @NotNull
    AbilityType getType();

    /**
     * Checks if this ability is enabled in the configuration.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Sets whether this ability is enabled.
     *
     * @param enabled The enabled state
     */
    void setEnabled(boolean enabled);
}