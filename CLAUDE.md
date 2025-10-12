# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Minecraft Forge 1.20.1 mod that enables synchronized player animations in multiplayer, featuring a breathing technique system for custom nichirin swords.

- **Mod ID**: `kimetsunoyaibamultiplayer`
- **Minecraft**: 1.20.1 | **Forge**: 47.4.0 | **Java**: 17

## Quick Reference

- **Build Commands**: See [docs/build-commands.md](docs/build-commands.md)
- **Architecture**: See [docs/architecture.md](docs/architecture.md)
- **Breathing Techniques**: See [docs/breathing-system.md](docs/breathing-system.md)
- **API Usage Guide**: See [docs/api-usage-guide.md](docs/api-usage-guide.md) - For using this mod as a library
- **Migration Guide**: See [docs/migration-guide.md](docs/migration-guide.md) - For refactoring existing swords
- **Critical Bugs**: See [docs/bug-prevention.md](docs/bug-prevention.md)
- **Server Compatibility**: See [docs/server-compatibility.md](docs/server-compatibility.md)

## Core Concepts

### Mod Registration Pattern
Uses DeferredRegister for all registrations:
1. Create DeferredRegister instances for each registry type
2. Register objects as RegistryObject fields
3. Register the DeferredRegister to mod event bus in constructor

### Client/Server Separation
- Client-only code MUST be in `client` package
- Use `@Mod.EventBusSubscriber(value = Dist.CLIENT)` for client events
- Network packets use `DistExecutor.unsafeRunWhenOn()` for client code

### Animation System
1. **Detection**: `AnimationTracker.java` uses reflection to detect active animations
2. **Network**: `AnimationSyncPacket.java` transmits animation events
3. **Application**: `AnimationSyncHandler.java` applies animations to other players
4. **Dependencies**: Requires `player-animation-lib-forge` + `mobplayeranimator`

## Test Commands

- `/testanim` - Server-side animation sync test (plays "sword_to_left" on all players)
- `/testanimc` - Client-side animation test (local playback + server sync)

## Key Rules

### Event Handling
- Always wrap event handlers in try-catch blocks
- Use `ThreadLocal<Boolean>` flags to prevent recursion
- Never call `hurt()` in `LivingAttackEvent` without recursion protection

### Logging
- **Never** use log4j in exception handlers (causes LinkageError crashes)
- Use `System.err.println()` or `System.out.println()` instead
- Exclude log4j from all dependencies in `build.gradle`

### Client-Only Code
- **Never** import client classes (`net.minecraft.client.*`) outside `client` package
- Client classes MUST be in `client` package (enforced by RuntimeDistCleaner)
- Test on dedicated server early: `./gradlew runServer`

## Package Structure

```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/
├── api/                       # Public API for other mods
│   ├── KnYAPI.java           # Main API entry point
│   ├── BreathingStyleRegistry.java
│   ├── SwordRegistry.java
│   └── NichirinSwordBuilder.java
├── client/                    # Client-only code (particles, renderers, GUI)
├── network/packets/           # Network packets (server-safe with DistExecutor)
├── items/                     # Items (server-safe)
├── breathingtechnique/        # Breathing system (server-safe)
└── KimetsunoyaibaMultiplayer.java  # Main mod class
```

## Dependencies

- `player-animation-lib-forge-1.20.1-1.0.2-rc1.jar` (in `libs/`)
- `mobplayeranimator` (enables animations on other players)
- All dependencies exclude log4j (see `build.gradle`)

## Configuration

- Config class: `Config.java` using ForgeConfigSpec
- Particle config: `config/kimetsunoyaibamultiplayer/particles.toml`
- Debug mode: Set `logDebug = true` in config

## API for Other Mods

This mod can be used as a library/dependency by other mods to add custom breathing styles and swords.

### Quick Start for API Users

```java
// In your mod's ModItems class:
public static final RegistryObject<Item> MY_SWORD =
    KnYAPI.createSword("nichirinsword_mysword")
        .breathingStyle("my_breathing", MyBreathingForms.createMyBreathing())
        .styleRange(1900)  // Choose unique range >= 1900
        .defaultParticle(ParticleTypes.SNOWFLAKE)
        .category(SwordRegistry.SwordCategory.NICHIRIN)
        .durability(2000)
        .build(ITEMS);
```

**See [docs/api-usage-guide.md](docs/api-usage-guide.md) for complete API documentation.**

### Key API Classes

- **KnYAPI**: Main entry point - register styles, create swords, access helpers
- **BreathingStyleRegistry**: Manages registered breathing styles
- **SwordRegistry**: Tracks nichirin swords and special swords
- **NichirinSwordBuilder**: Fluent builder for creating swords

### Benefits

1. **No boilerplate**: Single API call replaces multiple registration steps
2. **Automatic integration**: Particles, cycling, and animations work automatically
3. **Category management**: Separate nichirin swords from special swords
4. **Helper access**: Use AnimationHelper, MovementHelper, ParticleHelper, etc.

## Additional Documentation

For detailed guides on specific topics, see the `docs/` directory:
- **API Usage Guide**: Complete guide for using this mod as a library
- **Migration Guide**: Refactoring existing swords to use the new API
- Build and development commands
- Architecture deep-dive
- Breathing technique implementation guide
- Bug prevention (log4j crashes, event recursion)
- Dedicated server compatibility rules
