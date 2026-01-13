# Smite - High-Performance Ability Plugin

A high-performance, packet-based ability system for Paper 1.21.11 servers with a flexible Cell-based API, persistent SQLite storage, and keybind support.

## Features

- ⚡ **High Performance**: Packet-based particle effects, GUIs, and asynchronous processing
- 🔐 **Cell System**: Players select one permanent Cell containing two abilities
- 🗄️ **SQLite Database**: Persistent player data and keybind storage
- ⌨️ **Keybind Support**: Customizable keybinds for abilities
- 🎨 **Packet-Based GUIs**: Client-side inventory rendering using PacketEvents
- 🎯 **Zero Deprecated Methods**: Uses only modern Paper API methods
- 🌟 **Adventure API**: Modern text formatting using Adventure API
- ☕ **Java 21**: Built with modern Java features

## Cell System

### What are Cells?

Cells are permanent ability containers that players must choose when they first join. Each Cell contains exactly **two abilities**. Once a player selects a Cell, **they cannot change it**.

### Available Cells

#### Spark Cell
**Abilities:**
- **Heavenly Smite** - Lightning-based ability triggered by critical hits
- **Demonic Spark** - Damage storage and release ability

## Included Abilities

### Heavenly Smite

**Cooldown:** 45 seconds  
**Damage:** 10.0 (5 hearts)  
**Type:** Offensive

A lightning-based ability triggered by landing three critical hits in succession. Each strike deals 5 hearts (10.0 damage) bypassing armor entirely. Features cascading circular particles during critical hits and a dramatic explosion effect on activation.

**Activation:** Land 3 critical hits within 3 seconds  
**Default Keybind:** R

### Demonic Spark

**Cooldown:** 45 seconds  
**Duration:** 5 seconds  
**Type:** Hybrid

Temporarily disables damage output for 5 seconds while storing all damage dealt. After the duration ends, unleashes all stored damage in a single devastating strike with a massive X-shaped slash effect.

**Activation:** Manual activation  
**Default Keybind:** F

## Installation

1. Download the plugin JAR
2. Place in your server's `plugins` folder
3. Restart your server
4. Players will be prompted to select a Cell on first join

## Building from Source

### Maven

```bash
mvn clean package
```

The compiled JAR will be in `target/` directory.

## Commands

| Command                                    | Description                  | Permission    |
|--------------------------------------------|------------------------------|---------------|
| `/smite select`                            | Open cell selection GUI      | `smite.use`   |
| `/smite cells`                             | List all available cells     | `smite.use`   |
| `/smite list`                              | List all abilities           | `smite.use`   |
| `/smite activate <ability>`                | Manually activate an ability | `smite.use`   |
| `/smite info <ability>`                    | Show ability information     | `smite.use`   |
| `/smite keybind <ability> <key>`           | Set ability keybind          | `smite.use`   |
| `/smite cooldown <player> <ability> clear` | Clear a cooldown             | `smite.admin` |
| `/smite reload`                            | Reload configuration         | `smite.admin` |

## Permissions

- `smite.use` - Basic command usage (default: true)
- `smite.admin` - Admin commands (default: op)
- `smite.reload` - Reload config (default: op)
- `smite.cooldown` - Manage cooldowns (default: op)

## Keybinds

Players can set custom keybinds for their abilities using `/smite keybind <ability> <key>`.

**Default Keybinds:**
- First ability in cell: `KEY_R` (R key)
- Second ability in cell: `KEY_F` (F key)

**Note:** Due to Minecraft limitations, keybinds work through packet detection. Players can activate abilities by:
1. Sneaking (Shift) + using the assigned key
2. Using `/smite activate <ability>`

## Creating Custom Cells

The Cell API makes it easy to create custom ability combinations. Here's how:

### 1. Create Your Abilities

First, create your custom abilities by extending `AbstractAbility`:

```java
package com.example.abilities;

import me.sunmc.smite.Smite;
import me.sunmc.smite.ability.api.AbstractAbility;
import me.sunmc.smite.ability.api.AbilityType;
import me.sunmc.smite.ability.api.ActivationContext;
import me.sunmc.smite.ability.api.ActivationResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class IceShardAbility extends AbstractAbility {

    public IceShardAbility(@NotNull Smite plugin) {
        super(
                plugin,
                "ice_shard",           // Unique ID
                "Ice Shard",            // Display name
                30,                     // Cooldown in seconds
                "Launch deadly ice shards at enemies",
                AbilityType.OFFENSIVE
        );
    }

    @Override
    protected boolean canActivateCustom(@NotNull Player player, @NotNull ActivationContext context) {
        return true;
    }

    @Override
    protected CompletableFuture<ActivationResult> executeAbility(@NotNull Player player, @NotNull ActivationContext context) {
        // Your ability logic here
        sendActivationMessage(player, "Ice shards launched!");
        return CompletableFuture.completedFuture(ActivationResult.success());
    }
}
```

### 2. Register Your Cell

In your plugin's `onEnable()` or Smite's main class:

```java
// Register abilities
plugin.getAbilityManager().registerAbility(new IceShardAbility(plugin));
plugin.getAbilityManager().registerAbility(new FrozenHeartAbility(plugin));

// Create and register cell
Cell frostCell = new Cell(
        "frost_cell",               // Unique ID
        "Frost Cell",               // Display name
        "Control ice and freeze your enemies",
        "ice_shard",                // First ability ID
        "frozen_heart",             // Second ability ID
        "ICE",                      // Icon material
        true                        // Enabled
);

plugin.getCellManager().registerCell(frostCell);
```

## API Reference

### Core Interfaces

#### Cell

Represents a container of two abilities.

**Key Methods:**
- `String getId()` - Unique identifier
- `String getDisplayName()` - Display name
- `List<String> getAbilityIds()` - Get both ability IDs
- `String getFirstAbilityId()` - Get first ability
- `String getSecondAbilityId()` - Get second ability
- `boolean containsAbility(String)` - Check if cell has ability

#### CellManager

Manages all registered cells.

```java
CellManager manager = plugin.getCellManager();

// Register a cell
manager.registerCell(new Cell(...));

// Get a cell
Cell cell = manager.getCell("cell_id");

// Get all cells
Collection<Cell> cells = manager.getAllCells();

// Find cell by ability
Cell cell = manager.findCellByAbility("ability_id");
```

### Database System

#### DatabaseManager

Handles persistent data storage.

```java
DatabaseManager db = plugin.getDatabaseManager();

// Save player data
db.savePlayerData(player, "spark_cell", true);

// Load player data
db.loadPlayerData(player.getUniqueId())
    .thenAccept(data -> {
        // Use data
    });

// Save keybind
db.saveKeybind(player.getUniqueId(), "ability_id", "KEY_R");

// Load keybind
db.loadKeybind(player.getUniqueId(), "ability_id")
    .thenAccept(keybind -> {
        // Use keybind
    });
```

### Keybind System

#### KeybindManager

Manages player keybinds.

```java
KeybindManager keybinds = plugin.getKeybindManager();

// Set keybind
keybinds.setKeybind(player, "ability_id", "KEY_R");

// Get keybind
String keybind = keybinds.getKeybind(player, "ability_id");

// Load keybinds from database
keybinds.loadKeybinds(player);
```

### GUI System

#### PacketGUIManager

Manages packet-based GUIs.

```java
PacketGUIManager gui = plugin.getGuiManager();

// Open cell selection GUI
gui.openCellSelectionGUI(player);

// Close GUI
gui.closeGUI(player);
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

database:
  file: "smite.db"
```

## Database Schema

### player_data Table

| Column        | Type    | Description               |
|---------------|---------|---------------------------|
| uuid          | TEXT    | Player UUID (Primary Key) |
| player_name   | TEXT    | Player name               |
| selected_cell | TEXT    | Selected cell ID          |
| cell_locked   | BOOLEAN | Whether cell is permanent |
| created_at    | INTEGER | Creation timestamp        |
| updated_at    | INTEGER | Last update timestamp     |

### player_keybinds Table

| Column     | Type | Description    |
|------------|------|----------------|
| uuid       | TEXT | Player UUID    |
| ability_id | TEXT | Ability ID     |
| keybind    | TEXT | Keybind string |

## Advanced Features

### Packet-Based Systems

The plugin uses PacketEvents for:
1. **Particles** - Client-side particle rendering
2. **GUIs** - Inventory rendering without server-side inventories
3. **Keybind Detection** - Efficient key press detection
4. **Title/Action Bar** - Client-side text rendering

### Asynchronous Operations

All database operations are asynchronous using `CompletableFuture`:

```java
plugin.getDatabaseManager().loadPlayerData(uuid)
    .thenAccept(data -> {
        // Handle data
    })
    .exceptionally(ex -> {
        // Handle error
        return null;
    });
```

## Dependencies

- **Paper API** 1.21.11+
- **PacketEvents** 2.11.1
- **SQLite JDBC** 3.47.1.0
- **Java** 21

## Performance Considerations

1. **Packet-based rendering**: More efficient than spawning actual particles
2. **Asynchronous database**: All I/O operations don't block main thread
3. **Efficient cooldown tracking**: O(1) lookups using hash maps
4. **Client-side GUIs**: No server inventory overhead
5. **Minimal memory footprint**: Player data cleaned on disconnect

## Roadmap

- [ ] Add more ability cells
- [ ] Implement ability upgrade system
- [ ] Add ability combos
- [ ] Create web dashboard for statistics
- [ ] Add PvP arenas with ability restrictions

## License

This plugin is provided as-is for educational and commercial use.

## Support

For issues, feature requests, or questions:
- Check the [API Documentation](#api-reference)
- Review example abilities in `src/main/java/me/sunmc/smite/ability/impl/`
- Open an issue on GitHub

## Credits

Developed by SunMC with 5+ years of Java experience for Minecraft Paper servers.