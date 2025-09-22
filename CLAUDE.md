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
The mod depends on `player-animation-lib-forge` for multiplayer animation synchronization. This library is included as a JAR in the `libs/` folder and declared as a deobfuscated dependency in `build.gradle`.