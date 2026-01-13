package me.sunmc.smite.ability.cell;

import me.sunmc.smite.Smite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all cells in the system.
 */
public class CellManager {

    private final Smite plugin;
    private final Map<String, Cell> cells;

    public CellManager(@NotNull Smite plugin) {
        this.plugin = plugin;
        this.cells = new HashMap<>();
    }

    /**
     * Registers a cell.
     *
     * @param cell The cell to register
     */
    public void registerCell(@NotNull Cell cell) {
        cells.put(cell.getId(), cell);
        plugin.getLogger().info("Registered cell: " + cell.getDisplayName());
    }

    /**
     * Unregisters a cell.
     *
     * @param cellId The cell ID to unregister
     */
    public void unregisterCell(@NotNull String cellId) {
        cells.remove(cellId);
    }

    /**
     * Gets a cell by ID.
     *
     * @param cellId The cell ID
     * @return The cell, or null if not found
     */
    @Nullable
    public Cell getCell(@NotNull String cellId) {
        return cells.get(cellId);
    }

    /**
     * Gets all registered cells.
     *
     * @return Collection of all cells
     */
    @NotNull
    public Collection<Cell> getAllCells() {
        return cells.values();
    }

    /**
     * Gets all enabled cells.
     *
     * @return Collection of enabled cells
     */
    @NotNull
    public Collection<Cell> getEnabledCells() {
        return cells.values().stream()
                .filter(Cell::isEnabled)
                .toList();
    }

    /**
     * Finds a cell that contains the specified ability.
     *
     * @param abilityId The ability ID
     * @return The cell containing this ability, or null
     */
    @Nullable
    public Cell findCellByAbility(@NotNull String abilityId) {
        return cells.values().stream()
                .filter(cell -> cell.containsAbility(abilityId))
                .findFirst()
                .orElse(null);
    }
}