# Kimetsu no Yaiba Sword Combat System

## Overview

The Kimetsu no Yaiba mod implements a sophisticated combat system where **attacks clash with each other** and can be **deflected or mitigated during certain states**. This document explains how the deflection/blocking (guard) system works based on analysis of the decompiled mod source code.

## Core Concept: Attack Clashing

As stated in the mod description:

> "In this mod, your techniques will clash with and mitigate enemy attacks while they are in use. Almost all weapons in this mod generate a weak attack when swung. Use these swings to clash with and mitigate enemy attacks when you cannot use Breathing Techniques or Blood Demon Arts."

This means:
- **Breathing techniques** and **Blood Demon Arts** (abilities) provide defense while active
- **Weapon swings** also provide defensive properties
- When two attacks collide, damage reduction and special effects occur

## The Guard System: How It Works

### 1. Guard State Detection

The system checks if an entity is in a "guard" state when taking damage. An entity is considered to be guarding when:

**Guard Success Conditions** (`LogicGuardSuccessProcedure.java:18`):
```java
return !entity.isRangedAmmo()
    && entity != sourceentity  // Not self-damage
    && (skill != 0.0 || guard)  // Has skill value OR guard flag
    && entity.getDamage() > 0.0;  // Has defensive "Damage" value set
```

This means an entity successfully guards when:
- They are not a projectile/ranged ammo
- They are not attacking themselves
- They have either:
  - A non-zero `skill` value (set during certain animations/abilities)
  - The `guard` flag enabled (set when using abilities)
- They have a `Damage` value greater than 0

### 2. The "Damage" Field: Defensive Power

The `Damage` NBT field represents **defensive power** - the amount of incoming damage that can be negated:

**From `WhenEntityTakesDamageProcedure.java:83`:**
```java
if (LogicGuardSuccessProcedure.execute(entity, sourceentity)) {
    Damage_amount = Math.max(Damage_amount - entity.getPersistentData().getDouble("Damage"), 0.0);
    changeDamage = true;
    guard = true;
}
```

**From `DoDamage2Procedure.java:109-113`:**
```java
if (LogicGuardSuccessProcedure.execute(entityiterator, entity)) {
    num1 = Math.max(num1 - entityiterator.getPersistentData().getDouble("Damage"), 0.0);
    if (num1 <= 0.0) {
        noDamaged = true;  // Attack completely nullified
    }
}
```

### 3. When Guard Activates

The guard system activates in two scenarios:

#### A. During Breathing Techniques / Blood Demon Arts

When an entity uses a breathing technique or blood demon art:
- The ability sets `entity.getPersistentData().putDouble("Damage", value)`
- The ability sets `entity.getPersistentData().putBoolean("guard", true)`
- The `breathes` NBT field is set to identify the active technique

**From `WhenEntityTakesDamageProcedure.java:101`:**
```java
if ((entity_a.getPersistentData().getDouble("breathes") != 0.0
    || entity_a.getPersistentData().getBoolean("attack"))
    && hasCrimsonRedBlade) {
    // Red blade bonus damage vs demons during techniques
}
```

#### B. During Weapon Swings

When swinging a weapon:
- Basic swing procedures reset counters but don't explicitly set guard state
- However, if the entity has `skill` value or `Damage` value from ongoing animations, it still provides defense
- The `attack` flag may be set during attack animations

### 4. Damage Calculation with Guard

When both attacker and defender have swords/weapons, the combat calculation follows this flow:

**Step 1: Check if both have swords** (`LogicSwordProcedure.java:25`)
```java
logic_a = (mainHandItem instanceof SwordItem
    || mainHandItem instanceof AxeItem
    || mainHandItem instanceof PickaxeItem
    || mainHandItem instanceof ShovelItem
    || mainHandItem instanceof HoeItem
    || mainHandItem.is(ItemTags "forge:sword")
    || mainHandItem.is(ItemTags "forge:whip")
    || (mainHandItem.is(ItemTags "forge:metallic")
        && mainHandItem.is(ItemTags "minecraft:pickaxes")));
```

**Step 2: Apply damage reduction**
```
Final Damage = max(Incoming Damage - Defender's "Damage" Value, 0.0)
```

**Step 3: If damage is still > 0, apply to health**

**Step 4: Trigger guard effects**

### 5. Guard Effects: Visual and Audio Feedback

**Guard Effect Procedure** (`GuardEffectProcedureProcedure.java:23-53`):

When a successful guard occurs:

**If attacker has sword AND defender has sword:**
- Play `kimetsunoyaiba:guard_sword` sound
- Spawn fire spark particles (`PARTICLE_SPARK_FIRE`)
- Play guard animation on defender

**If attacker has sword but defender doesn't:**
- Play `kimetsunoyaiba:guard_punch` sound
- Spawn crit particles (`ParticleTypes.CRIT`)

**If attacker doesn't have sword:**
- Play `entity.player.attack.strong` sound
- Spawn crit particles

**Additional sound effects** (`DamageEffectProcedureProcedure.java:34-45`):
- When both have swords: `guard_sword` sound at pitch 1.0-1.1
- Repeated based on damage amount (more damage = more sounds)

## How Defensive Power Gets Set

### Dual Nature of the "Damage" Field

The `Damage` NBT field serves a **dual purpose**:

1. **Offensive Damage**: When attacking, abilities set this to specify how much damage to deal
2. **Defensive Power**: When being attacked, this value reduces incoming damage

### Setting Defensive Power in Practice

**Example from `KekkizyutuFudokutanProcedure.java:100`:**
```java
// Blood Demon Art ability setting offensive damage
// This same value acts as defensive power while the ability is active
entity.getPersistentData().putDouble("Damage", 14 * (1 + strengthLevel / 3));
entity.getPersistentData().putDouble("Range", 3.0);
entity.getPersistentData().putDouble("knockback", 0.5);
DoDamage2Procedure.execute(world, x, y, z, entity);  // Deal damage to enemies
// The "Damage" value remains set, providing defense until cleared
```

### How Breathing Techniques Provide Defense

**Important Discovery**: Breathing techniques in the original mod **do NOT directly set defensive power**. Instead, they work through a more subtle system:

#### When Swinging a Nichirin Sword:

**From `SwingKaminariProcedure.java` (Thunder Breathing):**
```java
public static void execute(final Entity entity) {
    if (TestSwingItemProcedure.execute(entity)) {
        SwingItemProcedure.execute(entity);  // Reset counters
        // Apply Total Concentration effect
        entity.addEffect(new MobEffectInstance(TOTAL_CONCENTRATION, Integer.MAX_VALUE, 0));
        // Set breathes field to identify active technique
        entity.getPersistentData().putDouble("breathes", 120.0);  // 120 = Thunder Breathing ID
    }
}
```

The `breathes` field acts as an **identifier**, not directly as defense. The actual defensive power comes from:

1. **Animation Attributes**: When animations play, they set the `skill` attribute
2. **Active Ability States**: During technique execution, the `Damage` field is set temporarily
3. **Weapon Swing Defense**: The act of swinging itself provides brief defensive windows

### The Animation System and Defense

**From `PlayAnimationProcedure.java:36`:**
```java
if (ANIMATION_1.getValue() == 0.0) {
    // Set animation attribute to skill value
    entity.getAttribute(ANIMATION_1).setBaseValue(entity.getPersistentData().getDouble("skill"));
}
```

The `skill` field enables guard mechanics **even when `Damage` is 0**. This is why entities can defend while their animations are playing, even between attack hits.

### When Does Each Provide Defense?

| State | `Damage` | `skill` | `guard` Flag | Defense Active? |
|-------|----------|---------|-------------|-----------------|
| Idle | 0 | 0 | false | No |
| Swinging weapon | 0 | 0 | false | No (unless animation active) |
| Breathing technique activating | Variable | Set by animation | May be true | Yes |
| During attack in technique | Set by ability | Set | true | Yes |
| Between attacks in technique | 0 | Set (from animation) | May be true | Yes (via skill) |
| Technique ending | 0 | 0 | false | No |

### Key Insight: Always Defensive While Using Techniques

**The genius of the system**: Once a breathing technique or blood demon art is activated:
- The `breathes` or `demon_art` field is set to a non-zero value
- This identifier persists for the technique's duration
- Individual attacks within the technique set `Damage` temporarily
- The `skill` attribute provides continuous defense between attacks
- The entity is **never fully defenseless** while the technique is active

### Example: Attack Sequence with Defense

```java
// Technique starts
entity.getPersistentData().putDouble("breathes", 320.0);  // Flame Breathing
entity.getPersistentData().putDouble("skill", 1.0);  // Enable guard mechanics

// First attack (tick 10)
entity.getPersistentData().putDouble("Damage", 12.0);  // Offensive + Defensive
entity.getPersistentData().putBoolean("attack", true);
DoDamage2Procedure.execute(world, x, y, z, entity);
// Attack completes, Damage gets reset to 0

// Between attacks (ticks 11-19)
// Damage = 0, but skill = 1.0 still provides defense via LogicGuardSuccessProcedure

// Second attack (tick 20)
entity.getPersistentData().putDouble("Damage", 15.0);
DoDamage2Procedure.execute(world, x, y, z, entity);
// etc...

// Technique ends
entity.getPersistentData().putDouble("breathes", 0.0);
entity.getPersistentData().putDouble("skill", 0.0);
// Now vulnerable again
```

## Key NBT Fields

| Field | Type | Purpose |
|-------|------|---------|
| `Damage` | Double | Dual purpose: offensive damage when attacking, defensive power when being attacked |
| `guard` | Boolean | Flag indicating entity is actively guarding (set during abilities) |
| `skill` | Double | Skill value that enables guard mechanics even when Damage = 0 (set during animations) |
| `breathes` | Double | Identifies which breathing technique is active (e.g., 120.0 for Thunder, 320.0 for Flame) |
| `demon_art` | Double | Identifies which blood demon art is active (e.g., 802.0 for Temari) |
| `attack` | Boolean | Flag indicating entity is performing an attack (affects knockback and clash mechanics) |
| `cnt1` to `cnt5` | Double | Counters used by animations/abilities, reset on weapon swing |
| `Range` | Double | Attack range for current ability |
| `knockback` | Double | Knockback strength for current attack |

## Implementation Summary for Your Mod

To implement a similar system in your Kimetsunoyaiba-Multiplayer mod:

### 1. Set Defensive Values During Abilities

In your breathing forms (e.g., `IceBreathingForms.java`, `FrostBreathingForms.java`):

```java
// At the start of each breathing form
player.getPersistentData().putDouble("Damage", defensivePower);  // e.g., 5.0 to block 5 damage
player.getPersistentData().putBoolean("guard", true);
player.getPersistentData().putDouble("skill", 1.0);  // Enable guard mechanics
```

### 2. Set Attack Values During Abilities

```java
// When performing attacks during abilities
player.getPersistentData().putBoolean("attack", true);
```

### 3. Clear Values After Ability Ends

```java
// In the final tick or cleanup phase
player.getPersistentData().putDouble("Damage", 0.0);
player.getPersistentData().putBoolean("guard", false);
player.getPersistentData().putDouble("skill", 0.0);
player.getPersistentData().putBoolean("attack", false);
```

### 4. Modify Your Damage Event Handler

In `WhenEntityTakesDamageProcedure` equivalent, add logic similar to:

```java
@SubscribeEvent
public static void onEntityAttacked(LivingAttackEvent event) {
    Entity defender = event.getEntity();
    Entity attacker = event.getSource().getEntity();
    double incomingDamage = event.getAmount();

    // Check if defender is guarding
    boolean hasDefense = defender.getPersistentData().getDouble("Damage") > 0.0;
    boolean isGuarding = defender.getPersistentData().getBoolean("guard")
                      || defender.getPersistentData().getDouble("skill") != 0.0;

    if (hasDefense && isGuarding && defender != attacker) {
        double defensePower = defender.getPersistentData().getDouble("Damage");
        double reducedDamage = Math.max(incomingDamage - defensePower, 0.0);

        // Apply reduced damage instead of original
        event.setCanceled(true);  // Cancel original damage

        // Reapply with reduced amount
        if (reducedDamage > 0) {
            defender.hurt(/* custom damage source */, (float)reducedDamage);
        }

        // Play guard effects
        playGuardSound(defender, attacker);
        spawnGuardParticles(defender);
        playGuardAnimation(defender);
    }
}
```

### 5. Add Weak Attacks to Weapon Swings

For basic weapon swings (left-click attacks):

```java
// In your attack event handler
player.getPersistentData().putDouble("Damage", 2.0);  // Weak defensive power
player.getPersistentData().putBoolean("attack", true);

// Schedule removal after a few ticks (e.g., 5 ticks)
scheduler.scheduleOnce(player, () -> {
    player.getPersistentData().putDouble("Damage", 0.0);
    player.getPersistentData().putBoolean("attack", false);
}, 5);
```

## Recommended Defensive Power Values

Based on the mod's design philosophy:

- **Basic weapon swing**: 2.0 - 3.0 damage reduction
- **Low-level breathing forms**: 5.0 - 8.0 damage reduction
- **Mid-level breathing forms**: 10.0 - 15.0 damage reduction
- **High-level breathing forms**: 20.0 - 30.0 damage reduction
- **Ultimate techniques**: 40.0+ damage reduction (can block entire attacks)

## Sound and Particle Effects

To replicate the KnY feel:

**Successful Sword Clash:**
- Sound: `kimetsunoyaiba:guard_sword` at volume 1.0, pitch 1.0-1.1
- Particles: Fire sparks in a small radius around impact point

**Partial Block (less than full damage negated):**
- Sound: Mix of `guard_sword` and impact sounds
- Particles: Fewer fire sparks

**Complete Block (damage fully negated):**
- Sound: Louder `guard_sword` with reverb
- Particles: Ring of fire sparks, possibly with slower velocity

## Animation Integration

The original mod plays guard animations when `entity.getPersistentData().getBoolean("guard")` is true:

**From `GuardEffectProcedureProcedure.java:20-22`:**
```java
if (entityiterator.getPersistentData().getBoolean("guard")) {
    PlayAnimationEntityGuardProcedure.execute(world, entityiterator);
}
```

You can integrate this with your `AnimationHelper` system:

```java
if (defender.getPersistentData().getBoolean("guard") && successfulGuard) {
    AnimationHelper.playAnimation(defender, "guard_animation_name");
}
```

## Advanced: Knockback Reduction

The original mod also reduces knockback during guards:

**From `DoDamage2Procedure.java:114-121`:**
```java
if (entityiterator.getPersistentData().getBoolean("guard")
    || entityiterator.getPersistentData().getBoolean("attack")) {
    if (entity.getPersistentData().getBoolean("attack")) {
        // Both attacking: knockback scales with damage reduction
        knockback *= (num1 / num2);  // reduced_damage / original_damage
    } else {
        // Only defender attacking: half knockback
        knockback *= 0.5;
    }
}
```

This creates interesting dynamics where:
- Two attacking entities push each other less when both are in attack stance
- Defending entities receive less knockback
- Complete damage negation also means minimal knockback

## Conclusion

The KnY combat system creates a dynamic where:

1. **Active entities are safer** - using abilities or attacking makes you harder to damage
2. **Timing matters** - attacks can be countered by swinging at the right moment
3. **Risk-reward balance** - you're vulnerable between attacks but protected during them
4. **Visual feedback** - sword clashes are clear with sounds and particles

This system encourages **aggressive, active playstyles** rather than passive blocking, perfectly fitting the Demon Slayer anime's combat philosophy where fighters are constantly moving and attacking.
