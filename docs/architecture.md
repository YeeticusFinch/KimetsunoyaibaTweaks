# Architecture Overview

## Core Structure

### Main Mod Class
`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/KimetsunoyaibaMultiplayer.java`
- Handles mod initialization, event registration, lifecycle management
- Registers blocks, items, creative tabs using DeferredRegister
- Contains client-side event handling in nested `ClientModEvents` class

### Configuration
`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/Config.java`
- Uses ForgeConfigSpec for mod configuration
- `Config.onLoad()` handles updating cached values when config changes

### Event System
- **Mod Event Bus**: Mod lifecycle events (setup, config loading)
- **Forge Event Bus**: Game events (server starting, etc.)
- **Client-only Events**: Handled in `ClientModEvents` with `@Mod.EventBusSubscriber(value = Dist.CLIENT)`

## Animation System

### Architecture
1. **AnimationTracker.java**: Detects active KeyframeAnimationPlayer instances using reflection
2. **AnimationSyncPacket.java + ModNetworking.java**: Network synchronization between client/server
3. **AnimationSyncHandler.java**: Receives packets and applies animations to other players
4. **Fallback System**: Tries kimetsunoyaiba animations (e.g., "sword_to_left") if original not found

### Key Lessons
- **PlayerAnimator**: Only applies animations to local player
- **Mob Player Animator**: Required extension to animate other players (critical for multiplayer)
- **Animation Registry**: Finding animations by ResourceLocation is challenging - fallback strategies essential
- **Reflection**: Required to access private fields in AnimationStack and ModifierLayer
- **Registry Lookup**: Animations may be registered under different namespaces

### Animation Identification Strategy
1. Extract name from KeyframeAnimation.extraData
2. Reflection scan for strings matching known animation names
3. Validate against complete list of kimetsunoyaiba animation names
4. Try various ResourceLocation combinations during registry lookup

## Dependencies

- **player-animation-lib-forge**: Base animation framework
- **mobplayeranimator**: Enables animations on other players (multiplayer sync)
- Both declared in `build.gradle` and `mods.toml`
- All dependencies exclude log4j to prevent classloader conflicts

## Resource Structure

- **Mod Metadata**: `src/main/resources/META-INF/mods.toml`
- **Pack Metadata**: `src/main/resources/pack.mcmeta`
- **Generated Resources**: `src/generated/resources/`

## Mod Compatibility

### ShoulderSurfing Integration
- Optional integration with ShoulderSurfing Reloaded mod
- Uses reflection to detect if loaded (no hard dependency)
- Syncs camera rotation during breathing technique abilities
- Implementation in `PlayerRotationSyncPacket.java` using `ModList.get().isLoaded("shouldersurfing")`
