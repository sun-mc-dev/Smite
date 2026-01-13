package me.sunmc.smite.keybind;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages keybinds for abilities.
 * Uses PacketEvents to detect key presses efficiently.
 */
public class KeybindManager extends PacketListenerAbstract {

    // Default keybinds
    public static final String DEFAULT_ABILITY_1_KEY = "KEY_R";
    public static final String DEFAULT_ABILITY_2_KEY = "KEY_F";
    private final Smite plugin;
    private final Map<UUID, Map<String, String>> playerKeybinds; // UUID -> (AbilityID -> Keybind)
    private final Map<UUID, Map<String, Long>> lastKeyPress; // Debouncing

    public KeybindManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.playerKeybinds = new ConcurrentHashMap<>();
        this.lastKeyPress = new ConcurrentHashMap<>();

        // Register with PacketEvents
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    /**
     * Sets a keybind for a player's ability.
     *
     * @param player    The player
     * @param abilityId The ability ID
     * @param keybind   The keybind string (e.g., "KEY_R")
     */
    public void setKeybind(@NotNull Player player, @NotNull String abilityId, @NotNull String keybind) {
        playerKeybinds.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(abilityId, keybind);

        // Save to database
        plugin.getDatabaseManager().saveKeybind(player.getUniqueId(), abilityId, keybind);
    }

    /**
     * Gets a player's keybind for an ability.
     *
     * @param player    The player
     * @param abilityId The ability ID
     * @return The keybind string, or null if not set
     */
    @Nullable
    public String getKeybind(@NotNull Player player, @NotNull String abilityId) {
        Map<String, String> keybinds = playerKeybinds.get(player.getUniqueId());
        return keybinds != null ? keybinds.get(abilityId) : null;
    }

    /**
     * Loads keybinds for a player from the database.
     *
     * @param player The player
     */
    public void loadKeybinds(@NotNull Player player) {
        // Load from database asynchronously
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                .thenAccept(data -> {
                    if (data != null && data.selectedCell() != null) {
                        var cell = plugin.getCellManager().getCell(data.selectedCell());
                        if (cell != null) {
                            // Load keybinds for both abilities in the cell
                            String ability1 = cell.getFirstAbilityId();
                            String ability2 = cell.getSecondAbilityId();

                            plugin.getDatabaseManager().loadKeybind(player.getUniqueId(), ability1)
                                    .thenAccept(keybind -> {
                                        if (keybind != null) {
                                            playerKeybinds.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                                                    .put(ability1, keybind);
                                        } else {
                                            // Set default
                                            setKeybind(player, ability1, DEFAULT_ABILITY_1_KEY);
                                        }
                                    });

                            plugin.getDatabaseManager().loadKeybind(player.getUniqueId(), ability2)
                                    .thenAccept(keybind -> {
                                        if (keybind != null) {
                                            playerKeybinds.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                                                    .put(ability2, keybind);
                                        } else {
                                            // Set default
                                            setKeybind(player, ability2, DEFAULT_ABILITY_2_KEY);
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * Removes keybinds for a player (on disconnect).
     *
     * @param player The player
     */
    public void removeKeybinds(@NotNull Player player) {
        playerKeybinds.remove(player.getUniqueId());
        lastKeyPress.remove(player.getUniqueId());
    }

    /**
     * Handles key press detection.
     * Note: Actual key detection requires client-side mod or using F3+N swap hands as trigger.
     * For now, we'll use the swap hands (F key) as a trigger mechanism.
     */
    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        // Detect item swap (F key) as ability trigger
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            Player player = event.getPlayer();
            if (player == null) return;

            // Check if player has a selected cell
            UUID uuid = player.getUniqueId();
            Map<String, String> keybinds = playerKeybinds.get(uuid);
            if (keybinds == null || keybinds.isEmpty()) return;

            // Debounce key presses (300ms)
            long now = System.currentTimeMillis();
            Map<String, Long> playerPresses = lastKeyPress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

            // For simplicity, we'll trigger abilities based on sneak + use combination
            // This is a workaround since we can't directly detect F/R keys without client mod
            if (player.isSneaking()) {
                // Check last press time for debouncing
                Long lastPress = playerPresses.get("ability_trigger");
                if (lastPress != null && (now - lastPress) < 300) {
                    return;
                }

                playerPresses.put("ability_trigger", now);

                // Trigger the first ability (you can enhance this logic)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var cellData = plugin.getAbilityManager().getPlayerData(player);
                    var activeCell = cellData.getData("selected_cell");
                    if (activeCell instanceof String cellId) {
                        var cell = plugin.getCellManager().getCell(cellId);
                        if (cell != null) {
                            // Trigger based on player state (crouching = ability 2, standing = ability 1)
                            String abilityId = player.isSneaking() ?
                                    cell.getSecondAbilityId() : cell.getFirstAbilityId();

                            ActivationContext context = new ActivationContext(ActivationTrigger.MANUAL);
                            plugin.getAbilityManager().activateAbility(player, abilityId, context);
                        }
                    }
                });
            }
        }
    }

    /**
     * Unregisters the packet listener.
     */
    public void shutdown() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        playerKeybinds.clear();
        lastKeyPress.clear();
    }
}