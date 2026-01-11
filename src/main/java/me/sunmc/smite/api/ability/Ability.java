package me.sunmc.smite.api.ability;

import java.util.UUID;

/**
 * Represents an executable ability in the Smite plugin system.
 *
 * <p>All custom abilities must implement this interface to integrate with the
 * ability management system. Abilities are identified by unique names and can
 * have configurable cooldowns, activation conditions, and execution logic.</p>
 *
 * <p><b>Example Implementation:</b></p>
 * <pre>{@code
 * public class MyCustomAbility implements Ability {
 *
 *     @Override
 *     public String getName() {
 *         return "my_custom_ability";
 *     }
 *
 *     @Override
 *     public String getDisplayName() {
 *         return "My Custom Ability";
 *     }
 *
 *     @Override
 *     public long getCooldownMillis() {
 *         return 30000; // 30 seconds
 *     }
 *
 *     @Override
 *     public boolean canActivate(AbilityContext context) {
 *         return context.player().isOnGround();
 *     }
 *
 *     @Override
 *     public void activate(AbilityContext context) {
 *         // Your ability logic here
 *     }
 *
 *     @Override
 *     public void deactivate(AbilityContext context) {
 *         // Cleanup logic here
 *     }
 * }
 * }</pre>
 *
 * @author SunMC
 * @version 1.0.0
 * @see AbilityContext
 * @see AbilityManager
 */
public interface Ability {

    /**
     * Gets the unique identifier for this ability.
     *
     * <p>This name is used internally for ability lookups and should be
     * lowercase with underscores for spaces (e.g., "heavenly_smite").</p>
     *
     * @return the unique ability identifier
     */
    String getName();

    /**
     * Gets the display name for this ability.
     *
     * <p>This name is shown to players in messages and can contain
     * colors, formatting, and spaces.</p>
     *
     * @return the formatted display name
     */
    String getDisplayName();

    /**
     * Gets the cooldown duration for this ability in milliseconds.
     *
     * <p>Return 0 for abilities with no cooldown. The cooldown begins
     * when the ability is activated and prevents re-activation until expired.</p>
     *
     * @return the cooldown duration in milliseconds
     */
    long getCooldownMillis();

    /**
     * Checks whether this ability can be activated with the given context.
     *
     * <p>This method is called before {@link #activate(AbilityContext)} to
     * validate activation conditions such as player state, cooldowns, or
     * custom requirements.</p>
     *
     * <p><b>Note:</b> Cooldown checking is handled automatically by the
     * {@link AbilityManager}, so implementations do not need to check cooldowns.</p>
     *
     * @param context the ability execution context
     * @return true if the ability can be activated, false otherwise
     */
    boolean canActivate(AbilityContext context);

    /**
     * Activates this ability with the given context.
     *
     * <p>This method is called after successful validation through
     * {@link #canActivate(AbilityContext)} and cooldown checks. All ability
     * logic, particle effects, sound effects, and damage calculations should
     * be performed here.</p>
     *
     * <p><b>Important:</b> This method is always called on the main server thread.</p>
     *
     * @param context the ability execution context containing player, target, and metadata
     */
    void activate(AbilityContext context);

    /**
     * Deactivates this ability and performs cleanup operations.
     *
     * <p>This method is called when an ability's duration expires or when
     * it is manually cancelled. Use this for cleanup operations such as
     * removing status effects, cancelling scheduled tasks, or resetting state.</p>
     *
     * <p>For instant abilities that don't have a duration, this method may
     * be a no-op.</p>
     *
     * @param context the ability execution context
     */
    void deactivate(AbilityContext context);

    /**
     * Checks if this ability is currently on cooldown for the specified player.
     *
     * <p>This method is typically implemented by delegating to the
     * {@link AbilityCooldownManager}. The default implementation should return
     * false if cooldown tracking is not used.</p>
     *
     * @param playerId the UUID of the player to check
     * @return true if the ability is on cooldown, false otherwise
     */
    boolean isOnCooldown(UUID playerId);

    /**
     * Starts the cooldown timer for this ability for the specified player.
     *
     * <p>This method is automatically called by the {@link AbilityManager}
     * after successful ability activation. Implementations should delegate
     * to the {@link AbilityCooldownManager}.</p>
     *
     * @param playerId the UUID of the player to set cooldown for
     */
    void startCooldown(UUID playerId);

    /**
     * Gets the remaining cooldown time in milliseconds for the specified player.
     *
     * <p>Returns 0 if the ability is not on cooldown.</p>
     *
     * @param playerId the UUID of the player to check
     * @return the remaining cooldown time in milliseconds, or 0 if no cooldown
     */
    default long getRemainingCooldown(UUID playerId) {
        return isOnCooldown(playerId) ? getCooldownMillis() : 0;
    }

    /**
     * Checks if this ability is enabled in the configuration.
     *
     * <p>Disabled abilities will not be activated even if all other conditions
     * are met. This allows server administrators to selectively enable/disable
     * abilities without removing them from the registry.</p>
     *
     * @return true if the ability is enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Gets a description of this ability for documentation or help commands.
     *
     * <p>The default implementation returns an empty string. Override this
     * to provide detailed information about the ability's mechanics.</p>
     *
     * @return a description of the ability
     */
    default String getDescription() {
        return "";
    }
}