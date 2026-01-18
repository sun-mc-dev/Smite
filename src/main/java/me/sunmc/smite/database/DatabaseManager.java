package me.sunmc.smite.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.sunmc.smite.Smite;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages SQLite database operations using HikariCP connection pool.
 * Provides async operations for all database interactions.
 */
public final class DatabaseManager implements AutoCloseable {

    private static final String PLAYER_DATA_TABLE = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                selected_cell TEXT,
                cell_locked BOOLEAN DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """;

    private final Smite plugin;
    private final ExecutorService executor;
    private HikariDataSource dataSource;

    public DatabaseManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    private static @NotNull HikariConfig getHikariConfig(File dataFolder) {
        File dbFile = new File(dataFolder, "smite.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("SmitePool");


        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        return config;
    }

    /**
     * Initializes the database connection pool and creates tables.
     */
    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IllegalStateException("Failed to create plugin data folder");
            }

            final HikariConfig config = getHikariConfig(dataFolder);

            this.dataSource = new HikariDataSource(config);

            createTables();

            plugin.getLogger().info("Database initialized with HikariCP connection pool");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Creates necessary database tables.
     */
    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(PLAYER_DATA_TABLE)) {
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    /**
     * Saves or updates player data asynchronously.
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

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

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
                throw new RuntimeException("Database save failed", e);
            }
        }, executor);
    }

    /**
     * Loads player data asynchronously.
     */
    @NotNull
    public CompletableFuture<PlayerData> loadPlayerData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
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
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            }

            return null;
        }, executor);
    }

    /**
     * Closes the database connection pool and executor.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }

        executor.shutdown();
    }

    /**
     * Represents player data from the database.
     */
    public record PlayerData(
            @NotNull UUID uuid,
            @NotNull String playerName,
            @Nullable String selectedCell,
            boolean cellLocked,
            long createdAt,
            long updatedAt
    ) {
    }
}