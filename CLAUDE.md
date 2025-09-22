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