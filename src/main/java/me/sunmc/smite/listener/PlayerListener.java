package me.sunmc.smite.listener;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.AbilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player join/quit events and data loading.
 */
public class PlayerListener implements Listener {

    private final Smite plugin;

    public PlayerListener(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        var player = event.getPlayer();

        AbilityManager.PlayerAbilityData playerData = plugin.getAbilityManager().getPlayerData(player);

        // Load player data from database
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                .thenAccept(data -> {
                    if (data != null) {
                        // Store loaded data
                        if (data.selectedCell() != null) {
                            playerData.setData("selected_cell", data.selectedCell());
                            playerData.setData("cell_locked", data.cellLocked());

                            // Load keybinds
                            plugin.getKeybindManager().loadKeybinds(player);

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(Component.text("Welcome back! ", NamedTextColor.GREEN)
                                        .append(Component.text("Your cell: ", NamedTextColor.GRAY))
                                        .append(Component.text(
                                                plugin.getCellManager().getCell(data.selectedCell()).getDisplayName(),
                                                NamedTextColor.GOLD
                                        )));
                            });
                        } else {
                            // No cell selected, prompt user
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
                                player.sendMessage(Component.text("  Welcome to ", NamedTextColor.YELLOW)
                                        .append(Component.text("Smite", NamedTextColor.GOLD)));
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text("  You need to select a Cell!", NamedTextColor.WHITE));
                                player.sendMessage(Component.text("  Use ", NamedTextColor.GRAY)
                                        .append(Component.text("/smite select", NamedTextColor.AQUA))
                                        .append(Component.text(" to choose", NamedTextColor.GRAY)));
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text("  ⚠ This choice is PERMANENT!", NamedTextColor.RED));
                                player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
                                player.sendMessage(Component.empty());
                            }, 40L); // 2 seconds delay
                        }
                    } else {
                        // New player
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.sendMessage(Component.empty());
                            player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
                            player.sendMessage(Component.text("  Welcome to ", NamedTextColor.YELLOW)
                                    .append(Component.text("Smite", NamedTextColor.GOLD)));
                            player.sendMessage(Component.empty());
                            player.sendMessage(Component.text("  Select your Cell to begin!", NamedTextColor.WHITE));
                            player.sendMessage(Component.text("  Use ", NamedTextColor.GRAY)
                                    .append(Component.text("/smite select", NamedTextColor.AQUA)));
                            player.sendMessage(Component.empty());
                            player.sendMessage(Component.text("  ⚠ This choice is PERMANENT!", NamedTextColor.RED));
                            player.sendMessage(Component.text("═══════════════════════════", NamedTextColor.GOLD));
                            player.sendMessage(Component.empty());
                        }, 40L);
                    }
                });
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();

        plugin.getAbilityManager().removePlayerData(player);
        plugin.getAbilityManager().getCooldownManager().clearCooldowns(player);
        plugin.getKeybindManager().removeKeybinds(player);
        plugin.getGuiManager().closeGUI(player);
    }
}