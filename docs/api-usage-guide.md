# KnY Multiplayer - API Usage Guide

**Version:** 1.6.x
**Minecraft:** 1.20.1
**Forge:** 47.4.0+

This guide explains how to use the Kimetsu no Yaiba Multiplayer mod as a library to create your own custom breathing styles, nichirin swords, and entities.

**Reference Implementation:** [KnY-Extra-Additions](https://github.com/YeeticusFinch/KnY-Extra-Additions)

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Adding as a Dependency](#adding-as-a-dependency)
3. [Creating Nichirin Swords](#creating-nichirin-swords)
4. [Creating Breathing Styles](#creating-breathing-styles)
5. [Registering Breathing Form Variations](#registering-breathing-form-variations)
6. [Creating Custom Entities](#creating-custom-entities)
7. [API Reference](#api-reference)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites

- Forge 1.20.1 mod development environment
- Java 17
- Basic understanding of Minecraft modding
- GeckoLib 4.4.4+ (for custom entities)

### What You Can Create

- **Custom Breathing Styles**: New breathing techniques with multiple forms
- **Nichirin Swords**: Swords that use your breathing styles
- **Custom Entities**: NPCs that wield your swords and use breathing forms
- **Custom Particles**: Unique particle effects for attacks
- **Special Abilities**: Movement, damage, animation systems

---

## Adding as a Dependency

### Step 1: Configure `build.gradle`

```gradle
repositories {
    // Modrinth Maven for Kimetsunoyaiba Tweaks
    maven {
        url = "https://api.modrinth.com/maven"
    }

    // CurseMaven for base KimetsunoYaiba mod
    maven {
        url = "https://cursemaven.com"
    }

    // GeckoLib (required for custom entities)
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
    }

    // KosmX maven for player animations
    maven {
        name = "KosmX's maven"
        url = 'https://maven.kosmx.dev/'
    }
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    // Base KimetsunoYaiba mod from CurseForge
    implementation fg.deobf("curse.maven:demonslayer-471263:7151280")

    // Kimetsunoyaiba Tweaks API from Modrinth (REQUIRED)
    implementation fg.deobf("maven.modrinth:kimetsunoyaiba-tweaks:${kny_tweaks_version}")

    // GeckoLib for custom entity models
    implementation fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.4.9")
}
```

### Step 2: Add version to `gradle.properties`

```properties
# Always use the latest version from Modrinth
kny_tweaks_version=1.6.29
```

> **Important:** Check [Modrinth](https://modrinth.com/mod/kimetsunoyaiba-tweaks) for the latest version (currently 1.6.29, soon 1.6.30).

### Step 3: Configure `mods.toml`

```toml
[[dependencies.yourmodid]]
    modId="kimetsunoyaibamultiplayer"
    mandatory=true
    versionRange="[1.5.0,)"
    ordering="AFTER"
    side="BOTH"
```

The `ordering="AFTER"` is critical - ensures KnY Multiplayer loads first.

---

## MCreator Integration

MCreator can use the KimetsunoYaiba Tweaks API with some additional setup. This requires using MCreator's custom code features.

### Step 1: Add Dependencies to MCreator

1. Open your MCreator workspace
2. Go to **Workspace** → **Workspace Settings** → **External APIs**
3. Click **Add dependency from mvnrepository**
4. Alternatively, manually edit `build.gradle` in your workspace folder:

```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
    maven { url = "https://cursemaven.com" }
}

dependencies {
    implementation fg.deobf("curse.maven:demonslayer-471263:7151280")
    implementation fg.deobf("maven.modrinth:kimetsunoyaiba-tweaks:1.6.2999")
}
```

5. Run **Build & Run** → **Regenerate code and build** to download dependencies

### Step 2: Configure mods.toml

Edit `src/main/resources/META-INF/mods.toml` and add the dependency:

```toml
[[dependencies.yourmodid]]
    modId="kimetsunoyaibamultiplayer"
    mandatory=true
    versionRange="[1.5.0,)"
    ordering="AFTER"
    side="BOTH"
```

### Step 3: Create Custom Code Elements

MCreator requires custom code elements to use the API:

#### Option A: Custom Procedure (Recommended)

1. Create a new **Procedure**
2. Click the **<>** button to switch to code view
3. Write your breathing form logic:

```java
// In a custom procedure
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.*;

public class YourProcedure {
    public static void execute(Entity entity) {
        if (entity instanceof Player player) {
            // Play animation
            KnYAPI.playAnimation(player, "sword_to_left");

            // Deal damage to nearby enemies
            // ... your logic here
        }
    }
}
```

#### Option B: Custom Element for Sword Registration

1. Create a **Custom Element** (requires MCreator 2023.1+)
2. Use the full sword builder pattern:

```java
package net.yourmod.item;

import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSwords {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "yourmodid");

    public static final RegistryObject<Item> MY_SWORD =
        KnYAPI.createSword("my_sword")
            .breathingStyle("my_breathing", YourBreathingForms.create())
            .styleRange(6000)  // Use even thousands
            .defaultParticle(ParticleTypes.FLAME)
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .durability(2000)
            .build(ITEMS);
}
```

### Limitations in MCreator

- **No visual editors**: Breathing forms must be coded manually
- **Custom code only**: Cannot use MCreator's block-based procedures for API calls
- **Manual registration**: Items need custom registration code
- **Debugging harder**: Less IDE support than IntelliJ/Eclipse

### Recommended Approach

For complex addons with multiple breathing styles and entities, consider:
1. Start in MCreator for basic mod structure
2. Export to IntelliJ IDEA or Eclipse for API integration
3. Use the full development environment for breathing forms

### Alternative: Hybrid Approach

1. Create basic items/blocks in MCreator's visual editor
2. Add custom code elements for KnY API integration
3. Use MCreator procedures for non-API logic (sounds, particles, etc.)

---

## Creating Nichirin Swords

### Basic Sword Registration

Create `ModItems.java`:

```java
package com.yourmod.items;

import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.yourmod.breathing.MyBreathingForms;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom nichirin swords.
 *
 * Style Ranges - USE EVEN THOUSANDS to avoid conflicts:
 * - 4000, 4100, 4200...: Your breathing styles
 * - 6000, 6100, 6200...: More breathing styles
 * - 8000+: Additional styles
 *
 * Slytharis uses ODD thousands (3000, 5000, 7000...)
 * We use EVEN thousands (4000, 6000, 8000...)
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "yourmodid");

    // Standard nichirin sword
    public static final RegistryObject<Item> NICHIRINSWORD_FROST =
        KnYAPI.createSword("nichirinsword_frost")
            .breathingStyle("frost_breathing", MyBreathingForms.createFrostBreathing())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .durability(2000)
            .build(ITEMS);

    // Special sword with unique abilities (e.g., 7th form access)
    public static final RegistryObject<Item> NICHIRINSWORD_KOMOREBI =
        KnYAPI.createSword("nichirinsword_komorebi")
            .breathingStyle("frost_breathing", MyBreathingForms.createFrostBreathingWithSeventh())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .swordParticle(ParticleTypes.END_ROD)  // Override style default
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .durability(2500)
            .build(ITEMS);

    // Hidden sword (not in creative tab - obtained through gameplay)
    public static final RegistryObject<Item> NICHIRINSWORD_GOLDEN =
        KnYAPI.createSword("nichirinsword_golden")
            .breathingStyle("frost_breathing", MyBreathingForms.createGoldenBreathing())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.END_ROD)
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .durability(3000)
            .registerToCreativeTab(false)  // Hidden from creative menu
            .build(ITEMS);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
```

### Style Range Guidelines

**Important:** Use EVEN thousands to avoid conflicts with Slytharis addon (which uses odd thousands).

| Range | Status |
|-------|--------|
| 0-2000 | Reserved (base mod - includes Sun Breathing at 2000) |
| 3000, 5000, 7000... | Reserved for Slytharis (odd thousands) |
| 4000 | KnY-Extra-Additions: Alcohol Breathing |
| 4100 | KnY-Extra-Additions: Forest Breathing |
| 4200 | KnY-Extra-Additions: Frost Breathing |
| 4300 | KnY-Extra-Additions: Ice Breathing |
| 6000+ | Used by KNY X (Star Breathing) |
| 8000+ | Available for your addon |
| 20000 | KnY Tweaks: Enhanced Mist Breathing |
| 22000 | KnY Tweaks: Enhanced Love Breathing |

**Pattern:** Use even thousands (4000, 6000, 8000...) with hundreds for sub-styles (4100, 4200, 6100, 6200...).

### Spawn Eggs

```java
public static final RegistryObject<Item> MY_ENTITY_SPAWN_EGG = ITEMS.register("my_entity_spawn_egg",
    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
        ModEntities.MY_ENTITY,
        0x5DBCD2, 0xFFFFFF,  // Primary and secondary colors
        new Item.Properties().stacksTo(64)));
```

---

## Creating Breathing Styles

### Basic Structure

Create a forms class for each breathing style:

```java
package com.yourmod.breathing;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.*;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FrostBreathingForms {

    /**
     * Create the complete breathing technique
     */
    public static BreathingTechnique createFrostBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        return new BreathingTechnique("Frost Breathing", forms);
    }

    /**
     * Version with 7th form for special swords
     */
    public static BreathingTechnique createFrostBreathingWithSeventh() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        forms.add(seventhForm());
        return new BreathingTechnique("Frost Breathing", forms);
    }

    // Form implementations below...
}
```

### Form IDs

**IMPORTANT**: Each breathing form requires a unique form ID.

**Form ID Ranges:**
- **0-1999**: Reserved (base KimetsunoYaiba mod forms)
  - Example: 102 = Water Second Form, 701 = Mist First Form
- **20000-21999**: Kimetsunoyaiba Tweaks internal forms
  - 20001-20007 = Enhanced Mist Breathing
  - 22001-22006 = Enhanced Love Breathing
- **30000+**: Available for your mod
  - Choose a unique range (e.g., 30000-30999 for your "Frost Breathing")

**Why Form IDs Matter:**
- Required for `GuardStateHelper.setGuardState()` (defensive power during forms)
- Used by the variation system to register alternate forms
- Only specified ONCE in the constructor - automatically passed to your effect

### Form Implementation Patterns

#### Pattern 1: Basic Attack Form

```java
private static BreathingForm firstForm() {
    return new BreathingForm(
        30001, // Form ID - choose unique range >= 30000 for your mod
        "First Form: Glacial Slash",
        "A powerful horizontal ice strike",
        5, // Cooldown in seconds
        (entity, level, formId) -> {
            // formId is automatically 30001 - use it for GuardStateHelper
            GuardStateHelper.setGuardState(entity, 8.0, formId);

            // Play animation
            AnimationHelper.playAnimation(entity, "sword_to_left");

            // Get targets in front
            Vec3 lookVec = entity.getLookAngle();
            Vec3 attackPos = entity.position().add(lookVec.scale(2.0));
            AABB hitBox = new AABB(attackPos, attackPos).inflate(3.0);

            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, hitBox,
                e -> e != entity && e.isAlive());

            // Deal damage
            for (LivingEntity target : targets) {
                float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
                Damager.hurt(entity, target, damage);
            }

            // Spawn particles (server-side only)
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    30, 0.5, 0.5, 0.5, 0.1);
            }

            // Play sound
            level.playSound(null, entity.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    );
}
```

#### Pattern 2: Movement + Timed Attacks

```java
private static BreathingForm secondForm() {
    return new BreathingForm(
        30002, // Form ID for Second Form
        "Second Form: Frost Dash",
        "Dash forward with multiple ice strikes",
        6,
        (entity, level, formId) -> {
            GuardStateHelper.setGuardState(entity, 10.0, formId);
            AnimationHelper.playAnimation(entity, "sword_to_left");

            // Apply speed buff
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));

            // Enable step climbing
            MovementHelper.setStepHeight(entity, 1.8F);

            final int totalTicks = 60;
            final int attackInterval = 10;

            // Schedule repeating attacks
            AbilityScheduler.scheduleRepeating(entity, () -> {
                Vec3 lookVec = entity.getLookAngle();

                // Move forward
                MovementHelper.setVelocity(entity,
                    lookVec.x * 1.2, entity.getDeltaMovement().y, lookVec.z * 1.2);

                // Damage nearby enemies
                AABB hitBox = entity.getBoundingBox().inflate(3.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    float damage = DamageCalculator.calculateScaledDamage(entity, 6.0F);
                    Damager.hurt(entity, target, damage);
                }
            }, attackInterval, totalTicks);

            // Reset step height after ability ends
            AbilityScheduler.scheduleOnce(entity, () -> {
                MovementHelper.setStepHeight(entity, 0.6F);
            }, totalTicks + 1);
        }
    );
}
```

#### Pattern 3: Multi-Phase Ability

```java
private static BreathingForm thirdForm() {
    return new BreathingForm(
        "Third Form: Aerial Strike",
        "Leap up, hover, then strike down",
        8,
        (entity, level) -> {
            // Phase 1: Initial leap
            MovementHelper.addVelocity(entity, 0, 1.2, 0);
            entity.setNoGravity(true);
            playEntityAnimation(entity, "sword_spin");

            final int hoverDuration = 30;

            // Phase 2: Hover and spawn particles
            AbilityScheduler.scheduleRepeating(entity, () -> {
                // Stop vertical movement
                MovementHelper.setVelocity(entity,
                    entity.getDeltaMovement().x, 0, entity.getDeltaMovement().z);

                // Spawn particles
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                        entity.getX(), entity.getY() + 2, entity.getZ(),
                        10, 2, 0.5, 2, 0.01);
                }
            }, 1, hoverDuration);

            // Phase 3: Strike down
            AbilityScheduler.scheduleOnce(entity, () -> {
                entity.setNoGravity(false);
                playEntityAnimation(entity, "speed_attack_sword");

                Vec3 lookVec = entity.getLookAngle();
                MovementHelper.setVelocity(entity, lookVec.scale(2.0));

                // Large AOE damage
                AABB hitBox = entity.getBoundingBox().inflate(5.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    float damage = DamageCalculator.calculateScaledDamage(entity, 12.0F);
                    Damager.hurt(entity, target, damage);
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }

                level.playSound(null, entity.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }, hoverDuration + 1);
        }
    );
}
```

#### Pattern 4: Effect Application

```java
private static BreathingForm fourthForm() {
    return new BreathingForm(
        "Fourth Form: Freezing Jab",
        "Quick jab that immobilizes",
        3,
        (entity, level) -> {
            playEntityAnimation(entity, "speed_attack_sword");

            Vec3 lookVec = entity.getLookAngle();
            Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
            Vec3 endPos = startPos.add(lookVec.scale(4.0));

            AABB hitBox = new AABB(startPos, endPos).inflate(1.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, hitBox,
                e -> e != entity && e.isAlive());

            for (LivingEntity target : targets) {
                float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
                Damager.hurt(entity, target, damage);

                // Apply debuffs
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 4));

                // Apply KnY Cold effect
                net.minecraft.world.effect.MobEffect coldEffect = KnYEffects.getColdEffect();
                if (coldEffect != null) {
                    target.addEffect(new MobEffectInstance(coldEffect, 160, 0));
                }

                // Visual freeze effect
                target.setTicksFrozen(target.getTicksFrozen() + 400);
            }

            // Spawn forward thrust particles
            if (level instanceof ServerLevel serverLevel) {
                ParticleHelper.spawnForwardThrust(serverLevel, startPos, lookVec, 4.0,
                    ParticleTypes.SNOWFLAKE, 25);
            }
        }
    );
}
```

### Entity-Agnostic Animation Helper

To make forms work with both players and custom entities:

```java
/**
 * Unified animation helper that works with both players and GeckoLib entities
 */
private static void playEntityAnimation(LivingEntity entity, String animationName) {
    if (entity instanceof Player player) {
        AnimationHelper.playAnimation(player, animationName);
    } else if (entity instanceof BreathingSlayerEntity slayer) {
        slayer.playGeckoAnimation(animationName, 20);
    }
}

/**
 * Animation with layer and speed control
 */
private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName,
        int maxTicks, float speed, int layer) {
    if (entity instanceof Player player) {
        AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
    } else if (entity instanceof BreathingSlayerEntity slayer) {
        slayer.playGeckoAnimation(animationName, maxTicks);
    }
}
```

### Available Animations

From `player-animation-lib`:

```java
// Basic attacks
"sword_to_left"      // Left horizontal slash
"sword_to_right"     // Right horizontal slash
"sword_overhead"     // Overhead downward slash
"sword_to_upper"     // Upward slash

// Special animations
"speed_attack_sword" // Fast thrust/jab
"sword_rotate"       // Spinning attack
"ragnaraku1"         // Multi-hit combo
"ragnaraku2"         // Spinning combo
"ragnaraku3"         // Ultimate attack
"kamusari3"          // Powerful strike
"kaishin3"           // Flash strike
"invisibility"       // Stealth pose
```

Animation layers:
- **Layer 3000**: Base animations (main ability)
- **Layer 4000**: Overlay animations (attacks during abilities)

---

## Registering Breathing Form Variations

**Variations** allow you to create alternate versions of existing breathing forms. They work with both:
- **Base mod forms** (0-1999 form IDs from KimetsunoYaiba mod)
- **Custom forms** (30000+ form IDs from your mod or other mods)

Variations are cycled with the **G key** (or mouse button 4) and persist per player across all swords that use that breathing style.

### How Variations Work

1. **Auto-Assigned Indices**: Variations are assigned indices (1, 2, 3...) in the order they're registered
2. **Index 0 = Base Form**: The original form is always index 0
3. **Player Selection Persists**: Each player's variation choice is saved to NBT
4. **Sword-Specific or Global**: Variations can apply to all swords or specific swords

### Registering Variations for Base Mod Forms

To add variations for breathing forms from the base KimetsunoYaiba mod:

```java
package com.yourmod.variations;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.world.level.Level;
import java.util.Collections;

public class WaterBreathingVariations {

    public static void register() {
        // Water Breathing Second Form has form ID 102 (breathes value 102.0)
        // Register two variations for it

        // Variation 1: Lateral Water Wheel
        BreathingFormVariation lateralWheel = new BreathingFormVariation(
            "Second Form: Lateral Water Wheel",
            "Spinning water wheel attack with wider horizontal range",
            3, // 3 second cooldown
            (entity, level, formId) -> {
                Log.debug("Executing Lateral Water Wheel (Form ID: " + formId + ")");

                // Your custom effect implementation
                // This completely replaces the base form's effect

                // Example: Set defensive power using auto-injected formId
                // GuardStateHelper.setGuardState(entity, 8.0, formId);

                // Add your particles, damage, movement, etc.
            },
            Collections.emptySet() // Empty = applies to all Water Breathing swords
        );

        // Register for form ID 102 (Water Second Form)
        // Auto-assigned variation index: 1
        VariationRegistry.register(102, lateralWheel);

        // Variation 2: Rolling Water Wheel
        BreathingFormVariation rollingWheel = new BreathingFormVariation(
            "Second Form: Rolling Water Wheel",
            "Water wheel that can curve around obstacles",
            4,
            (entity, level, formId) -> {
                Log.debug("Executing Rolling Water Wheel (Form ID: " + formId + ")");
                // Your custom effect implementation
            },
            Collections.emptySet()
        );

        // Auto-assigned variation index: 2
        VariationRegistry.register(102, rollingWheel);

        Log.info("Registered 2 variations for Water Breathing Second Form (ID 102)");
    }
}
```

**Base Mod Form IDs:**
- Water Breathing: 101-111 (First-Eleventh Form)
- Beast Breathing: 201-211
- Thunder Breathing: 301-306
- Flame Breathing: 401-409
- Wind Breathing: 501-509
- Stone Breathing: 601-605
- Mist Breathing: 701-707
- Serpent Breathing: 801-805
- Sound Breathing: 901-905
- Love Breathing: 1501-1505

*(Full list in `BaseModStyleMapping.java`)*

### Registering Variations for Custom Forms

To add variations for your own breathing forms:

```java
package com.yourmod.variations;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import java.util.Collections;

public class FrostBreathingVariations {

    public static void register() {
        // Frost Breathing First Form has form ID 30001 (from our custom breathing style)

        // Variation 1: Extended Glacial Slash
        BreathingFormVariation extendedSlash = new BreathingFormVariation(
            "First Form: Extended Glacial Slash",
            "Longer range ice slash with increased damage",
            6,
            (entity, level, formId) -> {
                // formId will be 30001 (auto-injected)
                // Your custom effect implementation
            },
            Collections.emptySet()
        );

        // Auto-assigned variation index: 1
        VariationRegistry.register(30001, extendedSlash);

        // Variation 2: Rapid Glacial Slash
        BreathingFormVariation rapidSlash = new BreathingFormVariation(
            "First Form: Rapid Glacial Slash",
            "Faster attack speed but normal range",
            3,
            (entity, level, formId) -> {
                // Your custom effect implementation
            },
            Collections.emptySet()
        );

        // Auto-assigned variation index: 2
        VariationRegistry.register(30001, rapidSlash);
    }
}
```

### Sword-Specific Variations

To create variations that only work with specific swords:

```java
import java.util.Set;

// Variation only for Frost Breathing swords
BreathingFormVariation frostOnlyVariation = new BreathingFormVariation(
    "Frost-Only Variation",
    "Only available when using Frost Breathing swords",
    5,
    (entity, level, formId) -> {
        // Effect implementation
    },
    Set.of("nichirinsword_frost") // Only applies to this sword ID
);

VariationRegistry.register(30001, frostOnlyVariation);

// Variation for multiple specific swords
BreathingFormVariation multiSwordVariation = new BreathingFormVariation(
    "Multi-Sword Variation",
    "Works with Frost and Ice swords",
    5,
    (entity, level, formId) -> {
        // Effect implementation
    },
    Set.of("nichirinsword_frost", "nichirinsword_ice")
);

VariationRegistry.register(30001, multiSwordVariation);
```

### Calling Your Registration Method

In your main mod class:

```java
@Mod.EventBusSubscriber(modid = YourMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class YourMod {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register your variations AFTER breathing styles are registered
            WaterBreathingVariations.register();
            FrostBreathingVariations.register();
        });
    }
}
```

### Usage In-Game

1. **Cycle Forms**: Press **R** to cycle through breathing forms (First, Second, Third...)
2. **Cycle Variations**: Press **G** (or mouse button 4) to cycle variations of current form
   - Display shows: `(1/3)` = Base form, `(2/3)` = First variation, `(3/3)` = Second variation
3. **Backward Cycling**: Hold **Shift** + **G** to cycle variations backward
4. **Persistence**: Variation selection is saved per player and persists across logout/login

### Key Points

- **Form ID must exist**: You can only register variations for forms that actually exist
- **Registration order matters**: Variations get indices 1, 2, 3... in registration order
- **No duplicate IDs needed**: The form ID is the only ID needed - variations auto-index
- **Complete replacement**: Variations completely replace the base form's effect
- **Auto-injected formId**: The `formId` parameter in the lambda is automatically injected
- **Multiplayer synced**: Variation changes are automatically synced to all clients
- **Config compatible**: Respects `VariationConfig.isBlacklisted()` for disabled variations

---

## Creating Custom Entities

### Step 1: Create Entity Class

```java
package com.yourmod.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.yourmod.breathing.FrostBreathingForms;
import com.yourmod.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class KomorebiEntity extends BreathingSlayerEntity {

    public KomorebiEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Setsu Komorebi"));
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FrostBreathingForms.createFrostBreathingWithSeventh();
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_KOMOREBI.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load KnY mod uniform boots
        net.minecraft.world.item.Item uniformBoots =
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        return new ItemStack[]{
            new ItemStack(ModItems.KOMOREBI_HAIR.get()),    // Head
            new ItemStack(ModItems.KOMOREBI_HAORI.get()),   // Chest
            new ItemStack(ModItems.KOMOREBI_HAKAMA.get()),  // Legs
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY  // Feet
        };
    }
}
```

### Step 2: Register Entity

Create `ModEntities.java`:

```java
package com.yourmod.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.yourmod.YourMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YourMod.MODID);

    public static final RegistryObject<EntityType<KomorebiEntity>> KOMOREBI =
        ENTITY_TYPES.register("komorebi",
            () -> EntityType.Builder.of(KomorebiEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)  // Player-sized hitbox
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("komorebi"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    // Register attributes
    @Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EntityAttributeRegistry {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            event.put(KOMOREBI.get(), BreathingSlayerEntity.createAttributes().build());
        }
    }
}
```

### Step 3: Create Renderer (Client)

```java
package com.yourmod.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.BreathingSlayerRenderer;
import com.yourmod.entities.KomorebiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class KomorebiRenderer extends BreathingSlayerRenderer<KomorebiEntity> {

    public KomorebiRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(KomorebiEntity entity) {
        return new ResourceLocation("yourmodid", "textures/entity/komorebi.png");
    }
}
```

### Step 4: Register Renderer

In your client setup event:

```java
@Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.KOMOREBI.get(), KomorebiRenderer::new);
    }
}
```

---

## API Reference

### KnYAPI Class

Main entry point for the API.

```java
// Create sword builder
KnYAPI.createSword(swordId)

// Create breathing forms
KnYAPI.createForm(name, description, cooldown, effect)
KnYAPI.createTechnique(name, forms)

// Animations
KnYAPI.playAnimation(player, animationName)
KnYAPI.playAnimation(player, animationName, maxTicks)
KnYAPI.playAnimationOnLayer(player, animationName, maxTicks, speed, layer)

// Scheduling
KnYAPI.scheduleOnce(player, action, delayTicks)
KnYAPI.scheduleRepeating(player, action, intervalTicks, durationTicks)

// Damage calculation
KnYAPI.calculateScaledDamage(player, baseDamage)
```

### NichirinSwordBuilder

Fluent builder for creating swords.

```java
NichirinSwordBuilder.create(swordId)
    .breathingStyle(styleId, technique)       // Required
    .styleRange(range)                        // Required, multiple of 100
    .defaultParticle(particle)                // Style default particle
    .swordParticle(particle)                  // Override for this sword
    .swingSound(soundEvent)                   // Custom hit sound
    .category(SwordCategory.NICHIRIN | SPECIAL)
    .durability(durability)                   // Default: 2000
    .registerToCreativeTab(boolean)           // Default: true
    .build(itemRegistry)
```

### Helper Classes

#### MovementHelper
```java
MovementHelper.setVelocity(entity, x, y, z)
MovementHelper.setVelocity(entity, vec3)
MovementHelper.addVelocity(entity, dx, dy, dz)
MovementHelper.setStepHeight(entity, height)
MovementHelper.stepUp(entity, x, y, z)
```

#### ParticleHelper
```java
ParticleHelper.spawnForwardThrust(level, start, direction, distance, particle, count)
ParticleHelper.spawnHorizontalArc(level, center, yaw, pitch, radius, increment, arc, step, offset, particle, count)
ParticleHelper.spawnVerticalArc(level, center, yaw, pitch, radius, increment, arc, step, offset, particle, count)
ParticleHelper.spawnCircleParticles(level, center, radius, particle, count)
```

#### DamageCalculator
```java
DamageCalculator.calculateScaledDamage(entity, baseDamage)
```

#### Damager

The `Damager` class provides safe damage application that prevents event recursion issues. There are two versions:

```java
// Original version - uses default invulnerability frames
Damager.hurt(LivingEntity source, LivingEntity target, float damage)

// New version - with invulnerability control
Damager.hurt(LivingEntity source, LivingEntity target, float damage, boolean resetInvulnerability)
```

**Parameters:**
- `source` - The entity dealing the damage (attacker)
- `target` - The entity receiving the damage
- `damage` - Amount of damage to deal
- `resetInvulnerability` - If true, resets invulnerability frames allowing rapid successive hits

**Reset Invulnerability:**

By default, when an entity takes damage, they get **10 ticks (~0.5 seconds) of invulnerability frames** where they cannot be damaged again. This prevents the same attack from hitting multiple times.

However, for **rapid multi-hit attacks** (like Love Breathing's whip flurry or Mist Breathing's continuous slashes), you may want every hit to deal damage. Set `resetInvulnerability = true` to override the invulnerability frames.

**Examples:**

```java
// Standard single-hit attack - use default invulnerability
Damager.hurt(player, target, 10.0f);

// Multi-hit combo - reset invulnerability for each hit
for (int i = 0; i < 5; i++) {
    AbilityScheduler.scheduleOnce(player, () -> {
        Damager.hurt(player, target, 3.0f, true); // Each hit damages
    }, i * 2); // Hit every 2 ticks
}

// Fast continuous slashes (hits every tick)
AbilityScheduler.scheduleRepeating(player, () -> {
    List<LivingEntity> targets = getNearbyTargets();
    for (LivingEntity target : targets) {
        Damager.hurt(player, target, 2.0f, true); // Bypass invulnerability
    }
}, 1, 20); // Every tick for 20 ticks
```

**When to Use Reset Invulnerability:**

- ✅ **Use `true`** for: Multi-hit combos, rapid slashes, continuous beam attacks, whip flurries
- ❌ **Use `false`** (or default) for: Single powerful strikes, area-of-effect explosions, projectile hits

**Important:** Overusing `resetInvulnerability = true` can make attacks feel unfair or cause excessive damage. Use it intentionally for forms designed as rapid multi-hit attacks.

#### AbilityScheduler
```java
AbilityScheduler.scheduleOnce(entity, task, delayTicks)
AbilityScheduler.scheduleRepeating(entity, task, intervalTicks, durationTicks)
```

#### KnYEffects
```java
KnYEffects.getColdEffect()  // Returns the Cold mob effect from base mod
```

---

## Custom Sword Slash Models

The KnY Multiplayer mod supports custom 3D sword slash effects that display when using breathing forms. You can create custom slash models for your breathing style.

### Available Slash Models

Built-in model keys:
- `generic` - Default slash effect
- `mist` - Mist breathing slash
- `love` - Love breathing slash
- `sound` - Sound breathing slash

### Creating a Custom Slash Model

#### Step 1: Create the Geometry File

Create a GeckoLib-compatible `.geo.json` file:

```
assets/yourmodid/geo/sword_slash_yourbreathing.geo.json
```

Use Blockbench to create the model. The slash should be a curved arc shape centered at origin.

#### Step 2: Create the Texture

Create a texture for your slash:

```
assets/yourmodid/textures/entity/sword_slash_yourbreathing.png
```

Tips:
- Use bright, saturated colors
- Add transparency for glow effects
- Size: 64x64 or 128x128 recommended

#### Step 3: Create the Model Class

```java
package com.yourmod.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModel;
import net.minecraft.resources.ResourceLocation;

public class YourBreathingSlashModel extends SwordSlashModel {

    public YourBreathingSlashModel() {
        super("yourbreathing");
    }

    @Override
    public ResourceLocation getModelResource() {
        return new ResourceLocation("yourmodid", "geo/sword_slash_yourbreathing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource() {
        return new ResourceLocation("yourmodid", "textures/entity/sword_slash_yourbreathing.png");
    }
}
```

#### Step 4: Register with the Slash Renderer

Register your custom model during client setup:

```java
@Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register custom slash model
        SwordSlashRenderer.registerModel("yourbreathing", () -> new YourBreathingSlashModel());
    }
}
```

### Using Slash Models in Forms

To render a slash effect during a breathing form:

```java
private static BreathingForm firstForm() {
    return new BreathingForm(
        "First Form: Your Attack",
        "Description",
        5,
        (entity, level) -> {
            // Play animation
            playEntityAnimation(entity, "sword_to_left");

            // Render slash model (client-side only)
            if (level.isClientSide && entity instanceof Player player) {
                Vec3 slashPos = entity.position().add(entity.getLookAngle().scale(1.5)).add(0, 1.2, 0);

                SwordSlashRenderer.render(
                    new PoseStack(),
                    Minecraft.getInstance().renderBuffers().bufferSource(),
                    slashPos,
                    entity.getYRot(),      // yaw
                    entity.getXRot(),      // pitch
                    0,                      // roll
                    1.5f,                   // scale
                    0.5f,                   // progress (0.0 to 1.0)
                    "yourbreathing",        // model key
                    0xF000F0               // packed light (full bright)
                );
            }

            // Deal damage, spawn particles, etc.
        }
    );
}
```

### Slash Model via Network Packet

For multiplayer synchronization, use the `RawSlashRenderPacket`:

```java
// Server-side: Send slash render to all clients
if (!level.isClientSide) {
    Vec3 pos = entity.position().add(entity.getLookAngle().scale(1.5)).add(0, 1.2, 0);

    ModNetworking.sendToAllClients(new RawSlashRenderPacket(
        pos.x, pos.y, pos.z,
        entity.getYRot(),
        entity.getXRot(),
        0,              // roll
        1.5f,           // scale
        20,             // duration ticks
        "yourbreathing" // model key
    ));
}
```

### Configuration

Users can configure slash models via `config/kimetsunoyaibamultiplayer/swordswing.toml`:

```toml
# Enable/disable sword swing models
useSwordSwingModel = true

# Global scale multiplier
modelScale = 1.0

# Rotation offsets
globalYawOffset = 0.0
globalPitchOffset = 0.0
globalRollOffset = 0.0
```

---

## Best Practices

### 1. Always Use Scaled Damage

```java
// Good - scales with player's attack damage attribute
float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
Damager.hurt(entity, target, damage);

// Bad - fixed damage ignores equipment/effects
target.hurt(level.damageSources().playerAttack(player), 8.0F);
```

### 2. Server-Side Particle Spawning

```java
// Always check for ServerLevel before spawning particles
if (level instanceof ServerLevel serverLevel) {
    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, count, dx, dy, dz, speed);
}
```

### 3. Clean Up Resources

```java
// Reset gravity after aerial abilities
entity.setNoGravity(false);

// Reset step height after movement abilities
MovementHelper.setStepHeight(entity, 0.6F);

// Remove tags when done
entity.removeTag("AbilityActive");
```

### 4. Entity-Agnostic Forms

Write forms that work with both players and entities:

```java
// Check entity type before player-specific operations
if (!(entity instanceof Player player)) {
    return;  // Skip for non-player entities
}

// Use capability only on players
player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
    data.setCancelAttackSwing(true);
});
```

### 5. Appropriate Cooldowns

| Form Type | Recommended Cooldown |
|-----------|---------------------|
| Basic attack | 2-5 seconds |
| Dash/movement | 5-8 seconds |
| Multi-phase | 8-12 seconds |
| Ultimate | 20-30 seconds |

### 6. Damage Guidelines

| Attack Type | Base Damage |
|-------------|-------------|
| Light attack | 3-5 |
| Medium attack | 6-8 |
| Heavy attack | 10-12 |
| Ultimate | 15+ |

### 7. Debug Logging

```java
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;

if (Config.logDebug) {
    Log.debug("Fifth Form: Jab {} hit {} for {} damage",
        jabNumber, target.getName().getString(), damage);
}
```

---

## Troubleshooting

### Sword doesn't appear in game

- Verify `ITEMS.register(eventBus)` is called in mod constructor
- Check item model JSON exists at `assets/yourmodid/models/item/swordname.json`
- Check logs for registration errors

### Particles don't appear

- Ensure spawning on server side (`level instanceof ServerLevel`)
- Verify particle count > 0
- Check particle config isn't blocking your particle

### Animations don't sync in multiplayer

- Use `AnimationHelper.playAnimation()` methods (auto-syncs)
- Ensure `player-animation-lib` is on both client and server
- Verify animation name is spelled correctly

### Entity doesn't spawn

- Check `EntityAttributeCreationEvent` handler is registered
- Verify entity type registration is correct
- Check spawn egg colors are valid hex values

### Form cycling doesn't work

- Verify `BreathingTechnique` is properly created with all forms
- Ensure sword is built with `.breathingStyle()`
- Check R key binding in controls

### Server crashes with client-only code

- Never import `net.minecraft.client.*` classes outside `client` package
- Use `DistExecutor` for client-specific code in shared classes
- Test on dedicated server early: `./gradlew runServer`

---

## Support

- **Reference Implementation**: [KnY-Extra-Additions](https://github.com/YeeticusFinch/KnY-Extra-Additions)
- **Architecture Docs**: `docs/architecture.md`
- **Breathing System**: `docs/breathing-system.md`
- **Bug Prevention**: `docs/bug-prevention.md`

---

## Version History

- **1.6.x**: Updated API with entity support
  - BreathingSlayerEntity base class
  - Entity-agnostic form patterns
  - Improved particle helpers
  - GeckoLib integration

- **1.0.0**: Initial API release
  - BreathingStyleRegistry
  - SwordRegistry
  - NichirinSwordBuilder
  - KnYAPI main interface
