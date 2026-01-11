package me.sunmc.smite.api.event;

import me.sunmc.smite.api.ability.Ability;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a cooldown is set or expires for an ability.
 *
 * <p>This event is not cancellable as cooldowns are managed internally.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
final class AbilityCooldownEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Ability ability;
    private final CooldownAction action;
    private final long durationMillis;

    /**
     * Constructs a new cooldown event.
     *
     * @param player         the player affected
     * @param ability        the ability
     * @param action         the cooldown action (START or EXPIRE)
     * @param durationMillis the cooldown duration
     */
    public AbilityCooldownEvent(@NotNull Player player, @NotNull Ability ability,
                                @NotNull CooldownAction action, long durationMillis) {
        this.player = player;
        this.ability = ability;
        this.action = action;
        this.durationMillis = durationMillis;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the player affected by the cooldown change.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the ability with the cooldown change.
     *
     * @return the ability
     */
    @NotNull
    public Ability getAbility() {
        return ability;
    }

    /**
     * Gets the cooldown action.
     *
     * @return the action
     */
    @NotNull
    public CooldownAction getAction() {
        return action;
    }

    /**
     * Gets the cooldown duration in milliseconds.
     *
     * @return the duration
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Represents the type of cooldown action.
     */
    public enum CooldownAction {
        /**
         * Cooldown has started.
         */
        START,

        /**
         * Cooldown has expired.
         */
        EXPIRE
    }
}