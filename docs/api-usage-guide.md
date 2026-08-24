# KnY Tweaks - API Usage Guide

**Version:** 1.6.x
**Minecraft:** 1.20.1
**Forge:** 47.4.0+

This guide explains how to use the Kimetsu no Yaiba Multiplayer mod as a library to create your own custom breathing styles, nichirin swords, and entities.

**Reference Implementation:** [KnY-Extra-Additions](https://github.com/YeeticusFinch/KnY-Extra-Additions)

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Adding as a Dependency](#adding-as-a-dependency)
3. [Generated-Code and MCreator Adapters](#generated-code-and-mcreator-adapters)
4. [Creating Nichirin Swords](#creating-nichirin-swords)
5. [Sword Levels](#sword-levels)
6. [Style Metadata Registry](#style-metadata-registry)
7. [Sword Metadata Registry](#sword-metadata-registry)
8. [Color Change System](#color-change-system)
9. [Custom Progression Configuration](#custom-progression-configuration)
10. [Training Sword System](#training-sword-system)
11. [Creating Breathing Styles](#creating-breathing-styles)
12. [Registering Breathing Form Variations](#registering-breathing-form-variations)
13. [Creating Custom Entities](#creating-custom-entities)
14. [API Reference](#api-reference)
15. [Custom Sword Slash Models](#custom-sword-slash-models)
16. [Sword Sheaths and Display Offsets](#sword-sheaths-and-display-offsets)
17. [Best Practices](#best-practices)
18. [Troubleshooting](#troubleshooting)

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

## Generated-Code and MCreator Adapters

MCreator plugins and other generated-code tools should prefer the adapter methods on `KnYAPI`. These methods keep generated code short and avoid importing internal implementation classes.

### Using the KnY Tweaks MCreator Plugin

The companion MCreator plugin provides KnY-specific procedure blocks and an External API entry named `kny_tweaks`. It is intended for MCreator workspaces targeting **Forge 1.20.1**.

The plugin can found here:
https://mcreator.net/plugin/124570/kimetsunoyaiba-tweaks

It can also be found here:
https://github.com/YeeticusFinch/KnyTweaksMCreator/releases/

#### Install the Plugin

1. Build or zip the plugin folder so the ZIP root contains `plugin.json`, `apis/`, `procedures/`, `generators/`, and `lang/`.
2. Put the plugin ZIP into your MCreator user plugins folder: `.mcreator/plugins`.
3. Open MCreator and go to **Preferences** -> **Manage plugins**.
4. Make sure plugins are enabled, then restart MCreator if prompted.
5. Open or create a Forge 1.20.1 workspace.

When developing locally from this repository, the plugin project is expected beside this repo at `../KnyTweaksMCreator`.

#### Enable the KnY API in a Workspace

1. Open **Workspace Settings**.
2. Go to **External APIs**.
3. Enable **Kimetsunoyaiba Tweaks** / `kny_tweaks`.
4. Regenerate code and build the workspace.

The plugin API entry adds the compile dependencies for the base Demon Slayer mod, GeckoLib, and Kimetsunoyaiba Tweaks. It also adds Forge runtime dependency entries for `kimetsunoyaiba` and `kimetsunoyaibamultiplayer` so missing mods fail with a normal dependency error instead of a `ClassNotFoundException`.

#### Procedure Blocks

After the API is enabled, a **KnY Tweaks** procedure category is available. The blocks generate calls into `KnYAPI`, `Damager`, and the public helper classes.

Useful blocks include:

- Set breathing form cooldown.
- Damage entities in a radius using `Damager.hurt`.
- Dash an entity forward.
- Apply bleeding.
- Spawn a breathing slash.
- Register slash models, namespaces, animated textures, and random textures.
- Set guard state and reset guard state.
- Get the currently equipped Nichirin sword.
- Get the current breathing form ID from `breathes`.
- Check whether an entity is using a breathing style ID.
- Check whether an entity is a demon using `Damager.isDemon`.
- Register sword sheaths, sheathed display overrides, sword positions, and offsets.

The damage blocks pass base damage into `Damager.hurt`. Do not pre-scale damage in the procedure, because `Damager.hurt` already performs scaling internally unless the block/template explicitly asks it not to.

#### Breathing Form Workflow

For generated breathing styles, the intended workflow is:

1. Create a **KnY Breathing Form** mod element or generated form definition.
2. Set its form ID, name, description, and cooldown.
3. Use **On use** to call an MCreator procedure.
4. Build that procedure visually with KnY blocks such as dash, slash, guard, damage radius, particles, and sounds.
5. Register the form with `KnYAPI.registerProcedureBreathingForm()`.
6. Register the style with `KnYAPI.registerProcedureBreathingStyle()` during common setup.

The generated form callback should call the MCreator procedure, while the KnY API owns the breathing-style architecture:

```java
BreathingForm firstForm = KnYAPI.registerProcedureBreathingForm(
    4101,
    "First Form: Frozen Lake",
    "A fast frozen slash.",
    5,
    (entity, level, formId) -> FrostFirstFormProcedure.execute(entity)
);
```

#### Demonized Variants

Use the **is entity a demon** block or `Damager.isDemon(entity)` to branch inside form procedures:

```java
if (Damager.isDemon(entity)) {
    FrostDemonizedProcedure.execute(entity);
} else {
    FrostFirstFormProcedure.execute(entity);
}
```

This is the recommended pattern for demonized variants of breathing forms.

#### Custom Mod Elements

Procedure blocks are the first stable part of the plugin. Custom mod elements should generate code through the new adapter APIs:

- `KnYAPI.registerProcedureBreathingStyle(...)`
- `KnYAPI.registerProcedureBreathingForm(...)`
- `KnYAPI.registerProcedureVariation(...)`
- `KnYAPI.registerSword(...)`
- `KnYAPI.registerSwordMetadata(...)`
- `KnYAPI.registerSheath(...)`
- `KnYAPI.registerDemonSlayerEntityCombat(...)`
- `KnYAPI.applyDemonSlayerEntityCombat(...)`

Forge item and entity registration still belongs to the generated addon mod. The adapters hide KnY-specific registry details, but the generated mod still needs to register its own `Item`, `EntityType`, attributes, renderer, and event-bus hooks in the normal Forge/MCreator phases.

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

### Step 3: Use the Adapter APIs

Procedure blocks can generate direct calls to these adapters.

#### Breathing Forms

Use `registerProcedureBreathingForm()` when generated code needs to wrap an MCreator procedure callback:

```java
BreathingForm firstForm = KnYAPI.registerProcedureBreathingForm(
    4101,
    "First Form: Frozen Lake",
    "A fast frozen slash.",
    5,
    (entity, level, formId) -> FrostFirstFormProcedure.execute(entity)
);
```

#### Breathing Styles

Use `registerProcedureBreathingStyle()` during common setup/enqueueWork after forms are created. This registers both style metadata and the breathing style:

```java
BreathingStyleRegistry.RegisteredBreathingStyle frostStyle =
    KnYAPI.registerProcedureBreathingStyle(
        "frost_breathing",
        "Frost Breathing",
        4100,
        "minecraft:snowflake",
        "water_breathing",
        true,
        true,
        List.of(firstForm)
    );
```

#### Swords

Forge items still need the owning mod's `DeferredRegister<Item>`, but generated code no longer needs the builder chain:

```java
RegistryObject<Item> frostSword = KnYAPI.registerSword(
    ITEMS,
    "nichirinsword_frost",
    "frost_breathing",
    KnYAPI.createTechnique("Frost Breathing", List.of(firstForm)),
    4100,
    "minecraft:snowflake",
    "NICHIRIN",
    0,
    2000
);
```

If MCreator already generated a regular item and you only need metadata for entity loadouts, use:

```java
KnYAPI.registerSwordMetadata("yourmod:nichirinsword_frost", "frost_breathing", 0);
```

#### Variations

Use `registerProcedureVariation()` during common setup after the base form exists:

```java
KnYAPI.registerProcedureVariation(
    4101,
    "Frozen Lake: Demonized",
    "A demonized variant of the first form.",
    4,
    (entity, level, formId) -> FrostDemonizedProcedure.execute(entity),
    Set.of("nichirinsword_frost")
);
```

#### Sheaths and Display

Generated setup code can register sheath mappings and offsets without importing client registries:

```java
KnYAPI.registerPersistentSheath("yourmod:nichirinsword_frost", "yourmod:frost_sheath");
KnYAPI.registerSheathDisplayOverride("yourmod:nichirinsword_frost", "yourmod:nichirinsword_frost_sheathed");
KnYAPI.addSwordPositionOverride("yourmod:nichirinsword_frost", SwordDisplayPosition.BACK);
KnYAPI.registerSwordOffsets(
    "yourmod:nichirinsword_frost",
    new SwordDisplayConfig.SwordOffsets(0.0, 0.1, 0.0, 0.0, 15.0, 0.0)
);
```

#### Demon Slayer Entities

Entity types still belong to the addon. Register the KnY combat profile during common setup, then apply it once when the entity spawns:

```java
KnYAPI.registerDemonSlayerEntityCombat(
    "yourmod:frost_slayer",
    EntityPowerScale.GENERIC_SLAYER,
    "frost_breathing",
    "yourmod:nichirinsword_frost",
    true,
    false
);

KnYAPI.applyDemonSlayerEntityCombat(livingEntity);
```

`applyDemonSlayerEntityCombat()` writes the KnY combat NBT, equips the default sword when it can resolve one, and configures built-in `DemonSlayerEntity` / `BreathingSlayerEntity` instances when applicable.

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

    // Standard nichirin sword (level 0 - eligible for color change)
    public static final RegistryObject<Item> NICHIRINSWORD_FROST =
        KnYAPI.createSword("nichirinsword_frost")
            .breathingStyle("frost_breathing", MyBreathingForms.createFrostBreathing())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .swordLevel(0)  // Base sword - eligible for color change
            .durability(2000)
            .build(ITEMS);

    // Named character sword (level 1 - NOT eligible for color change)
    public static final RegistryObject<Item> NICHIRINSWORD_KOMOREBI =
        KnYAPI.createSword("nichirinsword_komorebi")
            .breathingStyle("frost_breathing", MyBreathingForms.createFrostBreathingWithSeventh())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .swordParticle(ParticleTypes.END_ROD)  // Override style default
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .swordLevel(1)  // Named character sword
            .durability(2500)
            .build(ITEMS);

    // Hashira-tier sword (level 2 - NOT eligible for color change)
    public static final RegistryObject<Item> NICHIRINSWORD_GOLDEN =
        KnYAPI.createSword("nichirinsword_golden")
            .breathingStyle("frost_breathing", MyBreathingForms.createGoldenBreathing())
            .styleRange(4100)
            .defaultParticle(ParticleTypes.END_ROD)
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .swordLevel(2)  // Hashira-tier sword
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

| Range               | Status                                               |
| ------------------- | ---------------------------------------------------- |
| 0-2000              | Reserved (base mod - includes Sun Breathing at 2000) |
| 3000, 5000, 7000... | Reserved for Slytharis (odd thousands)               |
| 4000                | KnY-Extra-Additions: Alcohol Breathing               |
| 4100                | KnY-Extra-Additions: Forest Breathing                |
| 4200                | KnY-Extra-Additions: Frost Breathing                 |
| 4300                | KnY-Extra-Additions: Ice Breathing                   |
| 6000+               | Used by KNY X (Star Breathing)                       |
| 8000+               | Available for your addon                             |
| 20000               | KnY Tweaks: Enhanced Mist Breathing                  |
| 22000               | KnY Tweaks: Enhanced Love Breathing                  |

**Pattern:** Use even thousands (4000, 6000, 8000...) with hundreds for sub-styles (4100, 4200, 6100, 6200...).

---

## Sword Levels

Sword levels categorize swords by their power tier and determine their eligibility for the color change system.

### Level Definitions

| Level | Name            | Description                                                        | Color Change Eligible |
| ----- | --------------- | ------------------------------------------------------------------ | --------------------- |
| **0** | Base            | Generic breathing swords (e.g., `nichirinsword_water`)             | Yes                   |
| **1** | Named Character | Swords tied to specific characters (e.g., `nichirinsword_tanjiro`) | No                    |
| **2** | Hashira         | Elite swords from Hashira (e.g., `nichirinsword_rengoku`)          | No                    |

### Setting Sword Level

The `swordLevel()` method is **required** when building swords:

```java
// Level 0 - Base sword (eligible for color change transformation)
KnYAPI.createSword("nichirinsword_ice")
    .breathingStyle("ice_breathing", IceBreathingForms.create())
    .styleRange(4300)
    .defaultParticle(ParticleTypes.SNOWFLAKE)
    .category(SwordRegistry.SwordCategory.NICHIRIN)
    .swordLevel(0)  // REQUIRED
    .build(ITEMS);

// Level 1 - Named character sword
KnYAPI.createSword("nichirinsword_yukihiro")
    .breathingStyle("ice_breathing", IceBreathingForms.createWithSeventh())
    .styleRange(4300)
    .defaultParticle(ParticleTypes.END_ROD)
    .category(SwordRegistry.SwordCategory.SPECIAL)
    .swordLevel(1)  // Named character
    .build(ITEMS);

// Level 2 - Hashira sword
KnYAPI.createSword("nichirinsword_ice_hashira")
    .breathingStyle("ice_breathing", IceBreathingForms.createHashiraVariant())
    .styleRange(4300)
    .defaultParticle(ParticleTypes.END_ROD)
    .category(SwordRegistry.SwordCategory.SPECIAL)
    .swordLevel(2)  // Hashira tier
    .build(ITEMS);
```

### Guidelines

- **Level 0**: Use for generic breathing swords that players can obtain through color change
- **Level 1**: Use for swords that have a named character associated (like Tanjiro's or Inosuke's)
- **Level 2**: Reserve for the most powerful swords (Hashira tier)

---

## Style Metadata Registry

The Style Metadata Registry tracks breathing style parent relationships and eligibility flags for features like color change.

### Registering Style Metadata

If you're creating a new breathing style, register its metadata for color change integration:

```java
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;

// In your mod's commonSetup
event.enqueueWork(() -> {
    // Root style (no parent)
    KnYAPI.registerStyleMetadata(
        "ice_breathing",     // styleId
        null,                // parentStyleId (null for root styles)
        true,                // colorChangeEligible
        true                 // oreSelectionEligible (for future feature)
    );

    // Derived style (has parent)
    KnYAPI.registerStyleMetadata(
        "glacier_breathing",
        "ice_breathing",     // derives from Ice Breathing
        true,
        true
    );
});
```

### Style Hierarchy

The base mod's breathing styles form a hierarchy:

```
sun_breathing (root)
├── water_breathing
│   ├── flower_breathing
│   │   └── insect_breathing
│   └── serpent_breathing
├── flame_breathing
│   └── love_breathing
├── wind_breathing
│   ├── mist_breathing
│   └── beast_breathing
├── thunder_breathing
│   └── sound_breathing
└── stone_breathing

black (root)
```

When creating derived styles, set the appropriate parent to maintain the hierarchy.

### Querying Styles

```java
// Get metadata for a style
StyleMetadataRegistry.StyleMetadata metadata = KnYAPI.getStyleMetadata("water_breathing");
if (metadata != null) {
    String parent = metadata.getParentStyleId(); // "sun_breathing"
    boolean isRoot = metadata.isRootStyle();     // false
}

// Get all color-change-eligible styles
List<StyleMetadataRegistry.StyleMetadata> eligibleStyles = KnYAPI.getColorChangeEligibleStyles();

// Get all ore-selection-eligible styles (for future feature)
List<StyleMetadataRegistry.StyleMetadata> oreStyles = KnYAPI.getOreSelectionEligibleStyles();
```

---

## Sword Metadata Registry

The Sword Metadata Registry tracks base mod swords (from KimetsunoYaiba) that aren't created with our API but still need to be integrated with features like color change.

### When to Use This

Use `SwordMetadataRegistry` when:

- Registering base mod swords for color change eligibility
- Tracking swords that aren't `BreathingSwordItem` instances
- Querying swords by style and level

For swords created with `KnYAPI.createSword()`, use `SwordRegistry` instead (automatic registration).

### Registering Base Mod Swords

```java
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;

// In your mod's commonSetup
event.enqueueWork(() -> {
    // Register a base mod sword (lazy resolution - item looked up when accessed)
    SwordMetadataRegistry.registerLazy(
        "kimetsunoyaiba:nichirinsword_water",  // Full registry ID
        "water_breathing",                      // Style ID
        0                                       // Sword level (0=base, 1=named, 2=hashira)
    );

    // Or register with direct item reference
    Item sword = ForgeRegistries.ITEMS.getValue(
        new ResourceLocation("kimetsunoyaiba", "nichirinsword_flame"));
    if (sword != null) {
        SwordMetadataRegistry.register(
            "kimetsunoyaiba:nichirinsword_flame",
            sword,
            "flame_breathing",
            0
        );
    }
});
```

### Querying Swords

```java
// Get metadata by ID
SwordMetadataRegistry.SwordMetadata meta = SwordMetadataRegistry.getMetadata("kimetsunoyaiba:nichirinsword_water");
if (meta != null) {
    String style = meta.getStyleId();      // "water_breathing"
    int level = meta.getSwordLevel();       // 0
    Item item = meta.getSwordItem();        // The actual Item instance
}

// Get metadata by Item
SwordMetadataRegistry.SwordMetadata meta2 = SwordMetadataRegistry.getMetadata(someItemStack.getItem());

// Get all swords for a style and level
List<SwordMetadataRegistry.SwordMetadata> waterBaseSwords =
    SwordMetadataRegistry.getSwordsByStyleAndLevel("water_breathing", 0);

// Get all color-change-eligible swords
List<SwordMetadataRegistry.SwordMetadata> eligibleSwords =
    SwordMetadataRegistry.getColorChangeEligibleSwords();
```

### Pre-Registered Base Mod Swords

The following base mod swords are automatically registered by `BaseModRegistration`:

**Level 0 (Base Swords - Color Change Eligible):**

- `nichirinsword_water`, `nichirinsword_flame`, `nichirinsword_wind`
- `nichirinsword_mist`, `nichirinsword_thunder`, `nichirinsword_stone`
- `nichirinsword_love`, `nichirinsword_serpent`, `nichirinsword_sound`
- `nichirinsword_insect`, `nichirinsword_flower`, `nichirinsword_black`

**Level 1 (Named Character Swords):**

- `nichirinsword_tanjiro`, `nichirinsword_inosuke`

**Level 2 (Hashira Swords):**

- `nichirinsword_rengoku`, `nichirinsword_uzui`, `nichirinsword_shinobu`
- `nichirinsword_iguro`, `nichirinsword_sanemi`, `nichirinsword_tokito`
- `nichirinsword_kanroji`, `nichirinsword_gyomei`

---

## Color Change System

The color change system transforms the generic `kimetsunoyaiba:nichirinsword` into a random breathing sword based on registered metadata.

### How It Works

1. When a player holds the generic nichirinsword for 30 ticks
2. The system selects a random **color-change-eligible** style
3. From that style, it selects a random **level-0** sword
4. The sword transforms and the player receives a message

### Enabling Color Change

Color change must be enabled in the config (`config/kimetsunoyaibamultiplayer/custom_progression.toml`):

```toml
# Replace the base mod's color changing procedure with our custom one
replaceColorChangingProcedure = true
```

### Making Your Swords Eligible

For your swords to appear in the color change pool:

1. **Register style metadata** with `colorChangeEligible = true`
2. **Set sword level to 0** in the sword builder

```java
// 1. Register style metadata (in commonSetup)
KnYAPI.registerStyleMetadata("ice_breathing", "water_breathing", true, true);

// 2. Create level-0 sword
KnYAPI.createSword("nichirinsword_ice")
    .breathingStyle("ice_breathing", IceBreathingForms.create())
    .styleRange(4300)
    .defaultParticle(ParticleTypes.SNOWFLAKE)
    .category(SwordRegistry.SwordCategory.NICHIRIN)
    .swordLevel(0)  // Level 0 = eligible for color change
    .build(ITEMS);
```

### Excluding Swords from Color Change

To prevent a sword from appearing in color change:

- **Option 1**: Set `swordLevel(1)` or `swordLevel(2)` (for named/hashira swords)
- **Option 2**: Set `colorChangeEligible = false` in style metadata (excludes entire style)

```java
// This sword won't appear in color change (level 1)
KnYAPI.createSword("nichirinsword_special")
    .breathingStyle("ice_breathing", IceBreathingForms.createSpecial())
    .styleRange(4300)
    .defaultParticle(ParticleTypes.END_ROD)
    .category(SwordRegistry.SwordCategory.SPECIAL)
    .swordLevel(1)  // Named character sword - not in color change
    .build(ITEMS);
```

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
            GuardStateHelper.setAttackState(entity, 8.0);

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
                Damager.hurt(entity, target, 8.0F);
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

            AbilityScheduler.scheduleOnce(entity,
                () -> GuardStateHelper.clearGuardState(entity), 10);
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
                GuardStateHelper.setAttackState(entity, 6.0);

                // Move forward
                MovementHelper.setVelocity(entity,
                    lookVec.x * 1.2, entity.getDeltaMovement().y, lookVec.z * 1.2);

                // Damage nearby enemies
                AABB hitBox = entity.getBoundingBox().inflate(3.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    Damager.hurt(entity, target, 6.0F, true);
                }
            }, attackInterval, totalTicks);

            // Reset step height after ability ends
            AbilityScheduler.scheduleOnce(entity, () -> {
                MovementHelper.setStepHeight(entity, 0.6F);
                GuardStateHelper.clearGuardState(entity);
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
                    Damager.hurt(entity, target, 12.0F);
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
                Damager.hurt(entity, target, 8.0F);

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

_(Full list in `BaseModStyleMapping.java`)_

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

### Creating Demon Entities

For addon demons, extend `AbstractDemonEntity` when you want the shared demon behavior: `oni` NBT, sunlight burn handling, non-demon targeting, GeckoLib animation hooks, and optional Blood Demon Art execution.

```java
package com.yourmod.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class FrostDemonEntity extends AbstractDemonEntity {
    public FrostDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 50.0D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D);
    }
}
```

Register attributes the same way as other mobs:

```java
event.put(ModEntities.FROST_DEMON.get(), FrostDemonEntity.createAttributes().build());
```

Then register the entity with `DemonRegistry` through `KnYAPI` during common setup. This makes `Damager.isDemon()` recognize it and registers raid/categorization metadata.

```java
@SubscribeEvent
public static void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        KnYAPI.registerDemon(
            "yourmodid:frost_demon",
            EntityPowerScale.MEDIUM_DEMON,
            false,          // sunlightImmune
            "frost_art"     // optional Blood Demon Art ID, or null
        );
    });
}
```

If the entity should not use `AbstractDemonEntity`, make sure it is still discoverable as a demon by registering it with `KnYAPI.registerDemon()`, adding it to the `kimetsunoyaiba:demon` entity type tag, or setting the `oni` persistent NBT where appropriate.

---

## API Reference

### KnYAPI Class

Main entry point for the API.

```java
// Create sword builder
KnYAPI.createSword(swordId)

// Create breathing forms
KnYAPI.createForm(formId, name, description, cooldown, effect)
KnYAPI.createTechnique(name, forms)

// Animations
KnYAPI.playAnimation(player, animationName)
KnYAPI.playAnimation(player, animationName, maxTicks)
KnYAPI.playAnimationOnLayer(player, animationName, maxTicks, speed, layer)

// Scheduling
KnYAPI.scheduleOnce(player, action, delayTicks)
KnYAPI.scheduleRepeating(player, action, intervalTicks, durationTicks)

// Damage calculation for non-Damager uses
KnYAPI.calculateScaledDamage(player, baseDamage)

// Demon registration
KnYAPI.registerDemon(entityId, powerScale)
KnYAPI.registerDemon(entityId, powerScale, sunlightImmune)
KnYAPI.registerDemon(entityId, powerScale, sunlightImmune, bloodDemonArtId)

// Style Metadata Registration
KnYAPI.registerStyleMetadata(styleId, parentStyleId, colorChangeEligible, oreSelectionEligible)
KnYAPI.getStyleMetadata(styleId)
KnYAPI.getColorChangeEligibleStyles()
KnYAPI.getOreSelectionEligibleStyles()

// Sword display offsets
KnYAPI.addSwordPositionOverride(itemId, position)
KnYAPI.registerSwordOffsets(itemId, offsets)
KnYAPI.registerSwordOffsets(itemId, slot, offsets)

// Sword slash visuals
KnYAPI.registerSlashModel(swordItemPath, modelKey)
KnYAPI.registerSlashModelNamespace(modelKey, namespace)
KnYAPI.registerAnimatedSlashTexture(modelKey, frameCount, ticksPerFrame)
KnYAPI.registerRandomSlashTexture(modelKey, textureCount)
KnYAPI.setSlashTextureRandomSelection(modelKey, useRandom)
```

### NichirinSwordBuilder

Fluent builder for creating swords.

```java
NichirinSwordBuilder.create(swordId)
    .breathingStyle(styleId, technique)       // Required
    .styleRange(range)                        // Required, multiple of 100
    .swordLevel(level)                        // Required: 0=base, 1=named, 2=hashira
    .defaultParticle(particle)                // Style default particle
    .swordParticle(particle)                  // Override for this sword
    .swingSound(soundEvent)                   // Custom hit sound
    .category(SwordCategory.NICHIRIN | SPECIAL)
    .durability(durability)                   // Default: 2000
    .registerToCreativeTab(boolean)           // Default: true
    .build(itemRegistry)
```

**Sword Levels:**
| Level | Name | Color Change Eligible |
|-------|------|----------------------|
| 0 | Base | Yes |
| 1 | Named Character | No |
| 2 | Hashira | No |

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

Use `DamageCalculator.calculateScaledDamage()` only when you need a scaled number for non-damage state such as guard strength, preview text, or custom logic that does not call `Damager.hurt()`.

#### Damager

The `Damager` class provides safe damage application for abilities and entity attacks. It handles KnY damage scaling internally, applies Midas bonuses, records damage history, respects custom friendly-fire checks for demon slayers, and uses the correct damage source.

Pass the base damage amount to `Damager.hurt()`. Do not pre-scale with `DamageCalculator.calculateScaledDamage()` before calling it, or the hit will be scaled twice.

```java
// Standard hit: scales damage and uses normal invulnerability frames
Damager.hurt(LivingEntity source, LivingEntity target, float damage)

// Rapid hit: reset target invulnerability frames before damage
Damager.hurt(LivingEntity source, LivingEntity target, float damage, boolean resetInvulnerability)

// Advanced: reset invulnerability and optionally skip scaling
Damager.hurt(LivingEntity source, LivingEntity target, float damage,
             boolean resetInvulnerability, boolean dontScale)

// Advanced: force scaling even when resetInvulnerability would otherwise skip it
Damager.hurt(LivingEntity source, LivingEntity target, float damage,
             boolean resetInvulnerability, boolean dontScale, boolean forceScale)
```

**Parameters:**

- `source` - The entity dealing the damage (attacker)
- `target` - The entity receiving the damage
- `damage` - Base damage amount to deal
- `resetInvulnerability` - If true, sets `target.invulnerableTime = 0` before applying damage
- `dontScale` - If true, uses `damage` as-is instead of applying `Damager.calculateScaledDamage()`
- `forceScale` - If true, overrides `dontScale` and forces normal scaling

**Reset Invulnerability:**

By default, when an entity takes damage, they get **10 ticks (~0.5 seconds) of invulnerability frames** where they cannot be damaged again. This prevents the same attack from hitting multiple times.

For **rapid multi-hit attacks** like whip flurries or continuous slashes, set `resetInvulnerability = true` so each scheduled hit can land. If the target was already in invulnerability frames, `Damager.hurt(..., true)` automatically treats that hit as unscaled to avoid repeatedly applying scaling during the same iframe window. Use the six-argument overload with `forceScale = true` only when the ability intentionally needs scaled damage on every rapid hit.

**Scaling Options:**

- Default: `Damager.hurt(source, target, 8.0F)` scales the base damage once.
- Fixed damage: `Damager.hurt(source, target, 8.0F, false, true)` skips scaling.
- Rapid fixed damage: `Damager.hurt(source, target, 2.0F, true, true)` bypasses iframes and skips scaling.
- Rapid forced scaled damage: `Damager.hurt(source, target, 2.0F, true, false, true)` bypasses iframes and scales every hit.

**Examples:**

```java
// Standard single-hit attack - scaled once by Damager
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

#### Demon and Target Checks

```java
Damager.isDemon(entity)
Damager.isDemonSlayer(entity)
Damager.isHostile(target, attacker)
Damager.isNeutral(entity)
Damager.isAngry(entity)
Damager.isAngry(entity, target)
```

Use `Damager.isDemon(LivingEntity)` in breathing form abilities when you need a demonized variant:

```java
float damage = Damager.isDemon(entity) ? 10.0F : 7.0F;
String animation = Damager.isDemon(entity) ? "demonized_slash" : "sword_to_left";

playEntityAnimation(entity, animation);
Damager.hurt(entity, target, damage);
```

`isDemon()` checks demon players, base KnY demon tags, twelve kizuki tags, the `oni` NBT fallback, and addon demons registered through `DemonRegistry`.

#### GuardStateHelper

```java
GuardStateHelper.setGuardState(entity, defensivePower, formId)
GuardStateHelper.setGuardState(entity, defensivePower, formId, scale)
GuardStateHelper.setAttackState(entity, offensiveDamage)
GuardStateHelper.setAttackState(entity, offensiveDamage, scale)
GuardStateHelper.enableContinuousDefense(entity)
GuardStateHelper.clearAttackFlag(entity)
GuardStateHelper.clearDamageValue(entity)
GuardStateHelper.clearGuardState(entity)
```

Call `setGuardState()` when an ability starts so the base KnY clash system can use the entity's `Damage` and `guard` NBT. Call `setAttackState()` during active hit frames when the entity is attacking. Always call `clearGuardState()` when the form ends or is interrupted.

The `scale` parameter controls whether the guard/attack `Damage` NBT is pre-scaled. This is separate from `Damager.hurt()`: guard values are not applied through `Damager`, so scaling them here is appropriate. Use `scale = false` when you need a fixed guard strength.

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

The KnY Multiplayer mod supports custom 3D sword slash effects that display when using breathing forms. Addons register a sword item path to a slash model key, then provide the matching model and texture assets.

### Available Slash Models

Built-in model keys:

- `generic` - Default slash effect
- `mist` - Mist breathing slash
- `love` - Love breathing slash
- `sound` - Sound breathing slash
- `water` - Animated water slash
- `flame` - Animated flame slash
- `flower` - Animated flower slash
- `wind` - Random wind slash variants
- `beast` - Random beast slash variants
- `moon` - Moon slash

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

Animated and random-variant textures use numbered files:

```
assets/yourmodid/textures/entity/sword_slash_yourbreathing0.png
assets/yourmodid/textures/entity/sword_slash_yourbreathing1.png
assets/yourmodid/textures/entity/sword_slash_yourbreathing2.png
```

Tips:

- Use bright, saturated colors
- Add transparency for glow effects
- Size: 64x64 or 128x128 recommended

#### Step 3: Register the Slash Mapping

Register your model key during client setup. The API call is safe from common setup too, but only executes on the client.

```java
@Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            KnYAPI.registerSlashModelNamespace("yourbreathing", YourMod.MODID);
            KnYAPI.registerSlashModel("nichirinsword_yourbreathing", "yourbreathing");

            // Optional: sequential animation through numbered textures.
            KnYAPI.registerAnimatedSlashTexture("yourbreathing", 3, 2);

            // Optional alternative: choose one numbered texture at spawn time.
            // KnYAPI.registerRandomSlashTexture("yourbreathing", 3);
        });
    }
}
```

`registerSlashModel()` takes the item path, not the full item ID. For `yourmodid:nichirinsword_yourbreathing`, pass `nichirinsword_yourbreathing`.

### Using Slash Models in Forms

For multiplayer synchronization, trigger slashes from the server with one of the raw render packets. The packet attaches the slash to the source entity and applies rotation/position offsets on the client.

```java
private static BreathingForm firstForm() {
    return new BreathingForm(
        30001,
        "First Form: Your Attack",
        "Description",
        5,
        (entity, level, formId) -> {
            // Play animation
            playEntityAnimation(entity, "sword_to_left");

            if (!level.isClientSide) {
                ModNetworking.sendToAllClients(new RawSlashRenderPacket(
                    "yourbreathing",       // model key
                    0.0F,                  // slash angle
                    180.0F,                // arc range
                    150,                   // duration milliseconds
                    0.0F, 0.0F, 0.0F,      // yaw, pitch, roll offsets
                    1.0F,                  // radius scale
                    1.0F,                  // size scale
                    0.0F,                  // angle offset
                    false,                 // reverse/flip horizontal
                    entity.getUUID(),
                    "sword_to_left",       // animation name used for bone tracking
                    new Vec3(0.0D, 1.2D, 0.0D) // local position offset
                ));
            }

            // Deal damage, spawn particles, etc.
        }
    );
}
```

Use the orientation-specific packets when the slash should be constrained to a horizontal or vertical arc:

```java
new RawHorizontalSlashRenderPacket(modelKey, verticalOffset, arcRange, duration,
    yawOffset, pitchOffset, rollOffset, radiusScale, sizeScale, angleOffset,
    reverse, entity.getUUID(), animationName, posOffset);

new RawVerticalSlashRenderPacket(modelKey, verticalOffset, arcRange, duration,
    yawOffset, pitchOffset, rollOffset, radiusScale, sizeScale, angleOffset,
    reverse, entity.getUUID(), animationName, posOffset);
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

## Sword Sheaths and Display Offsets

Custom swords can register sheath items, sheathed display overrides, hip/back placement, and per-position transform offsets.

### Registering Sheaths

Register sheaths during client setup:

```java
@Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SwordSheathRegistry.registerPersistentSheath(
                ModItems.NICHIRINSWORD_YOURBREATHING.get(),
                ModItems.SWORD_SHEATH_YOURBREATHING.get()
            );

            // Use this for sheaths that disappear while the sword is drawn.
            // SwordSheathRegistry.registerTemporarySheath(sword, sheath);

            // Optional: render a different item model while the sword is sheathed.
            SwordSheathRegistry.registerSheathDisplayOverride(
                ModItems.NICHIRINSWORD_YOURBREATHING.get(),
                ModItems.NICHIRINSWORD_YOURBREATHING_SHEATHED.get()
            );
        });
    }
}
```

- `registerPersistentSheath()` keeps the sheath visible when the sword is drawn.
- `registerTemporarySheath()` hides the sheath while the sword is drawn.
- `setDefaultSheath()` sets a fallback sheath for swords without a custom entry.

### Sheath Model Offsets

Use `SheathModelRenderer.registerSheathOffsets()` for sheath-only transforms:

```java
SheathModelRenderer.registerSheathOffsets(
    ModItems.SWORD_SHEATH_YOURBREATHING.get(),
    new SheathModelRenderer.SheathOffsets(
        0.0D, 0.03D, -0.02D,  // translate X/Y/Z
        0.0D, 0.0D, 8.0D,     // rotate X/Y/Z in degrees
        1.05D                 // uniform scale
    )
);
```

`registerSheathScale()` still exists for legacy code, but `registerSheathOffsets()` is preferred because it controls translation, rotation, and scale.

### Sword Display Position and Offsets

Use the `KnYAPI` sword display helpers when a sword needs a custom hip/back location:

```java
KnYAPI.addSwordPositionOverride(
    "yourmodid:nichirinsword_yourbreathing",
    SwordDisplayConfig.SwordDisplayPosition.HIP
);

KnYAPI.registerSwordOffsets(
    "yourmodid:nichirinsword_yourbreathing",
    SwordDisplayConfig.SwordDisplaySlot.HIP_LEFT,
    new SwordDisplayConfig.SwordOffsets(
        0.02D, -0.04D, 0.0D,  // translate X/Y/Z
        0.0D, 0.0D, -5.0D     // rotate X/Y/Z
    )
);
```

Use `SwordDisplaySlot.HIP_LEFT`, `HIP_RIGHT`, `BACK_LEFT`, and `BACK_RIGHT` for position-specific tuning. The legacy `KnYAPI.registerSwordOffsets(itemId, offsets)` applies one offset everywhere. Sword display scale is controlled by the shared sword display config, while sheath-only scale can be adjusted through `SheathModelRenderer.SheathOffsets`.

---

## Best Practices

### 1. Use Damager for Ability Damage

```java
// Good - Damager scales once and applies KnY targeting/friendly-fire rules
Damager.hurt(entity, target, 8.0F);

// Good for non-damage guard state, where Damager is not involved
GuardStateHelper.setGuardState(entity, 8.0F, formId);

// Bad - bypasses KnY damage helpers, damage history, and custom rules
target.hurt(level.damageSources().playerAttack(player), 8.0F);

// Bad - damage is scaled before Damager scales it again
float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
Damager.hurt(entity, target, damage);
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

// Reset step height after movement abilities (0.6 is default step height)
MovementHelper.setStepHeight(entity, 0.6F);
// Or if you are storing the original step height
MovementHelper.setStepHeight(entity, originalStepHeight);

// Remove tags when done
entity.removeTag("AbilityActive");


// Clear the guard state when done
GuardStateHelper.clearGuardState(entity);

// Clear the cancel attack swing flag when done
setCancelAttackSwing(entity, false);
```

Example Cleanup (using ability scheduler to schedule after the ability is done):
```
               // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                	MovementHelper.setStepHeight(entity, 0.6f);
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalTicks+2);
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

| Form Type     | Recommended Cooldown |
| ------------- | -------------------- |
| Basic attack  | 2-5 seconds          |
| Dash/movement | 5-8 seconds          |
| Multi-phase   | 8-12 seconds         |
| Ultimate      | 20-30 seconds        |

### 6. Damage Guidelines

| Attack Type   | Base Damage |
| ------------- | ----------- |
| Light attack  | 3-5         |
| Medium attack | 6-8         |
| Heavy attack  | 10-12       |
| Ultimate      | 15+         |

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

- **1.6.30+**: Style Metadata and Color Change System
  - **StyleMetadataRegistry**: Track breathing style parent relationships and eligibility flags
  - **SwordMetadataRegistry**: Track base mod swords for color change integration
  - **Sword Levels**: Required `swordLevel()` parameter (0=base, 1=named, 2=hashira)
  - **Color Change System**: Custom transformation using registered metadata
  - **BaseModRegistration**: Auto-registration of base mod styles and swords
  - New KnYAPI methods: `registerStyleMetadata()`, `getStyleMetadata()`, `getColorChangeEligibleStyles()`

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
