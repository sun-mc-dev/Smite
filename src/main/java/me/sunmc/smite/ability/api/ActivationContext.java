package me.sunmc.smite.ability.api;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Context provided when an ability is activated.
 */
public class ActivationContext {
    private final ActivationTrigger trigger;
    private final Map<String, Object> data;
    private Entity target;

    public ActivationContext(@NotNull ActivationTrigger trigger) {
        this.trigger = trigger;
        this.data = new HashMap<>();
    }

    @NotNull
    public ActivationTrigger getTrigger() {
        return trigger;
    }

    @Nullable
    public Entity getTarget() {
        return target;
    }

    public void setTarget(@Nullable Entity target) {
        this.target = target;
    }

    public void setData(@NotNull String key, @Nullable Object value) {
        data.put(key, value);
    }

    @Nullable
    public Object getData(@NotNull String key) {
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getData(@NotNull String key, @NotNull Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}

