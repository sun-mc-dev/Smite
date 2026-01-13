package me.sunmc.smite.database;

import me.sunmc.smite.Smite;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages SQLite database operations for player data persistence.
 */
public class DatabaseManager {

    private final Smite plugin;
    private Connection connection;

    public DatabaseManager(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the database connection and creates tables.
     */
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "smite.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            createTables();

            plugin.getLogger().info("Database initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates necessary database tables.
     */
    private void createTables() throws SQLException {
        String playerDataTable = """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    selected_cell TEXT,
                    cell_locked BOOLEAN DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """;

        String keybindsTable = """
                CREATE TABLE IF NOT EXISTS player_keybinds (
                    uuid TEXT NOT NULL,
                    ability_id TEXT NOT NULL,
                    keybind TEXT NOT NULL,
                    PRIMARY KEY (uuid, ability_id),
                    FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
                )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playerDataTable);
            stmt.execute(keybindsTable);
        }
    }

    /**
     * Saves or updates player data asynchronously.
     *
     * @param player       The player
     * @param selectedCell The selected cell ID (can be null)
     * @param cellLocked   Whether the cell is locked
     * @return CompletableFuture that completes when save is done
     */
    @NotNull
    public CompletableFuture<Void> savePlayerData(@NotNull Player player, @Nullable String selectedCell, boolean cellLocked) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                    INSERT INTO player_data (uuid, player_name, selected_cell, cell_locked, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name = excluded.player_name,
                        selected_cell = excluded.selected_cell,
                        cell_locked = excluded.cell_locked,
                        updated_at = excluded.updated_at
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                long currentTime = System.currentTimeMillis();
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setString(3, selectedCell);
                stmt.setBoolean(4, cellLocked);
                stmt.setLong(5, currentTime);
                stmt.setLong(6, currentTime);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Loads player data asynchronously.
     *
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerData, or null if not found
     */
    @NotNull
    public CompletableFuture<PlayerData> loadPlayerData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getString("selected_cell"),
                            rs.getBoolean("cell_locked"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at")
                    );
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Saves a player's keybind asynchronously.
     *
     * @param uuid      Player UUID
     * @param abilityId Ability ID
     * @param keybind   Keybind string (e.g., "KEY_R", "KEY_F")
     * @return CompletableFuture that completes when save is done
     */
    @NotNull
    public CompletableFuture<Void> saveKeybind(@NotNull UUID uuid, @NotNull String abilityId, @NotNull String keybind) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                    INSERT INTO player_keybinds (uuid, ability_id, keybind)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uuid, ability_id) DO UPDATE SET
                        keybind = excluded.keybind
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, abilityId);
                stmt.setString(3, keybind);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save keybind: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Loads a player's keybind asynchronously.
     *
     * @param uuid      Player UUID
     * @param abilityId Ability ID
     * @return CompletableFuture with keybind string, or null if not found
     */
    @NotNull
    public CompletableFuture<String> loadKeybind(@NotNull UUID uuid, @NotNull String abilityId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT keybind FROM player_keybinds WHERE uuid = ? AND ability_id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, abilityId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("keybind");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load keybind: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Represents player data from the database.
     */
    public record PlayerData(
            UUID uuid,
            String playerName,
            String selectedCell,
            boolean cellLocked,
            long createdAt,
            long updatedAt
    ) {
    }
}