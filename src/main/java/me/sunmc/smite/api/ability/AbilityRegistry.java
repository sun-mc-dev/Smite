package me.sunmc.smite.api.ability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing ability registration and lookup.
 *
 * <p>This class provides thread-safe registration and retrieval of abilities.
 * All abilities must be registered here to be accessible by the ability management
 * system. The registry uses the ability's unique name as the key.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * AbilityRegistry registry = new AbilityRegistry();
 *
 * // Register a custom ability
 * MyCustomAbility ability = new MyCustomAbility();
 * registry.register(ability);
 *
 * // Retrieve an ability by name
 * Optional<Ability> found = registry.getAbility("my_custom_ability");
 *
 * // Check if registered
 * boolean exists = registry.isRegistered("my_custom_ability");
 * }</pre>
 *
 * @author SunMC
 * @version 1.0.0
 * @see Ability
 */
public final class AbilityRegistry {

    private final Map<String, Ability> abilities;
    private final Map<Class<? extends Ability>, Ability> abilitiesByClass;

    /**
     * Constructs a new empty ability registry.
     */
    public AbilityRegistry() {
        this.abilities = new ConcurrentHashMap<>();
        this.abilitiesByClass = new ConcurrentHashMap<>();
    }

    /**
     * Registers an ability in the registry.
     *
     * <p>If an ability with the same name already exists, it will be replaced
     * and a reference to the old ability will be returned.</p>
     *
     * @param ability the ability to register
     * @return the previously registered ability with the same name, or null if none existed
     * @throws NullPointerException     if ability is null
     * @throws IllegalArgumentException if ability name is null or empty
     */
    @Nullable
    public Ability register(@NotNull Ability ability) {
        Objects.requireNonNull(ability, "Ability cannot be null");

        String name = ability.getName();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Ability name cannot be null or empty");
        }

        abilitiesByClass.put(ability.getClass(), ability);
        return abilities.put(name.toLowerCase(), ability);
    }

    /**
     * Unregisters an ability by its name.
     *
     * @param name the name of the ability to unregister
     * @return the unregistered ability, or null if not found
     */
    @Nullable
    public Ability unregister(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");

        Ability removed = abilities.remove(name.toLowerCase());
        if (removed != null) {
            abilitiesByClass.remove(removed.getClass());
        }
        return removed;
    }

    /**
     * Unregisters an ability instance.
     *
     * @param ability the ability to unregister
     * @return true if the ability was registered and removed, false otherwise
     */
    public boolean unregister(@NotNull Ability ability) {
        Objects.requireNonNull(ability, "Ability cannot be null");

        boolean removed = abilities.remove(ability.getName().toLowerCase(), ability);
        if (removed) {
            abilitiesByClass.remove(ability.getClass());
        }
        return removed;
    }

    /**
     * Unregisters all abilities from the registry.
     */
    public void unregisterAll() {
        abilities.clear();
        abilitiesByClass.clear();
    }

    /**
     * Retrieves an ability by its name.
     *
     * @param name the name of the ability (case-insensitive)
     * @return an Optional containing the ability if found, empty otherwise
     */
    @NotNull
    public Optional<Ability> getAbility(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return Optional.ofNullable(abilities.get(name.toLowerCase()));
    }

    /**
     * Retrieves an ability by its class type.
     *
     * @param abilityClass the class of the ability
     * @param <T>          the ability type
     * @return an Optional containing the ability if found, empty otherwise
     */
    @NotNull
    public <T extends Ability> Optional<T> getAbility(@NotNull Class<T> abilityClass) {
        Objects.requireNonNull(abilityClass, "Ability class cannot be null");

        @SuppressWarnings("unchecked")
        T ability = (T) abilitiesByClass.get(abilityClass);
        return Optional.ofNullable(ability);
    }

    /**
     * Checks if an ability is registered by name.
     *
     * @param name the name of the ability (case-insensitive)
     * @return true if the ability is registered, false otherwise
     */
    public boolean isRegistered(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return abilities.containsKey(name.toLowerCase());
    }

    /**
     * Checks if an ability instance is registered.
     *
     * @param ability the ability to check
     * @return true if the ability is registered, false otherwise
     */
    public boolean isRegistered(@NotNull Ability ability) {
        Objects.requireNonNull(ability, "Ability cannot be null");
        return abilities.containsValue(ability);
    }

    /**
     * Checks if an ability class is registered.
     *
     * @param abilityClass the ability class to check
     * @return true if an instance of this class is registered, false otherwise
     */
    public boolean isRegistered(@NotNull Class<? extends Ability> abilityClass) {
        Objects.requireNonNull(abilityClass, "Ability class cannot be null");
        return abilitiesByClass.containsKey(abilityClass);
    }

    /**
     * Gets an unmodifiable collection of all registered abilities.
     *
     * @return a collection of all registered abilities
     */
    @NotNull
    public Collection<Ability> getRegisteredAbilities() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    /**
     * Gets an unmodifiable set of all registered ability names.
     *
     * @return a set of all registered ability names
     */
    @NotNull
    public Set<String> getRegisteredNames() {
        return Collections.unmodifiableSet(abilities.keySet());
    }

    /**
     * Gets the number of registered abilities.
     *
     * @return the count of registered abilities
     */
    public int getRegisteredCount() {
        return abilities.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no abilities are registered, false otherwise
     */
    public boolean isEmpty() {
        return abilities.isEmpty();
    }

    /**
     * Gets all enabled abilities from the registry.
     *
     * @return a list of enabled abilities
     */
    @NotNull
    public List<Ability> getEnabledAbilities() {
        return abilities.values().stream()
                .filter(Ability::isEnabled)
                .toList();
    }
}