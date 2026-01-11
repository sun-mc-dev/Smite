package me.sunmc.smite.command;

import me.sunmc.smite.Smite;
import me.sunmc.smite.abilities.DemonicSparkAbility;
import me.sunmc.smite.api.ability.AbilityContext;
import me.sunmc.smite.api.ability.AbilityTrigger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Command handler for activating Demonic Spark ability.
 *
 * @author SunMC
 * @version 1.0.0
 */
final class DemonicSparkCommand implements CommandExecutor {

    private final Smite plugin;

    /**
     * Constructs the command handler.
     *
     * @param plugin the plugin instance
     */
    public DemonicSparkCommand(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("errors.must-be-player"));
            return true;
        }

        if (!player.hasPermission("smite.ability.demonicspark")) {
            player.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage(
                    "errors.no-ability-permission",
                    "Demonic Spark"
            )));
            return true;
        }

        // Get Demonic Spark ability
        plugin.getAbilityRegistry().getAbility(DemonicSparkAbility.class).ifPresentOrElse(
                ability -> {
                    AbilityContext context = new AbilityContext.Builder(player)
                            .trigger(AbilityTrigger.MANUAL)
                            .build();

                    plugin.getAbilityManager().activateAbility(ability, context);
                },
                () -> player.sendMessage(Objects.requireNonNull(plugin.getConfigManager().getMessage(
                        "errors.ability-not-found",
                        "Demonic Spark"
                )))
        );

        return true;
    }
}
