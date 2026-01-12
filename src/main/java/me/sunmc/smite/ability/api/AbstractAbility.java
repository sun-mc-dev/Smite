package me.sunmc.smite.ability.api;

import me.sunmc.smite.Smite;
import me.sunmc.smite.util.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for abilities, providing common functionality.
 * Extend this class to create custom abilities with minimal boilerplate.
 */
public abstract class AbstractAbility implements Ability {

    protected final Smite plugin;
    protected final String id;
    protected final String displayName;
    protected final int cooldown;
    protected final String description;
    protected final AbilityType type;
    protected boolean enabled;

    protected AbstractAbility(@NotNull Smite plugin, @NotNull String id, @NotNull String displayName,
                              int cooldown, @NotNull String description, @NotNull AbilityType type) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.description = description;
        this.type = type;
        this.enabled = true;
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    @NotNull
    public String getDescription() {
        return description;
    }

    @Override
    @NotNull
    public AbilityType getType() {
        return type;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean canActivate(@NotNull Player player, @NotNull ActivationContext context) {
        if (!enabled) {
            return false;
        }

        CooldownManager cooldownManager = plugin.getAbilityManager().getCooldownManager();
        if (cooldownManager.isOnCooldown(player, this)) {
            long remaining = cooldownManager.getRemainingCooldown(player, this);
            sendCooldownMessage(player, remaining);
            return false;
        }

        return canActivateCustom(player, context);
    }

    /**
     * Custom activation check for subclasses to implement.
     *
     * @param player  The player attempting to activate
     * @param context The activation context
     * @return true if the ability can be activated
     */
    protected abstract boolean canActivateCustom(@NotNull Player player, @NotNull ActivationContext context);

    @Override
    @NotNull
    public CompletableFuture<ActivationResult> activate(@NotNull Player player, @NotNull ActivationContext context) {
        if (!canActivate(player, context)) {
            return CompletableFuture.completedFuture(ActivationResult.failure("Cannot activate ability"));
        }

        // Set cooldown
        plugin.getAbilityManager().getCooldownManager().setCooldown(player, this, cooldown);

        // Execute ability
        return executeAbility(player, context);
    }

    /**
     * Execute the ability logic. This is called asynchronously.
     *
     * @param player  The player activating the ability
     * @param context The activation context
     * @return A CompletableFuture with the activation result
     */
    protected abstract CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context);

    @Override
    public void onDeactivate(@NotNull Player player) {
        // Default implementation does nothing
    }

    /**
     * Sends a cooldown message to the player using titles.
     *
     * @param player           The player
     * @param remainingSeconds Remaining cooldown in seconds
     */
    protected void sendCooldownMessage(@NotNull Player player, long remainingSeconds) {
        Component subtitle = Component.text("Cooldown: ", NamedTextColor.RED)
                .append(Component.text(remainingSeconds + "s", NamedTextColor.YELLOW));

        Title title = Title.title(
                Component.empty(),
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(250),
                        Duration.ofMillis(1000),
                        Duration.ofMillis(250)
                )
        );

        player.showTitle(title);
    }

    /**
     * Sends an activation message to the player using titles.
     *
     * @param player  The player
     * @param message The message to display
     */
    protected void sendActivationMessage(@NotNull Player player, @NotNull String message) {
        Component title = Component.text(displayName, NamedTextColor.GOLD);
        Component subtitle = Component.text(message, NamedTextColor.YELLOW);

        Title titleObj = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                )
        );

        player.showTitle(titleObj);
    }

    /**
     * Sends an action bar message to the player.
     *
     * @param player  The player
     * @param message The message to display
     */
    protected void sendActionBar(@NotNull Player player, @NotNull Component message) {
        player.sendActionBar(message);
    }
}