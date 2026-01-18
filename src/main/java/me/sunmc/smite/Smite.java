package me.sunmc.smite;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.sunmc.smite.ability.AbilityManager;
import me.sunmc.smite.ability.cell.Cell;
import me.sunmc.smite.ability.cell.CellManager;
import me.sunmc.smite.ability.impl.DemonicSparkAbility;
import me.sunmc.smite.ability.impl.HeavenlySmiteAbility;
import me.sunmc.smite.command.SmiteCommand;
import me.sunmc.smite.config.ConfigManager;
import me.sunmc.smite.database.DatabaseManager;
import me.sunmc.smite.gui.PacketGUIManager;
import me.sunmc.smite.listener.CombatListener;
import me.sunmc.smite.listener.PlayerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Main plugin class for Smite - High-performance ability system.
 * Uses modern Java 21 features and best practices.
 */
public final class Smite extends JavaPlugin {

    private static Smite instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private CellManager cellManager;
    private AbilityManager abilityManager;
    private PacketGUIManager guiManager;

    @NotNull
    public static Smite getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Plugin not initialized");
        }
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

        try {

            PacketEvents.getAPI().init();

            this.configManager = new ConfigManager(this);
            this.databaseManager = new DatabaseManager(this);
            this.cellManager = new CellManager(this);
            this.abilityManager = new AbilityManager(this);
            this.guiManager = new PacketGUIManager(this);

            databaseManager.initialize();

            registerAbilities();
            registerCells();
            registerListeners();
            registerCommands();

            long loadTime = System.currentTimeMillis() - startTime;

            getServer().getConsoleSender().sendMessage(
                    Component.text("[Smite] ", NamedTextColor.GOLD)
                            .append(Component.text("Plugin enabled in " + loadTime + "ms", NamedTextColor.GREEN))
            );

            getServer().getConsoleSender().sendMessage(
                    Component.text("[Smite] ", NamedTextColor.GOLD)
                            .append(Component.text("Using Java " + Runtime.version(), NamedTextColor.AQUA))
            );
        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Shutdown managers in reverse order
            if (guiManager != null) {
                guiManager.shutdown();
            }

            if (abilityManager != null) {
                abilityManager.shutdown();
            }

            if (databaseManager != null) {
                databaseManager.close();
            }

            PacketEvents.getAPI().terminate();

            getServer().getConsoleSender().sendMessage(
                    Component.text("[Smite] ", NamedTextColor.GOLD)
                            .append(Component.text("Plugin disabled", NamedTextColor.RED))
            );
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            instance = null;
        }
    }

    /**
     * Registers all abilities.
     */
    private void registerAbilities() {
        abilityManager.registerAbility(new HeavenlySmiteAbility(this));
        abilityManager.registerAbility(new DemonicSparkAbility(this));
    }

    /**
     * Registers all cells.
     */
    private void registerCells() {
        Cell sparkCell = new Cell(
                "spark_cell",
                "Spark Cell",
                "Harness the power of lightning and darkness",
                "heavenly_smite",
                "demonic_spark",
                "NETHER_STAR",
                true
        );

        cellManager.registerCell(sparkCell);
    }

    /**
     * Registers event listeners.
     */
    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CombatListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    /**
     * Registers commands.
     */
    private void registerCommands() {
        var smiteCommand = new SmiteCommand(this);
        Objects.requireNonNull(getCommand("smite")).setExecutor(smiteCommand);
        Objects.requireNonNull(getCommand("smite")).setTabCompleter(smiteCommand);
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @NotNull
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    @NotNull
    public CellManager getCellManager() {
        return cellManager;
    }

    @NotNull
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @NotNull
    public PacketGUIManager getGuiManager() {
        return guiManager;
    }
}