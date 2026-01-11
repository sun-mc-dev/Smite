package me.sunmc.smite;

import me.sunmc.smite.abilities.DemonicSparkAbility;
import me.sunmc.smite.abilities.HeavenlySmiteAbility;
import me.sunmc.smite.api.ability.AbilityCooldownManager;
import me.sunmc.smite.api.ability.AbilityManager;
import me.sunmc.smite.api.ability.AbilityRegistry;
import me.sunmc.smite.command.DemonicSparkCommand;
import me.sunmc.smite.command.SmiteCommand;
import me.sunmc.smite.config.ConfigManager;
import me.sunmc.smite.listener.AbilityListener;
import me.sunmc.smite.listener.CombatListener;
import me.sunmc.smite.packet.PacketHandler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Smite - A high-performance packet-based ability system.
 *
 * <p>This plugin provides a robust framework for creating and managing combat abilities
 * in Minecraft Paper 1.21.1. It features two core abilities:</p>
 * <ul>
 *   <li><b>Heavenly Smite</b> - Lightning strike triggered by critical hit chains</li>
 *   <li><b>Demonic Spark</b> - Damage accumulation and burst mechanic</li>
 * </ul>
 *
 * <p>The plugin is built with extensibility in mind, providing a comprehensive API
 * for developers to create custom abilities.</p>
 *
 * @author SunMC
 * @version 1.0.0
 * @since 1.21.1
 */
public final class Smite extends JavaPlugin {

    private static Smite instance;
    private ConfigManager configManager;
    private AbilityRegistry abilityRegistry;
    private AbilityManager abilityManager;
    private AbilityCooldownManager cooldownManager;
    private PacketHandler packetHandler;

    /**
     * Gets the singleton instance of the Smite plugin.
     *
     * @return the plugin instance
     */
    public static Smite getInstance() {
        return instance;
    }

    /**
     * Called when the plugin is enabled during server startup or reload.
     * Initializes all managers, registers abilities, and sets up listeners.
     */
    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Initializing Smite plugin...");

        initializeManagers();

        // Register abilities
        registerAbilities();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Load cooldowns from storage
        cooldownManager.loadCooldowns();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info(String.format("Smite plugin enabled successfully in %dms", loadTime));
        getLogger().info(String.format("Loaded %d abilities", abilityRegistry.getRegisteredAbilities().size()));
    }

    /**
     * Called when the plugin is disabled during server shutdown or reload.
     * Saves all cooldowns and performs cleanup operations.
     */
    @Override
    public void onDisable() {
        getLogger().info("Disabling Smite plugin...");

        // Save cooldowns if persistence is enabled
        if (configManager.isPersistCooldowns()) {
            cooldownManager.saveCooldowns();
            getLogger().info("Cooldown data saved successfully");
        }

        // Clear active abilities
        abilityManager.clearAllActiveAbilities();

        getLogger().info("Smite plugin disabled successfully");
    }

    /**
     * Initializes all core managers required for plugin operation.
     * This includes configuration, registry, ability management, and packet handling.
     */
    private void initializeManagers() {
        getLogger().info("Initializing core managers...");

        // Configuration must be loaded first
        configManager = new ConfigManager(this);
        configManager.loadConfigurations();

        // Initialize ability system
        abilityRegistry = new AbilityRegistry();
        cooldownManager = new AbilityCooldownManager(this);
        abilityManager = new AbilityManager(this, cooldownManager);

        // Initialize packet handler
        packetHandler = new PacketHandler(this);

        getLogger().info("Core managers initialized successfully");
    }

    /**
     * Registers all built-in abilities with the ability registry.
     * Custom abilities can be registered through the API after plugin initialization.
     */
    private void registerAbilities() {
        getLogger().info("Registering abilities...");

        // Register Heavenly Smite
        HeavenlySmiteAbility heavenlySmite = new HeavenlySmiteAbility(this);
        abilityRegistry.register(heavenlySmite);
        getLogger().info("Registered ability: " + heavenlySmite.getDisplayName());

        // Register Demonic Spark
        DemonicSparkAbility demonicSpark = new DemonicSparkAbility(this);
        abilityRegistry.register(demonicSpark);
        getLogger().info("Registered ability: " + demonicSpark.getDisplayName());
    }

    /**
     * Registers all event listeners required for ability functionality.
     * This includes combat events and ability-specific listeners.
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");

        getServer().getPluginManager().registerEvents(
                new CombatListener(this, abilityManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new AbilityListener(this),
                this
        );

        getLogger().info("Event listeners registered successfully");
    }

    /**
     * Registers all plugin commands and their executors.
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");

        SmiteCommand smiteCmd = new SmiteCommand(this);
        getCommand("smite").setExecutor(smiteCmd);
        getCommand("smite").setTabCompleter(smiteCmd);

        getCommand("demonicspark").setExecutor(new DemonicSparkCommand(this));

        getLogger().info("Commands registered successfully");
    }

    /**
     * Gets the configuration manager instance.
     *
     * @return the configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the ability registry instance.
     *
     * @return the ability registry
     */
    public AbilityRegistry getAbilityRegistry() {
        return abilityRegistry;
    }

    /**
     * Gets the ability manager instance.
     *
     * @return the ability manager
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Gets the cooldown manager instance.
     *
     * @return the cooldown manager
     */
    public AbilityCooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Gets the packet handler instance.
     *
     * @return the packet handler
     */
    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    /**
     * Reloads all plugin configurations and reinitialized abilities.
     * This method is safe to call during runtime and will not interrupt active abilities.
     *
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadPlugin() {
        try {
            getLogger().info("Reloading plugin configuration...");

            // Reload configurations
            configManager.reloadConfigurations();

            // Clear and re-register abilities with new config
            abilityRegistry.unregisterAll();
            registerAbilities();

            getLogger().info("Plugin reloaded successfully");
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}