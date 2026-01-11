package me.sunmc.smite.api.event;

import me.sunmc.smite.api.ability.Ability;
import me.sunmc.smite.api.ability.AbilityContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an ability is about to be activated.
 *
 * <p>This event is cancellable. If cancelled, the ability will not activate
 * and no cooldown will be applied.</p>
 *
 * @author SunMC
 * @version 1.0.0
 */
public final class AbilityActivateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Ability ability;
    private final AbilityContext context;
    private boolean cancelled;

    /**
     * Constructs a new ability activate event.
     *
     * @param ability the ability being activated
     * @param context the activation context
     */
    public AbilityActivateEvent(@NotNull Ability ability, @NotNull AbilityContext context) {
        this.ability = ability;
        this.context = context;
        this.cancelled = false;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Gets the ability being activated.
     *
     * @return the ability
     */
    @NotNull
    public Ability getAbility() {
        return ability;
    }

    /**
     * Gets the activation context.
     *
     * @return the context
     */
    @NotNull
    public AbilityContext getContext() {
        return context;
    }

    /**
     * Gets the player activating the ability.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return context.player();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
