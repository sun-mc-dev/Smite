package me.sunmc.smite.abilities;

import me.sunmc.smite.Smite;
import me.sunmc.smite.api.ability.Ability;
import me.sunmc.smite.api.ability.AbilityContext;
import me.sunmc.smite.api.particle.ParticleBuilder;
import me.sunmc.smite.config.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Heavenly Smite - A lightning strike ability triggered by critical hit chains.
 *
 * <p>This ability activates after a player lands a configured number of consecutive
 * critical hits on the same target. Upon activation:</p>
 * <ul>
 *   <li>Spawns a lightning bolt at the target's location</li>
 *   <li>Deals pure damage ignoring armor</li>
 *   <li>Creates spectacular particle effects</li>
 *   <li>Plays thunder sound effects</li>
 * </ul>
 *
 * <p>Each critical hit in the chain displays falling particles around the player,
 * with the final critical hit creating an explosive particle burst.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class HeavenlySmiteAbility implements Ability {

    private static final String ABILITY_NAME = "heavenly_smite";
    private static final String DISPLAY_NAME = "&6&lHeavenly Smite";

    private final Smite plugin;
    private final AbilityConfig config;

    /**
     * Constructs the Heavenly Smite ability.
     *
     * @param plugin the plugin instance
     */
    public HeavenlySmiteAbility(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getAbilityConfig(ABILITY_NAME);
    }

    @Override
    public String getName() {
        return ABILITY_NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public long getCooldownMillis() {
        return config != null ? config.getCooldownMillis() : 45000L;
    }

    @Override
    public boolean canActivate(@NotNull AbilityContext context) {
        // Check if ability is enabled
        if (config == null || !config.enabled()) {
            return false;
        }

        // Ensure there's a target
        return context.getTarget().isPresent();
    }

    @Override
    public void activate(@NotNull AbilityContext context) {
        Player player = context.player();
        Entity targetEntity = context.target().orElse(null);

        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        Location targetLoc = target.getLocation();

        // Spawn lightning bolt (visual only, no damage from lightning itself)
        targetLoc.getWorld().strikeLightningEffect(targetLoc);

        // Create enhanced lightning particles
        spawnLightningParticles(targetLoc);

        // Play thunder sound
        plugin.getPacketHandler().sendSoundInRadius(
                targetLoc,
                64.0,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                3.0F,
                1.0F
        );

        // Deal pure damage ignoring armor
        double damage = config != null ? config.parameter2() : 10.0;
        dealPureDamage(target, damage, player);

        // Spawn final explosive particles
        spawnExplosionParticles(player.getLocation());
    }

    @Override
    public void deactivate(@NotNull AbilityContext context) {
        // Instant ability, no deactivation needed
    }

    @Override
    public boolean isOnCooldown(@NotNull UUID playerId) {
        return plugin.getCooldownManager().isOnCooldown(playerId, ABILITY_NAME);
    }

    @Override
    public void startCooldown(@NotNull UUID playerId) {
        plugin.getCooldownManager().setCooldown(playerId, ABILITY_NAME, getCooldownMillis());
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.enabled();
    }

    @Override
    public String getDescription() {
        int crits = getRequiredCrits();
        return String.format(
                "Land %d critical hits in a row to summon a lightning bolt that deals %.1f hearts of pure damage.",
                crits,
                (config != null ? config.parameter2() : 10.0) / 2.0
        );
    }

    /**
     * Gets the required number of critical hits to activate.
     *
     * @return the required crit count
     */
    public int getRequiredCrits() {
        return config != null ? config.parameter1() : 3;
    }

    /**
     * Spawns per-crit particles around the player.
     *
     * @param player  the player who landed the crit
     * @param isFinal whether this is the final crit in the chain
     */
    public void spawnCritParticles(@NotNull Player player, boolean isFinal) {
        Location loc = player.getLocation().add(0, 1, 0);

        if (isFinal) {
            // Final crit - explosive burst
            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.CRIT)
                    .location(loc)
                    .count(50)
                    .offset(1.5, 1.5, 1.5)
                    .speed(0.5)
                    .radius(32.0)
                    .spawn();

            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.ELECTRIC_SPARK)
                    .location(loc)
                    .count(30)
                    .offset(1.0, 1.0, 1.0)
                    .speed(0.3)
                    .radius(32.0)
                    .spawn();
        } else {
            // Regular crit - falling circle particles
            List<Location> circle = ParticleBuilder.createCircle(loc, 1.0, 15);

            for (Location point : circle) {
                new ParticleBuilder(plugin.getPacketHandler())
                        .particle(ParticleTypes.CRIT)
                        .location(point)
                        .count(1)
                        .offset(0.0, -0.5, 0.0)
                        .speed(0.1)
                        .radius(32.0)
                        .spawn();
            }
        }
    }

    /**
     * Spawns enhanced lightning particles at the target location.
     */
    private void spawnLightningParticles(@NotNull Location location) {
        Location topLoc = location.clone().add(0, 10, 0);

        // Electric sparks descending
        for (int i = 0; i < 20; i++) {
            double yOffset = i * 0.5;
            Location particleLoc = topLoc.clone().subtract(0, yOffset, 0);

            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.ELECTRIC_SPARK)
                    .location(particleLoc)
                    .count(5)
                    .offset(0.3, 0.1, 0.3)
                    .speed(0.1)
                    .radius(48.0)
                    .spawn();
        }

        // Impact particles
        new ParticleBuilder(plugin.getPacketHandler())
                .particle(ParticleTypes.EXPLOSION)
                .location(location)
                .count(1)
                .offset(0, 0, 0)
                .speed(0)
                .radius(48.0)
                .spawn();

        new ParticleBuilder(plugin.getPacketHandler())
                .particle(ParticleTypes.FLASH)
                .location(location)
                .count(1)
                .offset(0, 0, 0)
                .speed(0)
                .radius(48.0)
                .spawn();
    }

    /**
     * Spawns explosive particles around the player after final crit.
     */
    private void spawnExplosionParticles(@NotNull Location location) {
        Location playerLoc = location.clone().add(0, 1, 0);

        // Create sphere of particles exploding outward
        List<Location> sphere = ParticleBuilder.createSphere(playerLoc, 2.0, 50);

        for (Location point : sphere) {
            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.CRIT)
                    .location(playerLoc)
                    .count(1)
                    .offset(
                            point.getX() - playerLoc.getX(),
                            point.getY() - playerLoc.getY(),
                            point.getZ() - playerLoc.getZ()
                    )
                    .speed(0.5)
                    .radius(32.0)
                    .spawn();
        }
    }

    /**
     * Deals pure damage to an entity, bypassing armor.
     *
     * @param target the target entity
     * @param damage the damage amount
     * @param source the damage source player
     */
    private void dealPureDamage(@NotNull LivingEntity target, double damage, @NotNull Player source) {
        // Get current health
        double currentHealth = target.getHealth();

        // Calculate new health
        double newHealth = Math.max(0, currentHealth - damage);

        // Set health directly (bypasses armor)
        target.setHealth(newHealth);

        // Set last damage cause for proper death message
        target.setLastDamageCause(new org.bukkit.event.entity.EntityDamageByEntityEvent(
                source,
                target,
                org.bukkit.event.entity.EntityDamageEvent.DamageCause.LIGHTNING,
                damage
        ));

        // Visual feedback - damage indicator particles
        new ParticleBuilder(plugin.getPacketHandler())
                .particle(ParticleTypes.DAMAGE_INDICATOR)
                .location(target.getEyeLocation())
                .count((int) (damage / 2))
                .offset(0.5, 0.5, 0.5)
                .speed(0.1)
                .radius(32.0)
                .spawn();
    }
}