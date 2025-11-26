# Slyrien Addon Analysis: Hooking into KnY Ability System

This document analyzes the **decompiled-slyrien-1.0** mod to understand how it hooks into the kimetsunoyaiba mod's ability system for breathing forms and blood demon arts.

## Executive Summary

The Slyrien addon uses **SpongePowered Mixins** to intercept and extend the kimetsunoyaiba mod's procedure-based ability system. It does NOT create a separate ability system but instead hooks directly into existing execution points to add new breathing styles and modify existing ones.

## Table of Contents

1. [Mixin Configuration](#mixin-configuration)
2. [Integration Architecture](#integration-architecture)
3. [Breath ID System](#breath-id-system)
4. [Mixin Patterns](#mixin-patterns)
5. [Procedure Implementation](#procedure-implementation)
6. [Item Integration](#item-integration)
7. [How to Replicate](#how-to-replicate)

---

## Mixin Configuration

### Location
- **File**: `slyrien.mixins.json` (root of resources)
- **Package**: `net.slyrien.kiba.mixin`

### Configuration Structure
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "net.slyrien.kiba.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "slyrien.refmap.json",
  "mixins": [
    "DoDamage2ProcedureMixin",
    "PlayerBreathStoneProcedureMixin",
    "ChangeBreathProcedureFlowerMixin",
    "PlayerBreathesFlowerProcedureMixin",
    "StartBreathesProcedureFlowerMixin",
    "PlayerBreathHinokamiKaguraProcedureMixin",
    "BreathesHi13ProcedureMixin",
    "PlayerBreathSunProcedureMixin",
    "AIYoriichiProcedureMixin",
    "KeyDemonSlayerMarkOnKeyPressedProcedureMixin",
    "PotionYorichiPotionStartedappliedProcedureMixin",
    "ChangeBreathProcedureSunMixin",
    "CalculateCooldownTimeMixin",
    "ChangeBreathProcedureMixin",
    "StartBreathesProcedureMixin",
    "ActiveBreathProcedureMixin",
    "PotionDemonSlayerMarkStartedMixin"
  ],
  "client": [
    "client.MinecraftMixin",
    "client.EntityMixin"
  ]
}
```

### Key Points
- **remap: false** - Targets are already in production (not obfuscated MCP names)
- Separate client mixins for client-only hooks
- Uses refmap for stable method references

---

## Integration Architecture

### Core Hooks

The Slyrien mod hooks into 5 primary integration points in the kimetsunoyaiba mod:

| Hook Point | Mixin Target | Purpose |
|------------|--------------|---------|
| **Breath Activation** | `StartBreathesProcedure.execute()` | Intercept breath activation to add custom breaths |
| **Active Breath Tick** | `ActiveBreathProcedure.execute()` | Route active breath ticks to custom procedures |
| **Specific Breath Forms** | `PlayerBreath{Style}Procedure.execute()` | Modify or replace specific breathing forms |
| **Damage Calculation** | `DoDamage2Procedure.execute()` | Apply custom damage modifiers |
| **Cooldown Calculation** | `CalculateCooldownTimeProcedure.execute()` | Modify cooldowns with custom effects |

### Data Flow

```
Player Right-Click Item
    ↓
StartBreathesProcedure.execute() ← [MIXIN INTERCEPTS]
    ↓
Set entity.breathes = {breathId}
Set entity.skill = {breathId}
    ↓
Every Tick: ActiveBreathProcedure.execute() ← [MIXIN INTERCEPTS]
    ↓
Route to PlayerBreath{Style}Procedure ← [MIXIN INTERCEPTS]
    ↓
Execute form-specific logic (DoDamage2, particles, etc.)
```

---

## Breath ID System

### ID Range Allocation

The kimetsunoyaiba mod uses numerical IDs to identify breathing styles and forms:

| Range | Breathing Style | Owner |
|-------|----------------|-------|
| 100-199 | Water | kimetsunoyaiba |
| 200-299 | Thunder | kimetsunoyaiba |
| 300-399 | Flame | kimetsunoyaiba |
| ... | ... | ... |
| 1600-1699 | Stone | kimetsunoyaiba |
| **2000-2099** | **Slyrien** | **slyrien addon** |
| **3000-3099** | **Impact** | **slyrien addon** |

### How Slyrien Uses IDs

**StartBreathesProcedureMixin.java** (line 62-69):
```java
double breathId;
if (isSlyrienSword) {
    breathId = 2001.0 + itemstack.getTag().getDouble("select");
} else {
    breathId = 3001.0 + itemstack.getTag().getDouble("select");
}
entity.getPersistentData().putDouble("breathes", breathId);
entity.getPersistentData().putDouble("skill", breathId);
```

**ActiveBreathProcedureMixin.java** (line 22-36):
```java
final double breathId = entity.getPersistentData().getDouble("breathes");
if (breathId >= 2000.0 && breathId < 2100.0) {
    PlayerBreathSlyrienProcedure.execute(world, x, y, z, entity);
}
if (breathId >= 3000.0 && breathId < 3100.0) {
    PlayerBreathImpactProcedure.execute(world, x, y, z, entity);
}
```

### ID to Form Mapping

**PlayerBreathSlyrienProcedure.java**:
```java
final double breathId = entity.getPersistentData().getDouble("breathes");
if (breathId == 2001.0) {
    BreathesSlyrien1Procedure.execute(world, x, y, z, entity);
} else if (breathId == 2002.0) {
    BreathesSlyrien2Procedure.execute(world, x, y, z, entity);
} else if (breathId == 2020.0) {
    SwingSlyrienBaseProcedure.execute(world, x, y, z, entity);
}
```

**Key Insight**: ID 2020 is a special "swing" ID used for non-form attacks (basic swings).

---

## Mixin Patterns

### Pattern 1: HEAD Injection with Cancellation

**Purpose**: Completely replace original behavior for specific conditions

**Example**: `StartBreathesProcedureMixin.java`

```java
@Mixin(value = { StartBreathesProcedure.class }, remap = false)
public class StartBreathesProcedureMixin {
    @Inject(method = { "execute" }, at = { @At("HEAD") }, cancellable = true)
    private static void onStartSlyrienBreath(
        final LevelAccessor world,
        final Entity entity,
        final ItemStack itemstack,
        final CallbackInfo ci
    ) {
        // Check if this is a custom sword
        final boolean isSlyrienSword = itemstack.getItem() == ModItems.SLYRIEN_NICHIRIN_SWORD.get();

        if (!isSlyrienSword) {
            return; // Let original execute
        }

        // Custom logic here...
        entity.getPersistentData().putDouble("breathes", breathId);

        ci.cancel(); // Cancel original execution
    }
}
```

**Key Features**:
- `@At("HEAD")` - Runs before original method
- `cancellable = true` - Allows `ci.cancel()` to skip original
- Early return if not applicable

### Pattern 2: TAIL Injection

**Purpose**: Add behavior after original execution

**Example**: `ActiveBreathProcedureMixin.java`

```java
@Mixin(value = { ActiveBreathProcedure.class }, remap = false)
public class ActiveBreathProcedureMixin {
    @Inject(method = { "execute" }, at = { @At("TAIL") })
    private static void onActiveSkillTick(
        @Nullable final Event event,
        final Entity entity,
        final CallbackInfo ci
    ) {
        final double breathId = entity.getPersistentData().getDouble("breathes");
        if (breathId >= 2000.0 && breathId < 2100.0) {
            PlayerBreathSlyrienProcedure.execute(world, x, y, z, entity);
        }
    }
}
```

**Key Features**:
- `@At("TAIL")` - Runs after original completes
- No cancellation - both original and mixin run
- Adds new breath ranges without breaking existing ones

### Pattern 3: HEAD Injection with Conditional Cancellation

**Purpose**: Override specific forms while preserving others

**Example**: `PlayerBreathStoneProcedureMixin.java`

```java
@Mixin(value = { PlayerBreathStoneProcedure.class }, remap = false)
public class PlayerBreathStoneProcedureMixin {
    @Inject(method = { "execute" }, at = { @At("HEAD") }, cancellable = true)
    private static void slyrien_implementStoneForms(
        final LevelAccessor world,
        final double x, final double y, final double z,
        final Entity entity,
        final CallbackInfo ci
    ) {
        final double breathId = entity.getPersistentData().getDouble("breathes");
        if (breathId == 1601.0) {
            BreathesIwa1Procedure.execute(world, x, y, z, entity);
            ci.cancel(); // Replace form 1
        } else if (breathId == 1602.0) {
            BreathesIwa2Procedure.execute(world, x, y, z, entity);
            ci.cancel(); // Replace form 2
        }
        // Forms 1603, 1604, etc. fall through to original
    }
}
```

**Key Features**:
- Selective replacement of specific forms
- Conditional cancellation
- Other forms execute normally

### Pattern 4: ModifyVariable

**Purpose**: Modify return values or local variables

**Example**: `DoDamage2ProcedureMixin.java`

```java
@Mixin(value = { DoDamage2Procedure.class }, remap = false)
public class DoDamage2ProcedureMixin {
    @ModifyVariable(
        method = { "execute" },
        at = @At("STORE"),
        name = { "damage_sorce_set" }
    )
    private static double slyrien_applyAllDamageBuffs(
        double currentDamage,
        final LevelAccessor world,
        final double x, final double y, final double z,
        final Entity entity
    ) {
        if (entity instanceof LivingEntity) {
            final LivingEntity livingEntity = (LivingEntity)entity;

            // Apply Fury Strike effect
            if (livingEntity.hasEffect(ModMobEffects.FURY_STRIKE.get())) {
                int amplifier = livingEntity.getEffect(furyEffect).getAmplifier();
                currentDamage *= (1.0 + (amplifier + 1) * 0.2);
            }

            // Apply Vermillion Eye effect
            if (livingEntity.hasEffect(ModMobEffects.VERMILLION_EYE.get())) {
                currentDamage *= 1.5;
            }
        }
        return currentDamage;
    }
}
```

**Key Features**:
- `@At("STORE")` - Targets variable storage
- `name = { "damage_sorce_set" }` - Targets specific variable
- Returns modified value
- Original method continues with modified value

### Pattern 5: CallbackInfoReturnable for Return Values

**Purpose**: Modify method return values

**Example**: `CalculateCooldownTimeMixin.java`

```java
@Mixin(value = { CalculateCooldownTimeProcedure.class }, remap = false)
public class CalculateCooldownTimeMixin {
    @Inject(method = { "execute" }, at = { @At("RETURN") }, cancellable = true)
    private static void slyrien_applyKairaBlessingCooldown(
        final Entity entity,
        final ItemStack itemstack,
        final CallbackInfoReturnable<Double> cir
    ) {
        if (entity instanceof LivingEntity) {
            final LivingEntity livingEntity = (LivingEntity)entity;
            if (livingEntity.hasEffect(ModMobEffects.KAIRA_BLESSING.get())) {
                double cooldown = cir.getReturnValue();
                for (int i = 0; i <= amplifier; ++i) {
                    cooldown *= 0.9; // 10% reduction per level
                }
                cir.setReturnValue(cooldown);
            }
        }
    }
}
```

**Key Features**:
- `@At("RETURN")` - Runs at return statement
- `CallbackInfoReturnable<T>` - For methods with return values
- `cir.getReturnValue()` - Gets original return
- `cir.setReturnValue()` - Overrides return

---

## Procedure Implementation

### Standard Form Procedure Pattern

Every breathing form follows this pattern (from `BreathesSlyrien1Procedure.java`):

```java
public class BreathesSlyrien1Procedure {
    public static void execute(
        final LevelAccessor world,
        final double x, final double y, final double z,
        final Entity entity
    ) {
        if (entity == null) return;

        // 1. Increment tick counter
        entity.getPersistentData().putDouble("cnt1",
            entity.getPersistentData().getDouble("cnt1") + 1.0);
        final double tick = entity.getPersistentData().getDouble("cnt1");

        // 2. Initial activation (tick 1)
        if (tick == 1.0) {
            GetPowerFowardProcedure.execute(world, entity);
            // Play sounds
            world.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, ...);
        }

        // 3. Active phase (ticks 1-10)
        if (tick < 10.0) {
            // Movement
            entity.setDeltaMovement(new Vec3(
                entity.getPersistentData().getDouble("x_power") * 2.0,
                0.1,
                entity.getPersistentData().getDouble("z_power") * 2.0
            ));

            // Particles
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, ...);

            // Damage
            entity.getPersistentData().putDouble("Damage", 15.0);
            entity.getPersistentData().putDouble("knockback", 1.2);
            entity.getPersistentData().putDouble("Range", 3.0);
            DoDamage2Procedure.execute(world, x, y + 1.0, z, entity);
        }

        // 4. Cleanup (tick > 15)
        if (tick > 15.0) {
            entity.getPersistentData().putDouble("breathes", 0.0);
            entity.getPersistentData().putDouble("skill", 0.0);
        }
    }
}
```

### Key NBT Tags

| Tag | Type | Purpose |
|-----|------|---------|
| `breathes` | double | Current active breath ID |
| `skill` | double | Current active skill ID (usually same as breathes) |
| `cnt1` | double | Primary tick counter for form execution |
| `cnt2`, `cnt3`, etc. | double | Secondary counters for complex forms |
| `Damage` | double | Damage amount for next DoDamage2 call |
| `knockback` | double | Knockback strength |
| `Range` | double | Attack range |
| `x_power`, `z_power` | double | Movement direction (set by GetPowerFowardProcedure) |
| `projectile_type` | double | Projectile behavior type |

### Direct Procedure Calls

Slyrien procedures call kimetsunoyaiba procedures directly:

```java
// Get forward direction
GetPowerFowardProcedure.execute(world, entity);

// Apply damage
DoDamage2Procedure.execute(world, x, y, z, entity);

// Destroy blocks
BlockDestroy2Procedure.execute(world, x, y, z, entity);

// Play animation
PlayAnimationProcedure.execute(world, entity);

// Swing item
SwingItemProcedure.execute(entity);

// Test if can swing
TestSwingItemProcedure.execute(entity);

// Change breathing style
ChangingBreathesProcedure.execute(entity, itemstack);
```

### Animation System Integration

**BreathesIwa1Procedure.java** (line 47-57):
```java
if (entity instanceof LivingEntity) {
    final LivingEntity livingEntity = (LivingEntity)entity;

    // Trigger swing animation
    livingEntity.swing(InteractionHand.MAIN_HAND, true);

    // Set animation attributes
    if (livingEntity.getAttributes().hasAttribute(KimetsunoyaibaModAttributes.ANIMATION_1.get())) {
        livingEntity.getAttribute(ANIMATION_1).setBaseValue(-6.0);
    }
    if (livingEntity.getAttributes().hasAttribute(KimetsunoyaibaModAttributes.ANIMATION_2.get())) {
        livingEntity.getAttribute(ANIMATION_2).setBaseValue(7.0);
    }
}

// Trigger animation procedure
PlayAnimationProcedure.execute(world, entity);
```

**Key Animation Attributes**:
- `ANIMATION_1` - Primary animation ID
- `ANIMATION_2` - Secondary animation ID or variant
- Negative values may indicate reverse/mirrored animations

---

## Item Integration

### Custom Sword Implementation

**SlyrienNichirinSwordItem.java**:

```java
public class SlyrienNichirinSwordItem extends SwordItem {

    // Called when entity is hit
    @Override
    public boolean hurtEnemy(
        final ItemStack itemstack,
        final LivingEntity target,
        final LivingEntity attacker
    ) {
        boolean retval = super.hurtEnemy(itemstack, target, attacker);
        ActiveRedSwordProcedure.execute(
            attacker.level(),
            attacker.getX(), attacker.getY(), attacker.getZ(),
            attacker, itemstack
        );
        return retval;
    }

    // Called on right-click
    @Override
    public InteractionResultHolder<ItemStack> use(
        final Level world,
        final Player entity,
        final InteractionHand hand
    ) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);

        // This triggers the mixin!
        StartBreathesProcedure.execute(world, entity, ar.getObject());

        return ar;
    }

    // Called on entity swing
    @Override
    public boolean onEntitySwing(final ItemStack itemstack, final LivingEntity entity) {
        boolean retval = super.onEntitySwing(itemstack, entity);
        SwingSlyrienProcedure.execute(entity);
        return retval;
    }

    // Called every tick while held
    @Override
    public void inventoryTick(
        final ItemStack itemstack,
        final Level world,
        final Entity entity,
        final int slot,
        final boolean selected
    ) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected) {
            ChangingBreathesProcedure.execute(entity, itemstack);
        }
    }
}
```

### Item NBT Structure

Items store form selection in NBT:

```java
// In StartBreathesProcedureMixin.java
breathId = 2001.0 + itemstack.getTag().getDouble("select");
```

**Expected NBT structure**:
```json
{
  "select": 0.0,        // Form number (0 = form 1, 1 = form 2, etc.)
  "select_name": "First Form: Swift Strike"  // Display name
}
```

### Breath Cycling

Form selection is handled by `ChangingBreathesProcedure.execute()` which:
1. Detects scroll wheel input
2. Increments/decrements `select` tag
3. Updates `select_name` tag
4. Shows action bar message to player

---

## How to Replicate

### Step 1: Add Mixin Dependency

**build.gradle**:
```gradle
dependencies {
    // Add SpongePowered Mixin
    implementation 'org.spongepowered:mixin:0.8.5'

    // Annotate mixin config in jar
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

// Configure mixin refmap
mixin {
    add sourceSets.main, "yourmod.refmap.json"
}
```

**gradle.properties**:
```properties
# Enable mixin
mixin.env.disableRefMap=false
```

### Step 2: Create Mixin Configuration

**src/main/resources/yourmod.mixins.json**:
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.yourmod.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "yourmod.refmap.json",
  "mixins": [
    "StartBreathesProcedureMixin",
    "ActiveBreathProcedureMixin",
    "DoDamage2ProcedureMixin"
  ],
  "client": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### Step 3: Register Mixin Config

**src/main/resources/META-INF/MANIFEST.MF**:
```
Manifest-Version: 1.0
MixinConfigs: yourmod.mixins.json
```

OR in **@Mod** class:
```java
@Mod("yourmod")
public class YourMod {
    public YourMod() {
        // Mixins are loaded automatically from META-INF/MANIFEST.MF
    }
}
```

### Step 4: Create Mixins

**StartBreathesProcedureMixin.java**:
```java
package com.yourmod.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import com.yourmod.init.ModItems;

@Mixin(value = StartBreathesProcedure.class, remap = false)
public class StartBreathesProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void onStartCustomBreath(
        LevelAccessor world,
        Entity entity,
        ItemStack itemstack,
        CallbackInfo ci
    ) {
        if (entity == null) return;

        // Check if custom sword
        boolean isCustomSword = itemstack.getItem() == ModItems.CUSTOM_SWORD.get();
        if (!isCustomSword) return;

        // Set breath ID (choose unique range like 4000-4099)
        double breathId = 4001.0 + itemstack.getTag().getDouble("select");
        entity.getPersistentData().putDouble("breathes", breathId);
        entity.getPersistentData().putDouble("skill", breathId);

        // Reset counters
        entity.getPersistentData().putDouble("cnt1", 0.0);
        entity.getPersistentData().putDouble("cnt2", 0.0);

        // Set cooldown
        double cooldown = CalculateCooldownTimeProcedure.execute(entity, itemstack);
        if (entity instanceof Player) {
            ((Player)entity).getCooldowns().addCooldown(itemstack.getItem(), (int)cooldown);
        }

        ci.cancel(); // Don't run original
    }
}
```

**ActiveBreathProcedureMixin.java**:
```java
package com.yourmod.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.eventbus.api.*;
import com.yourmod.procedures.*;

@Mixin(value = ActiveBreathProcedure.class, remap = false)
public class ActiveBreathProcedureMixin {

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("TAIL"))
    private static void onActiveCustomBreath(
        @Nullable Event event,
        Entity entity,
        CallbackInfo ci
    ) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // Check if in our range (4000-4099)
        if (breathId >= 4000.0 && breathId < 4100.0) {
            PlayerBreathCustomProcedure.execute(
                entity.level(),
                entity.getX(), entity.getY(), entity.getZ(),
                entity
            );
        }
    }
}
```

### Step 5: Create Dispatcher Procedure

**PlayerBreathCustomProcedure.java**:
```java
package com.yourmod.procedures;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;

public class PlayerBreathCustomProcedure {
    public static void execute(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity
    ) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        if (breathId == 4001.0) {
            BreathesCustom1Procedure.execute(world, x, y, z, entity);
        } else if (breathId == 4002.0) {
            BreathesCustom2Procedure.execute(world, x, y, z, entity);
        } else if (breathId == 4020.0) {
            SwingCustomBaseProcedure.execute(world, x, y, z, entity);
        }
    }
}
```

### Step 6: Create Form Procedures

**BreathesCustom1Procedure.java**:
```java
package com.yourmod.procedures;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.*;
import net.mcreator.kimetsunoyaiba.procedures.*;

public class BreathesCustom1Procedure {
    public static void execute(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity
    ) {
        if (entity == null) return;

        // Increment tick counter
        entity.getPersistentData().putDouble("cnt1",
            entity.getPersistentData().getDouble("cnt1") + 1.0);
        double tick = entity.getPersistentData().getDouble("cnt1");

        // Initial activation
        if (tick == 1.0) {
            // Get forward direction
            GetPowerFowardProcedure.execute(world, entity);

            // Play sound
            if (world instanceof Level) {
                Level level = (Level)world;
                if (!level.isClientSide()) {
                    level.playSound(null, x, y, z,
                        SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        }

        // Active phase
        if (tick < 15.0) {
            // Apply movement
            entity.setDeltaMovement(new Vec3(
                entity.getPersistentData().getDouble("x_power") * 2.0,
                0.2,
                entity.getPersistentData().getDouble("z_power") * 2.0
            ));

            // Spawn particles
            if (world instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)world;
                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    x, y + 1.0, z,
                    10, 0.3, 0.5, 0.3, 0.1
                );
            }

            // Set damage and apply
            entity.getPersistentData().putDouble("Damage", 20.0);
            entity.getPersistentData().putDouble("knockback", 1.5);
            entity.getPersistentData().putDouble("Range", 3.5);
            DoDamage2Procedure.execute(world, x, y + 1.0, z, entity);
        }

        // Cleanup
        if (tick > 20.0) {
            entity.getPersistentData().putDouble("breathes", 0.0);
            entity.getPersistentData().putDouble("skill", 0.0);
            entity.getPersistentData().putDouble("cnt1", 0.0);
        }
    }
}
```

### Step 7: Create Custom Sword Item

**CustomNichirinSword.java**:
```java
package com.yourmod.items;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.*;
import net.mcreator.kimetsunoyaiba.procedures.*;

public class CustomNichirinSword extends SwordItem {

    public CustomNichirinSword() {
        super(Tiers.NETHERITE, 3, -2.4f, new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level world,
        Player entity,
        InteractionHand hand
    ) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);

        // This will trigger StartBreathesProcedureMixin
        StartBreathesProcedure.execute(world, entity, ar.getObject());

        return ar;
    }

    @Override
    public boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {
        boolean retval = super.onEntitySwing(itemstack, entity);

        // Set swing breath ID
        entity.getPersistentData().putDouble("breathes", 4020.0);

        return retval;
    }

    @Override
    public void inventoryTick(
        ItemStack itemstack,
        Level world,
        Entity entity,
        int slot,
        boolean selected
    ) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected) {
            // Handle form cycling
            ChangingBreathesProcedure.execute(entity, itemstack);
        }
    }
}
```

### Step 8: Register Items

**ModItems.java**:
```java
package com.yourmod.init;

import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.javafmlmod.*;
import com.yourmod.items.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "yourmod");

    public static final RegistryObject<Item> CUSTOM_SWORD =
        ITEMS.register("custom_nichirin_sword", CustomNichirinSword::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
```

---

## Advanced Techniques

### Modifying Existing Forms

To replace specific forms of existing breathing styles:

```java
@Mixin(value = PlayerBreathWaterProcedure.class, remap = false)
public class PlayerBreathWaterProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void replaceWaterForm10(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity,
        CallbackInfo ci
    ) {
        double breathId = entity.getPersistentData().getDouble("breathes");

        // Replace only form 10
        if (breathId == 110.0) {
            CustomWater10Procedure.execute(world, x, y, z, entity);
            ci.cancel();
        }
        // Other forms execute normally
    }
}
```

### Adding Global Damage Modifiers

```java
@Mixin(value = DoDamage2Procedure.class, remap = false)
public class DoDamage2ProcedureMixin {

    @ModifyVariable(
        method = "execute",
        at = @At("STORE"),
        name = "damage_sorce_set"
    )
    private static double applyCustomDamageModifiers(
        double currentDamage,
        LevelAccessor world,
        double x, double y, double z,
        Entity entity
    ) {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;

            // Apply custom effect
            if (living.hasEffect(ModEffects.POWER_BOOST.get())) {
                currentDamage *= 2.0;
            }

            // Apply custom attribute
            if (living.getAttributes().hasAttribute(ModAttributes.DAMAGE_MULT.get())) {
                double mult = living.getAttributeValue(ModAttributes.DAMAGE_MULT.get());
                currentDamage *= mult;
            }
        }

        return currentDamage;
    }
}
```

### Cooldown Reduction

```java
@Mixin(value = CalculateCooldownTimeProcedure.class, remap = false)
public class CalculateCooldownTimeMixin {

    @Inject(method = "execute", at = @At("RETURN"), cancellable = true)
    private static void applyCustomCooldownReduction(
        Entity entity,
        ItemStack itemstack,
        CallbackInfoReturnable<Double> cir
    ) {
        if (entity instanceof Player) {
            Player player = (Player)entity;

            // Check for special item in inventory
            if (player.getInventory().contains(new ItemStack(ModItems.COOLDOWN_CHARM.get()))) {
                double cooldown = cir.getReturnValue();
                cir.setReturnValue(cooldown * 0.5); // 50% reduction
            }
        }
    }
}
```

---

## Common Pitfalls

### 1. Remap Flag

**Always set `remap = false`** when mixing into kimetsunoyaiba classes:
```java
@Mixin(value = SomeProcedure.class, remap = false)
```

Without this, Mixin will try to remap the class name from MCP to SRG, which will fail since kimetsunoyaiba is already in production names.

### 2. Method Signatures

Use correct method signatures with obfuscated names (m_XXXXX_):
```java
entity.getPersistentData().m_128459_("breathes") // getDouble
entity.getPersistentData().m_128347_("breathes", value) // putDouble
entity.m_20185_() // getX
entity.m_9236_() // level
```

### 3. Null Checks

Always null-check entities:
```java
if (entity == null) return;
```

### 4. Client/Server Separation

Mixin code runs on both sides by default. Use world checks:
```java
if (!world.isClientSide()) {
    // Server-only code
}
```

### 5. Cancellation Timing

- `@At("HEAD")` with `ci.cancel()` - Prevents original execution
- `@At("TAIL")` - Cannot cancel (already executed)
- `@At("RETURN")` with `cancellable = true` - Can modify return and cancel

---

## Conclusion

The Slyrien addon demonstrates a **non-invasive integration pattern** using Mixins to:

1. **Extend** the breathing system with new styles (ID ranges 2000+, 3000+)
2. **Modify** existing forms selectively (Stone forms 1, 2, 4, 5)
3. **Enhance** damage and cooldown calculations globally
4. **Leverage** existing kimetsunoyaiba procedures directly

This approach allows addons to coexist peacefully without conflicting, as long as they:
- Use unique breath ID ranges
- Set `remap = false` in mixins
- Call original procedures for shared functionality
- Use conditional cancellation for selective replacement

### Benefits

- No need to fork or modify kimetsunoyaiba
- Multiple addons can coexist
- Updates to kimetsunoyaiba are mostly compatible
- Direct access to internal procedures and helpers

### Recommended ID Ranges for New Addons

| Range | Reserved For |
|-------|-------------|
| 0-1999 | kimetsunoyaiba core |
| 2000-2999 | Slyrien addon |
| 3000-3999 | Slyrien addon (Impact) |
| **4000-4999** | **Your addon here** |
| **5000-5999** | **Available** |
| **6000+** | **Available** |

Choose a unique range and document it to avoid conflicts!

---

# Integration Plan for Kimetsunoyaiba-Multiplayer

This section outlines a comprehensive plan to integrate Mixin-based breathing form modification into the Kimetsunoyaiba-Multiplayer mod.

## Strategic Overview

### Current State
- We have a working API-based system for creating nichirin swords
- We use the standard kimetsunoyaiba procedure-based system
- We want to add MANY custom breathing forms and modify MANY existing ones
- The Slyrien addon is actively being updated and will add more forms
- We need full compatibility with Slyrien to allow users to use both mods together

### Goals
1. **Add many new custom breathing forms** without modifying the base kimetsunoyaiba mod
2. **Enhance/modify many existing breathing forms** (not just Mist - all breathing styles)
3. **Maintain full compatibility with Slyrien addon** (both current and future versions)
4. **Maintain compatibility** with our existing API system
5. **Enable easy addition** of future breathing styles
6. **Create a scalable framework** that supports dozens of custom forms

### Scope
This is NOT just about Mist 7th Form - this is about creating a **comprehensive breathing form framework** that allows us to:
- Modify existing forms from ALL breathing styles (Water, Thunder, Flame, Stone, Wind, Mist, etc.)
- Add completely new breathing styles with unique mechanics
- Enhance specific forms while keeping others vanilla
- Coexist peacefully with Slyrien addon updates

### Approach
Use Mixins to hook into kimetsunoyaiba procedures, similar to Slyrien, but:
1. **Use non-conflicting breath ID ranges** that won't overlap with Slyrien's current or future additions
2. **Integrate with our existing API system** for a unified development experience
3. **Create a scalable dispatcher system** that can handle dozens of custom forms
4. **Monitor Slyrien updates** and maintain compatibility

---

## Slyrien Compatibility Strategy

### Current Slyrien Breath ID Usage

Based on analysis of decompiled-slyrien-1.0:
- **2000-2099**: Slyrien custom breathing style
- **3000-3099**: Impact breathing style (from Impact Tekko item)
- **1600s modifications**: Custom Stone breathing forms (1601, 1602, 1604, 1605)
- **1407**: Custom Flower breathing form (final form)
- **1300s modifications**: Custom Sun/Hinokami Kagura forms

### Slyrien Future Growth Projections

Since Slyrien is actively being updated, we must assume they will:
1. **Expand existing ranges**: They may use 2100-2999 and 3100-3999 for variants
2. **Add more enhanced forms**: They may modify more existing breathing styles (1400s, 1500s, 1600s, 1700s)
3. **Add new breathing styles**: They may claim additional thousands ranges (4000s?, 6000s?)

### Our Safe Breath ID Allocation: Even Thousands Strategy

**IMPORTANT**: Based on coordination with the Slyrien developer:
- **Slyrien uses ODD thousands**: 1000s, 3000s, 5000s, 7000s, 9000s, 11000s, etc.
- **We use EVEN thousands**: 10000s, 12000s, 14000s, 16000s, 18000s, 20000s, etc.

This ensures **perfect mathematical separation** with zero conflict possibility.

| Range | Purpose | Conflict Risk |
|-------|---------|---------------|
| **10000-10999** | **Our Enhanced Water Forms** | ✅ Safe (even thousand) |
| **12000-12999** | **Our Enhanced Thunder Forms** | ✅ Safe (even thousand) |
| **14000-14999** | **Our Enhanced Flame Forms** | ✅ Safe (even thousand) |
| **16000-16999** | **Our Enhanced Wind Forms** | ✅ Safe (even thousand) |
| **18000-18999** | **Our Enhanced Stone Forms** | ✅ Safe (even thousand) |
| **20000-20999** | **Our Enhanced Mist Forms** | ✅ Safe (even thousand) |
| **22000-22999** | **Our Enhanced Beast Forms** | ✅ Safe (even thousand) |
| **24000-24999** | **Our Enhanced Insect Forms** | ✅ Safe (even thousand) |
| **26000-26999** | **Our Enhanced Love Forms** | ✅ Safe (even thousand) |
| **28000-28999** | **Our Enhanced Snake Forms** | ✅ Safe (even thousand) |
| **30000-30999** | **Our Custom Breathing Style 1** | ✅ Safe (even thousand) |
| **32000-32999** | **Our Custom Breathing Style 2** | ✅ Safe (even thousand) |
| 34000+ | **Reserved for future even thousands** | ✅ Safe (even thousand) |

### Detailed Allocation Within Our Ranges

#### Enhanced Existing Forms (Even Thousands)
| Range | Breathing Style | Examples |
|-------|----------------|----------|
| 10000-10999 | Enhanced Water Forms | 10001 = Water 1st enhanced, 10010 = Water 10th enhanced |
| 12000-12999 | Enhanced Thunder Forms | 12001 = Thunder 1st enhanced, 12006 = Thunder 6th enhanced |
| 14000-14999 | Enhanced Flame Forms | 14001 = Flame 1st enhanced, 14009 = Flame 9th enhanced |
| 16000-16999 | Enhanced Wind Forms | 16001 = Wind 1st enhanced |
| 18000-18999 | Enhanced Stone Forms | 18001 = Stone 1st enhanced |
| 20000-20999 | Enhanced Mist Forms | 20001 = Mist 1st enhanced, **20007 = Mist 7th enhanced** |
| 22000-22999 | Enhanced Beast Forms | 22001 = Beast 1st enhanced |
| 24000-24999 | Enhanced Insect Forms | 24001 = Insect 1st enhanced |
| 26000-26999 | Enhanced Love Forms | 26001 = Love 1st enhanced |
| 28000-28999 | Enhanced Snake Forms | 28001 = Snake 1st enhanced |

#### New Custom Breathing Styles (Even Thousands)
| Range | Style Name | Description |
|-------|------------|-------------|
| 30000-30999 | Custom Style 1 | TBD - First completely new breathing style |
| 32000-32999 | Custom Style 2 | TBD - Second new breathing style |
| 34000-34999 | Custom Style 3 | TBD - Third new breathing style |
| 36000-36999 | Custom Style 4 | TBD - Fourth new breathing style |
| ... | ... | Unlimited even thousands available |

#### Reserved for Future (Even Thousands Beyond 36000)
Reserved for future expansion, special hybrid forms, or experimental techniques.

### Why This Range is Safe

1. **Perfect Mathematical Separation**: Slyrien uses ODD thousands, we use EVEN thousands
2. **Impossible to Conflict**: As long as both parties stick to the agreement, conflicts are mathematically impossible
3. **Far from base kimetsunoyaiba**: Base mod uses 100-1999, both addon mods use 2000+
4. **Unlimited Growth**: Infinite even thousands available (10000, 12000, 14000, 16000, 18000, 20000, ...)
5. **Simple Rule**: "Is it an even thousand? It's ours. Is it odd? It's Slyrien's."
6. **Easy to Verify**: `breathId / 1000 % 2 == 0` = ours, `breathId / 1000 % 2 == 1` = Slyrien's

### Compatibility Monitoring

**Action Items**:
1. **Monitor Slyrien GitHub/CurseForge** for updates and new breath ID allocations
2. **Test with latest Slyrien** before each release to verify no conflicts
3. **Document our ranges publicly** so Slyrien developers can avoid them if needed
4. **Create compatibility test suite** that loads both mods and verifies breath ID isolation

### Compatibility Testing Checklist

When testing with Slyrien:
- [ ] Both mods load without errors
- [ ] No breath ID conflicts (verify different IDs route to different procedures)
- [ ] Base kimetsunoyaiba forms still work
- [ ] Slyrien forms (2000s, 3000s) still work
- [ ] Our enhanced forms (7000s) work
- [ ] Our custom styles (8000s) work
- [ ] Damage calculation includes both mods' modifiers correctly
- [ ] Cooldown calculation includes both mods' modifiers correctly
- [ ] No performance degradation with both mods installed

---

## API Integration with Mixin System

### How the API and Mixins Work Together

The existing API system will be **enhanced** to automatically integrate with the mixin system:

1. **API Builder Integration**:
   ```java
   // When you use the API builder with custom breath ranges
   KnYAPI.createSword("enhanced_mist_sword")
       .enhancedFormBase(7500)  // This signals mixin integration
       .breathingStyle("mist", forms)
       .build(ITEMS);

   // The builder automatically:
   // - Creates a NichirinSwordBase instance with breathingStyleBaseId = 7500
   // - Sets usesCustomBreathIds = true
   // - The mixin detects this in StartBreathesProcedureMixin
   // - Routes breath execution to CustomBreathDispatcher
   ```

2. **Seamless Developer Experience**:
   - Developers don't need to understand mixins to use the system
   - The API abstracts away the mixin complexity
   - Just specify `.enhancedFormBase()` or `.customStyleBase()` and it works

3. **Backward Compatibility**:
   - Existing API calls without enhanced/custom bases still work normally
   - Only swords with breath IDs >= 7000 route through mixins
   - Base kimetsunoyaiba swords (100-1999) are unaffected

4. **Validation**:
   - The API builder validates breath ID ranges
   - Throws errors if you try to use Slyrien's ranges (2000-6999)
   - Prevents accidental conflicts

### API Builder Methods

```java
// For enhanced existing forms
.enhancedFormBase(int base)  // base must be 7000-7999
    → Sets breathingStyleBaseId
    → Enables mixin routing
    → Example: 7500 for enhanced Mist

// For completely new breathing styles
.customStyleBase(int base)   // base must be 8000-8999
    → Sets breathingStyleBaseId
    → Enables mixin routing
    → Example: 8000 for custom style 1

// Enable custom breaths without specifying base (manual mode)
.enableCustomBreaths()
    → Allows manual control of breath IDs
    → Use with .styleRange() for complete control
```

### Example Usage

```java
// Enhanced Mist Sword (modifies existing Mist breathing)
public static final RegistryObject<Item> ENHANCED_MIST_SWORD =
    KnYAPI.createSword("enhanced_mist_sword")
        .enhancedFormBase(7500)  // 7500-7599 range
        .breathingStyle("mist", EnhancedMistForms.create())
        .defaultParticle(ModParticles.MIST_PARTICLE.get())
        .category(SwordRegistry.SwordCategory.NICHIRIN_ENHANCED)
        .durability(2500)
        .build(ITEMS);

// Custom New Breathing Style
public static final RegistryObject<Item> VOID_BREATHING_SWORD =
    KnYAPI.createSword("void_breathing_sword")
        .customStyleBase(8000)  // 8000-8099 range
        .breathingStyle("void", VoidBreathingForms.create())
        .defaultParticle(ParticleTypes.PORTAL)
        .category(SwordRegistry.SwordCategory.CUSTOM)
        .durability(3000)
        .build(ITEMS);
```

### Internal Flow

```
Developer uses API builder
    ↓
API validates breath ID range (7000-9999 or error)
    ↓
API creates NichirinSwordBase with custom ID
    ↓
Player right-clicks sword
    ↓
StartBreathesProcedureMixin intercepts
    ↓
Checks: Is this NichirinSwordBase? Is baseId >= 7000?
    ↓ (yes)
Sets entity.breathes = baseId + formNumber
    ↓
ActiveBreathProcedureMixin intercepts on tick
    ↓
Checks: Is breathId >= 7000?
    ↓ (yes)
Routes to CustomBreathDispatcher
    ↓
Dispatcher routes to correct form procedure
    ↓
Form executes with custom behavior
```

---

## Phase 1: Setup Mixin Infrastructure

### Task 1.1: Add Mixin Dependencies

**File**: `build.gradle`

Add mixin dependency and configuration:

```gradle
buildscript {
    repositories {
        maven { url = 'https://repo.spongepowered.org/maven' }
    }
    dependencies {
        classpath 'org.spongepowered:mixingradle:0.7-SNAPSHOT'
    }
}

apply plugin: 'org.spongepowered.mixin'

dependencies {
    // Mixin
    implementation 'org.spongepowered:mixin:0.8.5'
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

mixin {
    add sourceSets.main, "kimetsunoyaibamultiplayer.refmap.json"
    config "kimetsunoyaibamultiplayer.mixins.json"
}

jar {
    manifest {
        attributes([
            "MixinConfigs": "kimetsunoyaibamultiplayer.mixins.json"
        ])
    }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 30 minutes

### Task 1.2: Create Mixin Configuration

**File**: `src/main/resources/kimetsunoyaibamultiplayer.mixins.json`

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.lerdorf.kimetsunoyaibamultiplayer.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "kimetsunoyaibamultiplayer.refmap.json",
  "mixins": [
    "ActiveBreathProcedureMixin",
    "StartBreathesProcedureMixin",
    "DoDamage2ProcedureMixin",
    "CalculateCooldownTimeMixin"
  ],
  "client": [],
  "injectors": {
    "defaultRequire": 1
  },
  "overwrites": {
    "requireAnnotations": true
  }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 15 minutes

### Task 1.3: Create Mixin Package Structure

Create directory structure:
```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/
├── mixin/
│   ├── ActiveBreathProcedureMixin.java
│   ├── StartBreathesProcedureMixin.java
│   ├── DoDamage2ProcedureMixin.java
│   └── CalculateCooldownTimeMixin.java
├── procedures/
│   ├── CustomBreathDispatcher.java
│   ├── mistbreathing/
│   │   ├── MistBreathing7thFormProcedure.java
│   │   └── ...
│   └── customstyles/
│       └── ...
```

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 10 minutes

---

## Phase 2: Implement Core Mixins

### Task 2.1: Active Breath Dispatcher Mixin

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/ActiveBreathProcedureMixin.java`

Purpose: Route our custom breathing forms to their procedures during active tick.

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraftforge.eventbus.api.*;
import javax.annotation.*;
import net.minecraft.world.entity.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.CustomBreathDispatcher;

/**
 * Hooks into ActiveBreathProcedure to route custom breathing forms.
 * Breath ID ranges:
 * - 7000-7999: Enhanced forms for existing breathing styles
 * - 8000-8999: Completely new custom breathing styles
 * - 9000-9999: Reserved for future expansion
 */
@Mixin(value = ActiveBreathProcedure.class, remap = false)
public class ActiveBreathProcedureMixin {

    @Inject(
        method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL")
    )
    private static void onActiveCustomBreath(
        @Nullable final Event event,
        final Entity entity,
        final CallbackInfo ci
    ) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // Route custom breath ranges
        // 7000-7999: Enhanced existing forms
        // 8000-8999: New custom breathing styles
        // 9000-9999: Reserved/experimental
        if (breathId >= 7000.0 && breathId < 10000.0) {
            CustomBreathDispatcher.execute(
                entity.level(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity
            );
        }
    }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 1 hour
**Dependencies**: Task 1.1, 1.2

### Task 2.2: Start Breath Interceptor Mixin

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/StartBreathesProcedureMixin.java`

Purpose: Intercept breath activation for our custom swords.

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.*;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBase;

/**
 * Intercepts StartBreathesProcedure to handle our custom swords
 * that may have custom breath IDs or special activation logic.
 */
@Mixin(value = StartBreathesProcedure.class, remap = false)
public class StartBreathesProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void onStartCustomBreath(
        final LevelAccessor world,
        final Entity entity,
        final ItemStack itemstack,
        final CallbackInfo ci
    ) {
        if (entity == null || itemstack.isEmpty()) return;

        // Check if this is one of our custom nichirin swords
        if (!(itemstack.getItem() instanceof NichirinSwordBase)) return;

        NichirinSwordBase sword = (NichirinSwordBase) itemstack.getItem();

        // Check if this sword uses custom breath IDs (7000+)
        double selectValue = itemstack.getOrCreateTag().getDouble("select");
        int baseId = sword.getBreathingStyleBaseId();

        // Only intercept if using our custom ranges (7000-9999)
        if (baseId < 7000) return;

        double breathId = baseId + selectValue;

        // Set breath data
        entity.getPersistentData().putDouble("breathes", breathId);
        entity.getPersistentData().putDouble("skill", breathId);
        entity.getPersistentData().putDouble("cnt1", 0.0);
        entity.getPersistentData().putDouble("cnt2", 0.0);
        entity.getPersistentData().putDouble("cnt3", 0.0);
        entity.getPersistentData().putDouble("cnt4", 0.0);
        entity.getPersistentData().putDouble("cnt5", 0.0);
        entity.getPersistentData().putDouble("Damage", 0.0);

        // Calculate and set cooldown
        double cooldown = Math.round(CalculateCooldownTimeProcedure.execute(entity, itemstack));
        if (entity instanceof Player) {
            Player player = (Player) entity;
            player.getCooldowns().addCooldown(itemstack.getItem(), (int) cooldown);
        }

        // Display form name
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (!player.level().isClientSide()) {
                String selectName = itemstack.getOrCreateTag().getString("select_name");
                if (!selectName.isEmpty()) {
                    player.displayClientMessage(
                        Component.literal(selectName),
                        true
                    );
                }
            }
        }

        ci.cancel(); // Prevent original execution
    }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 1.5 hours
**Dependencies**: Task 1.1, 1.2, Task 3.1

### Task 2.3: Damage Modifier Mixin

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/DoDamage2ProcedureMixin.java`

Purpose: Apply global damage modifiers from our custom effects/attributes.

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;

/**
 * Modifies damage calculation to include custom multipliers
 * from effects, attributes, or special conditions.
 */
@Mixin(value = DoDamage2Procedure.class, remap = false)
public class DoDamage2ProcedureMixin {

    @ModifyVariable(
        method = "execute",
        at = @At("STORE"),
        name = "damage_sorce_set"
    )
    private static double applyCustomDamageModifiers(
        double currentDamage,
        final LevelAccessor world,
        final double x,
        final double y,
        final double z,
        final Entity entity
    ) {
        if (entity == null) return currentDamage;

        // Apply custom damage multipliers based on breath ID
        double breathId = entity.getPersistentData().getDouble("breathes");

        // Enhanced forms (7000-7999) get varying damage boosts
        if (breathId >= 7000.0 && breathId < 8000.0) {
            // Enhanced forms typically get 20-50% damage boost
            // Specific form modifiers can be added here
            if (breathId == 7507.0) { // Enhanced Mist 7th Form
                currentDamage *= 1.3;
            }
            // Add more specific enhanced form modifiers as needed
        }

        // Custom breathing styles (8000-8999) have their own damage scaling
        if (breathId >= 8000.0 && breathId < 9000.0) {
            // Custom styles can have unique damage modifiers
            // Will be implemented per-style
        }

        // Future: Add custom effect-based modifiers here
        // if (entity instanceof LivingEntity) {
        //     LivingEntity living = (LivingEntity) entity;
        //     if (living.hasEffect(ModEffects.DEMON_SLAYER_MARK.get())) {
        //         currentDamage *= 1.5;
        //     }
        // }

        return currentDamage;
    }
}
```

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 45 minutes
**Dependencies**: Task 1.1, 1.2

### Task 2.4: Cooldown Modifier Mixin

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/CalculateCooldownTimeMixin.java`

Purpose: Apply custom cooldown modifiers.

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

/**
 * Modifies cooldown calculation for custom forms or effects.
 */
@Mixin(value = CalculateCooldownTimeProcedure.class, remap = false)
public class CalculateCooldownTimeMixin {

    @Inject(
        method = "execute",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void applyCustomCooldownModifiers(
        final Entity entity,
        final ItemStack itemstack,
        final CallbackInfoReturnable<Double> cir
    ) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // Enhanced forms (7000-7999) may have adjusted cooldowns
        if (breathId >= 7000.0 && breathId < 8000.0) {
            double cooldown = cir.getReturnValue();
            // Enhanced forms typically have slightly longer cooldowns due to increased power
            cir.setReturnValue(cooldown * 1.1); // 10% longer
        }

        // Custom breathing styles (8000-8999) have their own cooldown scaling
        if (breathId >= 8000.0 && breathId < 9000.0) {
            // Custom styles can have unique cooldown modifiers
            // Will be implemented per-style
        }

        // Future: Add custom cooldown reduction effects
    }
}
```

**Status**: Not implemented
**Priority**: Low
**Estimated Time**: 30 minutes
**Dependencies**: Task 1.1, 1.2

---

## Phase 3: Update API System

### Task 3.1: Extend NichirinSwordBuilder

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/api/NichirinSwordBuilder.java`

Add support for custom breath ID ranges:

```java
public class NichirinSwordBuilder {
    private String swordId;
    private BreathingStyle breathingStyle;
    private int styleRangeBase = -1;
    private boolean useCustomBreathIds = false; // NEW

    // ... existing code ...

    /**
     * Enable custom breath ID handling via mixins.
     * Use this when you want breath IDs in the 7000+ range
     * that route through CustomBreathDispatcher.
     */
    public NichirinSwordBuilder enableCustomBreaths() {
        this.useCustomBreathIds = true;
        return this;
    }

    /**
     * Set a custom breath ID base for enhanced existing forms (7000-7999).
     * Examples:
     * - 7000 for enhanced Water forms
     * - 7100 for enhanced Thunder forms
     * - 7500 for enhanced Mist forms
     */
    public NichirinSwordBuilder enhancedFormBase(int base) {
        if (base < 7000 || base >= 8000) {
            throw new IllegalArgumentException("Enhanced form base must be 7000-7999");
        }
        this.styleRangeBase = base;
        this.useCustomBreathIds = true;
        return this;
    }

    /**
     * Set a custom breath ID base for completely new breathing styles (8000-8999).
     * Examples:
     * - 8000 for first custom breathing style
     * - 8100 for second custom breathing style
     */
    public NichirinSwordBuilder customStyleBase(int base) {
        if (base < 8000 || base >= 9000) {
            throw new IllegalArgumentException("Custom style base must be 8000-8999");
        }
        this.styleRangeBase = base;
        this.useCustomBreathIds = true;
        return this;
    }

    // ... rest of builder ...

    // ... rest of builder ...
}
```

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 1 hour
**Dependencies**: None

### Task 3.2: Create NichirinSwordBase

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/items/NichirinSwordBase.java`

Create a base class that our custom swords extend:

```java
package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.world.item.*;

/**
 * Base class for all custom nichirin swords in this mod.
 * Provides interface for mixins to query breath ID ranges.
 */
public abstract class NichirinSwordBase extends SwordItem {

    protected final int breathingStyleBaseId;
    protected final boolean usesCustomBreathIds;

    public NichirinSwordBase(Tier tier, int attackDamage, float attackSpeed,
                             Properties properties, int breathingStyleBaseId,
                             boolean usesCustomBreathIds) {
        super(tier, attackDamage, attackSpeed, properties);
        this.breathingStyleBaseId = breathingStyleBaseId;
        this.usesCustomBreathIds = usesCustomBreathIds;
    }

    public int getBreathingStyleBaseId() {
        return breathingStyleBaseId;
    }

    public boolean usesCustomBreathIds() {
        return usesCustomBreathIds;
    }
}
```

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 30 minutes
**Dependencies**: None

---

## Phase 4: Implement Custom Breathing Forms (Multiple Styles)

### Task 4.1: Create Scalable Custom Breath Dispatcher

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/procedures/CustomBreathDispatcher.java`

This is the central routing hub for ALL custom breathing forms. It's designed to handle dozens of enhanced forms and multiple custom breathing styles.

```java
package com.lerdorf.kimetsunoyaibamultiplayer.procedures;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.water.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.thunder.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.flame.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.wind.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.stone.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.mist.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.beast.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.custom.style1.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.custom.style2.*;

/**
 * Central dispatcher for all custom breathing forms.
 * Routes breath IDs (7000-9999) to their corresponding procedures.
 *
 * Breath ID Structure:
 * - 7000-7099: Enhanced Water Forms
 * - 7100-7199: Enhanced Thunder Forms
 * - 7200-7299: Enhanced Flame Forms
 * - 7300-7399: Enhanced Wind Forms
 * - 7400-7499: Enhanced Stone Forms
 * - 7500-7599: Enhanced Mist Forms
 * - 7600-7699: Enhanced Beast Forms
 * - 7700-7799: Enhanced Insect Forms
 * - 7800-7899: Enhanced Love Forms
 * - 7900-7999: Enhanced Snake Forms
 * - 8000-8099: Custom Breathing Style 1
 * - 8100-8199: Custom Breathing Style 2
 * - 8200-8299: Custom Breathing Style 3
 * - 9000-9999: Reserved for future
 */
public class CustomBreathDispatcher {

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // ===== ENHANCED EXISTING FORMS (7000-7999) =====

        // Enhanced Water Forms (7000-7099)
        if (breathId >= 7000.0 && breathId < 7100.0) {
            dispatchWaterForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Thunder Forms (7100-7199)
        else if (breathId >= 7100.0 && breathId < 7200.0) {
            dispatchThunderForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Flame Forms (7200-7299)
        else if (breathId >= 7200.0 && breathId < 7300.0) {
            dispatchFlameForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Wind Forms (7300-7399)
        else if (breathId >= 7300.0 && breathId < 7400.0) {
            dispatchWindForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Stone Forms (7400-7499)
        else if (breathId >= 7400.0 && breathId < 7500.0) {
            dispatchStoneForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Mist Forms (7500-7599)
        else if (breathId >= 7500.0 && breathId < 7600.0) {
            dispatchMistForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Beast Forms (7600-7699)
        else if (breathId >= 7600.0 && breathId < 7700.0) {
            dispatchBeastForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Insect Forms (7700-7799)
        else if (breathId >= 7700.0 && breathId < 7800.0) {
            dispatchInsectForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Love Forms (7800-7899)
        else if (breathId >= 7800.0 && breathId < 7900.0) {
            dispatchLoveForm(world, x, y, z, entity, breathId);
        }
        // Enhanced Snake Forms (7900-7999)
        else if (breathId >= 7900.0 && breathId < 8000.0) {
            dispatchSnakeForm(world, x, y, z, entity, breathId);
        }

        // ===== NEW CUSTOM BREATHING STYLES (8000-8999) =====

        // Custom Breathing Style 1 (8000-8099)
        else if (breathId >= 8000.0 && breathId < 8100.0) {
            dispatchCustomStyle1(world, x, y, z, entity, breathId);
        }
        // Custom Breathing Style 2 (8100-8199)
        else if (breathId >= 8100.0 && breathId < 8200.0) {
            dispatchCustomStyle2(world, x, y, z, entity, breathId);
        }
        // Custom Breathing Style 3 (8200-8299)
        else if (breathId >= 8200.0 && breathId < 8300.0) {
            dispatchCustomStyle3(world, x, y, z, entity, breathId);
        }

        // ===== RESERVED FOR FUTURE (9000-9999) =====
        // Add experimental or special forms here
    }

    // ===== ENHANCED FORM DISPATCHERS =====

    private static void dispatchWaterForm(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7000.0);
        switch (formNumber) {
            case 1:  EnhancedWater1stFormProcedure.execute(world, x, y, z, entity); break;
            case 2:  EnhancedWater2ndFormProcedure.execute(world, x, y, z, entity); break;
            case 10: EnhancedWater10thFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Water forms as needed
        }
    }

    private static void dispatchThunderForm(LevelAccessor world, double x, double y, double z,
                                            Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7100.0);
        switch (formNumber) {
            case 1: EnhancedThunder1stFormProcedure.execute(world, x, y, z, entity); break;
            case 6: EnhancedThunder6thFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Thunder forms as needed
        }
    }

    private static void dispatchFlameForm(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7200.0);
        switch (formNumber) {
            case 1: EnhancedFlame1stFormProcedure.execute(world, x, y, z, entity); break;
            case 9: EnhancedFlame9thFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Flame forms as needed
        }
    }

    private static void dispatchWindForm(LevelAccessor world, double x, double y, double z,
                                         Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7300.0);
        switch (formNumber) {
            case 1: EnhancedWind1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Wind forms as needed
        }
    }

    private static void dispatchStoneForm(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7400.0);
        switch (formNumber) {
            case 1: EnhancedStone1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Stone forms as needed
        }
    }

    private static void dispatchMistForm(LevelAccessor world, double x, double y, double z,
                                         Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7500.0);
        switch (formNumber) {
            case 1: EnhancedMist1stFormProcedure.execute(world, x, y, z, entity); break;
            case 7: EnhancedMist7thFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Mist forms as needed
            case 20: SwingMistBaseProcedure.execute(world, x, y, z, entity); break; // Basic swing
        }
    }

    private static void dispatchBeastForm(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7600.0);
        switch (formNumber) {
            case 1: EnhancedBeast1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Beast forms as needed
        }
    }

    private static void dispatchInsectForm(LevelAccessor world, double x, double y, double z,
                                           Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7700.0);
        switch (formNumber) {
            case 1: EnhancedInsect1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Insect forms as needed
        }
    }

    private static void dispatchLoveForm(LevelAccessor world, double x, double y, double z,
                                         Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7800.0);
        switch (formNumber) {
            case 1: EnhancedLove1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Love forms as needed
        }
    }

    private static void dispatchSnakeForm(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double breathId) {
        int formNumber = (int)(breathId - 7900.0);
        switch (formNumber) {
            case 1: EnhancedSnake1stFormProcedure.execute(world, x, y, z, entity); break;
            // Add more enhanced Snake forms as needed
        }
    }

    // ===== CUSTOM STYLE DISPATCHERS =====

    private static void dispatchCustomStyle1(LevelAccessor world, double x, double y, double z,
                                             Entity entity, double breathId) {
        int formNumber = (int)(breathId - 8000.0);
        switch (formNumber) {
            case 1: CustomStyle1Form1Procedure.execute(world, x, y, z, entity); break;
            case 2: CustomStyle1Form2Procedure.execute(world, x, y, z, entity); break;
            // Add forms for Custom Style 1
        }
    }

    private static void dispatchCustomStyle2(LevelAccessor world, double x, double y, double z,
                                             Entity entity, double breathId) {
        int formNumber = (int)(breathId - 8100.0);
        switch (formNumber) {
            case 1: CustomStyle2Form1Procedure.execute(world, x, y, z, entity); break;
            // Add forms for Custom Style 2
        }
    }

    private static void dispatchCustomStyle3(LevelAccessor world, double x, double y, double z,
                                             Entity entity, double breathId) {
        int formNumber = (int)(breathId - 8200.0);
        switch (formNumber) {
            case 1: CustomStyle3Form1Procedure.execute(world, x, y, z, entity); break;
            // Add forms for Custom Style 3
        }
    }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 45 minutes
**Dependencies**: Task 2.1

### Task 4.2: Implement Example Enhanced Forms

This task demonstrates implementing enhanced forms. Start with one or two as examples, then add more as needed.

#### Example 1: Enhanced Mist 7th Form

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/procedures/enhanced/mist/EnhancedMist7thFormProcedure.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.procedures.enhanced.mist;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;
import net.minecraft.server.level.*;
import net.minecraft.core.particles.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.mcreator.kimetsunoyaiba.init.*;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

/**
 * Enhanced Mist Breathing 7th Form: Obscuring Clouds
 *
 * Improvements over base kimetsunoyaiba version:
 * - Multi-directional particle effects
 * - Improved visual feedback with custom mist particles
 * - Enhanced mobility with controlled dash
 * - Better hitbox detection for consistent hits
 * - Custom sound effects
 */
public class EnhancedMist7thFormProcedure {

    private static final int FORM_DURATION = 25;
    private static final double BASE_DAMAGE = 18.0;
    private static final double DASH_SPEED = 2.5;
    private static final double PARTICLE_DENSITY = 15.0;

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        // Increment tick counter
        entity.getPersistentData().putDouble("cnt1",
            entity.getPersistentData().getDouble("cnt1") + 1.0);
        double tick = entity.getPersistentData().getDouble("cnt1");

        // === PHASE 1: ACTIVATION (tick 1) ===
        if (tick == 1.0) {
            activationPhase(world, x, y, z, entity);
        }

        // === PHASE 2: DASH AND STRIKE (ticks 1-15) ===
        if (tick < 15.0) {
            dashPhase(world, x, y, z, entity, tick);
        }

        // === PHASE 3: LINGERING MIST (ticks 15-25) ===
        if (tick >= 15.0 && tick < FORM_DURATION) {
            lingeringMistPhase(world, x, y, z, entity, tick);
        }

        // === PHASE 4: CLEANUP ===
        if (tick >= FORM_DURATION) {
            cleanupPhase(entity);
        }
    }

    private static void activationPhase(LevelAccessor world, double x, double y, double z,
                                       Entity entity) {
        // Get forward direction
        GetPowerFowardProcedure.execute(world, entity);

        // Store initial position for particle trail
        entity.getPersistentData().putDouble("mist7_start_x", x);
        entity.getPersistentData().putDouble("mist7_start_y", y);
        entity.getPersistentData().putDouble("mist7_start_z", z);

        // Play activation sound
        if (world instanceof Level) {
            Level level = (Level) world;
            if (!level.isClientSide()) {
                level.playSound(null, x, y, z,
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.2f, 0.8f);
            }
        }

        // Initial mist burst
        if (world instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) world;
            serverLevel.sendParticles(
                ModParticles.MIST_PARTICLE.get(),
                x, y + 1.0, z,
                30, 1.0, 1.0, 1.0, 0.1
            );
        }
    }

    private static void dashPhase(LevelAccessor world, double x, double y, double z,
                                  Entity entity, double tick) {
        // Smooth dash movement
        double speedMult = 1.0 - (tick / 15.0) * 0.3; // Gradually slow down
        entity.setDeltaMovement(new Vec3(
            entity.getPersistentData().getDouble("x_power") * DASH_SPEED * speedMult,
            0.1,
            entity.getPersistentData().getDouble("z_power") * DASH_SPEED * speedMult
        ));

        // Spawn mist trail
        if (world instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) world;

            // Dense mist around player
            serverLevel.sendParticles(
                ModParticles.MIST_PARTICLE.get(),
                x, y + 1.0, z,
                (int) PARTICLE_DENSITY, 0.5, 0.5, 0.5, 0.05
            );

            // Directional particles
            double yaw = Math.toRadians(entity.getYRot() + 90);
            for (int i = -2; i <= 2; i++) {
                double angle = yaw + (i * Math.PI / 6);
                double offsetX = Math.cos(angle) * 1.5;
                double offsetZ = Math.sin(angle) * 1.5;

                serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    x + offsetX, y + 1.0, z + offsetZ,
                    5, 0.2, 0.3, 0.2, 0.02
                );
            }
        }

        // Apply damage every 3 ticks
        if (tick % 3.0 == 0.0) {
            // Get strength amplifier
            int strength = 0;
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                if (living.hasEffect(MobEffects.DAMAGE_BOOST)) {
                    strength = living.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
                }
            }

            double finalDamage = BASE_DAMAGE * (1.0 + strength / 3.0);

            entity.getPersistentData().putDouble("Damage", finalDamage);
            entity.getPersistentData().putDouble("knockback", 0.8);
            entity.getPersistentData().putDouble("Range", 3.0);
            DoDamage2Procedure.execute(world, x, y + 1.0, z, entity);
        }
    }

    private static void lingeringMistPhase(LevelAccessor world, double x, double y, double z,
                                          Entity entity, double tick) {
        // Spawn fading mist particles
        if (world instanceof ServerLevel && tick % 2.0 == 0.0) {
            ServerLevel serverLevel = (ServerLevel) world;

            double intensity = 1.0 - ((tick - 15.0) / 10.0); // Fade out

            serverLevel.sendParticles(
                ModParticles.MIST_PARTICLE.get(),
                x, y + 1.0, z,
                (int) (5 * intensity), 1.5, 1.0, 1.5, 0.02
            );
        }
    }

    private static void cleanupPhase(Entity entity) {
        entity.getPersistentData().putDouble("breathes", 0.0);
        entity.getPersistentData().putDouble("skill", 0.0);
        entity.getPersistentData().putDouble("cnt1", 0.0);

        // Clear stored positions
        entity.getPersistentData().remove("mist7_start_x");
        entity.getPersistentData().remove("mist7_start_y");
        entity.getPersistentData().remove("mist7_start_z");
    }
}
```

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 2.5 hours
**Dependencies**: Task 4.1, ModParticles implementation

### Task 4.3: Create Enhanced Breathing Swords (Examples)

Create example swords for enhanced breathing styles. These demonstrate the API integration with the mixin system.

#### Example 1: Enhanced Mist Sword

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/items/enhanced/EnhancedMistSword.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.items.enhanced;

import net.minecraft.world.item.*;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBase;

public class EnhancedMistSword extends NichirinSwordBase {

    public EnhancedMistSword() {
        super(
            Tiers.NETHERITE,
            3,
            -2.4f,
            new Item.Properties(),
            7500, // Breath ID base (7500-7599 for enhanced Mist)
            true  // Uses custom breath IDs
        );
    }

    // Item behavior methods (use, swing, etc.) handled by base class
}
```

#### Example 2: Enhanced Thunder Sword

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/items/enhanced/EnhancedThunderSword.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.items.enhanced;

import net.minecraft.world.item.*;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBase;

public class EnhancedThunderSword extends NichirinSwordBase {

    public EnhancedThunderSword() {
        super(
            Tiers.NETHERITE,
            3,
            -2.4f,
            new Item.Properties(),
            7100, // Breath ID base (7100-7199 for enhanced Thunder)
            true  // Uses custom breath IDs
        );
    }
}
```

**Note**: Repeat this pattern for each breathing style you want to enhance. Each enhanced sword should:
- Extend `NichirinSwordBase`
- Use the correct base ID from the 7000-7999 range
- Set `usesCustomBreathIds = true`

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 1 hour (for multiple swords)
**Dependencies**: Task 3.2, Task 4.2

### Task 4.4: Register Enhanced Swords with API

Use the existing API system to register enhanced swords. This demonstrates how the API integrates with the mixin system.

**File**: Example registration using API

```java
// In your mod's item registration class

// Option 1: Using API builder (recommended for consistency)
public static final RegistryObject<Item> ENHANCED_MIST_SWORD =
    KnYAPI.createSword("enhanced_mist_nichirin_sword")
        .enhancedFormBase(7500)  // Enhanced Mist forms
        .breathingStyle("mist", MistBreathingForms.createEnhancedMist())
        .defaultParticle(ParticleTypes.CLOUD)
        .category(SwordRegistry.SwordCategory.NICHIRIN_ENHANCED)
        .durability(2500)
        .build(ITEMS);

// Option 2: Direct registration (if you need more control)
public static final RegistryObject<Item> ENHANCED_THUNDER_SWORD =
    ITEMS.register("enhanced_thunder_nichirin_sword", EnhancedThunderSword::new);

// Register multiple enhanced swords
public static final RegistryObject<Item> ENHANCED_WATER_SWORD =
    ITEMS.register("enhanced_water_nichirin_sword", () -> new EnhancedWaterSword());

public static final RegistryObject<Item> ENHANCED_FLAME_SWORD =
    ITEMS.register("enhanced_flame_nichirin_sword", () -> new EnhancedFlameSword());
```

**API Integration Notes**:
- The API builder should automatically set up the mixin integration when `enhancedFormBase()` is called
- All enhanced swords should be categorized as `NICHIRIN_ENHANCED` for easy filtering
- Enhanced forms typically have higher durability and damage than base forms

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 30 minutes
**Dependencies**: Task 4.3

---

## Phase 5: Testing and Validation

### Task 5.1: Unit Tests

Create test cases for:
- Mixin injection verification
- Breath ID routing
- Damage calculation
- Cooldown calculation

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 2 hours

### Task 5.2: In-Game Testing

Test scenarios:
1. Enhanced Mist 7th form activation
2. Particle effects visual verification
3. Damage output verification
4. Cooldown timing
5. Compatibility with existing swords
6. Multiplayer synchronization

**Status**: Not implemented
**Priority**: High
**Estimated Time**: 1.5 hours

### Task 5.3: Performance Testing

Verify:
- No performance regression from mixins
- Particle spawning doesn't cause lag
- Multiple players using forms simultaneously

**Status**: Not implemented
**Priority**: Medium
**Estimated Time**: 1 hour

---

## Phase 6: Documentation

### Task 6.1: Update API Documentation

Update `docs/api-usage-guide.md` with:
- Custom breath ID system
- How to create enhanced forms
- Mixin integration examples

**Status**: Not implemented
**Priority**: Low
**Estimated Time**: 1 hour

### Task 6.2: Create Developer Guide

New file: `docs/custom-breathing-forms-guide.md`

Include:
- Step-by-step guide for adding new forms
- Breath ID allocation guide
- Procedure implementation patterns
- Particle and animation integration

**Status**: Not implemented
**Priority**: Low
**Estimated Time**: 2 hours

### Task 6.3: Update CLAUDE.md

Add sections on:
- Mixin system usage
- Custom breathing form architecture
- Breath ID ranges

**Status**: Not implemented
**Priority**: Low
**Estimated Time**: 30 minutes

---

## Implementation Timeline

### Week 1: Infrastructure Setup
- [ ] Phase 1: Setup Mixin Infrastructure (Tasks 1.1-1.3)
- [ ] Phase 2: Implement Core Mixins (Tasks 2.1-2.4)
- [ ] Phase 3: Update API System (Tasks 3.1-3.2)

**Total Time**: ~8 hours

### Week 2: Enhanced Forms Implementation
- [ ] Phase 4: Implement Custom Breathing Forms (Tasks 4.1-4.4)
- [ ] Implement scalable CustomBreathDispatcher
- [ ] Create example enhanced forms (Mist 7th, Thunder 1st, etc.)
- [ ] Ensure required particles are implemented (MIST_PARTICLE, etc.)
- [ ] Register enhanced swords with API integration
- [ ] Test basic functionality for multiple breathing styles

**Total Time**: ~8 hours (more forms = more time)

### Week 3: Testing and Polish
- [ ] Phase 5: Testing and Validation (Tasks 5.1-5.3)
- [ ] Fix bugs and refine effects
- [ ] Multiplayer testing

**Total Time**: ~5 hours

### Week 4: Documentation
- [ ] Phase 6: Documentation (Tasks 6.1-6.3)
- [ ] Create examples
- [ ] Final review

**Total Time**: ~4 hours

---

## Breath ID Allocation Plan

### Complete Allocation Table

| Range | Purpose | Conflict with Slyrien? | Status |
|-------|---------|------------------------|--------|
| 0-99 | kimetsunoyaiba reserved | No | N/A |
| 100-199 | Water Breathing (base) | No | N/A |
| 200-299 | Thunder Breathing (base) | No | N/A |
| 300-399 | Flame Breathing (base) | No | N/A |
| 400-499 | Wind Breathing (base) | No | N/A |
| 500-599 | Stone Breathing (base) | No | N/A |
| 600-699 | Mist Breathing (base) | No | N/A |
| 700-799 | Beast Breathing (base) | No | N/A |
| 800-899 | Insect Breathing (base) | No | N/A |
| 900-999 | Love Breathing (base) | No | N/A |
| 1000-1099 | Snake Breathing (base) | No | N/A |
| 1100-1199 | Moon Breathing (base) | No | N/A |
| 1200-1299 | Serpent Breathing (base) | No | N/A |
| 1300-1399 | Sun/Hinokami Kagura (base) | **Yes - Slyrien modifies** | N/A |
| 1400-1499 | Flower Breathing (base) | **Yes - Slyrien modifies 1407** | N/A |
| 1500-1599 | Sound Breathing (base) | Possibly (Slyrien may expand here) | N/A |
| 1600-1699 | Stone Breathing (base) | **Yes - Slyrien modifies 1601-1605** | N/A |
| 1700-1899 | Other base forms | Possibly (Slyrien may expand here) | N/A |
| **2000-2099** | **Slyrien Breathing Style** | N/A - Owned by Slyrien | N/A |
| **3000-3099** | **Impact Breathing (Slyrien)** | N/A - Owned by Slyrien | N/A |
| 2100-6999 | **Reserved for Slyrien expansion** | Yes - Leave for Slyrien | Avoid |
| **7000-7099** | **Our Enhanced Water Forms** | ✅ No conflict | Planned |
| **7100-7199** | **Our Enhanced Thunder Forms** | ✅ No conflict | Planned |
| **7200-7299** | **Our Enhanced Flame Forms** | ✅ No conflict | Planned |
| **7300-7399** | **Our Enhanced Wind Forms** | ✅ No conflict | Planned |
| **7400-7499** | **Our Enhanced Stone Forms** | ✅ No conflict | Planned |
| **7500-7599** | **Our Enhanced Mist Forms** | ✅ No conflict | Planned |
| 7507 | Our Enhanced Mist 7th Form | ✅ No conflict | Planned |
| **7600-7699** | **Our Enhanced Beast Forms** | ✅ No conflict | Planned |
| **7700-7799** | **Our Enhanced Insect Forms** | ✅ No conflict | Planned |
| **7800-7899** | **Our Enhanced Love Forms** | ✅ No conflict | Planned |
| **7900-7999** | **Our Enhanced Snake/Other Forms** | ✅ No conflict | Planned |
| **8000-8099** | **Our Custom Breathing Style 1** | ✅ No conflict | Available |
| **8100-8199** | **Our Custom Breathing Style 2** | ✅ No conflict | Available |
| **8200-8299** | **Our Custom Breathing Style 3** | ✅ No conflict | Available |
| 8300-8999 | Our future custom styles | ✅ No conflict | Available |
| **9000-9999** | **Reserved for experimental** | ✅ No conflict | Reserved |

---

## Risk Assessment

### High Risk
- **Mixin conflicts with other mods**: Mitigated by using TAIL injection and careful targeting
- **kimetsunoyaiba updates breaking mixins**: Mitigated by targeting stable procedures

### Medium Risk
- **Performance impact of additional procedure calls**: Test thoroughly
- **Particle spawning causing lag**: Use particle limits and distance checks

### Low Risk
- **API backward compatibility**: New features are additive
- **Breath ID conflicts**: Well-documented allocation system

---

## Success Criteria

1. ✅ Mixins successfully inject without errors
2. ✅ Enhanced Mist 7th Form executes with custom behavior
3. ✅ No regression in existing breathing forms
4. ✅ No performance degradation
5. ✅ Multiplayer synchronization works correctly
6. ✅ API remains backward compatible
7. ✅ Documentation is complete and clear

---

## Ongoing Slyrien Compatibility Maintenance

### Monitoring Strategy

To maintain compatibility with Slyrien as it evolves:

1. **Watch Slyrien Releases**
   - Monitor CurseForge/Modrinth for Slyrien updates
   - Check GitHub if source is available
   - Subscribe to update notifications

2. **Version Testing Matrix**
   ```
   Our Mod Version | Slyrien Version | kimetsunoyaiba Version | Status
   1.0.0          | 1.0.x          | Latest                 | ✅ Compatible
   1.0.0          | 2.0.x          | Latest                 | 🔍 Needs testing
   ```

3. **Automated Compatibility Checks**
   - Create CI/CD test that loads both mods
   - Verify no breath ID conflicts
   - Check that both mods' forms execute correctly
   - Test damage/cooldown calculations with both mods

4. **Communication with Slyrien Developers**
   - If conflicts arise, communicate our breath ID usage (7000-9999)
   - Suggest coordination to avoid overlap
   - Document compatibility on both mod pages

### Breath ID Conflict Detection

**Create a diagnostic command** to detect conflicts:

```java
// /knymultiplayer diagnose_breathids
public class DiagnoseBreathIDsCommand {
    public static void execute(CommandSourceStack source) {
        source.sendSuccess(Component.literal("Scanning breath ID usage..."), false);

        // Test each range
        testBreathRange(source, 2000, 2100, "Slyrien Main");
        testBreathRange(source, 3000, 3100, "Slyrien Impact");
        testBreathRange(source, 7000, 8000, "KnY Multiplayer Enhanced");
        testBreathRange(source, 8000, 9000, "KnY Multiplayer Custom");

        source.sendSuccess(Component.literal("✅ No conflicts detected!"), false);
    }
}
```

### Update Response Plan

If Slyrien adds breath IDs that conflict with ours (7000+):

1. **Immediate**: Add warning to mod description about incompatibility
2. **Short-term**: Create compatibility patch that remaps our IDs if conflict detected
3. **Long-term**: Coordinate with Slyrien to establish permanent ranges

**Example Compatibility Patch**:
```java
// Auto-remap if Slyrien uses 7000+ (unlikely but possible)
if (SlyrienDetector.isInstalled() && SlyrienDetector.uses7000Range()) {
    LOGGER.warn("Slyrien conflict detected! Remapping to 10000+ range");
    BREATH_ID_OFFSET = 3000; // Shift our IDs to 10000+
}
```

### Documentation

Maintain public documentation of our breath ID usage:
- On mod page (CurseForge/Modrinth)
- In GitHub README
- In in-game tooltip/book

Example tooltip:
```
"This mod uses breath IDs 7000-9999
Compatible with Slyrien (2000-3999)
Will not conflict with base kimetsunoyaiba (100-1999)"
```

---

## Future Enhancements

Once this system is in place, we can easily add:

1. **More Enhanced Forms**
   - Enhanced Thunder 1st Form (faster, more damage)
   - Enhanced Flame 9th Form (larger AoE)
   - Enhanced Water 11th Form (better flow)

2. **Completely New Breathing Styles**
   - Custom breathing style with unique mechanics
   - Special hybrid styles
   - Demon art enhancements

3. **Advanced Features**
   - Form combos (chain forms for bonuses)
   - Contextual form variations (underwater, airborne, etc.)
   - Custom animations via player-animation-lib integration
   - Blood demon art mixin system

4. **Quality of Life**
   - Form preview system
   - Training mode for practicing forms
   - Form progression/unlock system

---

## Conclusion

This integration plan provides a comprehensive roadmap for implementing a **scalable, Slyrien-compatible Mixin-based breathing form system** in the Kimetsunoyaiba-Multiplayer mod.

### Key Achievements

By following this plan, we will:

1. **Add MANY custom breathing forms** - Not just Mist, but enhanced versions of all breathing styles (Water, Thunder, Flame, Wind, Stone, Mist, Beast, Insect, Love, Snake)
2. **Create completely new breathing styles** - Up to 10 custom breathing styles (8000-8999 range)
3. **Maintain full Slyrien compatibility** - Use breath ID ranges 7000-9999, completely isolated from Slyrien's 2000-3999 range
4. **Seamlessly integrate with existing API** - Developers use the API, mixins work transparently
5. **Never modify the base kimetsunoyaiba mod** - All done via mixins and procedures
6. **Enable easy future expansion** - Add new forms by just creating procedures and registering dispatchers
7. **Maintain backward compatibility** - Existing API calls still work normally

### Technical Advantages

- **Scalable dispatcher pattern**: Easy to add dozens of forms without complexity
- **Clean separation of concerns**: Enhanced forms (7000s), custom styles (8000s), reserved (9000s)
- **Automatic conflict prevention**: API validates breath ID ranges and prevents Slyrien conflicts
- **Comprehensive monitoring**: Tools and strategies to detect and prevent future conflicts
- **Future-proof architecture**: Room for growth without breaking changes

### Compatibility Guarantees

✅ **Compatible with Slyrien** (current and future versions)
✅ **Compatible with base kimetsunoyaiba** (no modifications)
✅ **Compatible with existing API** (backward compatible)
✅ **Compatible with future expansion** (reserved ranges 9000-9999)

### Development Path

The phased approach ensures:
1. **Week 1**: Infrastructure setup (mixins, API extensions) - Foundation for everything
2. **Week 2**: Implementation of multiple breathing forms - Scalable from day one
3. **Week 3**: Comprehensive testing with Slyrien - Ensuring compatibility
4. **Week 4**: Documentation and examples - Enabling community contribution

### Long-Term Vision

This system enables the community to:
- Create enhanced versions of ANY existing breathing style
- Design completely new breathing styles with unique mechanics
- Contribute forms through pull requests
- Use both our mod and Slyrien simultaneously for maximum content

**Total estimated implementation time**: ~25 hours across 4 weeks

This is not just about adding a few forms - this is about creating a **comprehensive breathing form framework** that can scale to hundreds of custom forms while maintaining perfect compatibility with the Minecraft modding ecosystem.
