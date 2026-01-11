package me.sunmc.smite.command;

import me.sunmc.smite.Smite;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Main command handler for the Smite plugin.
 *
 * <p>Handles commands like /smite reload, /smite help, etc.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class SmiteCommand implements TabExecutor {

    private final Smite plugin;

    /**
     * Constructs the command handler.
     *
     * @param plugin the plugin instance
     */
    public SmiteCommand(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (!sender.hasPermission("smite.reload")) {
                    sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.no-permission")));
                    return true;
                }
                handleReload(sender);
            }
            case "help" -> sendHelp(sender);
            case "info" -> sendInfo(sender);
            default -> sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage(
                    "commands.usage",
                    label,
                    "[reload|help|info]"
            )));
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String subCmd : Arrays.asList("reload", "help", "info")) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    /**
     * Handles the reload subcommand.
     */
    private void handleReload(@NotNull CommandSender sender) {
        sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.reload-start")));

        if (plugin.reloadPlugin()) {
            sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.reload-success")));
        } else {
            sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.reload-fail")));
        }
    }

    /**
     * Sends help information to the sender.
     */
    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.help-header")));

        List<String> entries = plugin.getConfigManager().getMessagesConfig()
                .getStringList("messages.commands.help-entries");

        for (String entry : entries) {
            sender.sendMessage(entry);
        }

        sender.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage("commands.help-footer")));
    }

    /**
     * Sends plugin information to the sender.
     */
    private void sendInfo(@NotNull CommandSender sender) {
        sender.sendMessage("§6§l========== Smite Info ==========");
        sender.sendMessage("§7Version: §e" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7Author: §e" + plugin.getPluginMeta().getAuthors().getFirst());
        sender.sendMessage("§7Loaded Abilities: §e" + plugin.getAbilityRegistry().getRegisteredCount());
        sender.sendMessage("§6§l================================");
    }
}
