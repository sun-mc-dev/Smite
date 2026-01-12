package me.sunmc.smite.ability;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.Ability;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationResult;
import me.sunmc.smite.util.CooldownManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all abilities and player ability states.
 */
public class AbilityManager {

    private final Smite plugin;
    private final Map<String, Ability> abilities;
    private final Map<UUID, PlayerAbilityData> playerData;
    private final CooldownManager cooldownManager;

    public AbilityManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.abilities = new HashMap<>();
        this.playerData = new ConcurrentHashMap<>();
        this.cooldownManager = new CooldownManager();
    }

    /**
     * Registers an ability to the manager.
     *
     * @param ability The ability to register
     */
    public void registerAbility(@NotNull Ability ability) {
        abilities.put(ability.getId(), ability);
        plugin.getLogger().info("Registered ability: " + ability.getDisplayName());
    }

    /**
     * Unregisters an ability from the manager.
     *
     * @param abilityId The ability ID to unregister
     */
    public void unregisterAbility(@NotNull String abilityId) {
        abilities.remove(abilityId);
    }

    /**
     * Gets an ability by its ID.
     *
     * @param abilityId The ability ID
     * @return The ability, or null if not found
     */
    @Nullable
    public Ability getAbility(@NotNull String abilityId) {
        return abilities.get(abilityId);
    }

    /**
     * Gets all registered abilities.
     *
     * @return Map of ability IDs to abilities
     */
    @NotNull
    public Map<String, Ability> getAllAbilities() {
        return new HashMap<>(abilities);
    }

    /**
     * Activates an ability for a player.
     *
     * @param player    The player
     * @param abilityId The ability ID
     * @param context   The activation context
     * @return A CompletableFuture with the activation result
     */
    @NotNull
    public CompletableFuture<ActivationResult> activateAbility(@NotNull Player player, @NotNull String abilityId,
                                                               @NotNull ActivationContext context) {
        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            return CompletableFuture.completedFuture(ActivationResult.failure("Ability not found"));
        }

        return ability.activate(player, context);
    }

    /**
     * Gets or creates player ability data.
     *
     * @param player The player
     * @return The player's ability data
     */
    @NotNull
    public PlayerAbilityData getPlayerData(@NotNull Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerAbilityData(player.getUniqueId()));
    }

    /**
     * Removes player data.
     *
     * @param player The player
     */
    public void removePlayerData(@NotNull Player player) {
        playerData.remove(player.getUniqueId());
    }

    /**
     * Gets the cooldown manager.
     *
     * @return The cooldown manager
     */
    @NotNull
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Shuts down the ability manager.
     */
    public void shutdown() {
        playerData.clear();
        cooldownManager.clearAll();
    }

    /**
     * Represents player-specific ability data.
     */
    public static class PlayerAbilityData {
        private final UUID playerId;
        private final Map<String, Object> data;
        private String activeAbility;

        public PlayerAbilityData(@NotNull UUID playerId) {
            this.playerId = playerId;
            this.data = new ConcurrentHashMap<>();
        }

        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }

        public void setData(@NotNull String key, @Nullable Object value) {
            data.put(key, value);
        }

        @Nullable
        public Object getData(@NotNull String key) {
            return data.get(key);
        }

        public void removeData(@NotNull String key) {
            data.remove(key);
        }

        @NotNull
        public Optional<String> getActiveAbility() {
            return Optional.ofNullable(activeAbility);
        }

        public void setActiveAbility(@Nullable String abilityId) {
            this.activeAbility = abilityId;
        }

        public void clearData() {
            data.clear();
            activeAbility = null;
        }
    }
}