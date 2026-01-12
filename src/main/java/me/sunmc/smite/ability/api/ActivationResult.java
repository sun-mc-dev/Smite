package me.sunmc.smite.ability.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of an ability activation.
 */
public class ActivationResult {
    private final boolean success;
    private final String message;
    private final Map<String, Object> resultData;

    private ActivationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.resultData = new HashMap<>();
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull ActivationResult success() {
        return new ActivationResult(true, "");
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ActivationResult success(@NotNull String message) {
        return new ActivationResult(true, message);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull ActivationResult failure(@NotNull String reason) {
        return new ActivationResult(false, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    public void setResultData(@NotNull String key, @Nullable Object value) {
        resultData.put(key, value);
    }

    @Nullable
    public Object getResultData(@NotNull String key) {
        return resultData.get(key);
    }
}
