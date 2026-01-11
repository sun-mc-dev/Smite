package me.sunmc.smite.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration record for ability settings.
 *
 * <p>This record encapsulates all configurable parameters for an ability,
 * providing type-safe access to configuration values.</p>
 *
 * @param enabled         whether the ability is enabled
 * @param cooldownSeconds the cooldown duration in seconds
 * @param parameter1      first custom parameter (e.g., required crits, duration)
 * @param parameter2      second custom parameter (e.g., damage, multiplier)
 * @param parameter3      third custom parameter (e.g., ignore armor, prevent damage)
 * @param particleSection configuration section for particle settings
 * @param extraSection    additional configuration section for ability-specific settings
 * @author SunMC
 * @version 1.0.0
 */
public record AbilityConfig(
        boolean enabled,
        int cooldownSeconds,
        int parameter1,
        double parameter2,
        boolean parameter3,
        @Nullable ConfigurationSection particleSection,
        @Nullable ConfigurationSection extraSection
) {

    /**
     * Gets the cooldown duration in milliseconds.
     *
     * @return the cooldown in milliseconds
     */
    public long getCooldownMillis() {
        return cooldownSeconds * 1000L;
    }

    /**
     * Gets a particle configuration value.
     *
     * @param path         the configuration path within the particle section
     * @param defaultValue the default value if not found
     * @param <T>          the type of the value
     * @return the configuration value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getParticleConfig(@NotNull String path, @NotNull T defaultValue) {
        if (particleSection == null) {
            return defaultValue;
        }

        Object value = particleSection.get(path);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Gets an extra configuration value.
     *
     * @param path         the configuration path within the extra section
     * @param defaultValue the default value if not found
     * @param <T>          the type of the value
     * @return the configuration value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtraConfig(@NotNull String path, @NotNull T defaultValue) {
        if (extraSection == null) {
            return defaultValue;
        }

        Object value = extraSection.get(path);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Checks if particle configuration section exists.
     *
     * @return true if particle section is present
     */
    public boolean hasParticleSection() {
        return particleSection != null;
    }

    /**
     * Checks if extra configuration section exists.
     *
     * @return true if extra section is present
     */
    public boolean hasExtraSection() {
        return extraSection != null;
    }
}