# KnY Multiplayer - API Usage Guide

**Version:** 1.0.0
**Minecraft:** 1.20.1
**Forge:** 47.4.0+

This guide explains how to use the Kimetsu no Yaiba Multiplayer mod as a library to create your own custom breathing styles and nichirin swords.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Adding as a Dependency](#adding-as-a-dependency)
3. [Creating a Breathing Style](#creating-a-breathing-style)
4. [Creating a Nichirin Sword](#creating-a-nichirin-sword)
5. [Complete Example](#complete-example)
6. [Advanced Topics](#advanced-topics)
7. [API Reference](#api-reference)

---

## Getting Started

### Prerequisites

- Forge 1.20.1 mod development environment
- KnY Multiplayer mod as a dependency
- Basic understanding of Minecraft modding

### What You Can Create

With this API, you can:
- **Custom Breathing Styles**: Create new breathing techniques with multiple forms
- **Nichirin Swords**: Create swords that use your breathing styles
- **Custom Particles**: Define unique particle effects for sword swings
- **Special Abilities**: Use the built-in animation, movement, and damage systems

---

## Adding as a Dependency

### Step 1: Add to `build.gradle`

```gradle
repositories {
    maven {
        url "https://your-repository-url/maven"
    }
}

dependencies {
    implementation fg.deobf("com.lerdorf:kimetsunoyaibamultiplayer:1.0.0")
}
```

### Step 2: Add to `mods.toml`

```toml
[[dependencies.yourmodid]]
    modId="kimetsunoyaibamultiplayer"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

---

## Creating a Breathing Style

### Step 1: Create Your Forms

Create a class to hold your breathing forms (e.g., `MyBreathingForms.java`):

```java
package com.yourmod.breathing;

import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public class MyBreathingForms {

    /**
     * Create your custom breathing technique with all forms.
     */
    public static BreathingTechnique createMyBreathing() {
        return KnYAPI.createTechnique("My Breathing", Arrays.asList(
            firstForm(),
            secondForm(),
            thirdForm()
            // Add more forms...
        ));
    }

    /**
     * First Form: Example Attack
     */
    private static BreathingForm firstForm() {
        return KnYAPI.createForm(
            "First Form: Example Attack",
            "A powerful forward slash",
            5, // 5 second cooldown
            (player, level) -> {
                // Play animation
                KnYAPI.playAnimation(player, "sword_to_left", 10);

                // Deal damage to nearby enemies
                if (!level.isClientSide) {
                    var hitbox = player.getBoundingBox().inflate(3.0);
                    var targets = level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        hitbox,
                        e -> e != player && e.isAlive()
                    );

                    for (var target : targets) {
                        float damage = KnYAPI.calculateScaledDamage(player, 8.0f);
                        target.hurt(level.damageSources().playerAttack(player), damage);
                    }

                    // Spawn particles
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                            ParticleTypes.SNOWFLAKE,
                            player.getX(), player.getY() + 1, player.getZ(),
                            30, 0.5, 0.5, 0.5, 0.1
                        );
                    }
                }
            }
        );
    }

    /**
     * Second Form: Example Movement
     */
    private static BreathingForm secondForm() {
        return KnYAPI.createForm(
            "Second Form: Example Dash",
            "A quick dash forward",
            8,
            (player, level) -> {
                // Play animation
                KnYAPI.playAnimation(player, "speed_attack_sword");

                // Move player forward
                var lookAngle = player.getLookAngle();
                MovementHelper.setVelocity(player,
                    lookAngle.x * 2.0,
                    0.5,
                    lookAngle.z * 2.0
                );

                // Schedule damage during dash
                KnYAPI.scheduleRepeating(player, () -> {
                    // Deal damage each tick during dash
                    var hitbox = player.getBoundingBox().inflate(2.0);
                    var targets = level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        hitbox,
                        e -> e != player && e.isAlive()
                    );

                    for (var target : targets) {
                        float damage = KnYAPI.calculateScaledDamage(player, 3.0f);
                        target.hurt(level.damageSources().playerAttack(player), damage);
                    }
                }, 1, 20); // Every tick for 20 ticks (1 second)
            }
        );
    }

    /**
     * Third Form: Example Multi-Phase
     */
    private static BreathingForm thirdForm() {
        return KnYAPI.createForm(
            "Third Form: Example Combo",
            "A multi-hit combo attack",
            12,
            (player, level) -> {
                // Phase 1: Initial animation
                KnYAPI.playAnimation(player, "sword_overhead", 10);

                // Phase 2: After 10 ticks, start combo
                KnYAPI.scheduleOnce(player, () -> {
                    final int[] tickCounter = {0};

                    KnYAPI.scheduleRepeating(player, () -> {
                        int currentTick = tickCounter[0]++;

                        // Play attack animation every 10 ticks
                        if (currentTick % 10 == 0) {
                            KnYAPI.playAnimationOnLayer(
                                player, "sword_to_left", 10, 2.0f, 4000
                            );

                            // Deal damage
                            var hitbox = player.getBoundingBox().inflate(3.0);
                            var targets = level.getEntitiesOfClass(
                                net.minecraft.world.entity.LivingEntity.class,
                                hitbox,
                                e -> e != player && e.isAlive()
                            );

                            for (var target : targets) {
                                float damage = KnYAPI.calculateScaledDamage(player, 5.0f);
                                target.hurt(level.damageSources().playerAttack(player), damage);
                            }
                        }
                    }, 1, 40); // Run for 40 ticks (4 hits total)
                }, 10);
            }
        );
    }
}
```

### Step 2: Choose a Style Range

Each breathing style needs a unique numeric range (must be a multiple of 100):

```java
// Existing ranges (DO NOT USE):
// 100  - Water Breathing (kimetsunoyaiba mod)
// 200  - Beast Breathing
// 300  - Thunder Breathing
// 400  - Flame Breathing
// 500  - Wind Breathing
// 600  - Stone Breathing
// 700  - Mist Breathing
// 800  - Serpent Breathing
// 900  - Sound Breathing
// 1000 - Ice Breathing
// 1100 - Moon Breathing
// 1200 - Sun Breathing
// 1300 - Flower Breathing
// 1400 - Insect Breathing
// 1500 - Love Breathing
// 1600 - Frost Breathing
// 1700 - Cherry Blossom Breathing
// 1800 - Sakura Breathing

// Choose a range >= 1900:
int MY_STYLE_RANGE = 1900;
```

---

## Creating a Nichirin Sword

### Step 1: Set Up Your Items Registry

In your main mod class:

```java
package com.yourmod;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafxmod.FXMLLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("yourmodid")
public class YourMod {
    public static final String MODID = "yourmodid";

    public YourMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register your items
        ModItems.register(modEventBus);
    }
}
```

### Step 2: Create Your Sword Using the Builder

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

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "yourmodid");

    // Create your sword using the builder API
    public static final RegistryObject<Item> MY_SWORD =
        KnYAPI.createSword("nichirinsword_mysword")
            .breathingStyle("my_breathing", MyBreathingForms.createMyBreathing())
            .styleRange(1900)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .durability(2000)
            .build(ITEMS);

    // Example: Special sword (like Komorebi or Shimizu)
    public static final RegistryObject<Item> MY_SPECIAL_SWORD =
        KnYAPI.createSword("nichirinsword_special")
            .breathingStyle("my_breathing", MyBreathingForms.createMyBreathing())
            .styleRange(1900)
            .defaultParticle(ParticleTypes.ENCHANTED_HIT)
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .durability(3000)
            .build(ITEMS);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
```

### Step 3: Add to Creative Tab (Optional)

```java
public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "yourmodid");

public static final RegistryObject<CreativeModeTab> MY_TAB = CREATIVE_TABS.register("my_tab",
    () -> CreativeModeTab.builder()
        .title(Component.translatable("My Mod"))
        .icon(() -> new ItemStack(MY_SWORD.get()))
        .displayItems((parameters, output) -> {
            output.accept(MY_SWORD.get());
            output.accept(MY_SPECIAL_SWORD.get());
        })
        .build());
```

---

## Complete Example

Here's a complete, minimal mod that adds a custom breathing style and sword:

### Project Structure
```
src/main/java/com/yourmod/
├── YourMod.java
├── items/
│   └── ModItems.java
└── breathing/
    └── MyBreathingForms.java

src/main/resources/
├── META-INF/
│   └── mods.toml
├── assets/yourmodid/
│   ├── lang/
│   │   └── en_us.json
│   ├── models/item/
│   │   └── nichirinsword_mysword.json
│   └── textures/item/
│       └── nichirinsword_mysword.png
```

### `YourMod.java`
```java
package com.yourmod;

import com.yourmod.items.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafxmod.FXMLLoader;

@Mod("yourmodid")
public class YourMod {
    public static final String MODID = "yourmodid";

    public YourMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
    }
}
```

### `ModItems.java`
*(See Step 2 above)*

### `MyBreathingForms.java`
*(See Step 1 of Creating a Breathing Style)*

### `en_us.json`
```json
{
  "item.yourmodid.nichirinsword_mysword": "My Breathing Sword"
}
```

### `nichirinsword_mysword.json`
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "yourmodid:item/nichirinsword_mysword"
  }
}
```

---

## Advanced Topics

### Custom Particles

You can use custom particles or vanilla particles:

```java
.defaultParticle(ParticleTypes.SNOWFLAKE)      // Vanilla
.defaultParticle(ParticleTypes.ENCHANTED_HIT)  // Vanilla
.defaultParticle(ParticleTypes.FLAME)          // Vanilla

// For custom particles from another mod:
.defaultParticle(MyModParticles.MY_CUSTOM_PARTICLE.get())
```

### Sword-Specific Particles

Override the style's default particle for a specific sword:

```java
KnYAPI.createSword("nichirinsword_variant")
    .breathingStyle("my_breathing", MyBreathingForms.createMyBreathing())
    .styleRange(1900)
    .defaultParticle(ParticleTypes.SNOWFLAKE)    // Style default
    .swordParticle(ParticleTypes.ENCHANTED_HIT)  // This sword only
    .build(ITEMS);
```

### Animation System

Available animations (from `player-animation-lib`):

```java
// Basic attacks (use 10 tick limit for responsiveness)
"sword_to_left"      // Left slash
"sword_to_right"     // Right slash
"sword_overhead"     // Overhead slash
"sword_to_upper"     // Upward slash

// Special animations
"speed_attack_sword" // Fast thrust
"ragnaraku1"         // Multi-hit combo
"ragnaraku2"         // Spinning attack
"ragnaraku3"         // Ultimate attack
"sword_rotate"       // Spin attack
"kamusari3"          // Powerful strike
```

Animation layers:
- **Layer 3000**: Base animations (for main ability animations)
- **Layer 4000**: Overlay animations (for attacks during abilities)

```java
// Play on overlay layer at double speed
KnYAPI.playAnimationOnLayer(player, "sword_to_left", 10, 2.0f, 4000);
```

### Movement System

The `MovementHelper` class provides utilities:

```java
// Set velocity
MovementHelper.setVelocity(player, x, y, z);

// Add velocity
MovementHelper.addVelocity(player, dx, dy, dz);

// Look at position
MovementHelper.lookAt(player, targetPos);

// Set rotation
MovementHelper.setRotation(player, yaw, pitch);

// Enable block climbing (like step assist)
MovementHelper.setStepHeight(player, 1.8f);

// Disable gravity
player.setNoGravity(true);
// Remember to re-enable it!
player.setNoGravity(false);
```

### Particle System

The `ParticleHelper` class provides patterns:

```java
// Must spawn on server side
if (level instanceof ServerLevel serverLevel) {
    // Line of particles
    ParticleHelper.spawnParticleLine(
        serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 20
    );

    // Circle of particles
    ParticleHelper.spawnCircleParticles(
        serverLevel, center, radius, ParticleTypes.SNOWFLAKE, 30
    );

    // Forward thrust line
    ParticleHelper.spawnForwardThrust(
        serverLevel, startPos, direction, distance, ParticleTypes.SNOWFLAKE, 25
    );

    // Horizontal arc
    ParticleHelper.spawnHorizontalArc(
        serverLevel, center, yaw, pitch, baseRadius, radiusIncrement,
        arcDegrees, angleIncrement, verticalOffset, ParticleTypes.SNOWFLAKE, 50
    );
}
```

### Damage Calculation

Always use `DamageCalculator.calculateScaledDamage()` to respect player attributes:

```java
// Scales with player's attack damage (including strength effects)
float damage = KnYAPI.calculateScaledDamage(player, baseDamage);
target.hurt(level.damageSources().playerAttack(player), damage);

// Recommended base damage values:
// Light attack:  3-5
// Medium attack: 6-8
// Heavy attack:  10-12
// Ultimate:      15+
```

### Scheduling System

Use `AbilityScheduler` for timed effects:

```java
// One-time delayed action
KnYAPI.scheduleOnce(player, () -> {
    // Execute after delay
}, 20); // 20 ticks = 1 second

// Repeating action (RECOMMENDED for multi-phase abilities)
final int[] tickCounter = {0};
KnYAPI.scheduleRepeating(player, () -> {
    int currentTick = tickCounter[0]++;

    if (currentTick % 10 == 0) {
        // Every 10 ticks
    }

    if (currentTick >= 60 - 1) {
        // Cleanup on last tick
    }
}, 1, 60); // Every 1 tick for 60 ticks
```

---

## API Reference

### KnYAPI Class

Main entry point for the API.

#### Breathing Style Registration

```java
// Register a breathing style
KnYAPI.registerBreathingStyle(styleId, styleName, technique, styleRange, particle)

// Get registered style
KnYAPI.getBreathingStyle(styleId)

// Get all styles
KnYAPI.getAllBreathingStyles()
```

#### Sword Registration

```java
// Create sword builder
KnYAPI.createSword(swordId)

// Get registered sword
KnYAPI.getSword(swordId)
KnYAPI.getSword(item)

// Query swords
KnYAPI.getNichirinSwords()
KnYAPI.getSpecialSwords()
KnYAPI.getAllSwords()
```

#### Helper Methods

```java
// Create forms and techniques
KnYAPI.createForm(name, description, cooldown, effect)
KnYAPI.createTechnique(name, forms)

// Animations
KnYAPI.playAnimation(player, animationName)
KnYAPI.playAnimation(player, animationName, maxTicks)
KnYAPI.playAnimationOnLayer(player, animationName, maxTicks, speed, layer)

// Scheduling
KnYAPI.scheduleOnce(player, action, delayTicks)
KnYAPI.scheduleRepeating(player, action, intervalTicks, durationTicks)

// Damage
KnYAPI.calculateScaledDamage(player, baseDamage)

// Access to helper classes
KnYAPI.getMovementHelper()  // Returns MovementHelper.class
KnYAPI.getParticleHelper()  // Returns ParticleHelper.class
```

### NichirinSwordBuilder Class

Fluent builder for creating swords.

```java
NichirinSwordBuilder.create(swordId)
    .breathingStyle(styleId, technique)
    .styleRange(range)
    .defaultParticle(particle)       // For the style
    .swordParticle(particle)         // For this sword only
    .category(SwordCategory.NICHIRIN | SPECIAL)
    .durability(durability)
    .registerToCreativeTab(boolean)
    .build(itemRegistry)
```

### SwordRegistry.SwordCategory

```java
SwordRegistry.SwordCategory.NICHIRIN  // Standard breathing swords
SwordRegistry.SwordCategory.SPECIAL   // Special named swords
```

---

## Best Practices

1. **Always use unique style ranges**: Choose ranges >= 1900 to avoid conflicts
2. **Server-side particle spawning**: Always check `level instanceof ServerLevel`
3. **Scaled damage**: Use `KnYAPI.calculateScaledDamage()` for all damage
4. **Animation timing**: Use 10-tick limit for basic attack animations
5. **Cleanup**: Always reset player state (gravity, step height) after abilities
6. **Thread safety**: Registrations can be done during mod init (thread-safe)
7. **Cooldowns**: Set appropriate cooldowns (5-15 seconds for most forms)

---

## Troubleshooting

### Sword doesn't appear in game
- Check that `ITEMS.register(eventBus)` is called in your mod constructor
- Verify item model JSON exists at correct path
- Check logs for registration errors

### Particles don't appear
- Ensure particles are spawned on server side (`level instanceof ServerLevel`)
- Verify particle type is registered and available
- Check particle config file isn't blocking your particle

### Animations don't sync in multiplayer
- Animations are automatically synced by `AnimationHelper`
- Ensure you're using `KnYAPI.playAnimation()` methods
- Check that `player-animation-lib` is installed on both client and server

### Form cycling doesn't work
- Ensure your sword extends `BreathingSwordItem` (automatic via builder)
- Verify `BreathingTechnique` is properly created with all forms
- Check that player has the R key bound correctly

---

## Support

For additional help:
- Read the full breathing system docs: `docs/breathing-system.md`
- Check architecture docs: `docs/architecture.md`
- Review existing implementations: `FrostBreathingForms.java`, `IceBreathingForms.java`

---

## Version History

- **1.0.0**: Initial API release
  - BreathingStyleRegistry
  - SwordRegistry
  - NichirinSwordBuilder
  - KnYAPI main interface
