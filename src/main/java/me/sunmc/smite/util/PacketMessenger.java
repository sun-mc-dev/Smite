package me.sunmc.smite.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Utility class for sending packet-based messages to players.
 * Uses PacketEvents for efficient client-side rendering.
 */
public class PacketMessenger {

    /**
     * Sends an action bar message to a player using packets.
     *
     * @param player  The player
     * @param message The message component
     */
    public static void sendActionBar(@NotNull Player player, @NotNull Component message) {
        // Use Paper's native action bar (it's already optimized)
        player.sendActionBar(message);
    }

    /**
     * Sends a title to a player using packets.
     *
     * @param player   The player
     * @param title    The title component
     * @param subtitle The subtitle component
     * @param fadeIn   Fade in duration
     * @param stay     Stay duration
     * @param fadeOut  Fade out duration
     */
    public static void sendTitle(@NotNull Player player,
                                 @NotNull Component title,
                                 @NotNull Component subtitle,
                                 @NotNull Duration fadeIn,
                                 @NotNull Duration stay,
                                 @NotNull Duration fadeOut) {
        Title titleObj = Title.title(
                title,
                subtitle,
                Title.Times.times(fadeIn, stay, fadeOut)
        );

        player.showTitle(titleObj);
    }

    /**
     * Sends a simple title with default timings.
     *
     * @param player   The player
     * @param title    The title component
     * @param subtitle The subtitle component
     */
    public static void sendTitle(@NotNull Player player,
                                 @NotNull Component title,
                                 @NotNull Component subtitle) {
        sendTitle(player, title, subtitle,
                Duration.ofMillis(500),
                Duration.ofMillis(2000),
                Duration.ofMillis(500));
    }

    /**
     * Sends a system chat message to a player.
     *
     * @param player  The player
     * @param message The message component
     */
    public static void sendSystemMessage(@NotNull Player player, @NotNull Component message) {
        WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(
                false, // Not action bar
                SpigotConversionUtil.fromAdventureComponent(message)
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /**
     * Sends a hotbar message (above hotbar, different from action bar).
     *
     * @param player  The player
     * @param message The message component
     */
    public static void sendHotbarMessage(@NotNull Player player, @NotNull Component message) {
        WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(
                true, // Action bar position
                SpigotConversionUtil.fromAdventureComponent(message)
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /**
     * Clears the player's title.
     *
     * @param player The player
     */
    public static void clearTitle(@NotNull Player player) {
        player.clearTitle();
    }

    /**
     * Resets the player's title (clears and resets timings).
     *
     * @param player The player
     */
    public static void resetTitle(@NotNull Player player) {
        player.resetTitle();
    }
}