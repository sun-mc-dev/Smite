package me.sunmc.smite.config;

import me.sunmc.smite.Smite;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Manages plugin configuration.
 */
public class ConfigManager {

    private final Smite plugin;
    private FileConfiguration config;

    public ConfigManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isAbilityEnabled(@NotNull String abilityId) {
        return config.getBoolean("abilities." + abilityId + ".enabled", true);
    }

    public int getAbilityCooldown(@NotNull String abilityId, int defaultValue) {
        return config.getInt("abilities." + abilityId + ".cooldown", defaultValue);
    }

    public double getAbilityDamage(@NotNull String abilityId, double defaultValue) {
        return config.getDouble("abilities." + abilityId + ".damage", defaultValue);
    }

    public int getAbilityDuration(@NotNull String abilityId, int defaultValue) {
        return config.getInt("abilities." + abilityId + ".duration", defaultValue);
    }
}