# Migration Guide: Using the New Registration API

This guide shows how to refactor existing swords to use the new `KnYAPI` registration system.

---

## Before: Old Manual Registration

### Old `ModItems.java`
```java
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    // Old way: Manual registration
    public static final RegistryObject<Item> NICHIRINSWORD_FROST = ITEMS.register("nichirinsword_frost",
        () -> new NichirinSwordFrost(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_ICE = ITEMS.register("nichirinsword_ice",
        () -> new NichirinSwordIce(new Item.Properties().stacksTo(1).durability(2000)));
}
```

### Old `NichirinSwordFrost.java`
```java
public class NichirinSwordFrost extends BreathingSwordItem {
    private static final BreathingTechnique FROST_BREATHING = FrostBreathingForms.createFrostBreathing();

    public NichirinSwordFrost(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FROST_BREATHING;
    }
}
```

### Old `SwordParticleMapping.java`
```java
static {
    // Had to manually add each sword
    SWORD_TO_PARTICLE_MAP.put("nichirinsword_frost", ResourceLocation.fromNamespaceAndPath("minecraft", "snowflake"));
    SWORD_TO_PARTICLE_MAP.put("nichirinsword_ice", ResourceLocation.fromNamespaceAndPath("minecraft", "dust"));
}
```

---

## After: New API Registration

### New `ModItems.java`
```java
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    // New way: Use builder API
    public static final RegistryObject<Item> NICHIRINSWORD_FROST =
        KnYAPI.createSword("nichirinsword_frost")
            .breathingStyle("frost_breathing", FrostBreathingForms.createFrostBreathing())
            .styleRange(1600)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .durability(2000)
            .build(ITEMS);

    public static final RegistryObject<Item> NICHIRINSWORD_ICE =
        KnYAPI.createSword("nichirinsword_ice")
            .breathingStyle("ice_breathing", IceBreathingForms.createIceBreathing())
            .styleRange(1000)
            .defaultParticle(new DustParticleOptions(new Vector3f(0.5f, 0.8f, 1.0f), 1.5f))
            .category(SwordRegistry.SwordCategory.NICHIRIN)
            .durability(2000)
            .build(ITEMS);

    // Special swords
    public static final RegistryObject<Item> NICHIRINSWORD_KOMOREBI =
        KnYAPI.createSword("nichirinsword_komorebi")
            .breathingStyle("cherry_blossom_breathing", CherryBlossomForms.createCherryBlossomBreathing())
            .styleRange(1700)
            .defaultParticle(ParticleTypes.SNOWFLAKE)
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .durability(2000)
            .build(ITEMS);

    public static final RegistryObject<Item> NICHIRINSWORD_SHIMIZU =
        KnYAPI.createSword("nichirinsword_shimizu")
            .breathingStyle("sakura_breathing", SakuraForms.createSakuraBreathing())
            .styleRange(1800)
            .defaultParticle(new DustParticleOptions(new Vector3f(0.5f, 0.8f, 1.0f), 1.5f))
            .category(SwordRegistry.SwordCategory.SPECIAL)
            .durability(2000)
            .build(ITEMS);
}
```

### Benefits of New System

1. **No separate sword class needed**: The builder creates the sword item automatically
2. **Automatic particle registration**: Particles are automatically configured
3. **Automatic style registration**: Breathing styles are registered with proper ranges
4. **Category tracking**: Swords are automatically tracked as NICHIRIN or SPECIAL
5. **Less boilerplate**: Single fluent API call replaces multiple steps

---

## When to Delete Old Classes

After migrating to the new API, you can delete:

1. Individual sword classes (e.g., `NichirinSwordFrost.java`) - no longer needed
2. Manual particle mapping entries in `SwordParticleMapping.java` - handled by API

Keep:
- Breathing form classes (e.g., `FrostBreathingForms.java`)
- Base `BreathingSwordItem` class (used by builder)
- `BreathingTechnique` and `BreathingForm` classes (still needed)

---

## Step-by-Step Migration

### Step 1: Update ModItems.java

Replace manual `ITEMS.register()` calls with `KnYAPI.createSword()` builder:

```java
// Before
public static final RegistryObject<Item> MY_SWORD = ITEMS.register("my_sword",
    () -> new MySwordClass(new Item.Properties().stacksTo(1).durability(2000)));

// After
public static final RegistryObject<Item> MY_SWORD =
    KnYAPI.createSword("my_sword")
        .breathingStyle("my_style", MyForms.createMyStyle())
        .styleRange(1900)
        .defaultParticle(ParticleTypes.SNOWFLAKE)
        .category(SwordRegistry.SwordCategory.NICHIRIN)
        .durability(2000)
        .build(ITEMS);
```

### Step 2: Delete Individual Sword Classes

If you had classes like `NichirinSwordFrost.java`, you can delete them now.

### Step 3: Remove Manual Particle Mappings

In `SwordParticleMapping.java`, remove manual mappings for your swords from the static block.
The API now handles this automatically.

### Step 4: Test

1. Build your mod: `./gradlew build`
2. Test in-game:
   - Sword appears in creative tab
   - Right-click activates forms
   - R key cycles through forms
   - Particles appear on sword swings
   - Form cycling works correctly

---

## Handling Special Cases

### Golden Sword (Not in Creative Tab)

```java
public static final RegistryObject<Item> NICHIRINSWORD_GOLDEN =
    KnYAPI.createSword("nichirinsword_golden")
        .breathingStyle("frost_breathing", FrostBreathingForms.createFrostBreathing())
        .styleRange(1600)
        .defaultParticle(ParticleTypes.SNOWFLAKE)
        .category(SwordRegistry.SwordCategory.SPECIAL)
        .durability(2000)
        .registerToCreativeTab(false)  // Hide from creative tab
        .build(ITEMS);
```

### Custom Particle for Specific Sword

If you want a sword to use a different particle than the style default:

```java
public static final RegistryObject<Item> MY_VARIANT =
    KnYAPI.createSword("my_variant")
        .breathingStyle("frost_breathing", FrostBreathingForms.createFrostBreathing())
        .styleRange(1600)
        .defaultParticle(ParticleTypes.SNOWFLAKE)       // Style default
        .swordParticle(ParticleTypes.ENCHANTED_HIT)     // Override for this sword
        .category(SwordRegistry.SwordCategory.NICHIRIN)
        .build(ITEMS);
```

---

## Updating Creative Tabs

### Before
```java
public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB =
    CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_ICE.get()))
            .displayItems((parameters, output) -> {
                output.accept(NICHIRINSWORD_ICE.get());
                output.accept(NICHIRINSWORD_FROST.get());
                // ... manually add each sword
            })
            .build());
```

### After (Optional - Use Registry)
```java
public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB =
    CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_ICE.get()))
            .displayItems((parameters, output) -> {
                // Add all nichirin swords automatically
                for (var sword : KnYAPI.getNichirinSwords()) {
                    output.accept(sword.getSwordItem());
                }

                // Add all special swords automatically
                for (var sword : KnYAPI.getSpecialSwords()) {
                    output.accept(sword.getSwordItem());
                }

                // Or manually add specific items
                output.accept(NICHIRINSWORD_ICE.get());
            })
            .build());
```

---

## Compatibility Notes

- **Existing worlds**: Swords in player inventories will continue to work
- **Item IDs**: Use the same item ID when migrating to preserve compatibility
- **NBT data**: No changes to NBT structure, full compatibility maintained
- **Network packets**: No changes needed, uses existing infrastructure

---

## Troubleshooting Migration

### "Sword already registered" error

This means you're trying to register the same sword ID twice. Make sure you:
1. Removed the old manual registration
2. Only have one `KnYAPI.createSword()` call per sword ID

### Particle not appearing

Check that:
1. The particle type is valid and registered
2. You're using a `ParticleOptions` instance, not just `ParticleType`
3. For dust particles, use `new DustParticleOptions(color, size)`

### Form cycling not working

Ensure:
1. The breathing technique is properly created with all forms
2. The style range matches what's in `BreathingInfoDetector`
3. You're using the same style ID consistently

---

## Example: Complete Migration

### Before (Multiple Files)

**NichirinSwordFrost.java** (15 lines)
```java
package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.FrostBreathingForms;

public class NichirinSwordFrost extends BreathingSwordItem {
    private static final BreathingTechnique FROST_BREATHING = FrostBreathingForms.createFrostBreathing();

    public NichirinSwordFrost(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return FROST_BREATHING;
    }
}
```

**ModItems.java** (partial)
```java
public static final RegistryObject<Item> NICHIRINSWORD_FROST = ITEMS.register("nichirinsword_frost",
    () -> new NichirinSwordFrost(new Item.Properties().stacksTo(1).durability(2000)));
```

**SwordParticleMapping.java** (partial)
```java
SWORD_TO_PARTICLE_MAP.put("nichirinsword_frost", ResourceLocation.fromNamespaceAndPath("minecraft", "snowflake"));
```

### After (Single Declaration)

**ModItems.java** (only file needed)
```java
public static final RegistryObject<Item> NICHIRINSWORD_FROST =
    KnYAPI.createSword("nichirinsword_frost")
        .breathingStyle("frost_breathing", FrostBreathingForms.createFrostBreathing())
        .styleRange(1600)
        .defaultParticle(ParticleTypes.SNOWFLAKE)
        .category(SwordRegistry.SwordCategory.NICHIRIN)
        .durability(2000)
        .build(ITEMS);
```

**Result**: 3 separate pieces of code → 1 fluent API call

---

## Next Steps

After migrating your existing swords:

1. **Test thoroughly**: Verify all swords work correctly
2. **Update documentation**: Update any docs referencing the old system
3. **Share with others**: Other mods can now use your mod as a library!
4. **Extend**: Use the API to add new swords without touching core code

For help creating new content with the API, see `docs/api-usage-guide.md`.
