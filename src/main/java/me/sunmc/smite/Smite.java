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
import me.sunmc.smite.keybind.KeybindManager;
import me.sunmc.smite.listener.CombatListener;
import me.sunmc.smite.listener.PlayerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Main plugin class for Smite - High-performance ability system.
 */
public final class Smite extends JavaPlugin {

    private static Smite instance;
    private ConfigManager configManager;
    private AbilityManager abilityManager;
    private CellManager cellManager;
    private DatabaseManager databaseManager;
    private KeybindManager keybindManager;
    private PacketGUIManager guiManager;

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

        PacketEvents.getAPI().init();

        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.cellManager = new CellManager(this);
        this.abilityManager = new AbilityManager(this);
        this.keybindManager = new KeybindManager(this);
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
                        .append(Component.text("Packet-based systems initialized", NamedTextColor.AQUA))
        );
    }

    @Override
    public void onDisable() {
        if (abilityManager != null) {
            abilityManager.shutdown();
        }

        if (keybindManager != null) {
            keybindManager.shutdown();
        }

        if (guiManager != null) {
            guiManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        PacketEvents.getAPI().terminate();

        getServer().getConsoleSender().sendMessage(
                Component.text("[Smite] ", NamedTextColor.GOLD)
                        .append(Component.text("Plugin disabled", NamedTextColor.RED))
        );
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
        // Spark Cell - Contains Heavenly Smite and Demonic Spark
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
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /**
     * Registers commands.
     */
    private void registerCommands() {
        Objects.requireNonNull(getCommand("smite")).setExecutor(new SmiteCommand(this));
        Objects.requireNonNull(getCommand("smite")).setTabCompleter(new SmiteCommand(this));
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
    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    @NotNull
    public PacketGUIManager getGuiManager() {
        return guiManager;
    }
}