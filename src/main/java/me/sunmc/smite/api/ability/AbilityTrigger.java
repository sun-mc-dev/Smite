package me.sunmc.smite.api.ability;

/**
 * Enumeration of possible triggers that can activate an ability.
 *
 * <p>Each trigger represents a specific game event or player action that
 * can cause an ability to execute. Abilities can check the trigger type
 * in their activation logic to implement conditional behavior.</p>
 *
 * @author SunMC
 * @version 1.0.0
 * @see AbilityContext
 */
public enum AbilityTrigger {

    /**
     * Ability was manually activated by the player through a command,
     * keybind, or item usage.
     */
    MANUAL("Manual Activation"),

    /**
     * Ability was triggered by a critical hit on an entity.
     */
    CRITICAL_HIT("Critical Hit"),

    /**
     * Ability was triggered by dealing damage to an entity.
     */
    DAMAGE_DEALT("Damage Dealt"),

    /**
     * Ability was triggered by taking damage from an entity.
     */
    DAMAGE_TAKEN("Damage Taken"),

    /**
     * Ability was triggered by killing an entity.
     */
    ENTITY_KILL("Entity Kill"),

    /**
     * Ability was triggered by a player death.
     */
    PLAYER_DEATH("Player Death"),

    /**
     * Ability was triggered by blocking with a shield.
     */
    SHIELD_BLOCK("Shield Block"),

    /**
     * Ability was triggered by a projectile hit.
     */
    PROJECTILE_HIT("Projectile Hit"),

    /**
     * Ability was triggered by consuming an item.
     */
    ITEM_CONSUME("Item Consume"),

    /**
     * Ability was triggered by a timed interval or scheduled task.
     */
    SCHEDULED("Scheduled"),

    /**
     * Ability was triggered by reaching low health.
     */
    LOW_HEALTH("Low Health"),

    /**
     * Ability was triggered by a custom plugin event or external trigger.
     */
    CUSTOM("Custom Trigger");

    private final String displayName;

    /**
     * Constructs an ability trigger with a display name.
     *
     * @param displayName the human-readable name for this trigger
     */
    AbilityTrigger(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this trigger.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this trigger is combat-related.
     *
     * @return true if this trigger involves combat actions
     */
    public boolean isCombatTrigger() {
        return this == CRITICAL_HIT ||
                this == DAMAGE_DEALT ||
                this == DAMAGE_TAKEN ||
                this == ENTITY_KILL;
    }

    /**
     * Checks if this trigger requires player interaction.
     *
     * @return true if this trigger requires direct player action
     */
    public boolean requiresPlayerAction() {
        return this == MANUAL ||
                this == ITEM_CONSUME ||
                this == SHIELD_BLOCK;
    }
}