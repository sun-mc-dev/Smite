package me.sunmc.smite.abilities;

import me.sunmc.smite.Smite;
import me.sunmc.smite.api.ability.Ability;
import me.sunmc.smite.api.ability.AbilityContext;
import me.sunmc.smite.api.ability.AbilityTrigger;
import me.sunmc.smite.api.particle.ParticleBuilder;
import me.sunmc.smite.config.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonic Spark - A damage accumulation and burst ability.
 *
 * <p>When activated, this ability enters a 5-second accumulation phase where:</p>
 * <ul>
 *   <li>The player cannot deal direct damage to entities</li>
 *   <li>Each hit attempt accumulates damage</li>
 *   <li>Wither sound effects and red slash particles play on each hit</li>
 * </ul>
 *
 * <p>After the accumulation phase ends, the next successful hit releases
 * all accumulated damage in a single burst with dramatic X-pattern particles.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class DemonicSparkAbility implements Ability, Listener {

    private static final String ABILITY_NAME = "demonic_spark";
    private static final String DISPLAY_NAME = "&c&lDemonic Spark";

    private final Smite plugin;
    private final AbilityConfig config;
    private final Map<UUID, AccumulatedDamage> accumulatedDamage;

    /**
     * Constructs the Demonic Spark ability.
     *
     * @param plugin the plugin instance
     */
    public DemonicSparkAbility(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getAbilityConfig(ABILITY_NAME);
        this.accumulatedDamage = new ConcurrentHashMap<>();

        // Register as listener for finisher detection
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

        // Can only activate manually
        return context.trigger() == AbilityTrigger.MANUAL;
    }

    @Override
    public void activate(@NotNull AbilityContext context) {
        Player player = context.player();
        UUID playerId = player.getUniqueId();

        // Initialize accumulated damage tracking
        accumulatedDamage.put(playerId, new AccumulatedDamage());

        // Send activation message
        player.sendMessage("§c§lDemonic Spark §7activated! Hit enemies to accumulate damage!");

        // Play activation effects
        playActivationEffects(player);

        // Schedule automatic deactivation after duration
        int durationSeconds = config != null ? config.parameter1() : 5;
        long durationTicks = durationSeconds * 20L;

        plugin.getAbilityManager().scheduleDeactivation(this, player, durationTicks);
    }

    @Override
    public void deactivate(@NotNull AbilityContext context) {
        Player player = context.player();
        UUID playerId = player.getUniqueId();

        // Keep accumulated damage for finisher but mark as ready
        AccumulatedDamage damage = accumulatedDamage.get(playerId);
        if (damage != null) {
            damage.setReadyForFinisher(true);

            // Notify player
            double totalDamage = damage.getTotalDamage();
            player.sendMessage(String.format(
                    "§c§lDemonic Spark §7ended! Accumulated §c%.1f §7damage. Next hit will release it!",
                    totalDamage
            ));

            // Schedule cleanup if finisher not used within 30 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                AccumulatedDamage current = accumulatedDamage.get(playerId);
                if (current != null && current.isReadyForFinisher()) {
                    accumulatedDamage.remove(playerId);
                    player.sendMessage("§7Demonic Spark finisher expired.");
                }
            }, 600L); // 30 seconds
        }
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
        int duration = config != null ? config.parameter1() : 5;
        return String.format(
                "Enter accumulation mode for %d seconds. All hits accumulate damage, then your next hit deals all accumulated damage at once.",
                duration
        );
    }

    /**
     * Accumulates damage during the active period.
     *
     * @param playerId the player's UUID
     * @param damage   the damage to accumulate
     */
    public void accumulateDamage(@NotNull UUID playerId, double damage) {
        AccumulatedDamage accumulated = accumulatedDamage.get(playerId);
        if (accumulated != null && !accumulated.isReadyForFinisher()) {
            accumulated.addDamage(damage);
        }
    }

    /**
     * Gets the accumulated damage for a player.
     *
     * @param playerId the player's UUID
     * @return the accumulated damage, or 0 if none
     */
    public double getAccumulatedDamage(@NotNull UUID playerId) {
        AccumulatedDamage damage = accumulatedDamage.get(playerId);
        return damage != null ? damage.getTotalDamage() : 0.0;
    }

    /**
     * Plays particle and sound effects during damage accumulation.
     *
     * @param player         the player
     * @param targetLocation the hit location
     */
    public void playAccumulationEffects(@NotNull Player player, @NotNull Location targetLocation) {
        // Red diagonal slash particles
        Location startTop = targetLocation.clone().add(-0.5, 1.5, 0);
        Location endBottom = targetLocation.clone().add(0.5, 0.5, 0);

        List<Location> slashLine = ParticleBuilder.createLine(startTop, endBottom, 15);

        for (Location point : slashLine) {
            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.DAMAGE_INDICATOR)
                    .location(point)
                    .count(1)
                    .offset(0.05, 0.05, 0.05)
                    .speed(0)
                    .radius(32.0)
                    .spawn();
        }

        // Wither sound effect
        plugin.getPacketHandler().sendSound(
                player,
                SoundEvents.WITHER_HURT,
                SoundSource.HOSTILE,
                targetLocation,
                0.8F,
                1.2F
        );
    }

    /**
     * Listens for the finisher hit after accumulation period ends.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFinisherHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        AccumulatedDamage damage = accumulatedDamage.get(playerId);

        // Check if player has a finisher ready
        if (damage == null || !damage.isReadyForFinisher()) {
            return;
        }

        // Cancel original damage
        event.setCancelled(true);

        // Calculate finisher damage
        double multiplier = config != null ? config.parameter2() : 1.0;
        double totalDamage = damage.getTotalDamage() * multiplier;

        // Deal accumulated damage
        double currentHealth = target.getHealth();
        double newHealth = Math.max(0, currentHealth - totalDamage);
        target.setHealth(newHealth);

        // Play finisher effects
        playFinisherEffects(target.getLocation(), totalDamage);

        // Notify player
        player.sendMessage(String.format(
                "§c§l⚡ DEMONIC SPARK! §7Dealt §c%.1f §7damage!",
                totalDamage
        ));

        // Clear accumulated damage
        accumulatedDamage.remove(playerId);

        // Start cooldown (if not already on cooldown)
        if (!isOnCooldown(playerId)) {
            startCooldown(playerId);
        }
    }

    /**
     * Plays activation particle effects.
     */
    private void playActivationEffects(@NotNull Player player) {
        Location loc = player.getLocation().add(0, 1, 0);

        // Dark spiral particles
        List<Location> helix = ParticleBuilder.createHelix(loc, 3.0, 1.5, 2, 60);

        for (Location point : helix) {
            new ParticleBuilder(plugin.getPacketHandler())
                    .particle(ParticleTypes.SMOKE)
                    .location(point)
                    .count(1)
                    .offset(0.05, 0.05, 0.05)
                    .speed(0)
                    .radius(32.0)
                    .spawn();
        }

        // Wither sound
        plugin.getPacketHandler().sendSoundInRadius(
                loc,
                32.0,
                SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE,
                1.0F,
                0.8F
        );
    }

    /**
     * Plays the dramatic finisher effect with X-pattern particles.
     */
    private void playFinisherEffects(@NotNull Location location, double damage) {
        Location centerLoc = location.clone().add(0, 1, 0);

        // Large X-pattern with red and black particles
        List<Location> xPattern = ParticleBuilder.createXShape(centerLoc, 2.5, 30);

        for (int i = 0; i < xPattern.size(); i++) {
            Location point = xPattern.get(i);

            // Alternate between red and black (using different particle types)
            if (i % 2 == 0) {
                new ParticleBuilder(plugin.getPacketHandler())
                        .particle(ParticleTypes.DAMAGE_INDICATOR)
                        .location(point)
                        .count(2)
                        .offset(0.1, 0.1, 0.1)
                        .speed(0.1)
                        .radius(48.0)
                        .spawn();
            } else {
                new ParticleBuilder(plugin.getPacketHandler())
                        .particle(ParticleTypes.SMOKE)
                        .location(point)
                        .count(2)
                        .offset(0.1, 0.1, 0.1)
                        .speed(0.05)
                        .radius(48.0)
                        .spawn();
            }
        }

        // Central explosion
        new ParticleBuilder(plugin.getPacketHandler())
                .particle(ParticleTypes.EXPLOSION_EMITTER)
                .location(centerLoc)
                .count(1)
                .offset(0, 0, 0)
                .speed(0)
                .radius(48.0)
                .spawn();

        // Wither death sound for finisher
        plugin.getPacketHandler().sendSoundInRadius(
                centerLoc,
                48.0,
                SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE,
                2.0F,
                0.8F
        );

        // Damage indicator particles based on damage dealt
        new ParticleBuilder(plugin.getPacketHandler())
                .particle(ParticleTypes.DAMAGE_INDICATOR)
                .location(location.add(0, 1.5, 0))
                .count((int) Math.min(damage / 2, 50))
                .offset(0.8, 0.8, 0.8)
                .speed(0.2)
                .radius(48.0)
                .spawn();
    }

    /**
     * Internal class to track accumulated damage.
     */
    private static final class AccumulatedDamage {
        private double totalDamage;
        private boolean readyForFinisher;

        /**
         * Constructs a new accumulated damage tracker.
         */
        AccumulatedDamage() {
            this.totalDamage = 0.0;
            this.readyForFinisher = false;
        }

        /**
         * Adds damage to the accumulation.
         *
         * @param damage the damage to add
         */
        void addDamage(double damage) {
            this.totalDamage += damage;
        }

        /**
         * Gets the total accumulated damage.
         *
         * @return the total damage
         */
        double getTotalDamage() {
            return totalDamage;
        }

        /**
         * Checks if the finisher is ready.
         *
         * @return true if ready for finisher
         */
        boolean isReadyForFinisher() {
            return readyForFinisher;
        }

        /**
         * Sets whether the finisher is ready.
         *
         * @param ready true to mark ready
         */
        void setReadyForFinisher(boolean ready) {
            this.readyForFinisher = ready;
        }
    }
}