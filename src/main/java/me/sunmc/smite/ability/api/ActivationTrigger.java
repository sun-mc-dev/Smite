package me.sunmc.smite.ability.api;

/**
 * Enum representing how an ability was triggered.
 */
public enum ActivationTrigger {
    MANUAL,         // Manually activated by player
    COMBAT,         // Triggered by combat action
    CRITICAL_HIT,   // Triggered by critical hits
    DAMAGE_TAKEN,   // Triggered by taking damage
    KILL,           // Triggered by killing entity
    CUSTOM          // Custom trigger
}
