# Custom Progression Override

This document describes the custom progression override feature that allows disabling the base mod's demon slayer initiation rewards.

## Problem

The base KimetsunoYaiba mod has a feature where killing a demon for the first time triggers the `demon_slayer_corps` advancement, which:

1. Gives the player `uniform_chestplate`, `uniform_leggings`, `uniform_boots`
2. Gives the player a `nichirinsword`
3. Spawns and tames a `kasugai_crow` near the player
4. Grants the `mizunoto` advancement (which the base mod constantly re-grants)

This system often glitches and fires multiple times, giving players duplicate items.

## Solution

We added a new config file `custom_progression.toml` with a boolean option:

```toml
[custom_progression.demon_slayer_initiation]
# When enabled, blocks all base mod initiation rewards
disable_base_mod_demon_slayer_initiation = false
```

When enabled, this:

- **Removes** the uniform items and nichirinsword after the base mod gives them
- **Prevents** kasugai_crow from spawning near players who just earned the advancement
- **Revokes** the mizunoto advancement when the base mod grants it

## Files Added/Modified

### New Files

1. **`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomProgressionConfig.java`**

   New config file with:
   - `disable_base_mod_demon_slayer_initiation` - Master switch to block initiation rewards
   - `enable_debug_logging` - Logs when items are removed, crows are blocked, etc.

2. **`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/events/DemonSlayerInitiationHandler.java`**

   Event handler that:
   - Listens to `AdvancementEvent.AdvancementEarnEvent` (with LOWEST priority, runs after base mod)
   - Tracks players who just earned `demon_slayer_corps`
   - Schedules item removal 3 ticks later (after base mod gives items)
   - Blocks `kasugai_crow` spawning via `EntityJoinLevelEvent`
   - Revokes `mizunoto` advancement immediately when granted

### Modified Files

1. **`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/KimetsunoyaibaMultiplayer.java`**

   - Registered `CustomProgressionConfig` class on mod event bus
   - Added config file path: `kimetsunoyaibamultiplayer/custom_progression.toml`

2. **`src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/events/ModEvents.java`**

   - Added cleanup call to `DemonSlayerInitiationHandler.onPlayerLogout()` when player disconnects

## How It Works

### Technical Details

The base mod uses three procedures that we cannot directly prevent from running:

1. **SupplyProcedure** - Gives uniform items + nichirinsword on `demon_slayer_corps`
2. **AdvancementRewardProcedure** - Spawns and tames kasugai_crow on `demon_slayer_corps`
3. **CheckAdvancementDemonProcedure** - Re-grants mizunoto on every advancement event

Our approach:

```
Base mod grants demon_slayer_corps
        ↓
Base mod's SupplyProcedure runs (gives items)
        ↓
Base mod's AdvancementRewardProcedure runs (spawns crow)
        ↓
Our handler runs (LOWEST priority)
  - Adds player to pendingItemRemoval map
  - Adds player to blockMizunoto set
        ↓
On next EntityJoinLevelEvent:
  - If kasugai_crow near tracked player → cancel spawn
        ↓
On server tick (3 ticks later):
  - Remove uniform items from inventory
  - Revoke mizunoto advancement
```

### Config Location

The config file is created at:
```
config/kimetsunoyaibamultiplayer/custom_progression.toml
```

### Default Config

```toml
# Custom Progression Configuration
# Override base mod progression features

[custom_progression]

    [custom_progression.demon_slayer_initiation]
        # Disable the base mod's demon slayer initiation rewards
        # When enabled, this will prevent:
        #   - Automatic granting of uniform_chestplate, uniform_leggings, uniform_boots
        #   - Automatic granting of nichirinsword
        #   - Automatic spawning and taming of kasugai_crow
        #   - Automatic granting of mizunoto advancement
        disable_base_mod_demon_slayer_initiation = false

    [custom_progression.debug]
        # Enable debug logging for progression overrides
        enable_debug_logging = false
```

## Usage

1. Enable the config option in `custom_progression.toml`:
   ```toml
   disable_base_mod_demon_slayer_initiation = true
   ```

2. (Optional) Enable debug logging to see what's being blocked:
   ```toml
   enable_debug_logging = true
   ```

3. Create your own custom progression system using datapacks/commands

## Notes

- This is a server-side feature - the config must be set on the server
- Players who already have the items/crow will not be affected
- The mizunoto revocation only lasts for a short time after the demon_slayer_corps advancement
- After logout, the tracking is cleared - if the base mod tries to re-grant mizunoto later, it won't be blocked

## Base Mod Analysis

The following base mod procedures were analyzed:

| Procedure | Event | Action |
|-----------|-------|--------|
| `SupplyProcedure` | `AdvancementEvent` | Gives uniform + sword when `demon_slayer_corps` is earned |
| `AdvancementRewardProcedure` | `AdvancementEvent` | Spawns + tames kasugai_crow when `demon_slayer_corps` is earned |
| `CheckAdvancementDemonProcedure` | `AdvancementEvent` | Chains advancements - if player has mizunoe, grants mizunoto; if has mizunoto, grants demon_slayer_corps |
| `Advanvement1Procedure` | `ItemCraftedEvent` | Grants mizunoto when nichirinsword is crafted |
| `ColorChangeProcedure` | Item tick | Grants mizunoto when holding colored nichirinsword for 30+ ticks |

Our handler doesn't interfere with these - it just undoes their effects afterward when the config is enabled.
