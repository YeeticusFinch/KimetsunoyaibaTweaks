# Entity Creation Guide

This guide shows addon mods how to create custom Breathing Slayer entities that use custom breathing styles.

## Overview

The Kimetsunoyaiba Tweaks mod provides a base `BreathingSlayerEntity` class that handles all the complex entity logic. Addon mods can simply extend this class and specify which breathing technique and equipment to use.

## Quick Start

### 1. Create Your Entity Class

Extend `BreathingSlayerEntity` and override three simple methods:

```java
package com.yourmod.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.yourmod.breathingtechniques.YourBreathingForms;
import com.yourmod.items.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Your Custom Slayer - Wields your_sword, uses Your Breathing (forms 1-6)
 */
public class YourSlayerEntity extends BreathingSlayerEntity {

    public YourSlayerEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return YourBreathingForms.createYourBreathing(); // Forms 1-6 only
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.YOUR_SWORD.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Return armor items [feet, legs, chest, head]
        // Use ItemStack.EMPTY for empty slots
        return new ItemStack[]{
            ItemStack.EMPTY, // No boots
            ItemStack.EMPTY, // No leggings
            new ItemStack(YourArmorItems.CHEST.get()),
            new ItemStack(YourArmorItems.HELMET.get())
        };
    }
}
```

### 2. Register Your Entity

Create a `ModEntities.java` class in your addon mod:

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

    /**
     * Your Slayer - Wields your_sword, uses Your Breathing
     */
    public static final RegistryObject<EntityType<YourSlayerEntity>> YOUR_SLAYER =
        ENTITY_TYPES.register("your_slayer",
            () -> EntityType.Builder.of(YourSlayerEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("your_slayer"));

    /**
     * Register entity types to the mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    /**
     * Event handler for registering entity attributes
     */
    @Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EntityAttributeRegistry {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            // Register attributes for breathing slayer entities
            event.put(YOUR_SLAYER.get(), BreathingSlayerEntity.createAttributes().build());
        }
    }
}
```

### 3. Register ModEntities in Your Main Mod Class

```java
@Mod(YourMod.MODID)
public class YourMod {
    public static final String MODID = "yourmod";

    public YourMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register entities
        ModEntities.register(modEventBus);
    }
}
```

### 4. Create a Spawn Egg (Optional)

Add to your `ModItems.java`:

```java
public static final RegistryObject<Item> YOUR_SLAYER_SPAWN_EGG = ITEMS.register("your_slayer_spawn_egg",
    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
        ModEntities.YOUR_SLAYER,
        0x5DBCD2, 0xFFFFFF, // Body color, spot color (in hex RGB)
        new Item.Properties().stacksTo(64)));
```

### 5. Register Entity Renderer (Client-Side)

Create a renderer class in your `client` package:

```java
package com.yourmod.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.BreathingSlayerRenderer;
import com.yourmod.entities.YourSlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class YourSlayerRenderer extends BreathingSlayerRenderer<YourSlayerEntity> {
    public YourSlayerRenderer(EntityRendererProvider.Context context) {
        super(context, "your_slayer");
    }
}
```

Then register it in your main mod class:

```java
@Mod.EventBusSubscriber(modid = YourMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public static class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.YOUR_SLAYER.get(),
            YourSlayerRenderer::new);
    }
}
```

## What BreathingSlayerEntity Provides

The base `BreathingSlayerEntity` class automatically handles:

### ✅ Combat AI
- Targets hostile mobs within range
- Follows and attacks enemies
- Uses breathing forms in combat

### ✅ Breathing Form Cycling
- Automatically cycles through breathing forms (right-click or combat)
- Syncs form index across client/server
- Handles cooldowns and form restrictions

### ✅ GeckoLib Animation Support
- Plays GeckoLib animations for breathing forms
- Automatic animation sync for multiplayer
- Works with player animation system

### ✅ Equipment Management
- Automatically equips sword and armor
- Shows equipment in renderer
- Updates when forms change

### ✅ Attributes
- Pre-configured health, movement speed, attack damage
- Neutral towards players, aggressive to hostile mobs
- Can be customized by overriding `createAttributes()`

## Advanced Customization

### Custom Name

Override in your entity constructor:

```java
public YourSlayerEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
    super(entityType, level);
    this.setCustomName(Component.literal("Your Character Name"));
}
```

### Special Forms (7th Form, etc.)

Use the "WithSeventh" version of your breathing technique:

```java
@Override
public BreathingTechnique getBreathingTechnique() {
    return YourBreathingForms.createYourBreathingWithSeventh(); // All 7 forms
}
```

### Loading Armor from Other Mods

```java
@Override
public ItemStack[] getArmorEquipment() {
    // Load armor from the base KnY mod
    Item uniformChest = ForgeRegistries.ITEMS.getValue(
        ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_chestplate"));
    Item uniformLegs = ForgeRegistries.ITEMS.getValue(
        ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_leggings"));
    Item uniformBoots = ForgeRegistries.ITEMS.getValue(
        ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

    return new ItemStack[]{
        uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY,
        uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY,
        uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY,
        ItemStack.EMPTY // No helmet
    };
}
```

## Complete Example

See the **KnY-Extra-Additions** mod for complete working examples:
- `IceSlayerEntity` - Basic slayer using Ice Breathing
- `FrostSlayerEntity` - Basic slayer using Frost Breathing
- `KomorebiEntity` - Special character with custom name and 7 forms
- `ShimizuEntity` - Special character with custom armor

## Troubleshooting

### Entity doesn't spawn
- Make sure you registered the entity in `ModEntities.register()`
- Check that entity attributes are registered in `EntityAttributeCreationEvent`
- Verify the spawn egg is registered in `ModItems`

### Entity doesn't render
- Ensure renderer is registered in client-side event handler
- Check that renderer class is in the `client` package
- Verify renderer extends `BreathingSlayerRenderer`

### Breathing forms don't work
- Confirm breathing technique is properly created and returned
- Check that sword item is registered and equipped
- Verify breathing style is registered via KnYAPI

### Entity AI not working
- `BreathingSlayerEntity` uses goal selectors automatically
- Override `registerGoals()` if you need custom AI
- Check entity is neutral (won't attack unless provoked)

## Need Help?

- See [docs/api-usage-guide.md](api-usage-guide.md) for breathing style creation
- Check [docs/breathing-system.md](breathing-system.md) for form implementation
- Look at the KnY-Extra-Additions mod source code for working examples
