package me.sunmc.smite.api.ability;

import me.sunmc.smite.Smite;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldown timers for all abilities across all players.
 *
 * <p>This manager provides thread-safe cooldown tracking with optional persistence
 * across server restarts. Cooldowns are stored per-player per-ability and can be
 * saved to disk if configured.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Thread-safe cooldown tracking</li>
 *   <li>Optional persistence across restarts</li>
 *   <li>Automatic cleanup of expired cooldowns</li>
 *   <li>Efficient lookup and update operations</li>
 * </ul>
 *
 * @author SunMC
 * @version 1.0.0
 * @see Ability
 */
public final class AbilityCooldownManager {

    private final Smite plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final File cooldownFile;

    /**
     * Constructs a new cooldown manager.
     *
     * @param plugin the plugin instance
     */
    public AbilityCooldownManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");

        // Schedule periodic cleanup of expired cooldowns every 60 seconds
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::cleanupExpiredCooldowns,
                1200L, // 60 seconds initial delay
                1200L  // 60 seconds period
        );
    }

    /**
     * Sets a cooldown for a specific ability for a player.
     *
     * @param playerId       the UUID of the player
     * @param abilityName    the name of the ability
     * @param durationMillis the cooldown duration in milliseconds
     */
    public void setCooldown(@NotNull UUID playerId, @NotNull String abilityName, long durationMillis) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(abilityName, "Ability name cannot be null");

        if (durationMillis <= 0) {
            return;
        }

        long expirationTime = System.currentTimeMillis() + durationMillis;
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(abilityName.toLowerCase(), expirationTime);
    }

    /**
     * Checks if an ability is on cooldown for a player.
     *
     * @param playerId    the UUID of the player
     * @param abilityName the name of the ability
     * @return true if the ability is on cooldown, false otherwise
     */
    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String abilityName) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(abilityName, "Ability name cannot be null");

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return false;
        }

        Long expirationTime = playerCooldowns.get(abilityName.toLowerCase());
        if (expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expirationTime) {
            playerCooldowns.remove(abilityName.toLowerCase());
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining cooldown time in milliseconds for an ability.
     *
     * @param playerId    the UUID of the player
     * @param abilityName the name of the ability
     * @return the remaining cooldown time in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(@NotNull UUID playerId, @NotNull String abilityName) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(abilityName, "Ability name cannot be null");

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }

        Long expirationTime = playerCooldowns.get(abilityName.toLowerCase());
        if (expirationTime == null) {
            return 0L;
        }

        long remaining = expirationTime - System.currentTimeMillis();
        if (remaining <= 0) {
            playerCooldowns.remove(abilityName.toLowerCase());
            return 0L;
        }

        return remaining;
    }

    /**
     * Gets the remaining cooldown time in seconds for an ability.
     *
     * @param playerId    the UUID of the player
     * @param abilityName the name of the ability
     * @return the remaining cooldown time in seconds, rounded up
     */
    public int getRemainingCooldownSeconds(@NotNull UUID playerId, @NotNull String abilityName) {
        long remainingMillis = getRemainingCooldown(playerId, abilityName);
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    /**
     * Clears the cooldown for a specific ability for a player.
     *
     * @param playerId    the UUID of the player
     * @param abilityName the name of the ability
     */
    public void clearCooldown(@NotNull UUID playerId, @NotNull String abilityName) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(abilityName, "Ability name cannot be null");

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(abilityName.toLowerCase());
        }
    }

    /**
     * Clears all cooldowns for a specific player.
     *
     * @param playerId the UUID of the player
     */
    public void clearAllCooldowns(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        cooldowns.remove(playerId);
    }

    /**
     * Clears all cooldowns for all players.
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * Gets all active cooldowns for a specific player.
     *
     * @param playerId the UUID of the player
     * @return an unmodifiable map of ability names to expiration times
     */
    @NotNull
    public Map<String, Long> getPlayerCooldowns(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return Collections.emptyMap();
        }

        return Map.copyOf(playerCooldowns);
    }

    /**
     * Removes all expired cooldowns from the system.
     * This method is called automatically on a scheduled task.
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();

        cooldowns.values().forEach(playerCooldowns ->
                playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime)
        );

        // Remove empty player entries
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Loads cooldown data from disk.
     * This should be called during plugin initialization.
     */
    public void loadCooldowns() {
        if (!plugin.getConfigManager().isPersistCooldowns()) {
            return;
        }

        if (!cooldownFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(cooldownFile);
        long currentTime = System.currentTimeMillis();

        for (String playerIdStr : config.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                Map<String, Long> playerCooldowns = new ConcurrentHashMap<>();

                for (String abilityName : Objects.requireNonNull(config.getConfigurationSection(playerIdStr)).getKeys(false)) {
                    long expirationTime = config.getLong(playerIdStr + "." + abilityName);

                    // Only load cooldowns that haven't expired
                    if (expirationTime > currentTime) {
                        playerCooldowns.put(abilityName.toLowerCase(), expirationTime);
                    }
                }

                if (!playerCooldowns.isEmpty()) {
                    cooldowns.put(playerId, playerCooldowns);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in cooldowns file: " + playerIdStr);
            }
        }

        plugin.getLogger().info("Loaded cooldown data for " + cooldowns.size() + " players");
    }

    /**
     * Saves cooldown data to disk.
     * This should be called during plugin shutdown if persistence is enabled.
     */
    public void saveCooldowns() {
        if (!plugin.getConfigManager().isPersistCooldowns()) {
            return;
        }

        FileConfiguration config = new YamlConfiguration();
        long currentTime = System.currentTimeMillis();

        cooldowns.forEach((playerId, playerCooldowns) ->
                playerCooldowns.forEach((abilityName, expirationTime) -> {
                    // Only save cooldowns that haven't expired
                    if (expirationTime > currentTime) {
                        config.set(playerId.toString() + "." + abilityName, expirationTime);
                    }
                }));

        try {
            config.save(cooldownFile);
            plugin.getLogger().info("Saved cooldown data for " + cooldowns.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save cooldown data: " + e.getMessage());
        }
    }
}