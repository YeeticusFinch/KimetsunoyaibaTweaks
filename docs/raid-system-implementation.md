# Raid System Implementation Documentation

**Last Updated:** 2025-10-19
**Status:** Phase 1 Complete (7/7 components) - Phase 2 Ready to Start
**Version:** 1.0.0
**Build Status:** ✅ All code compiles successfully

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Configuration](#configuration)
4. [Entity Power Scaling](#entity-power-scaling)
5. [Structure Detection](#structure-detection)
6. [Raid Flow](#raid-flow)
7. [Wave Generation](#wave-generation)
8. [Hardcore Mode Integration](#hardcore-mode-integration)
9. [Boss Bar UI](#boss-bar-ui)
10. [Rewards System](#rewards-system)
11. [Implementation Status](#implementation-status)
12. [Testing Guide](#testing-guide)
13. [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose
The raid system provides wave-based combat encounters triggered when players with omen effects enter civilian structures. It features:
- Two raid types: Demon Raids and Demon Slayer Raids
- Five difficulty levels (I-V)
- Power-scaled entity spawning
- Hardcore mode support (respects killed named entities)
- Boss bar UI with progress tracking
- Omen potion rewards for progression

### Trigger Conditions

**Demon Raids:**
- Player has `omen_of_muzan` effect
- Player is NOT a demon (no `oni` NBT tag)
- Player enters civilian structure
- No active raid at that structure

**Demon Slayer Raids:**
- Player has `omen_of_ubuyashiki` effect
- Player enters civilian structure
- No active raid at that structure

### Victory/Defeat Conditions

**Victory:**
- All waves completed
- All raid entities killed
- Participants receive rewards

**Defeat:**
- No players within 500 blocks for 30 minutes (timeout)
- All raid entities despawn
- No penalties, omen effect stays removed

---

## System Architecture

### Component Hierarchy

```
RaidRegistry (WorldSavedData)
├── KnYRaid (per structure)
│   ├── WaveData (per wave)
│   │   └── Spawned Entities
│   ├── RaidBossBar
│   ├── RaidPlayerTracker
│   └── HardcoreModeTracker
├── RaidTriggerHandler (Event Listener)
├── RaidSpawner (Entity Creation)
└── RaidEntityTracker (Death Tracking)
```

### Data Flow

```
1. Player enters structure with omen effect
   ↓
2. RaidTriggerHandler detects trigger
   ↓
3. RaidRegistry creates KnYRaid instance
   ↓
4. WaveGenerator builds wave compositions
   ↓
5. RaidSpawner spawns entities (staggered)
   ↓
6. RaidBossBar shows progress
   ↓
7. RaidEntityTracker monitors kills
   ↓
8. Next wave triggers when current complete
   ↓
9. RaidRewardHandler grants omen potions
```

---

## Configuration

### File Location
`config/kimetsunoyaibamultiplayer/raids.toml`

### Master Switches

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `enable_raids` | boolean | true | Master config switch |
| `enable_debug_logging` | boolean | false | Verbose raid logging |

**Note:** `kimetsu_raids` gamerule provides in-game toggle.

### Spawn Radii

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `small_structure_radius` | int | 32 | Houses (house_a, house_tanjiro, etc.) |
| `medium_structure_radius` | int | 48 | Larger houses (house_kocho, house_rengoku) |
| `large_structure_radius` | int | 96 | Villages (village_swamp, village_yukak, vanilla villages) |

### Timing

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `wave_preparation_time` | int | 10 | Seconds before wave starts |
| `entity_spawn_interval` | int | 2 | Seconds between entity spawns |
| `raid_timeout` | int | 1800 | Seconds (30 min) before defeat |

### Participation

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `participation_radius` | int | 500 | Blocks for rewards/boss bar |

### Rewards

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `enable_omen_potion_rewards` | boolean | true | Grant omen potions |
| `same_level_chance` | double | 0.5 | Chance for same level vs +1 level |

---

## Entity Power Scaling

### Demon Power Scales

| Scale | Description | Entities |
|-------|-------------|----------|
| `EASY_DEMON` | Weak demons | demon, demon_2, demon_3, spider_demon |
| `MEDIUM_DEMON` | Mid-tier demons | demon_4, demon_5, demon_9, temple_demon, swamp_demon |
| `HARD_DEMON` | Strong demons | demon_6-10, hand_demon, dice_steak_senior_demon, rui family, susamaru, yahaba |
| `EASY_BOSS_DEMON` | Lower moons (weak) | kyogai, kamanue, rui, mukago |
| `MEDIUM_BOSS_DEMON` | Lower moons (strong) | wakuraba, rokuro, hairo, enmu |
| `HARD_BOSS_DEMON` | Upper moons | daki, kaigaku, gyokko, hantengu, nakime, akaza, doma, kokushibo |
| `DEMON_KING` | Strongest demons | muzan, tanjiro_demon (only if muzan killed) |

### Demon Slayer Power Scales

| Scale | Description | Entities |
|-------|-------------|----------|
| `GENERIC_SLAYER` | Common slayers | demon_slayer, frost_slayer, ice_slayer, murata |
| `NAMED_SLAYER` | Named characters (weak→strong) | genya, masachika, inosuke, tanjiro, kaigaku_human, sabito, zennitsu, kanawo |
| `HARD_SLAYER` | Elite slayers | dice_steak_senior, dice_steak_senior_super |
| `HASHIRA` | Pillars (weak→strong) | kocho, kanroji, kanae, shinazugawa, rengoku, iguro, uzui, tomioka, muichirou, himejima |
| `SUPER_HASHIRA` | Legendary | michikatsu, yoriichi, yoriichi_old |

### Implementation

**File:** `raids/EntityPowerScale.java` - Enum definition
**File:** `raids/EntityCategorization.java` - Entity mappings

```java
// Get random entity from scale with hardcore filtering
EntityType<?> entity = EntityCategorization.getRandomEntity(
    EntityPowerScale.HARD_BOSS_DEMON,
    level,
    excludedEntities
);
```

---

## Structure Detection

### Civilian Structures

**KnY Structures:**
- `kimetsunoyaiba:village_swamp` (LARGE)
- `kimetsunoyaiba:village_yukak` (LARGE)
- `kimetsunoyaiba:house_tamayo` (SMALL)
- `kimetsunoyaiba:house_tanjiro` (SMALL)
- `kimetsunoyaiba:house_ubuyashiki` (MEDIUM)
- `kimetsunoyaiba:house_urokodaki` (MEDIUM)
- `kimetsunoyaiba:house_a` (SMALL)
- `kimetsunoyaiba:house_kocho` (MEDIUM)
- `kimetsunoyaiba:house_rengoku` (MEDIUM)

**Vanilla Structures:**
- All village types (LARGE)

### Structure Sizing

**File:** `util/CivilianStructureRegistry.java`

```java
public enum StructureSize {
    SMALL(32),   // Single buildings
    MEDIUM(48),  // Named character houses
    LARGE(96);   // Villages

    public final int defaultRadius;
}
```

### Caching System

**File:** `util/StructureLocationCache.java`

Performance optimization:
- Caches structure locations when player enters chunk
- Stores: structure ID, center position, bounding box, size
- Clears when structure destroyed

---

## Raid Flow

### Phase 1: Detection
1. Player ticks (every second)
2. Check for omen effects
3. Check if in civilian structure
4. Verify no active raid

### Phase 2: Initialization
1. Remove omen effect from player
2. Create `KnYRaid` instance
3. Generate waves via `WaveGenerator`
4. Create boss bar
5. Play raid horn sound
6. Broadcast message to nearby players

### Phase 3: Wave Execution
1. Enter PREPARING status (10 seconds)
2. Boss bar shows countdown
3. Enter ACTIVE status
4. Spawn entities (2-second intervals)
5. Boss bar shows progress
6. Track entity deaths
7. When wave complete → next wave

### Phase 4: Completion
**Victory:**
1. All waves complete
2. Show victory on boss bar
3. Grant rewards (XP + omen potions)
4. Clean up after 5 seconds

**Defeat (Timeout):**
1. No players for 30 minutes
2. Show defeat on boss bar
3. Despawn all raid entities
4. Clean up after 5 seconds

---

## Wave Generation

### Demon Raid Waves

**Level I (3 waves):**
- Wave 1: 6 easy demons
- Wave 2: 3 easy + 3 medium
- Wave 3: 1 hard + 2 medium + 2 easy (BOSS)

**Level II (4 waves):**
- Wave 1: 6 easy
- Wave 2: 5 easy + 5 medium
- Wave 3: 6 easy + 6 medium
- Wave 4: 2 hard + 4 medium + 4 easy (BOSS)

**Level III (5 waves):**
- Wave 1: 8 easy
- Wave 2: 6 easy + 4 medium
- Wave 3: 5 easy + 8 medium
- Wave 4: 8 medium + 4 hard
- Wave 5: 3 hard + 6 medium (BOSS)

**Level IV (6 waves):**
- Wave 1: 10 easy
- Wave 2: 8 easy + 6 medium
- Wave 3: 15 medium
- Wave 4: 6 hard + 10 medium
- Wave 5: 10 hard + 5 medium
- Wave 6: 1 easy boss + 3 hard + 5 medium (BOSS)

**Level V (7 waves):**
- Wave 1: 2 hard + 8 medium + 10 easy
- Wave 2: 5 hard + 10 medium + 10 easy
- Wave 3: 20 medium + 30 easy
- Wave 4: 10 hard + 10 medium
- Wave 5: 2 medium boss (lower) + 5 hard + 10 medium (BOSS)
- Wave 6: 2 hard boss (upper) + 5 hard + 10 medium (BOSS)
- Wave 7: 1 demon king + 10 hard (BOSS)

### Demon Slayer Raid Waves

**Level I (3 waves):**
- Wave 1: 3 generic
- Wave 2: 1 hard + 3 generic
- Wave 3: 1 named (BOSS)

**Level II (4 waves):**
- Wave 1: 4 generic
- Wave 2: 1 hard + 4 generic
- Wave 3: 2 hard + 5 generic
- Wave 4: 1 named + 3 generic (BOSS)

**Level III (5 waves):**
- Wave 1: 6 generic
- Wave 2: 2 hard + 5 generic
- Wave 3: 3 hard + 6 generic
- Wave 4: 2 named + 3 hard (BOSS)
- Wave 5: 1 hashira + 5 generic (BOSS)

**Level IV (6 waves):**
- Wave 1: 6 generic + 3 hard
- Wave 2: 5 hard + 10 generic
- Wave 3: 6 hard + 10 generic
- Wave 4: 3 named + 3 hard + 5 generic (BOSS)
- Wave 5: 1 hashira + 1 super hard + 5 hard (BOSS)
- Wave 6: 1 stronger hashira + 2 named + 3 hard (BOSS)

**Level V (7 waves):**
- Wave 1: 8 generic + 6 hard
- Wave 2: 10 hard + 15 generic
- Wave 3: 6 super hard + 10 hard
- Wave 4: 3 named + 10 hard + 10 generic (BOSS)
- Wave 5: 2 hashira + 6 super hard + 10 hard (BOSS)
- Wave 6: 2 stronger hashira + 3 named + 8 hard (BOSS)
- Wave 7: 1 super hashira + 5 super hard (BOSS)

**File:** `raids/WaveGenerator.java`

---

## Hardcore Mode Integration

### kimetsu_hardcore Gamerule

When enabled (from kimetsunoyaiba mod):
- Killed named entities never respawn
- Affects: Hashira, Kizuki, Named Slayers

### Tracking System

**File:** `raids/HardcoreModeTracker.java` (WorldSavedData)

```java
Set<ResourceLocation> killedNamedEntities;

// Called on entity death
void markKilled(EntityType<?> entityType);

// Check before spawning
boolean isKilled(EntityType<?> entityType);
```

### Spawn Selection

```java
// Filter killed entities
List<EntityType<?>> available = categoryEntities.stream()
    .filter(type -> !HardcoreModeTracker.isKilled(type))
    .collect(Collectors.toList());

// Fallback if all killed
if (available.isEmpty()) {
    // Try lower power scale
    // Log: "Unable to spawn, all entities killed in hardcore mode"
    // Chat: "A wave entity could not spawn due to hardcore restrictions"
    return null;
}
```

### Fallback Strategy

1. Try requested power scale
2. If all killed → try one scale lower
3. If all lower killed → try two scales lower
4. If all exhausted → return null (skip spawn)

---

## Boss Bar UI

### Display States

**PREPARING (10 seconds):**
```
[████████▁▁] Demon Raid - Wave 1 Starting in 5 seconds...
```

**ACTIVE:**
```
[██████▁▁▁▁] Demon Raid - Wave 1: 6/10 Demons Remaining
```

**VICTORY:**
```
[          ] Demon Raid - Victory!
```

**DEFEAT:**
```
[          ] Demon Raid - Defeat
```

### Color Scheme

| State | Color | Condition |
|-------|-------|-----------|
| Demon Raid | RED | Default |
| Demon Slayer Raid | BLUE | Default |
| Boss Wave | PURPLE | isBossWave = true |
| Victory | GREEN | All waves complete |
| Defeat | GRAY | Timeout |

### Player Tracking

- All players within 500 blocks see boss bar
- Updated every 5 seconds
- Removed when leaving radius (but stay in participants for rewards)

**File:** `raids/RaidBossBar.java`
**File:** `raids/RaidPlayerTracker.java`

---

## Rewards System

### Omen Potion Items

**10 New Items:**
- `omen_of_muzan_potion_i` through `omen_of_muzan_potion_v`
- `omen_of_ubuyashiki_potion_i` through `omen_of_ubuyashiki_potion_v`

**Usage:** Right-click to consume → applies effect

### Reward Distribution

**Victory Rewards:**
```java
For each participant:
    - Experience: difficulty * 100 XP
    - Boss drops: Natural from killed entities
    - Omen Potion:
        - 50% chance: Same level as completed raid
        - 50% chance: Next level (max V)
```

**Example:**
- Complete Demon Raid III
- Receive: 300 XP + `omen_of_muzan_potion_iii` OR `omen_of_muzan_potion_iv`

**File:** `raids/RaidRewardHandler.java`
**File:** `items/OmenPotionItem.java`

---

## Implementation Status

### ✅ Phase 1: Foundation & Configuration (COMPLETE)

**Status:** All components implemented and compiled successfully
**Date Completed:** 2025-10-19

#### Completed Components:

1. **RaidConfig.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/RaidConfig.java`
   - Config file: `config/kimetsunoyaibamultiplayer/raids.toml`
   - Features:
     - Master switches (enable_raids, enable_debug_logging)
     - Structure spawn radii (small: 32, medium: 48, large: 96 blocks)
     - Timing settings (wave_preparation_time: 10s, entity_spawn_interval: 2s, raid_timeout: 1800s)
     - Participation radius: 500 blocks
     - Reward settings (enable_omen_potion_rewards, same_level_chance: 0.5)
   - Registered in `KimetsunoyaibaMultiplayer.java` constructor

2. **ModGameRules.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/ModGameRules.java`
   - Features:
     - `kimetsu_raids` gamerule (default: true) - registered successfully
     - `isHardcoreModeEnabled()` helper (placeholder - see Known Issues)
   - Registered in `commonSetup()` method

3. **EntityPowerScale.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/EntityPowerScale.java`
   - Features:
     - 7 demon scales: EASY_DEMON → DEMON_KING
     - 5 slayer scales: GENERIC_SLAYER → SUPER_HASHIRA
     - Helper methods: isDemonScale(), isSlayerScale(), isBossTier()
     - Display names for UI

4. **EntityCategorization.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/EntityCategorization.java`
   - Features:
     - 91 entities categorized across all power scales
     - Bidirectional lookup: entity→scale and scale→entities
     - Static initialization for performance
     - Follows exact specifications from raids.md
   - Demon entities: 34 total (4 easy, 5 medium, 13 hard, 8 lower kizuki, 8 upper kizuki, 2 demon kings)
   - Slayer entities: 57 total (2 generic, 8 named, 2 hard, 10 hashira, 3 super hashira)

5. **CivilianStructureRegistry.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/CivilianStructureRegistry.java`
   - Features:
     - 14 civilian structures registered
     - 3 size categories: SMALL (7 houses), MEDIUM (2 structures), LARGE (5 villages)
     - Spawn radius calculation based on structure size
     - Includes kimetsunoyaiba structures and vanilla villages

6. **StructureLocationCache.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/StructureLocationCache.java`
   - Features:
     - 5-minute cache duration for structure lookups
     - Per-dimension caching with automatic cleanup
     - Optimizes expensive structure searches
     - Cache statistics for debugging

7. **WaveData.java** ✅
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/WaveData.java`
   - Features:
     - Wave state machine: PREPARING → SPAWNING → IN_PROGRESS → COMPLETED → FAILED
     - Entity tracking: to spawn, spawned, alive, killed
     - NBT serialization for persistence
     - Progress calculation for boss bar display
     - Completion percentage tracking

#### Known Issues & Notes:

**kimetsu_hardcore Gamerule Integration:**
- The `ModGameRules.isHardcoreModeEnabled()` method currently returns `false` (placeholder)
- Issue: Minecraft 1.20.1 doesn't provide a public API to access gamerules by string name
- The `kimetsunoyaiba` mod registers `kimetsu_hardcore` gamerule, but we can't access it directly
- Solutions to implement in Phase 3:
  1. Get direct reference to their GameRule.Key (requires kimetsunoyaiba API)
  2. Use reflection to access the gamerule
  3. Request kimetsunoyaiba mod to expose an API
- For now: hardcore mode features will work but won't respect the actual gamerule

**Compilation Status:**
- ✅ All Phase 1 files compile successfully
- ⚠️ 4 deprecation warnings in WisteriaTreeGrowers.java (pre-existing, unrelated to raids)

### 🔄 Phase 2: Core System (NEXT)
- [ ] KnYRaid.java - Core raid instance manager
- [ ] RaidRegistry.java - Global raid management with WorldSavedData
- [ ] WaveGenerator.java - Demon and slayer wave generation

### ⏳ Phase 3: Triggering & Spawning (PENDING)
- [ ] RaidTriggerHandler.java
- [ ] RaidSpawner.java
- [ ] RaidEntityTracker.java
- [ ] HardcoreModeTracker.java

### ⏳ Phase 4: UI & Rewards (PENDING)
- [ ] RaidBossBar.java
- [ ] RaidPlayerTracker.java
- [ ] RaidRewardHandler.java
- [ ] OmenPotionItem.java (10 variants)

### ⏳ Phase 5: Integration & Polish (PENDING)
- [ ] RaidUpdatePacket.java
- [ ] RaidCommand.java
- [ ] Sound events
- [ ] Effect modifications
- [ ] Spawn rule integration

---

## How to Continue: Phase 2 Implementation Guide

### Overview
Phase 1 (Foundation & Configuration) is complete. Next is Phase 2 (Core System), which implements the main raid logic and wave generation.

### Phase 2 Components (In Order)

#### 1. KnYRaid.java - Core Raid Instance Manager
**Purpose:** Manages a single raid instance (lifecycle, state, waves)

**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/KnYRaid.java`

**Key Responsibilities:**
- Raid lifecycle management (PREPARING → ACTIVE → VICTORY/DEFEAT)
- Wave progression logic
- Entity spawn coordination via RaidSpawner
- Timeout tracking (30-minute defeat timer)
- Integration with RaidBossBar and RaidPlayerTracker
- NBT serialization for persistence

**Key Fields:**
```java
private UUID raidId;
private RaidType raidType; // DEMON or SLAYER
private int difficultyLevel; // 1-5
private ResourceLocation structureId;
private BlockPos structureCenter;
private List<WaveData> waves;
private int currentWaveIndex;
private RaidState state;
private long raidStartTime;
private long lastActivityTime;
```

**Key Methods:**
```java
public void tick(ServerLevel level)  // Called every tick
public void startNextWave()
public void onEntityKilled(UUID entityUUID)
public void checkDefeatCondition()
public boolean isActive()
public CompoundTag toNBT()
public static KnYRaid fromNBT(CompoundTag tag)
```

#### 2. RaidRegistry.java - Global Raid Management
**Purpose:** WorldSavedData that tracks all active raids across all structures

**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/RaidRegistry.java`

**Key Responsibilities:**
- Singleton WorldSavedData instance per dimension
- Create/destroy raid instances
- Track active raids by structure location
- Tick all active raids
- Save/load raid data to world save file

**Key Fields:**
```java
private static final String DATA_NAME = "kny_raids";
private Map<BlockPos, KnYRaid> activeRaids; // structure center -> raid
```

**Key Methods:**
```java
public static RaidRegistry get(ServerLevel level)
public KnYRaid createRaid(RaidType type, int level, BlockPos structureCenter, ResourceLocation structureId)
public void removeRaid(BlockPos structureCenter)
public KnYRaid getRaidAt(BlockPos pos)
public void tick(ServerLevel level)
public void load(CompoundTag nbt)
public CompoundTag save(CompoundTag nbt)
```

**Registration:** Add tick event handler to call `RaidRegistry.get(level).tick(level)` on server tick

#### 3. WaveGenerator.java - Wave Composition Generator
**Purpose:** Generates wave compositions based on raid type and difficulty level

**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/WaveGenerator.java`

**Key Responsibilities:**
- Generate wave compositions from raids.md specifications
- Support for all 5 difficulty levels (demon and slayer raids)
- Entity selection with power scale filtering
- Boss wave detection (last wave in most raids)

**Key Methods:**
```java
public static List<WaveData> generateDemonRaidWaves(int difficultyLevel)
public static List<WaveData> generateSlayerRaidWaves(int difficultyLevel)
private static WaveData createWave(int waveNumber, EntityPowerScale[] scales, int[] counts)
private static Map<ResourceLocation, Integer> selectEntities(EntityPowerScale scale, int count)
```

**Wave Specifications Reference:**

**Demon Raid Level I (3 waves):**
- Wave 1: 6 EASY_DEMON
- Wave 2: 3 EASY_DEMON + 3 MEDIUM_DEMON
- Wave 3: 1 HARD_DEMON + 2 MEDIUM_DEMON + 2 EASY_DEMON (boss wave)

**Demon Raid Level V (7 waves):**
- Wave 1: 2 HARD_DEMON + 8 MEDIUM_DEMON + 10 EASY_DEMON
- Wave 2: 5 HARD_DEMON + 10 MEDIUM_DEMON + 10 EASY_DEMON
- Wave 3: 20 MEDIUM_DEMON + 30 EASY_DEMON
- Wave 4: 10 HARD_DEMON + 10 MEDIUM_DEMON
- Wave 5: 2 MEDIUM_BOSS_DEMON + 5 HARD_DEMON + 10 MEDIUM_DEMON
- Wave 6: 2 HARD_BOSS_DEMON + 5 HARD_DEMON + 10 MEDIUM_DEMON (boss wave)
- Wave 7: 1 DEMON_KING + 10 HARD_DEMON (boss wave)

**Slayer Raid Level I (3 waves):**
- Wave 1: 3 GENERIC_SLAYER
- Wave 2: 1 HARD_SLAYER + 3 GENERIC_SLAYER
- Wave 3: 1 NAMED_SLAYER (boss wave)

**Slayer Raid Level V (7 waves):**
- Wave 1: 8 GENERIC_SLAYER + 6 HARD_SLAYER
- Wave 2: 10 HARD_SLAYER + 15 GENERIC_SLAYER
- Wave 3: 6 HARD_SLAYER + 10 HARD_SLAYER
- Wave 4: 3 NAMED_SLAYER + 10 HARD_SLAYER + 10 GENERIC_SLAYER
- Wave 5: 2 HASHIRA + 6 HARD_SLAYER + 10 HARD_SLAYER (boss wave)
- Wave 6: 2 HASHIRA + 3 NAMED_SLAYER + 8 HARD_SLAYER (boss wave)
- Wave 7: 1 SUPER_HASHIRA + 5 HARD_SLAYER (boss wave)

(See raids.md for complete specifications of levels II, III, IV)

### Implementation Tips

**Compilation Check:**
```bash
./gradlew.bat compileJava --no-daemon
```

**Existing Components to Reference:**
- `EntityCategorization.getEntitiesForScale(scale)` - Get entities for a power scale
- `CivilianStructureRegistry.getSpawnRadius(structureId)` - Get spawn radius for structure
- `RaidConfig.*` - Access all configuration values
- `WaveData` - Already implemented, use for wave tracking

**Event Integration:**
- Add server tick event listener to tick RaidRegistry
- Will need RaidTriggerHandler in Phase 3 to detect omen effects

**NBT Format Examples:**
```java
// KnYRaid NBT structure:
{
    raidId: UUID,
    raidType: "DEMON" | "SLAYER",
    difficultyLevel: 1-5,
    structureId: "kimetsunoyaiba:village_swam",
    structureCenter: {x, y, z},
    waves: [WaveData.toNBT(), ...],
    currentWaveIndex: int,
    state: "PREPARING" | "ACTIVE" | "VICTORY" | "DEFEAT",
    raidStartTime: long,
    lastActivityTime: long
}
```

**Next Steps After Phase 2:**
- Phase 3 will add RaidTriggerHandler to detect omen effects and start raids
- Phase 4 will add UI (boss bar) and rewards
- Phase 5 will add polish (commands, sounds, effects)

---

## Testing Guide

### Test Commands

```
/knyraidstart demon 1      # Start demon raid level I
/knyraidstart slayer 5     # Start slayer raid level V
/knyraidstop               # Stop current raid
/knyraidinfo               # Show active raid info
/knyraidreset              # Reset hardcore tracker
/knyraidtest 3             # Test wave generation for level 3
```

### Test Scenarios

**Basic Functionality:**
1. Give omen effect: `/effect give @s kimetsunoyaibamultiplayer:omen_of_muzan 300 0`
2. Enter village/house
3. Verify raid starts, omen removed
4. Verify boss bar appears
5. Kill all entities
6. Verify rewards granted

**Hardcore Mode:**
1. Enable: `/gamerule kimetsu_hardcore true`
2. Kill a named entity (e.g., Akaza)
3. Start raid that would spawn Akaza
4. Verify Akaza doesn't spawn
5. Verify fallback or skip message

**Defeat Condition:**
1. Start raid
2. Leave 500-block radius
3. Wait 30 minutes (or adjust timeout config)
4. Verify defeat triggers
5. Verify entities despawn

**Multiplayer:**
1. Multiple players enter structure
2. Verify all see boss bar
3. Verify all get rewards
4. Test one player leaving

---

## Troubleshooting

### Raid Won't Start

**Check:**
- `kimetsu_raids` gamerule enabled
- Actually in civilian structure
- No active raid at location
- Omen effect present and correct level

### Entities Not Spawning

**Check:**
- Spawn radius config
- Valid spawn positions available
- Hardcore mode not blocking all entities
- Check logs for "Unable to spawn" messages

### Boss Bar Not Showing

**Check:**
- Player within 500 blocks
- Boss bar not hidden in client settings
- Check F3 screen for active boss bars

### Rewards Not Granted

**Check:**
- Player in participants list (was within 500 blocks during raid)
- Raid completed successfully (not defeated)
- Inventory space available

### Performance Issues

**Optimize:**
- Reduce spawn radius configs
- Increase entity spawn interval
- Reduce wave entity counts (modify WaveGenerator)
- Check for lag with `/forge tps`

---

## Future Enhancements

### Potential Features
- [ ] Custom raid compositions via data packs
- [ ] Raid leaderboards (fastest clear, no deaths, etc.)
- [ ] Special raid modifiers (double entities, boss rush, etc.)
- [ ] Raid-specific loot tables
- [ ] Integration with quest system
- [ ] Multi-structure raids (protect multiple villages)
- [ ] Raid preview before starting
- [ ] Difficulty scaling based on player count

### Known Limitations
- Cannot spawn in structures with no valid spawn positions
- Boss bar limited to 500-block radius
- Hardcore mode requires manual tracking (no auto-detect vanilla kills)
- Single raid per structure at a time

---

## Technical Notes

### Performance Considerations

**Entity Spawning:**
- Staggered spawns (2-second intervals) prevent lag spikes
- Maximum entities per wave: ~50
- Entities despawn on defeat to free memory

**Structure Detection:**
- Cached to avoid repeated lookups
- Only checks when player changes chunk
- Clear cache when structure destroyed

**Boss Bar Updates:**
- Updated only when count changes
- Player list refreshed every 5 seconds
- Removed when raid ends

### Save Data

**WorldSavedData Files:**
- `kny_raids.dat` - Active raids, raid history
- `kny_hardcore.dat` - Killed named entities

**Persisted Across Restarts:**
- Active raids resume if server restarts mid-raid
- Hardcore kill tracking
- Player participation for pending rewards

### Network Efficiency

**Packets Sent:**
- Raid start: Once to nearby players
- Wave updates: Only on wave transitions
- Progress updates: Only when entity count changes
- Boss bar: Handled by vanilla system

---

## References

- Source Spec: `/docs/raids.md`
- Entity Tags: `/docs/KnY-Entity-Tags.md`
- Spawning Rules: `/docs/spawning-rules.md`
- Power Scale Source: raids.md lines 18-114

---

## Quick Reference: Phase 1 Files

### Configuration & Core
```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/
├── ModGameRules.java                    (kimetsu_raids gamerule)
└── config/
    └── RaidConfig.java                  (raids.toml config file)
```

### Raid System Package
```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/
├── EntityPowerScale.java                (7 demon + 5 slayer scales)
├── EntityCategorization.java            (91 entities categorized)
├── CivilianStructureRegistry.java       (14 structures, 3 size categories)
├── StructureLocationCache.java          (5-min cache optimization)
└── WaveData.java                        (Wave state + entity tracking)
```

### Configuration Files (Auto-Generated)
```
config/kimetsunoyaibamultiplayer/raids.toml
```

### Important Constants

**Power Scales:**
- Demons: EASY → MEDIUM → HARD → EASY_BOSS → MEDIUM_BOSS → HARD_BOSS → DEMON_KING
- Slayers: GENERIC → NAMED → HARD → HASHIRA → SUPER_HASHIRA

**Structure Sizes:**
- SMALL (32 blocks): 7 houses
- MEDIUM (48 blocks): mugen_train, temple_doma
- LARGE (96 blocks): 5 villages

**Raid Configuration:**
- Wave prep time: 10 seconds
- Entity spawn interval: 2 seconds
- Raid timeout: 1800 seconds (30 minutes)
- Participation radius: 500 blocks
- Omen reward chance: 50% same level, 50% +1 level

### Known Limitations

1. **kimetsu_hardcore Gamerule:**
   - ModGameRules.isHardcoreModeEnabled() currently returns false
   - Requires API from kimetsunoyaiba mod or reflection
   - Placeholder implemented, functionality deferred to Phase 3

2. **Deprecation Warnings:**
   - 4 warnings in WisteriaTreeGrowers.java (pre-existing, unrelated to raids)
   - Related to ResourceLocation constructor

### Build Commands

**Compile:**
```bash
./gradlew.bat compileJava --no-daemon
```

**Full Build:**
```bash
./gradlew.bat build --no-daemon
```

### Next Action Items (Phase 2)

1. **KnYRaid.java** - Core raid instance manager with state machine
2. **RaidRegistry.java** - WorldSavedData singleton for raid tracking
3. **WaveGenerator.java** - Wave composition generator from raids.md specs

See "How to Continue: Phase 2 Implementation Guide" section above for detailed implementation instructions.

---

**End of Documentation**
