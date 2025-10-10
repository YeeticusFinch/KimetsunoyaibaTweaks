# KimetsunoYaiba Mod - Potion Effects Reference

This document provides detailed information about all potion effects added by the KimetsunoYaiba mod (version 2, Forge 1.20.1).

## Overview

The KimetsunoYaiba mod adds 18 custom potion effects that enhance gameplay with demon slayer and demon-related mechanics. These effects are registered under the mod ID `kimetsunoyaiba`.

---

## Potion Effects

### 1. attack_endure
**Registry Name:** `kimetsunoyaiba:attack_endure`
**Class:** `AttackEndureMobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Hidden in GUI and inventory

**Description:**
A hidden effect that likely provides protection against attacks or prevents hit stagger. No active tick behavior observed.

**Key Features:**
- Always active (returns true for `isDurationEffectTick`)
- Completely invisible to players (no GUI rendering)

---

### 2. blood_demon_art
**Registry Name:** `kimetsunoyaiba:blood_demon_art`
**Class:** `BloodDemonArtMobEffect`
**Category:** BENEFICIAL
**Color:** -16777216 (black)
**Visibility:** Hidden in GUI and inventory

**Description:**
Activates Blood Demon Art abilities for demon entities. This is a core mechanic for demon characters, enabling special demon powers and AI behaviors.

**Key Features:**
- Runs `BloodDemonArtEffectStartedappliedProcedure` when applied
- Executes `AIplayerOniProcedure` every tick to control demon AI
- Calls `TotalConcentrationConstantPotionExpiresProcedure` when expired
- Hidden from player view but has significant gameplay effects

---

### 3. cell_destruction
**Registry Name:** `kimetsunoyaiba:cell_destruction`
**Class:** `CellDestructionMobEffect`
**Category:** HARMFUL
**Color:** -52429 (pinkish)
**Visibility:** Visible in GUI

**Description:**
A devastating effect that destroys cells, dealing increasing damage over time and draining stamina. The damage scales with the amplifier level.

**Key Features:**
- Deals generic damage every 100 ticks (5 seconds)
- Damage increases with amplifier: `level = amplifier + 1`
- Drains stamina (`cnt_x`) when not using breathing techniques or demon arts
- Stamina drain rate: `-0.1 * level` (capped at -0.8 per tick)
- Bypassed by active breathing techniques or demon art usage

**Technical Details:**
```java
// Deals damage every 100 ticks
if (duration % 100 == 0) {
    entity.hurt(DamageSource.GENERIC, (float)level);
}

// Drains stamina if not using abilities
if (breathes == 0 && demon_art == 0) {
    cnt_x += Math.max(-0.1 * level, -0.8);
}
```

---

### 4. cold (Reiki)
**Registry Name:** `kimetsunoyaiba:cold`
**Class:** `ReikiMobEffect`
**Category:** HARMFUL
**Color:** -10027009 (purple)
**Visibility:** Visible in GUI

**Description:**
Cold damage effect, internally named "Reiki". Applies continuous cold-based damage through the `ActivePotionRekiProcedure`.

**Key Features:**
- Executes damage procedure every tick
- Purple visual effect
- Likely reduces movement speed and deals frost damage

---

### 5. compass_needle
**Registry Name:** `kimetsunoyaiba:compass_needle`
**Class:** `CompassNeedleMobEffect`
**Category:** NEUTRAL
**Color:** -10027009 (purple)
**Visibility:** Visible in GUI

**Description:**
Compass Needle tracking effect. Runs positioning logic through `CompassNeedleOnPotionActiveTickProcedure` using entity coordinates.

**Key Features:**
- Tracks entity position (x, y, z coordinates)
- Active tick procedure for continuous updates
- Possibly used for demon tracking or navigation abilities

---

### 6. cool_time
**Registry Name:** `kimetsunoyaiba:cool_time`
**Class:** `CoolTimeMobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Visible in GUI

**Description:**
Primary cooldown effect that controls breathing style ability durations and prevents combat actions. While active, players cannot use breathing techniques or perform regular sword attacks.

**Key Features:**
- Controls breathing style ability cooldowns
- Prevents regular sword attack swings
- Executes `CoolTimePotionStartedappliedProcedure` when applied
- No visual effect (invisible color)
- Critical for preventing ability spam and maintaining balance

---

### 7. cooltime_2
**Registry Name:** `kimetsunoyaiba:cooltime_2`
**Class:** `Cooltime2MobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Visible in GUI

**Description:**
Cooldown effect specifically for the backwards jump/dodge mechanic. Prevents players from spamming the dodge ability that launches them backwards when jumping while moving backwards.

**Key Features:**
- Controls backwards jump/dodge mechanic cooldown
- Minimal implementation (no custom procedures)
- Always active tick behavior
- No visual effect (invisible color)
- Prevents dodge spam for balanced mobility

---

### 8. demon (Oni)
**Registry Name:** `kimetsunoyaiba:demon`
**Class:** `OniMobEffect`
**Category:** NEUTRAL
**Color:** -13434880 (dark)
**Visibility:** Visible in GUI

**Description:**
Core demon transformation effect. Converts entities into demons with special abilities and behaviors.

**Key Features:**
- Calls `StartOniProcedure` when applied (demon transformation)
- Runs `OniOnEffectActiveTickProcedure` every tick (demon AI/behaviors)
- Executes `ExpireOniProcedure` when removed (reversion)
- Full lifecycle management for demon state

---

### 9. dream
**Registry Name:** `kimetsunoyaiba:dream`
**Class:** `DreamMobEffect`
**Category:** NEUTRAL
**Color:** -13434829 (purple-ish)
**Visibility:** Visible in GUI

**Description:**
Dream/sleep effect, likely inspired by Enmu's Blood Demon Art. Applies dream state with positioning effects.

**Key Features:**
- Runs `DreamPotionStartedappliedProcedure` when applied
- Executes `DreamOnPotionActiveTickProcedure` every tick with coordinates
- Has removal procedure (decompilation failed for exact code)
- Likely immobilizes or disorients the target

---

### 10. god_hand
**Registry Name:** `kimetsunoyaiba:god_hand`
**Class:** `GodHandMobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Visible in GUI

**Description:**
Mystery effect with minimal implementation. No custom procedures, suggesting it may be a marker effect checked by other systems.

**Key Features:**
- Simplest implementation of all effects
- No active tick behavior or procedures
- Likely used as a flag/marker for external checks

---

### 11. immvable
**Registry Name:** `kimetsunoyaiba:immvable`
**Class:** `ImmvableMobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Hidden in GUI and inventory

**Description:**
Immobilization effect that completely prevents entity movement. Likely used for special abilities that root enemies in place.

**Key Features:**
- Zeroes horizontal velocity every tick
- Allows downward movement (gravity) but prevents upward
- Applies Resistance 99 effect (10 ticks, level 99)
- Complete movement lockdown

**Technical Details:**
```java
// Removes horizontal movement, keeps gravity
entity.setDeltaMovement(new Vec3(0.0, Math.min(entity.getDeltaMovement().y, 0.0), 0.0));

// Applies maximum resistance
entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 99));
```

---

### 12. potion_demon_slayer_mark
**Registry Name:** `kimetsunoyaiba:potion_demon_slayer_mark`
**Class:** `PotionDemonSlayerMarkMobEffect`
**Category:** NEUTRAL
**Color:** -13434880 (dark)
**Visibility:** Visible in GUI

**Description:**
Activates the Demon Slayer Mark, a powerful enhancement that boosts slayer abilities. One of the most important demon slayer buffs.

**Key Features:**
- Executes `PotionDemonSlayerMarkPotionStartedappliedProcedure` on application
- Runs `PotionDemonSlayerMarkOnEffectActiveTickProcedure` every tick
- Calls `PotionDemonSlayerMarkPotionExpiresProcedure` when expired
- Full lifecycle management for mark activation/deactivation

---

### 13. potion_transparent_world
**Registry Name:** `kimetsunoyaiba:potion_transparent_world`
**Class:** `PotionTransparentWorldMobEffect`
**Category:** NEUTRAL
**Color:** -1 (invisible)
**Visibility:** Visible in GUI

**Description:**
Transparent World ability - allows users to see through enemies and predict movements. An advanced demon slayer technique.

**Key Features:**
- Runs `PotionTransparentWorldPotionStartedappliedProcedure` when applied
- No active tick behavior (single activation)
- Likely enables special vision/perception abilities
- No visual effect on player

---

### 14. potion_yorichi
**Registry Name:** `kimetsunoyaiba:potion_yorichi`
**Class:** `PotionYorichiMobEffect`
**Category:** NEUTRAL
**Color:** -13434880 (dark)
**Visibility:** Visible in GUI

**Description:**
Yorichi's power effect - grants abilities of the legendary demon slayer Yorichi Tsugikuni. The most powerful slayer enhancement.

**Key Features:**
- Has startup procedure (decompilation failed for exact code)
- Executes `PotionYorichiOnPotionActiveTickProcedure` every tick with coordinates
- Calls `PotionDemonSlayerMarkPotionExpiresProcedure` when expired
- Likely grants Sun Breathing and enhanced combat abilities

---

### 15. regeneration_inhibition
**Registry Name:** `kimetsunoyaiba:regeneration_inhibition`
**Class:** `RegenerationInhibitionMobEffect`
**Category:** HARMFUL
**Color:** -65536 (red)
**Visibility:** Visible in GUI

**Description:**
Prevents regeneration, particularly effective against demons who normally have strong regenerative abilities.

**Key Features:**
- Simple implementation with no custom procedures
- Likely checked by regeneration systems to block healing
- Red visual effect indicating danger
- Critical for fighting demons

---

### 16. suffocation
**Registry Name:** `kimetsunoyaiba:suffocation`
**Class:** `SuffocationMobEffect`
**Category:** HARMFUL
**Color:** -1 (invisible)
**Visibility:** Visible in GUI

**Description:**
Suffocation effect that damages entities and prevents jumping/flying. Simulates being unable to breathe.

**Key Features:**
- Deals 2.0 generic damage per tick when entity is on ground
- Zeroes stamina (`cnt_x = 0`)
- Prevents upward movement while allowing falling
- Particularly devastating as it drains stamina completely

**Technical Details:**
```java
if (entity.onGround()) {
    entity.hurt(DamageSource.GENERIC, 2.0f);
    entity.getPersistentData().putDouble("cnt_x", 0.0);
    // Prevents jumping (removes upward velocity)
    entity.setDeltaMovement(new Vec3(motionX, Math.min(motionY, 0.0), motionZ));
}
```

---

### 17. total_concentration
**Registry Name:** `kimetsunoyaiba:total_concentration`
**Class:** `TotalConcentrationConstantMobEffect`
**Category:** BENEFICIAL
**Color:** -1 (invisible)
**Visibility:** Hidden in GUI and inventory

**Description:**
Total Concentration Constant - the advanced breathing technique that maintains breathing power continuously. A core mechanic for demon slayers.

**Key Features:**
- Stores current breathing technique level in `skill` NBT tag
- Runs `AIPlayerProcedure` every tick with coordinates (breathing AI)
- Calls expiration procedure when removed
- Maintains breathing state even when not actively using forms
- Hidden from GUI but critical for gameplay

**Technical Details:**
```java
// On application: store breathing level
entity.getPersistentData().putDouble("skill", entity.getPersistentData().getDouble("breathes"));

// Active tick: runs breathing AI
AIPlayerProcedure.execute(level, x, y, z, entity);
```

---

### 18. wisteriapoison (Fujinohana)
**Registry Name:** `kimetsunoyaiba:wisteriapoison`
**Class:** `FujinohanaMobEffect`
**Category:** HARMFUL
**Color:** -39169 (purple)
**Visibility:** Visible in GUI

**Description:**
Wisteria flower poison - deadly to demons. Fujinohana is Japanese for wisteria flower. A key weakness of demons in the Demon Slayer universe.

**Key Features:**
- Executes `PoisonOfFujiflowerProcedure` every tick
- Purple visual effect (wisteria color)
- Particularly effective against demon entities
- Continuous damage while active

---

## Effect Categories

### Beneficial Effects
- `blood_demon_art` - Demon abilities
- `total_concentration` - Breathing technique maintenance

### Harmful Effects
- `cell_destruction` - Scaling damage and stamina drain
- `cold` (reiki) - Cold damage
- `regeneration_inhibition` - Blocks healing
- `suffocation` - Damage and movement prevention
- `wisteriapoison` - Anti-demon poison

### Neutral Effects (Utility/Markers)
- `attack_endure` - Attack protection
- `compass_needle` - Tracking
- `cool_time` - Breathing ability cooldown + prevents sword attacks
- `cooltime_2` - Backwards jump/dodge cooldown
- `demon` (oni) - Demon transformation
- `dream` - Dream state
- `god_hand` - Marker effect
- `immvable` - Movement lockdown
- `potion_demon_slayer_mark` - Slayer mark activation
- `potion_transparent_world` - Enhanced perception
- `potion_yorichi` - Legendary abilities

---

## Technical Notes

### Effect Visibility
Several effects are hidden from the GUI despite being active:
- `attack_endure`
- `blood_demon_art`
- `immvable`
- `total_concentration`

These effects use `IClientMobEffectExtensions` to override visibility methods.

### Decompilation Issues
Some methods could not be fully decompiled due to Java bytecode complexity:
- `DreamMobEffect.removeAttributeModifiers()`
- `PotionYorichiMobEffect.addAttributeModifiers()`

These methods likely contain startup logic for their respective effects.

### NBT Data Fields
The effects interact with several persistent NBT fields:
- `breathes` - Active breathing technique ID
- `demon_art` - Active demon art ID
- `skill` - Stored skill/breathing level
- `cnt_x` - Stamina/concentration counter
- `Damage` - Damage value for sword clashing

### Lifecycle Methods
Most complex effects use three lifecycle hooks:
1. `addAttributeModifiers()` - Called when effect is applied
2. `applyEffectTick()` - Called every tick while active
3. `removeAttributeModifiers()` - Called when effect expires

---

## Integration with Sword Clashing

Several effects interact with the sword clashing system:
- Effects that modify `skill` NBT data affect guard success
- `total_concentration` maintains defensive capabilities
- `demon` and `blood_demon_art` likely affect demon combat mechanics

For more information on sword clashing mechanics, see `KnY_Sword_Combat.md`.

---

## Color Reference

Effect colors are specified as integers representing RGB values:
- `-1` - Transparent/Invisible
- `-39169` - Purple (wisteria)
- `-52429` - Pink (cell destruction)
- `-65536` - Red (regeneration inhibition)
- `-10027009` - Purple (cold, compass needle)
- `-13434829` - Dark purple (dream)
- `-13434880` - Dark (demon, slayer mark, yorichi)
- `-16777216` - Black (blood demon art)

---

## Related Documentation
- [Breathing System](breathing-system.md) - How breathing techniques work
- [Sword Combat](KnY_Sword_Combat.md) - Sword clashing mechanics
- [Architecture](architecture.md) - Overall mod structure

---

*Documentation generated from decompiled source code of KimetsunoYaiba mod version 2 for Forge 1.20.1*
