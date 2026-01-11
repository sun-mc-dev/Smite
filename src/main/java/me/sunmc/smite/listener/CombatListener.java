package me.sunmc.smite.listener;

import me.sunmc.smite.Smite;
import me.sunmc.smite.abilities.DemonicSparkAbility;
import me.sunmc.smite.abilities.HeavenlySmiteAbility;
import me.sunmc.smite.api.ability.AbilityContext;
import me.sunmc.smite.api.ability.AbilityManager;
import me.sunmc.smite.api.ability.AbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to combat-related events for ability triggering.
 *
 * <p>This listener tracks critical hit chains for Heavenly Smite and
 * manages damage accumulation for Demonic Spark. It integrates with
 * the ability management system to handle activation and state.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class CombatListener implements Listener {

    private final Smite plugin;
    private final AbilityManager abilityManager;
    private final Map<UUID, CritChain> critChains;

    /**
     * Constructs a new combat listener.
     *
     * @param plugin         the plugin instance
     * @param abilityManager the ability manager
     */
    public CombatListener(@NotNull Smite plugin, @NotNull AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        this.critChains = new HashMap<>();

        // Schedule cleanup of old crit chains every 10 seconds
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::cleanupOldChains,
                200L,
                200L
        );
    }

    /**
     * Handles player damage to entities for ability triggering.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Check if Demonic Spark is active (damage accumulation mode)
        if (abilityManager.isAbilityActive(playerId, "demonic_spark")) {
            handleDemonicSparkAccumulation(event, player, target);
            return;
        }

        // Check for critical hits for Heavenly Smite
        // In Minecraft, critical hits occur when:
        // - Player is falling (velocity.y < 0)
        // - Player is not on ground
        // - Player is not sprinting
        // - Player is not in water
        // - Player has no blindness effect
        // - Attack is not a sweep attack

        boolean isCriticalHit = player.getFallDistance() > 0.0F &&
                !player.isOnGround() &&
                !player.isSprinting() &&
                !player.isInWater() &&
                player.getAttackCooldown() > 0.9F;

        if (isCriticalHit) {
            handleCriticalHit(player, target, event.getFinalDamage());
        } else {
            // Reset crit chain on non-crit hit
            critChains.remove(playerId);
        }
    }

    /**
     * Handles critical hit detection and chain tracking for Heavenly Smite.
     */
    private void handleCriticalHit(@NotNull Player player, @NotNull LivingEntity target, double damage) {
        UUID playerId = player.getUniqueId();

        CritChain chain = critChains.computeIfAbsent(playerId, k -> new CritChain());
        chain.addCrit(target.getUniqueId());

        // Get Heavenly Smite ability
        plugin.getAbilityRegistry().getAbility(HeavenlySmiteAbility.class).ifPresent(ability -> {
            HeavenlySmiteAbility heavenlySmite = (HeavenlySmiteAbility) ability;
            int requiredCrits = heavenlySmite.getRequiredCrits();

            // Trigger per-crit particles
            heavenlySmite.spawnCritParticles(player, chain.getCount() == requiredCrits);

            // Check if we've reached the required crit count
            if (chain.getCount() >= requiredCrits) {
                AbilityContext context = new AbilityContext.Builder(player)
                        .target(target)
                        .trigger(AbilityTrigger.CRITICAL_HIT)
                        .metadata("crit_count", chain.getCount())
                        .metadata("last_damage", damage)
                        .build();

                // Activate Heavenly Smite
                if (abilityManager.activateAbility(heavenlySmite, context)) {
                    critChains.remove(playerId); // Reset chain after successful activation
                }
            }
        });
    }

    /**
     * Handles damage accumulation during Demonic Spark active period.
     */
    private void handleDemonicSparkAccumulation(@NotNull EntityDamageByEntityEvent event,
                                                @NotNull Player player, @NotNull LivingEntity target) {

        plugin.getAbilityRegistry().getAbility(DemonicSparkAbility.class).ifPresent(ability -> {
            DemonicSparkAbility demonicSpark = (DemonicSparkAbility) ability;

            // Cancel the original damage
            event.setCancelled(true);

            // Accumulate damage
            double damage = event.getFinalDamage();
            demonicSpark.accumulateDamage(player.getUniqueId(), damage);

            // Play accumulation effects
            demonicSpark.playAccumulationEffects(player, target.getLocation());
        });
    }

    /**
     * Cleans up player data when they quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        critChains.remove(playerId);
        abilityManager.clearPlayerAbilities(event.getPlayer());
    }

    /**
     * Removes crit chains that are older than 5 seconds.
     */
    private void cleanupOldChains() {
        long currentTime = System.currentTimeMillis();
        critChains.entrySet().removeIf(entry ->
                currentTime - entry.getValue().getLastHitTime() > 5000
        );
    }

    /**
     * Represents a chain of critical hits for a player.
     */
    private static final class CritChain {
        private int count;
        private UUID lastTarget;
        private long lastHitTime;

        /**
         * Constructs a new crit chain.
         */
        CritChain() {
            this.count = 0;
            this.lastHitTime = System.currentTimeMillis();
        }

        /**
         * Adds a critical hit to the chain.
         *
         * @param targetId the UUID of the target entity
         */
        void addCrit(UUID targetId) {
            // Reset chain if targeting a different entity or too much time passed
            if (lastTarget != null && !lastTarget.equals(targetId)) {
                count = 0;
            }

            count++;
            lastTarget = targetId;
            lastHitTime = System.currentTimeMillis();
        }

        /**
         * Gets the current crit count.
         *
         * @return the count
         */
        int getCount() {
            return count;
        }

        /**
         * Gets the time of the last critical hit.
         *
         * @return the timestamp in milliseconds
         */
        long getLastHitTime() {
            return lastHitTime;
        }
    }
}