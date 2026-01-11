/**
 * Root package for the Smite ability system plugin.
 *
 * <h2>Overview</h2>
 * <p>Smite is a high-performance, packet-based ability system for Minecraft Paper 1.21.11.
 * It provides a comprehensive framework for creating and managing combat abilities with
 * features like cooldown management, particle effects, and sound integration.</p>
 *
 * <h2>Architecture</h2>
 * <p>The plugin follows a modular architecture with clear separation of concerns:</p>
 * <ul>
 *   <li><b>API Layer</b> ({@link me.sunmc.smite.api}) - Public interfaces and contracts</li>
 *   <li><b>Abilities</b> ({@link me.sunmc.smite.abilities}) - Concrete ability implementations</li>
 *   <li><b>Configuration</b> ({@link me.sunmc.smite.config}) - Configuration management</li>
 *   <li><b>Listeners</b> ({@link me.sunmc.smite.listener}) - Event handling</li>
 *   <li><b>Packets</b> ({@link me.sunmc.smite.packet}) - NMS packet operations</li>
 *   <li><b>Commands</b> ({@link me.sunmc.smite.command}) - Command execution</li>
 * </ul>
 *
 * <h2>Core Abilities</h2>
 * <p>The plugin ships with two built-in abilities:</p>
 *
 * <h3>Heavenly Smite</h3>
 * <p>A lightning strike ability triggered by consecutive critical hits.
 * See {@link me.sunmc.smite.abilities.HeavenlySmiteAbility} for implementation details.</p>
 *
 * <h3>Demonic Spark</h3>
 * <p>A damage accumulation ability that stores hits and releases them in one burst.
 * See {@link me.sunmc.smite.abilities.DemonicSparkAbility} for implementation details.</p>
 *
 * <h2>API Usage</h2>
 * <p>External plugins can integrate with Smite using the public API:</p>
 *
 * <pre>{@code
 * // Get Smite instance
 * Smite smite = (Smite) Bukkit.getPluginManager().getPlugin("Smite");
 *
 * // Register a custom ability
 * MyCustomAbility ability = new MyCustomAbility();
 * smite.getAbilityRegistry().register(ability);
 *
 * // Activate an ability programmatically
 * AbilityContext context = new AbilityContext.Builder(player)
 *     .trigger(AbilityTrigger.MANUAL)
 *     .build();
 * smite.getAbilityManager().activateAbility(ability, context);
 *
 * // Listen to ability events
 * @EventHandler
 * public void onAbilityActivate(AbilityActivateEvent event) {
 *     // Your logic here
 * }
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <p>Smite is designed for maximum performance:</p>
 * <ul>
 *   <li>Direct NMS packet usage for particle/sound effects</li>
 *   <li>Efficient cooldown tracking with O(1) lookups</li>
 *   <li>Async configuration loading and saving</li>
 *   <li>Automatic cleanup of expired data structures</li>
 *   <li>Memory-efficient WeakHashMaps for player data</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All public APIs are thread-safe. However, ability activation and entity
 * interaction must occur on the main server thread. The plugin handles
 * thread coordination internally for async operations.</p>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>Paper API 1.21.11+</li>
 *   <li>Java 21+</li>
 *   <li>Kyori Adventure (bundled with Paper)</li>
 * </ul>
 *
 * @author SunMC
 * @version 1.0.0
 * @since 1.21.11
 */
package me.sunmc.smite;