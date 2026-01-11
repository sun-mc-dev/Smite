package me.sunmc.smite.packet;

import me.sunmc.smite.Smite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * High-performance packet handler for sending visual and audio effects.
 *
 * <p>This class provides direct packet manipulation for optimal performance,
 * bypassing Bukkit's abstraction layer for particle and sound effects.
 * All methods are designed to be called from the main server thread.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class PacketHandler {

    private final Smite plugin;

    /**
     * Constructs a new packet handler.
     *
     * @param plugin the plugin instance
     */
    public PacketHandler(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a particle packet to a specific player.
     *
     * @param player   the player to send to
     * @param particle the particle options
     * @param location the spawn location
     * @param count    the number of particles
     * @param offsetX  the X offset
     * @param offsetY  the Y offset
     * @param offsetZ  the Z offset
     * @param speed    the particle speed
     */
    public void sendParticle(@NotNull Player player, @NotNull ParticleOptions particle,
                             @NotNull Location location, int count,
                             double offsetX, double offsetY, double offsetZ, double speed) {

        CraftPlayer craftPlayer = (CraftPlayer) player;

        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle,
                false, // longDistance
                location.getX(),
                location.getY(),
                location.getZ(),
                (float) offsetX,
                (float) offsetY,
                (float) offsetZ,
                (float) speed,
                count
        );

        craftPlayer.getHandle().connection.send(packet);
    }

    /**
     * Sends a particle packet to multiple players within range.
     *
     * @param players  the players to send to
     * @param particle the particle options
     * @param location the spawn location
     * @param count    the number of particles
     * @param offsetX  the X offset
     * @param offsetY  the Y offset
     * @param offsetZ  the Z offset
     * @param speed    the particle speed
     */
    public void sendParticleBatch(@NotNull Collection<Player> players, @NotNull ParticleOptions particle,
                                  @NotNull Location location, int count,
                                  double offsetX, double offsetY, double offsetZ, double speed) {

        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle,
                false,
                location.getX(),
                location.getY(),
                location.getZ(),
                (float) offsetX,
                (float) offsetY,
                (float) offsetZ,
                (float) speed,
                count
        );

        for (Player player : players) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    /**
     * Sends particles to all players within a certain radius of a location.
     *
     * @param location the center location
     * @param radius   the radius in blocks
     * @param particle the particle options
     * @param count    the number of particles
     * @param offsetX  the X offset
     * @param offsetY  the Y offset
     * @param offsetZ  the Z offset
     * @param speed    the particle speed
     */
    public void sendParticleInRadius(@NotNull Location location, double radius,
                                     @NotNull ParticleOptions particle, int count,
                                     double offsetX, double offsetY, double offsetZ, double speed) {

        if (location.getWorld() == null) return;

        Collection<Player> nearbyPlayers = location.getWorld().getNearbyPlayers(location, radius);
        sendParticleBatch(nearbyPlayers, particle, location, count, offsetX, offsetY, offsetZ, speed);
    }

    /**
     * Sends a sound packet to a specific player.
     *
     * @param player   the player to send to
     * @param sound    the sound event
     * @param source   the sound source category
     * @param location the sound location
     * @param volume   the sound volume
     * @param pitch    the sound pitch
     */
    public void sendSound(@NotNull Player player, @NotNull SoundEvent sound,
                          @NotNull SoundSource source, @NotNull Location location,
                          float volume, float pitch) {

        CraftPlayer craftPlayer = (CraftPlayer) player;

        ClientboundSoundPacket packet = new ClientboundSoundPacket(
                sound.getLocation(),
                source,
                location.getX(),
                location.getY(),
                location.getZ(),
                volume,
                pitch,
                craftPlayer.getHandle().getRandom().nextLong()
        );

        craftPlayer.getHandle().connection.send(packet);
    }

    /**
     * Sends a sound packet to multiple players.
     *
     * @param players  the players to send to
     * @param sound    the sound event
     * @param source   the sound source category
     * @param location the sound location
     * @param volume   the sound volume
     * @param pitch    the sound pitch
     */
    public void sendSoundBatch(@NotNull Collection<Player> players, @NotNull SoundEvent sound,
                               @NotNull SoundSource source, @NotNull Location location,
                               float volume, float pitch) {

        long seed = System.currentTimeMillis();

        for (Player player : players) {
            CraftPlayer craftPlayer = (CraftPlayer) player;

            ClientboundSoundPacket packet = new ClientboundSoundPacket(
                    sound.getLocation(),
                    source,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    volume,
                    pitch,
                    seed
            );

            craftPlayer.getHandle().connection.send(packet);
        }
    }

    /**
     * Sends a sound to all players within a radius of a location.
     *
     * @param location the center location
     * @param radius   the radius in blocks
     * @param sound    the sound event
     * @param source   the sound source category
     * @param volume   the sound volume
     * @param pitch    the sound pitch
     */
    public void sendSoundInRadius(@NotNull Location location, double radius,
                                  @NotNull SoundEvent sound, @NotNull SoundSource source,
                                  float volume, float pitch) {

        if (location.getWorld() == null) return;

        Collection<Player> nearbyPlayers = location.getWorld().getNearbyPlayers(location, radius);
        sendSoundBatch(nearbyPlayers, sound, source, location, volume, pitch);
    }
}