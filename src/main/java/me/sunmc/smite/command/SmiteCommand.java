package me.sunmc.smite.command;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.Ability;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationTrigger;
import me.sunmc.smite.ability.cell.Cell;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for the Smite plugin.
 */
public class SmiteCommand implements TabExecutor {

    private final Smite plugin;

    public SmiteCommand(@NotNull Smite plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "select", "choose" -> handleSelect(sender);
            case "cells" -> handleCells(sender);
            case "activate", "use" -> handleActivate(sender, args);
            case "cooldown", "cd" -> handleCooldown(sender, args);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            case "keybind", "key" -> handleKeybind(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("═══════ Smite Commands ═══════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/smite select", NamedTextColor.YELLOW)
                .append(Component.text(" - Open cell selection GUI", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/smite cells", NamedTextColor.YELLOW)
                .append(Component.text(" - List all available cells", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/smite list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all abilities", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/smite activate <ability>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manually activate an ability", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/smite info <ability>", NamedTextColor.YELLOW)
                .append(Component.text(" - Show ability information", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/smite keybind <ability> <key>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set keybind", NamedTextColor.GRAY)));

        if (sender.hasPermission("smite.admin")) {
            sender.sendMessage(Component.text("/smite cooldown <player> <ability> clear", NamedTextColor.YELLOW)
                    .append(Component.text(" - Clear cooldown", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/smite reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        }
    }

    private void handleSelect(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return;
        }

        // Open packet-based GUI
        plugin.getGuiManager().openCellSelectionGUI(player);
    }

    private void handleCells(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("═══════ Available Cells ═══════", NamedTextColor.GOLD));

        for (Cell cell : plugin.getCellManager().getAllCells()) {
            Component status = cell.isEnabled()
                    ? Component.text("✓", NamedTextColor.GREEN)
                    : Component.text("✗", NamedTextColor.RED);

            sender.sendMessage(status
                    .append(Component.text(" " + cell.getDisplayName(), NamedTextColor.YELLOW))
                    .append(Component.text(" - " + cell.getDescription(), NamedTextColor.GRAY)));

            // List abilities in this cell
            for (String abilityId : cell.getAbilityIds()) {
                Ability ability = plugin.getAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    sender.sendMessage(Component.text("   • ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(ability.getDisplayName(), NamedTextColor.WHITE))
                            .append(Component.text(" (" + ability.getCooldown() + "s)", NamedTextColor.GRAY)));
                }
            }
        }
    }

    private void handleList(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("═══════ Registered Abilities ═══════", NamedTextColor.GOLD));

        plugin.getAbilityManager().getAllAbilities().values().forEach(ability -> {
            Component status = ability.isEnabled()
                    ? Component.text("✓", NamedTextColor.GREEN)
                    : Component.text("✗", NamedTextColor.RED);

            sender.sendMessage(status
                    .append(Component.text(" " + ability.getDisplayName(), NamedTextColor.YELLOW))
                    .append(Component.text(" (" + ability.getCooldown() + "s)", NamedTextColor.GRAY)));
        });
    }

    private void handleActivate(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /smite activate <ability>", NamedTextColor.RED));
            return;
        }

        String abilityId = args[1].toLowerCase().replace("_", "");
        Ability ability = findAbility(abilityId);

        if (ability == null) {
            sender.sendMessage(Component.text("Ability not found: " + args[1], NamedTextColor.RED));
            return;
        }

        if (!ability.isEnabled()) {
            sender.sendMessage(Component.text("This ability is currently disabled!", NamedTextColor.RED));
            return;
        }

        // Check if player has this ability in their cell
        var playerData = plugin.getAbilityManager().getPlayerData(player);
        var selectedCell = playerData.getData("selected_cell");

        if (selectedCell == null) {
            sender.sendMessage(Component.text("You must select a cell first! Use /smite select", NamedTextColor.RED));
            return;
        }

        Cell cell = plugin.getCellManager().getCell((String) selectedCell);
        if (cell == null || !cell.containsAbility(ability.getId())) {
            sender.sendMessage(Component.text("This ability is not in your cell!", NamedTextColor.RED));
            return;
        }

        ActivationContext context = new ActivationContext(ActivationTrigger.MANUAL);
        plugin.getAbilityManager().activateAbility(player, ability.getId(), context);
    }

    private void handleCooldown(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("smite.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /smite cooldown <player> <ability> clear", NamedTextColor.RED));
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        String abilityId = args[2].toLowerCase().replace("_", "");
        Ability ability = findAbility(abilityId);

        if (ability == null) {
            sender.sendMessage(Component.text("Ability not found: " + args[2], NamedTextColor.RED));
            return;
        }

        if (args[3].equalsIgnoreCase("clear")) {
            plugin.getAbilityManager().getCooldownManager().clearCooldown(target, ability);
            sender.sendMessage(Component.text("Cleared cooldown for " + ability.getDisplayName(), NamedTextColor.GREEN));
        }
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("smite.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
    }

    private void handleInfo(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /smite info <ability>", NamedTextColor.RED));
            return;
        }

        String abilityId = args[1].toLowerCase().replace("_", "");
        Ability ability = findAbility(abilityId);

        if (ability == null) {
            sender.sendMessage(Component.text("Ability not found: " + args[1], NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══════ " + ability.getDisplayName() + " ═══════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
                .append(Component.text(ability.getId(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text(ability.getType().name(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Cooldown: ", NamedTextColor.GRAY)
                .append(Component.text(ability.getCooldown() + "s", NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Description: ", NamedTextColor.GRAY)
                .append(Component.text(ability.getDescription(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Enabled: ", NamedTextColor.GRAY)
                .append(Component.text(ability.isEnabled() ? "Yes" : "No",
                        ability.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // Show which cell this ability belongs to
        Cell cell = plugin.getCellManager().findCellByAbility(ability.getId());
        if (cell != null) {
            sender.sendMessage(Component.text("Cell: ", NamedTextColor.GRAY)
                    .append(Component.text(cell.getDisplayName(), NamedTextColor.AQUA)));
        }
    }

    private void handleKeybind(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /smite keybind <ability> <key>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Available keys: KEY_R, KEY_F", NamedTextColor.GRAY));
            return;
        }

        String abilityId = args[1].toLowerCase().replace("_", "");
        Ability ability = findAbility(abilityId);

        if (ability == null) {
            sender.sendMessage(Component.text("Ability not found: " + args[1], NamedTextColor.RED));
            return;
        }

        String keybind = args[2].toUpperCase();
        if (!keybind.startsWith("KEY_")) {
            keybind = "KEY_" + keybind;
        }

        plugin.getKeybindManager().setKeybind(player, ability.getId(), keybind);
        sender.sendMessage(Component.text("Keybind set: ", NamedTextColor.GREEN)
                .append(Component.text(ability.getDisplayName(), NamedTextColor.YELLOW))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text(keybind, NamedTextColor.AQUA)));
    }

    private @Nullable Ability findAbility(@NotNull String searchTerm) {
        // Try exact ID match first
        Ability ability = plugin.getAbilityManager().getAbility(searchTerm);
        if (ability != null) {
            return ability;
        }

        // Try fuzzy match
        for (Ability a : plugin.getAbilityManager().getAllAbilities().values()) {
            if (a.getId().replace("_", "").equalsIgnoreCase(searchTerm)) {
                return a;
            }
            if (a.getDisplayName().replace(" ", "").equalsIgnoreCase(searchTerm)) {
                return a;
            }
        }

        return null;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("select", "cells", "list", "activate", "cooldown", "reload", "info", "keybind"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("activate") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("keybind")) {
                completions.addAll(plugin.getAbilityManager().getAllAbilities().keySet());
            } else if (args[0].equalsIgnoreCase("cooldown")) {
                completions.addAll(plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("cooldown")) {
                completions.addAll(plugin.getAbilityManager().getAllAbilities().keySet());
            } else if (args[0].equalsIgnoreCase("keybind")) {
                completions.addAll(Arrays.asList("KEY_R", "KEY_F", "R", "F"));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("cooldown")) {
            completions.add("clear");
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}