package me.sunmc.smite.util;

import me.sunmc.smite.ability.api.Ability;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for abilities.
 */
public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns;

    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Sets a cooldown for a player and ability.
     *
     * @param player  The player
     * @param ability The ability
     * @param seconds The cooldown duration in seconds
     */
    public void setCooldown(@NotNull Player player, @NotNull Ability ability, int seconds) {
        long expiryTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(ability.getId(), expiryTime);
    }

    /**
     * Checks if a player has a cooldown for an ability.
     *
     * @param player  The player
     * @param ability The ability
     * @return true if on cooldown
     */
    public boolean isOnCooldown(@NotNull Player player, @NotNull Ability ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return false;
        }

        Long expiryTime = playerCooldowns.get(ability.getId());
        if (expiryTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expiryTime) {
            playerCooldowns.remove(ability.getId());
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @param player  The player
     * @param ability The ability
     * @return Remaining cooldown in seconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(@NotNull Player player, @NotNull Ability ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }

        Long expiryTime = playerCooldowns.get(ability.getId());
        if (expiryTime == null) {
            return 0;
        }

        long remaining = (expiryTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Clears all cooldowns for a player.
     *
     * @param player The player
     */
    public void clearCooldowns(@NotNull Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * Clears a specific cooldown for a player.
     *
     * @param player  The player
     * @param ability The ability
     */
    public void clearCooldown(@NotNull Player player, @NotNull Ability ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) {
            playerCooldowns.remove(ability.getId());
        }
    }

    /**
     * Clears all cooldowns for all players.
     */
    public void clearAll() {
        cooldowns.clear();
    }
}