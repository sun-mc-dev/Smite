package me.sunmc.smite.ability.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.AbilityManager;
import me.sunmc.smite.ability.api.AbilityType;
import me.sunmc.smite.ability.api.AbstractAbility;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Heavenly Smite - Lightning-based ability triggered by a three-hit critical chain.
 */
public class HeavenlySmiteAbility extends AbstractAbility {

    private static final String CRIT_COUNT_KEY = "heavenly_smite_crit_count";
    private static final String LAST_CRIT_TIME_KEY = "heavenly_smite_last_crit";
    private static final int REQUIRED_CRITS = 3;
    private static final long CRIT_CHAIN_TIMEOUT = 3000; // 3 seconds
    private static final double DAMAGE = 10.0; // 5 hearts

    public HeavenlySmiteAbility(@NotNull Smite plugin) {
        super(
                plugin,
                "heavenly_smite",
                "Heavenly Smite",
                45,
                "A powerful lightning strike triggered by landing three critical hits in succession",
                AbilityType.OFFENSIVE
        );
    }

    private static @NotNull WrapperPlayServerParticle getWrapperPlayServerParticle(@NotNull Location center, int i) {
        double angle = (2 * Math.PI * i) / 60;
        double radius = 2.0;
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);
        double y = center.getY() + 1.0;

        WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                (Particle<?>) ParticleTypes.ELECTRIC_SPARK,
                true,
                new Vector3d(x, y, z),
                new Vector3f((float) (Math.cos(angle) * 0.5), 0.1f, (float) (Math.sin(angle) * 0.5)),
                0.2f,
                3
        );
        return packet;
    }

    /**
     * Called when a player lands a critical hit.
     *
     * @param player The player
     * @param target The target entity
     */
    public void onCriticalHit(@NotNull Player player, @NotNull Entity target) {
        if (!enabled || !(target instanceof LivingEntity)) {
            return;
        }

        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);

        // Get current crit count
        Integer critCount = (Integer) data.getData(CRIT_COUNT_KEY);
        Long lastCritTime = (Long) data.getData(LAST_CRIT_TIME_KEY);

        long currentTime = System.currentTimeMillis();

        // Reset if timeout exceeded
        if (lastCritTime != null && (currentTime - lastCritTime) > CRIT_CHAIN_TIMEOUT) {
            critCount = 0;
        }

        critCount = (critCount == null ? 0 : critCount) + 1;
        data.setData(CRIT_COUNT_KEY, critCount);
        data.setData(LAST_CRIT_TIME_KEY, currentTime);

        // Play crit particles
        playCritParticles(player, critCount);

        // Check if we've reached the required crits
        if (critCount >= REQUIRED_CRITS) {
            data.setData(CRIT_COUNT_KEY, 0);

            ActivationContext context = new ActivationContext(me.sunmc.smite.ability.api.ActivationTrigger.CRITICAL_HIT);
            context.setTarget(target);
            activate(player, context);
        } else {
            // Show progress
            sendActionBar(player, Component.text("Crit Chain: ", NamedTextColor.YELLOW)
                    .append(Component.text(critCount + "/" + REQUIRED_CRITS, NamedTextColor.GOLD)));
        }
    }

    @Override
    protected boolean canActivateCustom(@NotNull Player player, @NotNull ActivationContext context) {
        return context.getTarget() instanceof LivingEntity;
    }

    @Override
    protected CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context) {
        Entity targetEntity = context.getTarget();
        if (!(targetEntity instanceof LivingEntity target)) {
            return CompletableFuture.completedFuture(ActivationResult.failure("Invalid target"));
        }

        // Schedule on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location targetLoc = target.getLocation();

            // Play explosion particles at final crit
            playExplosionParticles(player, targetLoc);

            // Spawn lightning
            player.getWorld().strikeLightningEffect(targetLoc);

            // Play sound
            player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

            // Deal damage (bypasses armor)
            target.damage(DAMAGE, player);
            target.setNoDamageTicks(0);

            // Apply knockback
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            direction.multiply(0.5);
            direction.setY(0.3);
            target.setVelocity(direction);
        });

        sendActivationMessage(player, "Lightning Strike!");
        return CompletableFuture.completedFuture(ActivationResult.success());
    }

    /**
     * Plays circular cascading particles during critical hits.
     */
    private void playCritParticles(@NotNull Player player, int critNumber) {
        Location loc = player.getLocation();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int particleCount = critNumber < REQUIRED_CRITS ? 20 : 0; // Don't play on final crit
            double radius = 1.0;

            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI * i) / particleCount;
                double x = loc.getX() + radius * Math.cos(angle);
                double z = loc.getZ() + radius * Math.sin(angle);
                double y = loc.getY() + 2.0 - (0.1 * i); // Cascade downward

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        (Particle<?>) ParticleTypes.CRIT,
                        true,
                        new Vector3d(x, y, z),
                        new Vector3f(0, 0, 0),
                        0.0f,
                        1
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        });
    }

    /**
     * Plays explosion particles at final critical hit.
     */
    private void playExplosionParticles(@NotNull Player player, @NotNull Location center) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Circular explosion outward
            for (int i = 0; i < 60; i++) {
                final WrapperPlayServerParticle packet = getWrapperPlayServerParticle(center, i);

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }

            // Upward burst
            for (int i = 0; i < 30; i++) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetZ = (Math.random() - 0.5) * 2;

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        (Particle<?>) ParticleTypes.CRIT,
                        true,
                        new Vector3d(
                                center.getX() + offsetX,
                                center.getY() + 1.0,
                                center.getZ() + offsetZ
                        ),
                        new Vector3f((float) offsetX * 0.2f, 0.5f, (float) offsetZ * 0.2f),
                        0.3f,
                        2
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        });
    }
}