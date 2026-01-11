package me.sunmc.smite.api.particle;

import me.sunmc.smite.packet.PacketHandler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fluent builder for creating and spawning particle effects.
 *
 * <p>This builder provides a convenient API for creating complex particle
 * patterns and effects with precise control over positioning, motion, and appearance.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * new ParticleBuilder(packetHandler)
 *     .particle(ParticleTypes.CRIT)
 *     .location(player.getLocation())
 *     .count(20)
 *     .offset(0.5, 0.5, 0.5)
 *     .speed(0.1)
 *     .viewers(nearbyPlayers)
 *     .spawn();
 * }</pre>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class ParticleBuilder {

    private final PacketHandler packetHandler;
    private ParticleOptions particle;
    private Location location;
    private int count;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double speed;
    private Collection<Player> viewers;
    private double radius;

    /**
     * Constructs a new particle builder.
     *
     * @param packetHandler the packet handler to use
     */
    public ParticleBuilder(@NotNull PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.particle = ParticleTypes.CRIT;
        this.count = 1;
        this.offsetX = 0;
        this.offsetY = 0;
        this.offsetZ = 0;
        this.speed = 0;
        this.radius = 32.0;
    }

    /**
     * Creates a circular particle pattern around a location.
     *
     * @param center the center location
     * @param radius the circle radius
     * @param points the number of points on the circle
     * @return a list of locations forming a circle
     */
    public static List<Location> createCircle(@NotNull Location center, double radius, int points) {
        List<Location> locations = new ArrayList<>();
        double angleStep = (2 * Math.PI) / points;

        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location point = center.clone().add(x, 0, z);
            locations.add(point);
        }

        return locations;
    }

    /**
     * Creates a sphere particle pattern around a location.
     *
     * @param center the center location
     * @param radius the sphere radius
     * @param points the number of points on the sphere
     * @return a list of locations forming a sphere
     */
    public static List<Location> createSphere(@NotNull Location center, double radius, int points) {
        List<Location> locations = new ArrayList<>();
        double goldenRatio = (1 + Math.sqrt(5)) / 2;
        double angleIncrement = Math.PI * 2 * goldenRatio;

        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double inclination = Math.acos(1 - 2 * t);
            double azimuth = angleIncrement * i;

            double x = radius * Math.sin(inclination) * Math.cos(azimuth);
            double y = radius * Math.sin(inclination) * Math.sin(azimuth);
            double z = radius * Math.cos(inclination);

            Location point = center.clone().add(x, y, z);
            locations.add(point);
        }

        return locations;
    }

    /**
     * Creates a line particle pattern between two locations.
     *
     * @param start  the start location
     * @param end    the end location
     * @param points the number of points on the line
     * @return a list of locations forming a line
     */
    public static List<Location> createLine(@NotNull Location start, @NotNull Location end, int points) {
        List<Location> locations = new ArrayList<>();
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (int i = 0; i <= points; i++) {
            double distance = (length / points) * i;
            Location point = start.clone().add(direction.clone().multiply(distance));
            locations.add(point);
        }

        return locations;
    }

    /**
     * Creates an X-shaped particle pattern.
     *
     * @param center        the center location
     * @param size          the size of the X
     * @param pointsPerLine the number of points per line
     * @return a list of locations forming an X
     */
    public static List<Location> createXShape(@NotNull Location center, double size, int pointsPerLine) {
        List<Location> locations = new ArrayList<>();

        // Diagonal line from bottom-left to top-right
        Location start1 = center.clone().add(-size, -size, 0);
        Location end1 = center.clone().add(size, size, 0);
        locations.addAll(createLine(start1, end1, pointsPerLine));

        // Diagonal line from bottom-right to top-left
        Location start2 = center.clone().add(size, -size, 0);
        Location end2 = center.clone().add(-size, size, 0);
        locations.addAll(createLine(start2, end2, pointsPerLine));

        return locations;
    }

    /**
     * Creates a helix particle pattern.
     *
     * @param start     the start location
     * @param height    the height of the helix
     * @param radius    the radius of the helix
     * @param rotations the number of complete rotations
     * @param points    the total number of points
     * @return a list of locations forming a helix
     */
    public static List<Location> createHelix(@NotNull Location start, double height,
                                             double radius, double rotations, int points) {
        List<Location> locations = new ArrayList<>();
        double angleStep = (2 * Math.PI * rotations) / points;
        double heightStep = height / points;

        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double y = i * heightStep;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location point = start.clone().add(x, y, z);
            locations.add(point);
        }

        return locations;
    }

    /**
     * Sets the particle type.
     *
     * @param particle the particle options
     * @return this builder
     */
    public ParticleBuilder particle(@NotNull ParticleOptions particle) {
        this.particle = particle;
        return this;
    }

    /**
     * Sets the spawn location.
     *
     * @param location the location
     * @return this builder
     */
    public ParticleBuilder location(@NotNull Location location) {
        this.location = location.clone();
        return this;
    }

    /**
     * Sets the particle count.
     *
     * @param count the number of particles
     * @return this builder
     */
    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }

    /**
     * Sets the particle offset on all axes.
     *
     * @param offset the offset value
     * @return this builder
     */
    public ParticleBuilder offset(double offset) {
        this.offsetX = offset;
        this.offsetY = offset;
        this.offsetZ = offset;
        return this;
    }

    /**
     * Sets the particle offset on each axis individually.
     *
     * @param offsetX the X offset
     * @param offsetY the Y offset
     * @param offsetZ the Z offset
     * @return this builder
     */
    public ParticleBuilder offset(double offsetX, double offsetY, double offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        return this;
    }

    /**
     * Sets the particle speed/data value.
     *
     * @param speed the speed
     * @return this builder
     */
    public ParticleBuilder speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the specific viewers who will see the particles.
     *
     * @param viewers the collection of players
     * @return this builder
     */
    public ParticleBuilder viewers(@NotNull Collection<Player> viewers) {
        this.viewers = new ArrayList<>(viewers);
        return this;
    }

    /**
     * Sets the viewing radius (used when viewers are not specified).
     *
     * @param radius the radius in blocks
     * @return this builder
     */
    public ParticleBuilder radius(double radius) {
        this.radius = radius;
        return this;
    }

    /**
     * Spawns the configured particles.
     */
    public void spawn() {
        if (location == null) {
            throw new IllegalStateException("Location must be set before spawning particles");
        }

        if (viewers != null) {
            packetHandler.sendParticleBatch(viewers, particle, location, count,
                    offsetX, offsetY, offsetZ, speed);
        } else {
            packetHandler.sendParticleInRadius(location, radius, particle, count,
                    offsetX, offsetY, offsetZ, speed);
        }
    }
}