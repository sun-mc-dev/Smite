package me.sunmc.smite;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.sunmc.smite.ability.AbilityManager;
import me.sunmc.smite.ability.impl.DemonicSparkAbility;
import me.sunmc.smite.ability.impl.HeavenlySmiteAbility;
import me.sunmc.smite.command.SmiteCommand;
import me.sunmc.smite.config.ConfigManager;
import me.sunmc.smite.listener.CombatListener;
import me.sunmc.smite.listener.PlayerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Smite extends JavaPlugin {

    private static Smite instance;
    private ConfigManager configManager;
    private AbilityManager abilityManager;

    public static Smite getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();

        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.abilityManager = new AbilityManager(this);

        // Register abilities
        registerAbilities();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        long loadTime = System.currentTimeMillis() - startTime;
        getServer().getConsoleSender().sendMessage(
                Component.text("[Smite] ", NamedTextColor.GOLD)
                        .append(Component.text("Plugin enabled in " + loadTime + "ms", NamedTextColor.GREEN))
        );
    }

    @Override
    public void onDisable() {
        if (abilityManager != null) {
            abilityManager.shutdown();
        }

        PacketEvents.getAPI().terminate();

        getServer().getConsoleSender().sendMessage(
                Component.text("[Smite] ", NamedTextColor.GOLD)
                        .append(Component.text("Plugin disabled", NamedTextColor.RED))
        );
    }

    private void registerAbilities() {
        abilityManager.registerAbility(new HeavenlySmiteAbility(this));
        abilityManager.registerAbility(new DemonicSparkAbility(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        getCommand("smite").setExecutor(new SmiteCommand(this));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
}