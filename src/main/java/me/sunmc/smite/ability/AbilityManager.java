package me.sunmc.smite.ability;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.Ability;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationResult;
import me.sunmc.smite.util.CooldownManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all abilities and player ability states.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public final class AbilityManager {

    private final Smite plugin;
    private final Map<String, Ability> abilities;
    private final Map<UUID, PlayerAbilityData> playerData;
    private final CooldownManager cooldownManager;

    public AbilityManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.abilities = new ConcurrentHashMap<>();
        this.playerData = new ConcurrentHashMap<>();
        this.cooldownManager = new CooldownManager();
    }

    /**
     * Registers an ability to the manager.
     */
    public void registerAbility(@NotNull Ability ability) {
        if (abilities.putIfAbsent(ability.getId(), ability) == null) {
            plugin.getLogger().info("Registered ability: " + ability.getDisplayName());
        } else {
            plugin.getLogger().warning("Ability already registered: " + ability.getId());
        }
    }

    /**
     * Unregisters an ability from the manager.
     */
    public void unregisterAbility(@NotNull String abilityId) {
        if (abilities.remove(abilityId) != null) {
            plugin.getLogger().info("Unregistered ability: " + abilityId);
        }
    }

    /**
     * Gets an ability by its ID.
     */
    @Nullable
    public Ability getAbility(@NotNull String abilityId) {
        return abilities.get(abilityId);
    }

    /**
     * Gets all registered abilities as an immutable map.
     */
    @Contract(pure = true)
    @NotNull
    public @Unmodifiable Map<String, Ability> getAllAbilities() {
        return Map.copyOf(abilities);
    }

    /**
     * Activates an ability for a player.
     */
    @NotNull
    public CompletableFuture<ActivationResult> activateAbility(
            @NotNull Player player,
            @NotNull String abilityId,
            @NotNull ActivationContext context) {

        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            return CompletableFuture.completedFuture(
                    ActivationResult.failure("Ability not found"));
        }

        return ability.activate(player, context);
    }

    /**
     * Gets or creates player ability data.
     */
    @NotNull
    public PlayerAbilityData getPlayerData(@NotNull Player player) {
        return playerData.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerAbilityData(uuid));
    }

    /**
     * Removes player data.
     */
    public void removePlayerData(@NotNull Player player) {
        PlayerAbilityData data = playerData.remove(player.getUniqueId());
        if (data != null) {
            data.clearData();
        }
    }

    /**
     * Gets the cooldown manager.
     */
    @NotNull
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Shuts down the ability manager.
     */
    public void shutdown() {
        playerData.values().forEach(PlayerAbilityData::clearData);
        playerData.clear();
        cooldownManager.clearAll();
    }

    /**
     * Represents player-specific ability data.
     * Thread-safe implementation.
     */
    public static final class PlayerAbilityData {

        private final UUID playerId;
        private final Map<String, Object> data;
        private volatile String activeAbility;

        PlayerAbilityData(@NotNull UUID playerId) {
            this.playerId = playerId;
            this.data = new ConcurrentHashMap<>();
        }

        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }

        public void setData(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        @Nullable
        public Object getData(@NotNull String key) {
            return data.get(key);
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getData(@NotNull String key, @NotNull Class<T> type) {
            Object value = data.get(key);
            return type.isInstance(value) ? (T) value : null;
        }

        public void removeData(@NotNull String key) {
            data.remove(key);
        }

        @Nullable
        public String getActiveAbility() {
            return activeAbility;
        }

        public void setActiveAbility(@Nullable String abilityId) {
            this.activeAbility = abilityId;
        }

        void clearData() {
            data.clear();
            activeAbility = null;
        }
    }
}