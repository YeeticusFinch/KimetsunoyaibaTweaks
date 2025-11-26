# Mist Breathing Replacement Guide

## Overview

This guide explains how to replace/modify Mist Breathing forms for **both players and mobs** using mixins that intercept the base kimetsunoyaiba mod's procedures. This approach allows us to:

1. Replace specific Mist breathing forms (e.g., only forms 1, 3, and 7) while keeping others vanilla
2. Apply replacements to both players using Mist breathing AND mobs like Muichiro
3. Seamlessly integrate with our existing breathing style and form classes
4. Maintain compatibility with Slyrien addon (we use even thousands: 10000, 12000, 14000...)

## Breath ID Allocation Agreement

### Even Thousands Strategy

Based on coordination with the Slyrien developer:
- **Slyrien uses ODD thousands**: 1000s, 3000s, 5000s, 7000s, 9000s, 11000s, etc.
- **We use EVEN thousands**: 10000s, 12000s, 14000s, 16000s, 18000s, etc.

### Our Allocation Table

| Range | Purpose | Conflict Risk |
|-------|---------|---------------|
| 0-9999 | Reserved (kimetsunoyaiba base + Slyrien odd thousands) | N/A |
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
| 34000+ | Reserved for future even thousands | ✅ Safe |

**Mist Breathing Specific**:
- Original Mist IDs: 401-407 (base kimetsunoyaiba)
- Our Mist IDs: **20000-20999** (even thousand)
- Example: Our enhanced Mist 7th form = **20007**

---

## Base Kimetsunoyaiba Analysis

### Mist Breathing in Base Mod

#### Player Mist Breathing

**File**: `net.mcreator.kimetsunoyaiba.procedures.PlayerBreathMistProcedure`

```java
public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
    if (entity.getPersistentData().getDouble("breathes") == 401.0) {
        BreathesKasumi1Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("breathes") == 402.0) {
        BreathesKasumiProcedure.execute(world, x, y, z, entity);  // Form 2
    }
    else if (entity.getPersistentData().getDouble("breathes") == 403.0) {
        BreathesKasumi3Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("breathes") == 404.0) {
        BrethesKasumi4Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("breathes") == 405.0) {
        BreathesKasumi5Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("breathes") == 406.0) {
        BreathesKasumi6Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("breathes") == 407.0) {
        BreathesKasumi7particleProcedure.execute(world, x, y, z, entity);
    }
    else {
        SwingKasumi1Procedure.execute(world, x, y, z, entity);  // Basic swing
    }
}
```

**Breath ID Mapping**:
- 401 = Mist 1st Form
- 402 = Mist 2nd Form
- 403 = Mist 3rd Form
- 404 = Mist 4th Form
- 405 = Mist 5th Form
- 406 = Mist 6th Form
- 407 = Mist 7th Form
- else = Basic swing (no form)

#### Muichiro/Muichirou Mob

**File**: `net.mcreator.kimetsunoyaiba.procedures.AIMuichiroProcedure`

The Muichiro mob uses a "mode" variable instead of "breathes":

```java
public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
    // ... AI logic ...

    if (entity.getPersistentData().getDouble("mode") == 1.0) {
        BreathesKasumi1Procedure.execute(world, x, y, z, entity);
    }
    else if (entity.getPersistentData().getDouble("mode") == 2.0) {
        BreathesKasumiProcedure.execute(world, x, y, z, entity);
    }
    // ... forms 3-7 ...
    else if (entity.getPersistentData().getDouble("mode") == 7.0) {
        BreathesKasumi7particleProcedure.execute(world, x, y, z, entity);
    }

    // ... AI logic to choose random mode 1-7 ...
}
```

**Mode Mapping**:
- mode = 1-7 corresponds to Mist forms 1-7
- Muichiro is the ONLY mob that uses Mist breathing
- demon_slayer and dice_steak_senior do NOT use Mist breathing

---

## Implementation Plan

### Architecture

```
Player right-clicks Mist sword
    ↓
PlayerBreathMistProcedure.execute() ← [PlayerBreathMistProcedureMixin intercepts]
    ↓
Check: Is breathId 20001-20007? (our custom)
    ↓ (yes)
Execute our custom form (e.g., EnhancedMist7thForm from our API)
    ↓ (no)
Fall through to original form (e.g., BreathesKasumi1Procedure)


Muichiro mob in combat
    ↓
AIMuichiroProcedure.execute() ← [AIMuichiroProcedureMixin intercepts]
    ↓
Check: Should we replace this form?
    ↓ (yes)
Temporarily set entity.breathes = 20001-20007
Execute our custom form
Restore original state
    ↓ (no)
Fall through to original form
```

### Key Principles

1. **Selective Replacement**: Only replace forms we have custom versions for
2. **Dual Interception**: Intercept both PlayerBreathMistProcedure AND AIMuichiroProcedure
3. **API Integration**: Use our existing BreathingStyle and Form classes
4. **Fallback**: Original forms execute if we don't have a replacement

---

## Phase 1: Create Custom Mist Forms Using Existing API

### Define Enhanced Mist Forms

Use our existing breathing form class structure:

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/breathingtechnique/EnhancedMistForms.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyle;

public class EnhancedMistForms {

    public static BreathingStyle create() {
        BreathingStyle mist = new BreathingStyle("enhanced_mist");

        // Only define forms we want to replace
        // If a form isn't defined here, vanilla will be used

        // Enhanced 1st Form: Low Clouds, Distant Haze
        mist.addForm(1, "Low Clouds, Distant Haze", (world, player, level) -> {
            // Your custom implementation using existing form logic
            // Can reuse code from our existing breathing forms
        });

        // Enhanced 3rd Form: Scattering Mist Splash
        mist.addForm(3, "Scattering Mist Splash", (world, player, level) -> {
            // Custom implementation
        });

        // Enhanced 7th Form: Obscuring Clouds (our main focus)
        mist.addForm(7, "Obscuring Clouds", (world, player, level) -> {
            // This is our improved version with:
            // - Better particle effects
            // - Improved dash mechanics
            // - Enhanced visual feedback
            // - Multiple attack phases
        });

        // Forms 2, 4, 5, 6 NOT defined = vanilla versions will be used

        return mist;
    }
}
```

**Integration with Existing Code**:

This uses our EXISTING BreathingStyle class structure, so it seamlessly integrates with:
- Our form execution system
- Our particle helpers
- Our animation helpers
- Our damage calculation
- Our existing API patterns

---

## Phase 2: Create Mixins for Interception

### Mixin 1: Player Mist Breathing

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/PlayerBreathMistProcedureMixin.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.EnhancedMistDispatcher;

/**
 * Intercepts player Mist breathing to replace specific forms with our enhanced versions.
 *
 * Strategy:
 * - Check if breathId is in our custom range (20000-20999)
 * - If yes, route to our EnhancedMistDispatcher
 * - If no, let original execute (vanilla forms or other mods' forms)
 *
 * This allows selective replacement - we only replace forms we've defined.
 */
@Mixin(value = PlayerBreathMistProcedure.class, remap = false)
public class PlayerBreathMistProcedureMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void onPlayerUseMistBreathing(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity,
        CallbackInfo ci
    ) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // Check if this is one of our custom Mist forms (20000-20999)
        if (breathId >= 20000.0 && breathId < 21000.0) {
            // Route to our enhanced dispatcher
            EnhancedMistDispatcher.execute(world, x, y, z, entity);
            ci.cancel(); // Prevent original execution
        }

        // Otherwise, let original PlayerBreathMistProcedure handle it
        // This includes vanilla forms (401-407) and any other mod's forms
    }
}
```

### Mixin 2: Muichiro Mob AI

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/AIMuichiroProcedureMixin.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.mcreator.kimetsunoyaiba.procedures.*;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import com.lerdorf.kimetsunoyaibamultiplayer.procedures.EnhancedMistDispatcher;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;

/**
 * Intercepts Muichiro mob's Mist breathing to use our enhanced forms.
 *
 * Strategy:
 * - Detect when Muichiro is about to use a Mist form (mode 1-7)
 * - Check if we have an enhanced version of that form
 * - If yes, temporarily set breathId to 20001-20007 and execute our form
 * - If no, let original execute (vanilla form)
 *
 * This is trickier because Muichiro uses "mode" not "breathes",
 * so we need to translate mode -> breathId temporarily.
 */
@Mixin(value = AIMuichiroProcedure.class, remap = false)
public class AIMuichiroProcedureMixin {

    @Inject(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/mcreator/kimetsunoyaiba/procedures/BreathesKasumi1Procedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            remap = false
        ),
        cancellable = true
    )
    private static void onMuichiroUseForm1(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity,
        CallbackInfo ci
    ) {
        if (entity == null) return;

        // Check if we have an enhanced version of Form 1
        if (EnhancedMistForms.create().hasForm(1)) {
            // Temporarily set breathId to our custom range
            double originalBreath = entity.getPersistentData().getDouble("breathes");
            entity.getPersistentData().putDouble("breathes", 20001.0); // Our Mist Form 1

            // Execute our enhanced form
            EnhancedMistDispatcher.execute(world, x, y, z, entity);

            // Restore original breathId (important for AI state)
            entity.getPersistentData().putDouble("breathes", originalBreath);

            ci.cancel(); // Prevent original form execution
        }
        // Otherwise, let original BreathesKasumi1Procedure execute
    }

    // Repeat for forms 2-7 with similar inject points

    @Inject(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/mcreator/kimetsunoyaiba/procedures/BreathesKasumi7particleProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            remap = false
        ),
        cancellable = true
    )
    private static void onMuichiroUseForm7(
        LevelAccessor world,
        double x, double y, double z,
        Entity entity,
        CallbackInfo ci
    ) {
        if (entity == null) return;

        // Check if we have an enhanced version of Form 7
        if (EnhancedMistForms.create().hasForm(7)) {
            double originalBreath = entity.getPersistentData().getDouble("breathes");
            entity.getPersistentData().putDouble("breathes", 20007.0); // Our Mist Form 7

            EnhancedMistDispatcher.execute(world, x, y, z, entity);

            entity.getPersistentData().putDouble("breathes", originalBreath);
            ci.cancel();
        }
    }

    // Add similar methods for forms 2, 3, 4, 5, 6 as needed
}
```

---

## Phase 3: Create Enhanced Mist Dispatcher

This dispatcher routes our custom breath IDs (20001-20007) to our enhanced forms.

**File**: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/procedures/EnhancedMistDispatcher.java`

```java
package com.lerdorf.kimetsunoyaibamultiplayer.procedures;

import net.minecraft.world.level.*;
import net.minecraft.world.entity.*;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyle;

/**
 * Dispatcher for our enhanced Mist breathing forms.
 * Routes breath IDs 20000-20999 to our custom implementations.
 */
public class EnhancedMistDispatcher {

    private static final BreathingStyle ENHANCED_MIST = EnhancedMistForms.create();

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        double breathId = entity.getPersistentData().getDouble("breathes");

        // Extract form number from breath ID
        // 20001 -> form 1, 20007 -> form 7
        int formNumber = (int)(breathId - 20000.0);

        // Execute the form using our existing API
        if (ENHANCED_MIST.hasForm(formNumber)) {
            ENHANCED_MIST.executeForm(formNumber, world, entity, 0);
        } else {
            // Safety fallback - shouldn't happen if mixins are correct
            System.err.println("EnhancedMistDispatcher: No form defined for " + formNumber);
        }
    }
}
```

---

## Phase 4: Update Mixin Configuration

**File**: `src/main/resources/kimetsunoyaibamultiplayer.mixins.json`

Add our new mixins:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.lerdorf.kimetsunoyaibamultiplayer.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "kimetsunoyaibamultiplayer.refmap.json",
  "mixins": [
    "PlayerBreathMistProcedureMixin",
    "AIMuichiroProcedureMixin"
  ],
  "client": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

---

## Phase 5: Create Mist Sword with Enhanced Forms

Use our API to create a Mist sword that uses the enhanced forms:

**File**: Example item registration

```java
public static final RegistryObject<Item> ENHANCED_MIST_SWORD =
    KnYAPI.createSword("enhanced_mist_nichirin_sword")
        .breathingStyle("enhanced_mist", EnhancedMistForms.create())
        .styleRange(20000)  // Our even thousand for Mist
        .defaultParticle(ModParticles.MIST_PARTICLE.get())
        .category(SwordRegistry.SwordCategory.NICHIRIN_ENHANCED)
        .durability(2500)
        .build(ITEMS);
```

When a player uses this sword:
1. They select form via scroll wheel (stored in NBT as "select" = 0-6)
2. Right-click activates form
3. `entity.breathes` is set to `20000 + select + 1` (e.g., 20007 for form 7)
4. PlayerBreathMistProcedureMixin intercepts
5. Routes to EnhancedMistDispatcher
6. Executes our custom form from EnhancedMistForms

---

## Testing Plan

### Test 1: Player Enhanced Forms

```
1. Give player enhanced_mist_nichirin_sword
2. Scroll to Form 7
3. Right-click to activate
4. Verify:
   - Our enhanced particle effects appear
   - Our enhanced dash mechanics work
   - Damage is calculated correctly
   - Form completes without errors
```

### Test 2: Player Vanilla Forms

```
1. Give player enhanced_mist_nichirin_sword
2. Scroll to Form 2 (which we didn't enhance)
3. Right-click to activate
4. Verify:
   - Vanilla BreathesKasumiProcedure executes
   - No errors occur
   - Vanilla behavior is preserved
```

### Test 3: Muichiro Mob Enhanced Forms

```
1. Spawn Muichiro entity
2. Attack it to trigger AI
3. Wait for it to use Form 7
4. Verify:
   - Our enhanced Form 7 executes (not vanilla)
   - Mob's AI continues correctly
   - No state corruption
```

### Test 4: Muichiro Mob Vanilla Forms

```
1. Spawn Muichiro entity
2. Wait for it to use Form 2 (which we didn't enhance)
3. Verify:
   - Vanilla form executes
   - Mob's AI works normally
```

### Test 5: Compatibility with Slyrien

```
1. Install both our mod and Slyrien
2. Test that:
   - Slyrien's forms (2000s, 3000s) work
   - Our forms (20000s) work
   - No breath ID conflicts
   - No crashes
```

---

## Advanced: Adding More Breathing Styles

Once Mist is working, the same pattern applies to other breathing styles:

### Thunder Breathing (12000-12999)

```java
// 1. Create EnhancedThunderForms.java using our API
// 2. Create PlayerBreathThunderProcedureMixin
// 3. Create AI mixins for thunder-using mobs (if any)
// 4. Create EnhancedThunderDispatcher
// 5. Register enhanced_thunder_sword with styleRange(12000)
```

### Water Breathing (10000-10999)

```java
// Same pattern, use styleRange(10000)
```

### Flame Breathing (14000-14999)

```java
// Same pattern, use styleRange(14000)
```

---

## Fallback Strategy

What if a player has BOTH vanilla Mist sword and our enhanced Mist sword?

**Answer**: They work independently!

- Vanilla Mist sword uses breath IDs 401-407
  - Routes to original PlayerBreathMistProcedure
  - No interception by our mixins (401 < 20000)

- Enhanced Mist sword uses breath IDs 20001-20007
  - Routes through our mixins
  - Executes our enhanced forms

Both can coexist in a player's inventory without conflicts.

---

## Common Pitfalls

### Pitfall 1: Forgetting to Restore Mob State

```java
// WRONG - corrupts mob AI
entity.getPersistentData().putDouble("breathes", 20007.0);
EnhancedMistDispatcher.execute(world, x, y, z, entity);
// Missing restore! Mob state is now corrupted.

// CORRECT
double original = entity.getPersistentData().getDouble("breathes");
entity.getPersistentData().putDouble("breathes", 20007.0);
EnhancedMistDispatcher.execute(world, x, y, z, entity);
entity.getPersistentData().putDouble("breathes", original); // Restore!
```

### Pitfall 2: Wrong Breath ID Range

```java
// WRONG - conflicts with Slyrien (they use odd thousands)
.styleRange(21000)  // 21000 is ODD thousand!

// CORRECT - we use even thousands
.styleRange(20000)  // 20000 is EVEN thousand
```

### Pitfall 3: Not Checking Form Existence

```java
// WRONG - crashes if form not defined
EnhancedMistDispatcher.execute(world, x, y, z, entity);

// CORRECT - check first
if (EnhancedMistForms.create().hasForm(formNumber)) {
    EnhancedMistDispatcher.execute(world, x, y, z, entity);
    ci.cancel();
}
```

---

## Summary

This system allows us to:

✅ **Replace specific Mist breathing forms** (e.g., only 1, 3, 7) while keeping others vanilla
✅ **Apply to both players and mobs** (Muichiro uses our enhanced forms too)
✅ **Use our existing API** (BreathingStyle, Form classes)
✅ **Maintain Slyrien compatibility** (we use even thousands, they use odd)
✅ **Selective replacement** (only forms we define are replaced)
✅ **Coexist with vanilla** (vanilla Mist swords still work)

**Implementation Time**: ~8-10 hours for complete Mist breathing replacement

**Next Steps**:
1. Implement EnhancedMistForms.java with our custom Form 7
2. Create both mixins (player and Muichiro)
3. Test thoroughly
4. Expand to other breathing styles using the same pattern
