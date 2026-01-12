# Smite - High-Performance Ability Plugin

A high-performance, packet-based ability system for Paper 1.21.11 servers with a flexible API for creating custom
abilities.

## Features

- ⚡ **High Performance**: Packet-based particle effects and asynchronous processing
- 🎨 **Client-Side Effects**: All GUI, titles, and action bars are client-based using PacketEvents
- 🔧 **Flexible API**: Easy-to-use API for creating custom abilities
- 🎯 **Zero Deprecated Methods**: Uses only modern Paper API methods
- 🌟 **Adventure API**: Modern text formatting using Adventure API
- ☕ **Java 21**: Built with modern Java features

## Included Abilities

### Heavenly Smite

**Cooldown:** 45 seconds

A lightning-based ability triggered by landing three critical hits in succession. Each strike deals 5 hearts (10.0
damage) bypassing armor entirely. Features cascading circular particles during critical hits and a dramatic explosion
effect on activation.

**Activation:** Land 3 critical hits within 3 seconds

### Demonic Spark

**Cooldown:** 45 seconds

Temporarily disables damage output for 5 seconds while storing all damage dealt. After the duration ends, unleashes all
stored damage in a single devastating strike with a massive X-shaped slash effect.

**Activation:** Manual activation via command or custom trigger

## Installation

1. Download the plugin JAR
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure in `plugins/Smite/config.yml`

## Building from Source

### Maven

```bash
mvn clean package
```

The compiled JAR will be in `target/` (Maven).

## Commands

| Command                                    | Description                  | Permission    |
|--------------------------------------------|------------------------------|---------------|
| `/smite list`                              | List all abilities           | `smite.use`   |
| `/smite activate <ability>`                | Manually activate an ability | `smite.use`   |
| `/smite info <ability>`                    | Show ability information     | `smite.use`   |
| `/smite cooldown <player> <ability> clear` | Clear a cooldown             | `smite.admin` |
| `/smite reload`                            | Reload configuration         | `smite.admin` |

## Permissions

- `smite.use` - Basic command usage (default: true)
- `smite.admin` - Admin commands (default: op)
- `smite.reload` - Reload config (default: op)
- `smite.cooldown` - Manage cooldowns (default: op)

## Creating Custom Abilities

The Smite API makes it easy to create custom abilities. Here's a complete example:

### Example: Fire Dash Ability

```java
package com.example.abilities;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.AbstractAbility;
import me.sunmc.smite.ability.api.AbilityType;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class FireDashAbility extends AbstractAbility {

    public FireDashAbility(@NotNull Smite plugin) {
        super(
                plugin,
                "fire_dash",                    // Unique ID
                "Fire Dash",                    // Display name
                30,                             // Cooldown in seconds
                "Dash forward in flames",       // Description
                AbilityType.MOVEMENT            // Ability type
        );
    }

    @Override
    protected boolean canActivateCustom(@NotNull Player player, @NotNull ActivationContext context) {
        // Custom activation checks
        return player.isOnGround();
    }

    @Override
    protected CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context) {
        // Execute on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Vector direction = player.getLocation().getDirection();
            direction.multiply(2).setY(0.5);
            player.setVelocity(direction);

            // Spawn fire particles using PacketEvents
            // ... particle code here ...
        });

        sendActivationMessage(player, "Blazing forward!");
        return CompletableFuture.completedFuture(ActivationResult.success());
    }
}
```

### Registering Your Ability

In your plugin's `onEnable()`:

```java

@Override
public void onEnable() {
    Smite smitePlugin = (Smite) Bukkit.getPluginManager().getPlugin("Smite");
    if (smitePlugin != null) {
        smitePlugin.getAbilityManager().registerAbility(new FireDashAbility(smitePlugin));
    }
}
```

## API Reference

### Core Interfaces

#### Ability

The main interface for all abilities. Implement this or extend `AbstractAbility`.

**Key Methods:**

- `String getId()` - Unique identifier
- `String getDisplayName()` - Display name
- `int getCooldown()` - Cooldown in seconds
- `boolean canActivate(Player, ActivationContext)` - Check if you can activate
- `CompletableFuture<ActivationResult> activate(Player, ActivationContext)` - Execute ability

#### AbstractAbility

Abstract base class providing common functionality:

- Automatic cooldown management
- Built-in title/action bar messaging
- Asynchronous execution support

### AbilityManager

Manages all registered abilities and player data.

```java
AbilityManager manager = plugin.getAbilityManager();

// Register an ability
manager.

registerAbility(new MyAbility(plugin));

// Get an ability
Ability ability = manager.getAbility("ability_id");

// Activate an ability
ActivationContext context = new ActivationContext(ActivationTrigger.MANUAL);
manager.

activateAbility(player, "ability_id",context);

// Get player data
PlayerAbilityData data = manager.getPlayerData(player);
data.

setData("custom_key",value);
```

### ActivationContext

Provides context for ability activation:

```java
ActivationContext context = new ActivationContext(ActivationTrigger.COMBAT);
context.

setTarget(targetEntity);
context.

setData("damage",10.0);

// Retrieve data
Entity target = context.getTarget();
Double damage = context.getData("damage", Double.class);
```

### AbilityType

Enum for categorizing abilities:

- `OFFENSIVE` - Damage-dealing abilities
- `DEFENSIVE` - Protective abilities
- `UTILITY` - Support abilities
- `MOVEMENT` - Movement-based abilities
- `HYBRID` - Combination abilities

### CooldownManager

Manages ability cooldowns:

```java
CooldownManager cooldowns = manager.getCooldownManager();

// Set cooldown
cooldowns.

setCooldown(player, ability, 45);

// Check cooldown
boolean onCooldown = cooldowns.isOnCooldown(player, ability);

// Get remaining time
long remaining = cooldowns.getRemainingCooldown(player, ability);

// Clear cooldown
cooldowns.

clearCooldown(player, ability);
```

## Using PacketEvents for Effects

PacketEvents allows for high-performance, client-side particle effects:

```java
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

// Create particle packet
WrapperPlayServerParticle packet = new WrapperPlayServerParticle(
        ParticleTypes.FLAME,           // Particle type
        true,                          // Long distance
        SpigotConversionUtil.fromBukkitVector(location.toVector()),
        new Vector3f(0, 0, 0),        // Offset
        0.1f,                         // Speed
        10                            // Count
);

// Send to player
PacketEvents.

        getAPI().

        getPlayerManager().

        sendPacket(player, packet);
```

## Advanced Features

### Asynchronous Processing

All abilities support asynchronous execution:

```java

@Override
private CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context) {
    return CompletableFuture.supplyAsync(() -> {
        // Heavy computation here
        return ActivationResult.success();
    });
}
```

### Player Data Storage

Store custom data per player:

```java
PlayerAbilityData data = manager.getPlayerData(player);

// Store data
data.

setData("combo_count",5);
data.

setData("last_ability","fire_dash");

// Retrieve data
Integer comboCount = (Integer) data.getData("combo_count");

// Track active ability
data.

setActiveAbility("heavenly_smite");

Optional<String> active = data.getActiveAbility();
```

### Custom Activation Triggers

Define when abilities activate:

```java
public enum ActivationTrigger {
    MANUAL,         // Player command
    COMBAT,         // Combat action
    CRITICAL_HIT,   // Critical hit
    DAMAGE_TAKEN,   // Taking damage
    KILL,           // Killing an entity
    CUSTOM          // Your custom trigger
}
```

## Configuration

`config.yml`:

```yaml
abilities:
  heavenly_smite:
    enabled: true
    cooldown: 45
    damage: 10.0
    required_crits: 3
    crit_chain_timeout: 3000

  demonic_spark:
    enabled: true
    cooldown: 45
    duration: 5

particles:
  use_packets: true
  max_distance: 64

performance:
  async_processing: true
  thread_pool_size: 4
```

## Dependencies

- **Paper API** 1.21.11+
- **PacketEvents** 2.7.0
- **Java** 21

## Performance Considerations

1. **Packet-based particles**: More efficient than spawning actual particles
2. **Asynchronous execution**: Heavy computations don't block main thread
3. **Efficient cooldown tracking**: O(1) lookups using hash maps
4. **Minimal memory footprint**: Player data cleaned on disconnect

## License

This plugin is provided as-is for educational and commercial use.

## Support

For issues, feature requests, or questions:

- Check the [API Documentation](#api-reference)
- Review example abilities in `src/main/java/me/sunmc/smite/ability/impl/`
- Open an issue on GitHub

## Credits

Developed by SunMC with 5+ years of Java experience for Minecraft Paper servers.