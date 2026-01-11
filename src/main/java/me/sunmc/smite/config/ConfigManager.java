package me.sunmc.smite.config;

import me.sunmc.smite.Smite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Central manager for all plugin configurations.
 *
 * <p>This manager handles loading, caching, and reloading of configuration files
 * including the main config and messages. It provides type-safe access to
 * configuration values with proper defaults.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class ConfigManager {

    private final Smite plugin;
    private final MiniMessage miniMessage;
    private final Map<String, AbilityConfig> abilityConfigs;
    private FileConfiguration config;
    private FileConfiguration messages;

    /**
     * Constructs a new configuration manager.
     *
     * @param plugin the plugin instance
     */
    public ConfigManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.abilityConfigs = new HashMap<>();
    }

    /**
     * Loads all configuration files and creates defaults if they don't exist.
     */
    public void loadConfigurations() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Load messages configuration
        loadMessagesConfig();

        // Load ability configurations
        loadAbilityConfigs();

        plugin.getLogger().info("Configuration files loaded successfully");
    }

    /**
     * Reloads all configuration files from disk.
     */
    public void reloadConfigurations() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadMessagesConfig();
        loadAbilityConfigs();

        plugin.getLogger().info("Configuration files reloaded");
    }

    /**
     * Loads the messages configuration file.
     */
    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults
        try (InputStream defConfigStream = plugin.getResource("messages.yml")) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                );
                messages.setDefaults(defConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default messages: " + e.getMessage());
        }
    }

    /**
     * Loads ability-specific configurations.
     */
    private void loadAbilityConfigs() {
        abilityConfigs.clear();

        // Load Heavenly Smite config
        AbilityConfig heavenlySmite = new AbilityConfig(
                config.getBoolean("abilities.heavenly-smite.enabled", true),
                config.getInt("abilities.heavenly-smite.cooldown", 45),
                config.getInt("abilities.heavenly-smite.required-crits", 3),
                config.getDouble("abilities.heavenly-smite.damage", 10.0),
                config.getBoolean("abilities.heavenly-smite.ignore-armor", true),
                config.getConfigurationSection("abilities.heavenly-smite.particles"),
                config.getConfigurationSection("abilities.heavenly-smite.lightning")
        );
        abilityConfigs.put("heavenly_smite", heavenlySmite);

        // Load Demonic Spark config
        AbilityConfig demonicSpark = new AbilityConfig(
                config.getBoolean("abilities.demonic-spark.enabled", true),
                config.getInt("abilities.demonic-spark.cooldown", 45),
                config.getInt("abilities.demonic-spark.duration", 5),
                config.getDouble("abilities.demonic-spark.damage-multiplier", 1.0),
                config.getBoolean("abilities.demonic-spark.prevent-damage", true),
                config.getConfigurationSection("abilities.demonic-spark.particles"),
                config.getConfigurationSection("abilities.demonic-spark.sounds")
        );
        abilityConfigs.put("demonic_spark", demonicSpark);
    }

    /**
     * Gets the ability configuration for a specific ability.
     *
     * @param abilityName the name of the ability
     * @return the ability configuration, or null if not found
     */
    @Nullable
    public AbilityConfig getAbilityConfig(@NotNull String abilityName) {
        return abilityConfigs.get(abilityName.toLowerCase());
    }

    /**
     * Gets a message from the messages configuration with placeholder replacement.
     *
     * @param key          the message key
     * @param replacements the values to replace placeholders with
     * @return the formatted message, or null if not found
     */
    @Nullable
    public String getMessage(@NotNull String key, Object... replacements) {
        String message = messages.getString("messages." + key);

        if (message == null) {
            return null;
        }

        // Apply prefix if configured
        String prefix = messages.getString("messages.prefix", "");
        if (!prefix.isEmpty() && !key.equals("prefix")) {
            message = prefix + message;
        }

        // Replace placeholders
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }

        // Common replacements
        message = message.replace("{ability}", replacements.length > 0 ? String.valueOf(replacements[0]) : "");
        message = message.replace("{time}", replacements.length > 1 ? String.valueOf(replacements[1]) : "");

        return message;
    }

    /**
     * Gets a message as an Adventure Component with MiniMessage formatting.
     *
     * @param key          the message key
     * @param replacements the values to replace placeholders with
     * @return the formatted component, or empty component if not found
     */
    @NotNull
    public Component getMessageComponent(@NotNull String key, Object... replacements) {
        String message = getMessage(key, replacements);
        if (message == null) {
            return Component.empty();
        }

        return miniMessage.deserialize(message);
    }

    /**
     * Checks if cooldown persistence is enabled.
     *
     * @return true if cooldowns should be saved across restarts
     */
    public boolean isPersistCooldowns() {
        return config.getBoolean("cooldowns.persist-on-restart", true);
    }

    /**
     * Gets the cooldown display format.
     *
     * @return the cooldown display format string
     */
    @NotNull
    public String getCooldownFormat() {
        return config.getString("cooldowns.display-format", "&cCooldown: &e{time}s");
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug logging is enabled
     */
    public boolean isDebugMode() {
        return config.getBoolean("plugin.debug", false);
    }

    /**
     * Gets the configured language code.
     *
     * @return the language code (e.g., "en_US")
     */
    @NotNull
    public String getLanguage() {
        return config.getString("plugin.language", "en_US");
    }

    /**
     * Gets the main configuration.
     *
     * @return the main config
     */
    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Gets the messages configuration.
     *
     * @return the messages config
     */
    @NotNull
    public FileConfiguration getMessagesConfig() {
        return messages;
    }
}