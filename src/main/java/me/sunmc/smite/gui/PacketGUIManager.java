package me.sunmc.smite.gui;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.Ability;
import me.sunmc.smite.ability.cell.Cell;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages packet-based GUIs for cell selection.
 * Uses PacketEvents for client-side inventory rendering.
 */
public class PacketGUIManager extends PacketListenerAbstract {

    // GUI window IDs
    private static final int CELL_SELECTION_WINDOW_ID = 100;
    private static final int CONFIRMATION_WINDOW_ID = 101;

    // Inventory type constants (for Minecraft 1.14+)
    private static final int GENERIC_9X3 = 2; // 27 slots, 3 rows

    private final Smite plugin;
    private final Map<UUID, GUIType> openGUIs;
    private final Map<UUID, String> pendingCellSelection; // For confirmation GUI

    public PacketGUIManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
        this.pendingCellSelection = new HashMap<>();

        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    /**
     * Opens the cell selection GUI for a player.
     *
     * @param player The player
     */
    public void openCellSelectionGUI(@NotNull Player player) {
        // Check if player already has a locked cell
        plugin.getDatabaseManager().loadPlayerData(player.getUniqueId())
                .thenAccept(data -> {
                    if (data != null && data.cellLocked()) {
                        player.sendMessage(Component.text("You have already selected a cell!", NamedTextColor.RED));
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Create cell selection GUI
                        openGUIs.put(player.getUniqueId(), GUIType.CELL_SELECTION);

                        // Send open window packet - FIXED: Use int type instead of enum
                        WrapperPlayServerOpenWindow openPacket = new WrapperPlayServerOpenWindow(
                                CELL_SELECTION_WINDOW_ID,
                                GENERIC_9X3, // Changed from enum to int
                                Component.text("Select Your Cell", NamedTextColor.GOLD, TextDecoration.BOLD)
                        );
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, openPacket);

                        // Populate GUI with cells
                        populateCellSelectionGUI(player);
                    });
                });
    }

    /**
     * Populates the cell selection GUI with available cells.
     */
    private void populateCellSelectionGUI(@NotNull Player player) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(27, null));

        Collection<Cell> enabledCells = plugin.getCellManager().getEnabledCells();
        int slot = 10; // Start at slot 10 (centered)

        for (Cell cell : enabledCells) {
            if (slot >= 16) break; // Max 6 cells displayed

            // Create cell item
            ItemStack cellItem = createCellItem(cell);
            items.set(slot, cellItem);
            slot++;
        }

        // Send window items packet
        WrapperPlayServerWindowItems itemsPacket = new WrapperPlayServerWindowItems(
                CELL_SELECTION_WINDOW_ID,
                0,
                items,
                null
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, itemsPacket);
    }

    /**
     * Creates an ItemStack for a cell.
     */
    private @NotNull ItemStack createCellItem(@NotNull Cell cell) {
        // Convert Bukkit ItemStack to PacketEvents ItemStack
        org.bukkit.inventory.ItemStack bukkitItem = new org.bukkit.inventory.ItemStack(
                Material.valueOf(cell.getIconMaterial())
        );

        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(cell.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(cell.getDescription(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Abilities:", NamedTextColor.YELLOW));

            // Add ability names
            for (String abilityId : cell.getAbilityIds()) {
                Ability ability = plugin.getAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    lore.add(Component.text("  • ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(ability.getDisplayName(), NamedTextColor.WHITE)));
                    lore.add(Component.text("    " + ability.getDescription(), NamedTextColor.DARK_GRAY));
                }
            }

            lore.add(Component.empty());
            lore.add(Component.text("Click to select this cell", NamedTextColor.GREEN, TextDecoration.ITALIC));

            meta.lore(lore);
            bukkitItem.setItemMeta(meta);
        }

        return SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
    }

    /**
     * Opens the confirmation GUI for cell selection.
     */
    private void openConfirmationGUI(@NotNull Player player, @NotNull String cellId) {
        Cell cell = plugin.getCellManager().getCell(cellId);
        if (cell == null) return;

        openGUIs.put(player.getUniqueId(), GUIType.CONFIRMATION);
        pendingCellSelection.put(player.getUniqueId(), cellId);

        // Send open window packet - FIXED: Use int type instead of enum
        WrapperPlayServerOpenWindow openPacket = new WrapperPlayServerOpenWindow(
                CONFIRMATION_WINDOW_ID,
                GENERIC_9X3, // Changed from enum to int
                Component.text("Confirm Cell Selection", NamedTextColor.RED, TextDecoration.BOLD)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, openPacket);

        // Populate confirmation GUI
        populateConfirmationGUI(player, cell);
    }

    /**
     * Populates the confirmation GUI.
     */
    private void populateConfirmationGUI(@NotNull Player player, @NotNull Cell cell) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(27, null));

        // Cell display
        ItemStack cellDisplay = createCellItem(cell);
        items.set(13, cellDisplay);

        // Confirm button (Green)
        org.bukkit.inventory.ItemStack confirmItem = new org.bukkit.inventory.ItemStack(Material.LIME_TERRACOTTA);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("CONFIRM", NamedTextColor.GREEN, TextDecoration.BOLD));
            confirmMeta.lore(List.of(
                    Component.empty(),
                    Component.text("This choice is PERMANENT!", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("You cannot change cells later!", NamedTextColor.RED),
                    Component.empty(),
                    Component.text("Click to confirm", NamedTextColor.GREEN)
            ));
            confirmItem.setItemMeta(confirmMeta);
        }
        items.set(11, SpigotConversionUtil.fromBukkitItemStack(confirmItem));

        // Cancel button (Red)
        org.bukkit.inventory.ItemStack cancelItem = new org.bukkit.inventory.ItemStack(Material.RED_TERRACOTTA);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("CANCEL", NamedTextColor.RED, TextDecoration.BOLD));
            cancelMeta.lore(List.of(
                    Component.empty(),
                    Component.text("Go back to cell selection", NamedTextColor.GRAY)
            ));
            cancelItem.setItemMeta(cancelMeta);
        }
        items.set(15, SpigotConversionUtil.fromBukkitItemStack(cancelItem));

        // Send window items packet
        WrapperPlayServerWindowItems itemsPacket = new WrapperPlayServerWindowItems(
                CONFIRMATION_WINDOW_ID,
                0,
                items,
                null
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, itemsPacket);
    }

    /**
     * Handles GUI click events.
     */
    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        GUIType guiType = openGUIs.get(uuid);
        if (guiType == null) return;

        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);

        event.setCancelled(true); // Prevent actual inventory modification

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (guiType) {
                case CELL_SELECTION -> handleCellSelectionClick(player, packet.getSlot());
                case CONFIRMATION -> handleConfirmationClick(player, packet.getSlot());
            }
        });
    }

    /**
     * Handles clicks in the cell selection GUI.
     */
    private void handleCellSelectionClick(@NotNull Player player, int slot) {
        // Get cell at slot
        Collection<Cell> enabledCells = plugin.getCellManager().getEnabledCells();
        List<Cell> cellList = new ArrayList<>(enabledCells);

        int cellIndex = slot - 10;
        if (cellIndex < 0 || cellIndex >= cellList.size()) return;

        Cell cell = cellList.get(cellIndex);
        closeGUI(player);

        // Open confirmation GUI
        openConfirmationGUI(player, cell.getId());
    }

    /**
     * Handles clicks in the confirmation GUI.
     */
    private void handleConfirmationClick(@NotNull Player player, int slot) {
        String cellId = pendingCellSelection.get(player.getUniqueId());
        if (cellId == null) return;

        if (slot == 11) { // Confirm
            // Lock the cell for the player
            plugin.getDatabaseManager().savePlayerData(player, cellId, true);

            // Store in player data
            var playerData = plugin.getAbilityManager().getPlayerData(player);
            playerData.setData("selected_cell", cellId);
            playerData.setData("cell_locked", true);

            player.sendMessage(Component.text("Cell selected: ", NamedTextColor.GREEN)
                    .append(Component.text(Objects.requireNonNull(plugin.getCellManager().getCell(cellId)).getDisplayName(), NamedTextColor.GOLD)));

            closeGUI(player);
            pendingCellSelection.remove(player.getUniqueId());

        } else if (slot == 15) { // Cancel
            closeGUI(player);
            pendingCellSelection.remove(player.getUniqueId());

            // Reopen cell selection
            openCellSelectionGUI(player);
        }
    }

    /**
     * Closes the GUI for a player.
     */
    public void closeGUI(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        GUIType guiType = openGUIs.remove(uuid);

        if (guiType != null) {
            int windowId = guiType == GUIType.CELL_SELECTION ?
                    CELL_SELECTION_WINDOW_ID : CONFIRMATION_WINDOW_ID;

            WrapperPlayServerCloseWindow closePacket = new WrapperPlayServerCloseWindow(windowId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, closePacket);
        }
    }

    /**
     * Shuts down the GUI manager.
     */
    public void shutdown() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        openGUIs.clear();
        pendingCellSelection.clear();
    }

    /**
     * Enum for GUI types.
     */
    private enum GUIType {
        CELL_SELECTION,
        CONFIRMATION
    }
}