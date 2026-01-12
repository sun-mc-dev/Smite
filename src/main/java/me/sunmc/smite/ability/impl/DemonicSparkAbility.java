package me.sunmc.smite.ability.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleData;
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
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonic Spark - Stores damage from hits and unleashes it in one devastating strike.
 */
public class DemonicSparkAbility extends AbstractAbility {

    private static final String ACTIVE_KEY = "demonic_spark_active";
    private static final String STORED_DAMAGE_KEY = "demonic_spark_damage";
    private static final String ACTIVATION_TIME_KEY = "demonic_spark_time";
    private static final int DURATION = 5; // 5 seconds

    public DemonicSparkAbility(@NotNull Smite plugin) {
        super(
                plugin,
                "demonic_spark",
                "Demonic Spark",
                45,
                "Store damage from hits for 5 seconds, then unleash it in one devastating strike",
                AbilityType.HYBRID
        );
    }

    /**
     * Called when a player deals damage while the ability is active.
     *
     * @param player The player
     * @param damage The damage dealt
     */
    public void onDamageDealt(@NotNull Player player, double damage) {
        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);
        Boolean active = (Boolean) data.getData(ACTIVE_KEY);

        if (active != null && active) {
            // Store the damage
            Double storedDamage = (Double) data.getData(STORED_DAMAGE_KEY);
            storedDamage = (storedDamage == null ? 0.0 : storedDamage) + damage;
            data.setData(STORED_DAMAGE_KEY, storedDamage);

            // Play wither sound and particles
            playStorageEffects(player);

            // Update action bar
            sendActionBar(player, Component.text("Stored Damage: ", NamedTextColor.DARK_RED)
                    .append(Component.text(String.format("%.1f", storedDamage), NamedTextColor.RED)));
        }
    }

    /**
     * Checks if the ability is currently active for a player.
     *
     * @param player The player
     * @return true if active
     */
    public boolean isActive(@NotNull Player player) {
        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);
        Boolean active = (Boolean) data.getData(ACTIVE_KEY);
        return active != null && active;
    }

    @Override
    protected boolean canActivateCustom(@NotNull Player player, @NotNull ActivationContext context) {
        return true; // Can always activate if not on cooldown
    }

    @Override
    protected CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context) {
        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);

        // Mark as active
        data.setData(ACTIVE_KEY, true);
        data.setData(STORED_DAMAGE_KEY, 0.0);
        data.setData(ACTIVATION_TIME_KEY, System.currentTimeMillis());

        sendActivationMessage(player, "Storing damage for " + DURATION + " seconds!");

        // Start countdown
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (cancelled.get() || !player.isOnline()) {
                task.cancel();
                return;
            }

            Long activationTime = (Long) data.getData(ACTIVATION_TIME_KEY);
            if (activationTime == null) {
                task.cancel();
                return;
            }

            long elapsed = (System.currentTimeMillis() - activationTime) / 1000;
            long remaining = DURATION - elapsed;

            if (remaining <= 0) {
                task.cancel();
                cancelled.set(true);
                unleashDamage(player);
            } else {
                sendActionBar(player, Component.text("Demonic Spark: ", NamedTextColor.DARK_RED)
                        .append(Component.text(remaining + "s", NamedTextColor.RED)));
            }
        }, 0L, 20L);

        return CompletableFuture.completedFuture(ActivationResult.success());
    }

    /**
     * Unleashes the stored damage.
     */
    private void unleashDamage(@NotNull Player player) {
        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);

        Double storedDamage = (Double) data.getData(STORED_DAMAGE_KEY);
        if (storedDamage == null) {
            storedDamage = 0.0;
        }

        // Clear active state
        data.setData(ACTIVE_KEY, false);
        data.removeData(STORED_DAMAGE_KEY);
        data.removeData(ACTIVATION_TIME_KEY);

        if (storedDamage <= 0) {
            sendActionBar(player, Component.text("No damage stored!", NamedTextColor.RED));
            return;
        }

        final double finalDamage = storedDamage;

        // Find target in front of player
        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity target = getTargetEntity(player, 5.0);

            if (target instanceof LivingEntity livingTarget) {
                // Play massive X-slash particles
                playUnleashParticles(player, livingTarget.getLocation());

                // Deal stored damage
                livingTarget.damage(finalDamage, player);

                // Knockback
                Vector direction = livingTarget.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.multiply(1.5);
                direction.setY(0.5);
                livingTarget.setVelocity(direction);

                // Play sound
                player.getWorld().playSound(livingTarget.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0f, 0.5f);

                sendActionBar(player, Component.text("Unleashed ", NamedTextColor.DARK_RED)
                        .append(Component.text(String.format("%.1f", finalDamage) + " damage!", NamedTextColor.RED)));
            } else {
                sendActionBar(player, Component.text("No target found!", NamedTextColor.RED));
            }
        });
    }

    /**
     * Plays particle effects when storing damage.
     */
    private void playStorageEffects(@NotNull Player player) {
        Location loc = player.getLocation().add(0, 1, 0);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Red diagonal slash
            for (int i = 0; i < 10; i++) {
                double offset = i * 0.2 - 1.0;
                Vector particleVec = loc.toVector().add(new Vector(offset, -offset, 0));

                // Create Particle object from ParticleType
                Particle<?> particle = new Particle<>(ParticleTypes.DAMAGE_INDICATOR, ParticleData.emptyData());

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        particle,
                        true,
                        new Vector3d(
                                particleVec.getX(),
                                particleVec.getY(),
                                particleVec.getZ()
                        ),
                        new Vector3f(0, 0, 0),
                        0.0f,
                        1
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        });

        // Play wither sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
    }

    /**
     * Plays massive X-slash particles when unleashing damage.
     */
    private void playUnleashParticles(@NotNull Player player, @NotNull Location center) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Create Particle object once for reuse
            Particle<?> damageParticle = new Particle<>(ParticleTypes.DAMAGE_INDICATOR, ParticleData.emptyData());
            Particle<?> smokeParticle = new Particle<>(ParticleTypes.LARGE_SMOKE, ParticleData.emptyData());

            // First diagonal (top-left to bottom-right) - RED
            for (int i = 0; i < 30; i++) {
                double t = i / 30.0;
                double x = center.getX() + (t * 4 - 2);
                double y = center.getY() + (2 - t * 4);
                double z = center.getZ();

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        damageParticle,
                        true,
                        new Vector3d(x, y, z),
                        new Vector3f(0, 0, 0),
                        0.0f,
                        2
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }

            // Second diagonal (top-right to bottom-left) - RED
            for (int i = 0; i < 30; i++) {
                double t = i / 30.0;
                double x = center.getX() + (2 - t * 4);
                double y = center.getY() + (2 - t * 4);
                double z = center.getZ();

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        damageParticle,
                        true,
                        new Vector3d(x, y, z),
                        new Vector3f(0, 0, 0),
                        0.0f,
                        2
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }

            // Black smoke particles for depth
            for (int i = 0; i < 40; i++) {
                double offsetX = (Math.random() - 0.5) * 3;
                double offsetY = (Math.random() - 0.5) * 3;
                double offsetZ = (Math.random() - 0.5) * 0.5;

                WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
                        smokeParticle,
                        true,
                        new Vector3d(
                                center.getX() + offsetX,
                                center.getY() + offsetY + 1,
                                center.getZ() + offsetZ
                        ),
                        new Vector3f(0, 0.1f, 0),
                        0.05f,
                        1
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        });
    }

    /**
     * Gets the entity the player is looking at.
     */
    private @Nullable Entity getTargetEntity(@NotNull Player player, double range) {
        Vector direction = player.getLocation().getDirection();
        Location start = player.getEyeLocation();

        for (double i = 0; i < range; i += 0.5) {
            Location check = start.clone().add(direction.clone().multiply(i));

            for (Entity entity : player.getWorld().getNearbyEntities(check, 1.0, 1.0, 1.0)) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {
                    return entity;
                }
            }
        }

        return null;
    }

    @Override
    public void onDeactivate(@NotNull Player player) {
        AbilityManager.PlayerAbilityData data = plugin.getAbilityManager().getPlayerData(player);
        data.setData(ACTIVE_KEY, false);
        data.removeData(STORED_DAMAGE_KEY);
        data.removeData(ACTIVATION_TIME_KEY);
    }
}