package me.sunmc.smite.api.ability;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable context object containing all relevant information for ability execution.
 *
 * <p>This record encapsulates the player activating the ability, the optional target entity,
 * the trigger that caused activation, and additional metadata. It is passed to all
 * ability lifecycle methods to provide complete context for execution.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * AbilityContext context = new AbilityContext.Builder(player)
 *     .target(targetEntity)
 *     .trigger(AbilityTrigger.CRITICAL_HIT)
 *     .metadata("crit_count", 3)
 *     .metadata("damage", 15.0)
 *     .build();
 * }</pre>
 *
 * @param player   the player activating the ability
 * @param target   the target entity, or null if no specific target
 * @param trigger  the event or action that triggered this ability
 * @param metadata additional contextual data for ability execution
 * @author SunMC
 * @version 1.0.0
 * @see Ability
 * @see AbilityTrigger
 */
public record AbilityContext(
        @NotNull Player player,
        @Nullable Entity target,
        @NotNull AbilityTrigger trigger,
        @NotNull Map<String, Object> metadata
) {

    /**
     * Compact constructor that creates a defensive copy of the metadata map.
     *
     * @param player   the player activating the ability
     * @param target   the target entity, or null
     * @param trigger  the activation trigger
     * @param metadata the metadata map
     */
    public AbilityContext {
        metadata = Map.copyOf(metadata); // Immutable copy
    }

    /**
     * Gets a metadata value by key, returning an Optional.
     *
     * @param key the metadata key
     * @return an Optional containing the value if present, empty otherwise
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Gets a typed metadata value by key.
     *
     * @param key  the metadata key
     * @param type the expected type class
     * @param <T>  the type parameter
     * @return an Optional containing the typed value if present and correct type, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Checks if a metadata key exists.
     *
     * @param key the metadata key to check
     * @return true if the key exists, false otherwise
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Gets the target as an Optional.
     *
     * @return an Optional containing the target if present, empty otherwise
     */
    public Optional<Entity> getTarget() {
        return Optional.ofNullable(target);
    }

    /**
     * Builder class for creating AbilityContext instances.
     *
     * <p>Provides a fluent API for constructing context objects with optional parameters.</p>
     */
    public static final class Builder {
        private final Player player;
        private final Map<String, Object> metadata;
        private Entity target;
        private AbilityTrigger trigger;

        /**
         * Creates a new builder with the specified player.
         *
         * @param player the player activating the ability
         */
        public Builder(@NotNull Player player) {
            this.player = player;
            this.metadata = new HashMap<>();
            this.trigger = AbilityTrigger.MANUAL; // Default trigger
        }

        /**
         * Sets the target entity.
         *
         * @param target the target entity
         * @return this builder for chaining
         */
        public Builder target(@Nullable Entity target) {
            this.target = target;
            return this;
        }

        /**
         * Sets the activation trigger.
         *
         * @param trigger the activation trigger
         * @return this builder for chaining
         */
        public Builder trigger(@NotNull AbilityTrigger trigger) {
            this.trigger = trigger;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key   the metadata key
         * @param value the metadata value
         * @return this builder for chaining
         */
        public Builder metadata(@NotNull String key, @Nullable Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Adds multiple metadata entries from a map.
         *
         * @param metadata the metadata map to add
         * @return this builder for chaining
         */
        public Builder metadata(@NotNull Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Builds the AbilityContext instance.
         *
         * @return a new immutable AbilityContext
         */
        @Contract(value = " -> new", pure = true)
        public @NotNull AbilityContext build() {
            return new AbilityContext(player, target, trigger, metadata);
        }
    }
}