package me.sunmc.smite.ability.cell;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Cell that contains two abilities.
 * Players select one Cell permanently and gain access to its abilities.
 */
public class Cell {

    private final String id;
    private final String displayName;
    private final String description;
    private final List<String> abilityIds;
    private final String iconMaterial;
    private final boolean enabled;

    /**
     * Constructs a new Cell.
     *
     * @param id           Unique identifier for the cell
     * @param displayName  Display name
     * @param description  Cell description
     * @param ability1Id   First ability ID
     * @param ability2Id   Second ability ID
     * @param iconMaterial Material name for GUI icon
     * @param enabled      Whether the cell is enabled
     */
    public Cell(@NotNull String id, @NotNull String displayName, @NotNull String description,
                @NotNull String ability1Id, @NotNull String ability2Id,
                @NotNull String iconMaterial, boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.abilityIds = new ArrayList<>(2);
        this.abilityIds.add(ability1Id);
        this.abilityIds.add(ability2Id);
        this.iconMaterial = iconMaterial;
        this.enabled = enabled;
    }

    /**
     * Gets the cell's unique ID.
     *
     * @return The cell ID
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Gets the cell's display name.
     *
     * @return The display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the cell's description.
     *
     * @return The description
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Gets the list of ability IDs in this cell.
     *
     * @return List of ability IDs (always 2 abilities)
     */
    @NotNull
    public List<String> getAbilityIds() {
        return new ArrayList<>(abilityIds);
    }

    /**
     * Gets the first ability ID.
     *
     * @return First ability ID
     */
    @NotNull
    public String getFirstAbilityId() {
        return abilityIds.get(0);
    }

    /**
     * Gets the second ability ID.
     *
     * @return Second ability ID
     */
    @NotNull
    public String getSecondAbilityId() {
        return abilityIds.get(1);
    }

    /**
     * Gets the icon material name.
     *
     * @return Material name for GUI display
     */
    @NotNull
    public String getIconMaterial() {
        return iconMaterial;
    }

    /**
     * Checks if this cell is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if this cell contains the specified ability.
     *
     * @param abilityId The ability ID to check
     * @return true if the cell contains this ability
     */
    public boolean containsAbility(@NotNull String abilityId) {
        return abilityIds.contains(abilityId);
    }
}