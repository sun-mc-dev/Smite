package me.sunmc.smite.api.event;

import me.sunmc.smite.api.ability.Ability;
import me.sunmc.smite.api.ability.AbilityContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an ability ends or is deactivated.
 *
 * <p>This event is not cancellable as the ability has already completed.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class AbilityEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Ability ability;
    private final AbilityContext context;

    /**
     * Constructs a new ability end event.
     *
     * @param ability the ability that ended
     * @param context the execution context
     */
    public AbilityEndEvent(@NotNull Ability ability, @NotNull AbilityContext context) {
        this.ability = ability;
        this.context = context;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the ability that ended.
     *
     * @return the ability
     */
    @NotNull
    public Ability getAbility() {
        return ability;
    }

    /**
     * Gets the execution context.
     *
     * @return the context
     */
    @NotNull
    public AbilityContext getContext() {
        return context;
    }

    /**
     * Gets the player the ability was active for.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return context.player();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
