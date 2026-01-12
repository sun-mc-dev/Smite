package me.sunmc.smite.listener;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.impl.DemonicSparkAbility;
import me.sunmc.smite.ability.impl.HeavenlySmiteAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles combat-related events for abilities.
 */
public class CombatListener implements Listener {

    private final Smite plugin;

    public CombatListener(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Check for critical hits (Heavenly Smite)
        if (isCriticalHit(player)) {
            HeavenlySmiteAbility heavenlySmite = (HeavenlySmiteAbility) plugin.getAbilityManager()
                    .getAbility("heavenly_smite");

            if (heavenlySmite != null) {
                heavenlySmite.onCriticalHit(player, event.getEntity());
            }
        }

        // Check for Demonic Spark active
        DemonicSparkAbility demonicSpark = (DemonicSparkAbility) plugin.getAbilityManager()
                .getAbility("demonic_spark");

        if (demonicSpark != null && demonicSpark.isActive(player)) {
            // Store the damage
            double damage = event.getFinalDamage();
            demonicSpark.onDamageDealt(player, damage);

            // Cancel the damage since we're storing it
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the player is performing a critical hit.
     */
    private boolean isCriticalHit(@NotNull Player player) {
        // Critical hit conditions in Minecraft:
        // 1. Player is falling (negative Y velocity)
        // 2. Player is not on ground
        // 3. Player is not in liquid
        // 4. Player is not climbing
        // 5. Player is not sprinting (for sweep attacks)

        return player.getFallDistance() > 0
                && !player.isOnGround()
                && !player.isInWater()
                && !player.isClimbing()
                && player.getVelocity().getY() < 0;
    }
}