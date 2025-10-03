# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Minecraft Forge 1.20.1 mod called "Kimetsunoyaiba Multiplayer" that enables player animations to work in multiplayer environments. The mod integrates with the `player-animation-lib-forge` library to provide synchronized player animations across multiplayer sessions.

- **Mod ID**: `kimetsunoyaibamultiplayer`
- **Minecraft Version**: 1.20.1
- **Forge Version**: 47.4.0
- **Java Version**: 17

## Build and Development Commands

### Basic Build Commands
```bash
# Build the mod
./gradlew build

# Clean build artifacts
./gradlew clean

# Run development client
./gradlew runClient

# Run development server
./gradlew runServer

# Run data generators
./gradlew runData

# Generate Eclipse run configurations
./gradlew genEclipseRuns

# Generate IntelliJ run configurations
./gradlew genIntellijRuns

# Refresh dependencies
./gradlew --refresh-dependencies
```

### IDE Setup
- **Eclipse**: Run `./gradlew genEclipseRuns` then import as existing Gradle project
- **IntelliJ**: Import `build.gradle` file, then run `./gradlew genIntellijRuns`

## Architecture Overview

### Core Structure
- **Main Mod Class**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/KimetsunoyaibaMultiplayer.java`
  - Handles mod initialization, event registration, and lifecycle management
  - Registers blocks, items, and creative tabs using DeferredRegister
  - Contains client-side event handling in nested `ClientModEvents` class

- **Configuration**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/Config.java`
  - Uses ForgeConfigSpec for mod configuration
  - Provides example config values (logDirtBlock, magicNumber, items list)
  - Automatically validates and converts config values on load

### Event System
The mod uses Forge's event bus system:
- **Mod Event Bus**: For mod lifecycle events (setup, config loading)
- **Forge Event Bus**: For game events (server starting, etc.)
- **Client-only Events**: Handled in `ClientModEvents` nested class with `@Mod.EventBusSubscriber`

### Dependencies
- **External Library**: `player-animation-lib-forge-1.20.1-1.0.2-rc1.jar` in `libs/` directory
- **Forge Dependency**: Declared in `build.gradle` as deobfuscated dependency
- **Config**: Uses ForgeConfigSpec for configuration management

### Resource Structure
- **Mod Metadata**: `src/main/resources/META-INF/mods.toml` - Contains mod information and dependencies
- **Pack Metadata**: `src/main/resources/pack.mcmeta` - Resource pack version info
- **Generated Resources**: `src/generated/resources/` - Output from data generators

## Test Commands

The mod includes test commands to verify the animation sync system is working:

### Server-side Command: `/testanim`
- Available on servers and LAN worlds
- Plays the "sword_to_left" animation on all connected players
- Sends animation packets to all clients
- Useful for testing multiplayer animation synchronization

### Client-side Command: `/testanimc`
- Available in single-player and on clients
- Plays the animation locally and sends sync packet to server
- Useful for testing client-to-server animation transmission

Both commands will show debug messages if debug mode is enabled in the config.

## Key Development Notes

### Mod Registration Pattern
The mod uses DeferredRegister pattern for all registrations:
- Create DeferredRegister instances for each registry type
- Register objects as RegistryObject fields
- Register the DeferredRegister to the mod event bus in constructor

### Configuration System
Config values are defined as ForgeConfigSpec values and cached as static fields. The `Config.onLoad()` method handles updating cached values when config changes.

### Client/Server Separation
Client-only code is properly separated using `@Mod.EventBusSubscriber(value = Dist.CLIENT)` and the `ClientModEvents` nested class pattern.

### Animation Library Integration
The mod depends on both `player-animation-lib-forge` and `mobplayeranimator` for multiplayer animation synchronization:
- **PlayerAnimator**: Provides the base animation framework for detecting and creating animations
- **Mob Player Animator**: Extends PlayerAnimator to enable animations on other players/mobs (critical for multiplayer sync)
- Both libraries are declared as dependencies in `build.gradle` and `mods.toml`

### Animation System Architecture
1. **Animation Detection** (`AnimationTracker.java`): Uses reflection to detect active KeyframeAnimationPlayer instances in the local player's AnimationStack
2. **Network Synchronization** (`AnimationSyncPacket.java`, `ModNetworking.java`): Transmits animation events between client and server
3. **Animation Application** (`AnimationSyncHandler.java`): Receives animation sync packets and applies animations to other players using Mob Player Animator
4. **Fallback System**: If original animation cannot be found, tries kimetsunoyaiba animations like "sword_to_left" as fallbacks

### Key Lessons Learned
- **PlayerAnimator Limitation**: The base PlayerAnimator library can only apply animations to the local player, not to other players
- **Mob Player Animator Solution**: This extension enables animations on other players, solving the core multiplayer sync requirement
- **Animation Registry**: Finding animations by ResourceLocation can be challenging; fallback strategies are essential
- **Reflection Usage**: Animation detection requires reflection to access private fields in AnimationStack and ModifierLayer classes
- **Animation Name vs UUID Issue**: KeyframeAnimation objects have UUIDs, but the registry stores animations by their simple names (e.g., "sword_to_left")
- **Multiple Registry Lookup**: Animations may be registered under different namespaces ("kimetsunoyaiba", "playeranimator", etc.)

### Animation Identification Strategy
The mod uses a multi-step approach to extract animation names:
1. **extraData Field**: First tries to extract name from KeyframeAnimation.extraData
2. **Reflection Scan**: Searches all fields for strings matching known animation names
3. **Known Names List**: Validates against the complete list of kimetsunoyaiba animation names
4. **Multiple Namespace Search**: Tries various ResourceLocation combinations during registry lookup

## Breathing Technique System

The mod implements a comprehensive breathing technique system for custom nichirin swords with special abilities.

### Architecture Overview

#### Core Components

1. **BreathingSwordItem** (`items/BreathingSwordItem.java`)
   - Abstract base class for all breathing swords
   - Extends `SwordItem` with Diamond tier stats (3 attack damage, -2.4 attack speed)
   - Handles right-click activation of breathing forms
   - Manages form cycling with R key integration
   - Displays form names with color codes (§6 for technique, §b for form name)

2. **BreathingForm** (`breathingtechnique/BreathingForm.java`)
   - Data class representing a single breathing technique form
   - Contains: name, description, cooldown, and effect executor
   - Effect is a BiConsumer<Player, Level> lambda for maximum flexibility

3. **BreathingTechnique** (`breathingtechnique/BreathingTechnique.java`)
   - Container for a complete set of breathing forms
   - Manages form indexing and retrieval
   - Each technique has a unique name and list of forms

4. **AnimationHelper** (`breathingtechnique/AnimationHelper.java`)
   - Centralized animation playback system
   - Supports timed animations with automatic cancellation
   - `playAnimation(player, name)` - plays full animation
   - `playAnimation(player, name, maxTicks)` - plays animation with max duration (used for attack animations, limited to 10 ticks)
   - Handles client-side playback and network synchronization
   - Uses threaded scheduling for animation cancellation

5. **AbilityScheduler** (`breathingtechnique/AbilityScheduler.java`)
   - Server-side task scheduler for delayed/repeated actions
   - `scheduleOnce(player, action, delayTicks)` - one-time delayed execution
   - `scheduleRepeating(player, action, intervalTicks, durationTicks)` - repeated execution
   - Tick-based execution synchronized with server tick event
   - Automatically cleans up completed tasks

6. **PlayerBreathingData** (`breathingtechnique/PlayerBreathingData.java`)
   - Stores per-player breathing technique state
   - Tracks currently selected form index for each player
   - Thread-safe UUID-based storage
   - Persists form selection during gameplay session

### Implemented Breathing Techniques

#### Ice Breathing (nichirinsword_ice, nichirinsword_hanazawa)
- **Particle Effects**: Light blue dust particles (RGB: 0.5, 0.8, 1.0)
- **7 Forms Total**:
  1. **Paralyzing Icicle**: Speed thrust attack with slowness/mining fatigue debuffs (5 block range)
  2. **Winter Wrath**: Circular attack pattern around target with tornado-like particles
  3. **Merciful Hail Fall**: Aerial hovering with rapid downward slashes
  4. **Flash Freeze Path**: Ice block path creation with teleportation
  5. **Frozen Barrage**: High-speed multi-directional attacks
  6. **Arctic Devastation**: Ground slam with freeze effect
  7. **Absolute Zero** (Hanazawa exclusive): Ultimate freezing technique

#### Frost Breathing (nichirinsword_frost, nichirinsword_hiori)
- **Particle Effects**: Snowflake particles
- **7 Forms Total**:
  1. **Frost Blade Rush**: Triple forward thrust
  2. **Whirling Snowstorm**: Spinning AoE attack
  3. **Crystalline Spear**: Piercing ice projectile
  4. **Glacial Prison**: Ice wall creation
  5. **Diamond Dust Barrage**: Rapid multi-hit combo
  6. **Permafrost Domain**: Area freeze effect
  7. **Eternal Winter** (Hiori exclusive): Ultimate frost technique

### Key Implementation Details

#### Attack Animations
- **Left-click attacks** trigger animations for all breathing swords via `BreathingSwordAnimationHandler`
- Animations play on air clicks, block clicks, and entity hits
- **10-tick animation limit** for basic attacks (sword_to_left, sword_to_right, sword_overhead) to prevent animation lock
- 5% chance for overhead animation, otherwise alternates left/right
- Animations are synchronized across multiplayer using `AnimationSyncPacket`

#### AOE Attack System
- All breathing swords have **automatic AoE attacks** (3x3x3 cube in front of player)
- Triggered on any left-click attack via `LivingAttackEvent`
- Damage is applied to all entities in range except primary target and attacker
- Sweep attack particles provide visual feedback
- Range: 2 blocks forward from player's eye position

#### Particle System Integration
- **Particle filtering**: `speed_attack_sword` and `ragnaraku` animations skip automatic sword particles
- Breathing forms control their own particle effects
- Particles configured per-sword in `SwordParticleMapping.java`
- Config-based particle customization supported via `particles.toml`

#### Ice Breathing Second Form - Advanced Implementation
Special velocity-based circular attack pattern:
- **Target Detection**: Raycasts 6 blocks to find entity on crosshair
- **Movement System**: Uses `setDeltaMovement()` for smooth circular motion (no teleportation)
- **Rotation Control**: Player always faces circle center with calculated yaw/pitch
- **Particle Effects**:
  - Spiral tornado pattern around player (8 particles per tick)
  - Sweep attack particles every 3 ticks
  - Circular path markers (12 snowflakes around circle)
- **Attack Pattern**: 3 attacks per second (every 7 ticks) with alternating sword animations
- **Speed**: 3x angular velocity for fast rotation
- **Duration**: 5 seconds (100 ticks)

### Form Cycling System
- Press **R key** to cycle through forms (configured via `ModKeyBindings`)
- Current form index stored per-player in `PlayerBreathingData`
- Selected form displayed in chat with color-coded message
- Form selection persists until changed or player disconnects

### Cooldown System
- Each form has individual cooldown in seconds
- Cooldowns displayed on hotbar as item cooldown overlay
- Cooldown message shown when attempting to use ability on cooldown
- Cooldowns are per-item (all swords of same type share cooldown)

### Particle Configuration
Breathing sword particles are configured in:
- `SwordParticleMapping.java` - hardcoded fallback mappings
- `config/kimetsunoyaibamultiplayer/particles.toml` - runtime particle customization
- Ice swords return `DustParticleOptions` with light blue color
- Frost swords return `ParticleTypes.SNOWFLAKE`

### Best Practices

1. **Adding New Breathing Forms**:
   ```java
   public static BreathingForm newForm() {
       return new BreathingForm(
           "Form Name",
           "Description",
           cooldownSeconds,
           (player, level) -> {
               // Play animation
               AnimationHelper.playAnimation(player, "animation_name");

               // Schedule delayed/repeated actions
               AbilityScheduler.scheduleOnce(player, () -> {
                   // Delayed action
               }, delayTicks);

               // Spawn particles on server
               if (level instanceof ServerLevel serverLevel) {
                   serverLevel.sendParticles(/*...*/);
               }

               // Apply damage/effects to entities
               AABB hitbox = player.getBoundingBox().inflate(range);
               List<LivingEntity> targets = level.getEntitiesOfClass(/*...*/);
           }
       );
   }
   ```

2. **Particle Spawning Patterns**:
   - **Forward thrust**: Use `spawnForwardThrust()` helper for straight-line particles
   - **Circular patterns**: Calculate angles and use trigonometry for positioning
   - **Tornado/spiral**: Combine time-based angle changes with vertical offsets
   - **Always spawn on server**: Check `level instanceof ServerLevel` before spawning

3. **Animation Guidelines**:
   - Use full animation names with namespace: `"kimetsunoyaiba:animation_name"`
   - For timed animations: `AnimationHelper.playAnimation(player, name, 10)`
   - Avoid animations during `speed_attack_sword` or `ragnaraku` to prevent particle conflicts
   - Always play attack animations even when ability misses (for visual consistency)

4. **Movement & Physics**:
   - **Velocity-based movement**: Use `player.setDeltaMovement(velocity)` for smooth motion
   - **Teleportation**: Use `player.teleportTo(x, y, z)` for instant movement
   - **Rotation**: Set `player.setYRot()`, `player.setXRot()`, and `player.setYHeadRot()`
   - **Anti-gravity**: Set Y velocity to positive value each tick during hover abilities

### Testing Commands
- `/testanim` - Test animation sync across multiplayer
- `/testanimc` - Test client-side animation triggering
- Debug logging can be enabled in config for particle and animation troubleshooting

### Mod Compatibility

#### ShoulderSurfing Integration
The mod includes optional integration with [ShoulderSurfing Reloaded](https://github.com/Exopandora/ShoulderSurfing) mod:
- Uses reflection to detect if ShoulderSurfing is loaded (no hard dependency required)
- When present, automatically syncs camera rotation during breathing technique abilities
- Falls back gracefully if ShoulderSurfing is not installed
- Implementation in `PlayerRotationSyncPacket.java` using `ModList.get().isLoaded("shouldersurfing")`
- No configuration changes needed - works automatically when both mods are present