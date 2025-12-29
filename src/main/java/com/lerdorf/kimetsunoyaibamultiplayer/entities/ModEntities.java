package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom entities in this mod
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KimetsunoyaibaMultiplayer.MODID);

    /**
     * GeckoLib crow entity that mirrors kasugai_crow from the kimetsunoyaiba mod
     */
    public static final RegistryObject<EntityType<GeckolibCrowEntity>> GECKOLIB_CROW =
        ENTITY_TYPES.register("geckolib_crow",
            () -> EntityType.Builder.of(GeckolibCrowEntity::new, MobCategory.CREATURE)
                .sized(0.4F, 0.5F) // Same size as typical crow
                .clientTrackingRange(64) // FIXED: Increased from 8 to track clones at 5-15 blocks
                .updateInterval(3)
                .build("geckolib_crow"));

    /**
     * Ghostly Clone - Visual effect for Mist Breathing 7th Form: Obscuring Clouds
     * Cannot be attacked or damaged, purely decorative
     */
    public static final RegistryObject<EntityType<GhostlyCloneEntity>> GHOSTLY_CLONE =
        ENTITY_TYPES.register("ghostly_clone",
            () -> EntityType.Builder.<GhostlyCloneEntity>of(GhostlyCloneEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(64) // FIXED: Increased from 8 to track clones at 5-15 blocks
                .updateInterval(1) // FIXED: Update every tick for smooth fade/movement
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("ghostly_clone"));

    /**
     * Muichiro Tokito - Mist Hashira
     * Wields nichirinsword_muichiro, uses Enhanced Mist Breathing (all 7 forms)
     * Neutral entity that targets hostile mobs
     */
    public static final RegistryObject<EntityType<MuichiroEntity>> MUICHIRO =
        ENTITY_TYPES.register("muichiro",
            () -> EntityType.Builder.of(MuichiroEntity::new, MobCategory.MISC)
                .sized(0.48F, 1.44F) // 80% of player size (young appearance)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("muichiro"));

    /**
     * Mitsuri Kanroji - Love Hashira
     * Wields nichirinsword_kanroji (animated whip sword), uses Enhanced Love Breathing (all 6 forms)
     * Long-range whip attacks with multi-target damage
     */
    public static final RegistryObject<EntityType<KanrojiEntity>> KANROJI =
        ENTITY_TYPES.register("kanroji",
            () -> EntityType.Builder.of(KanrojiEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized (adult appearance)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kanroji"));

    /**
     * Mugen Door - Decorative entity for kizuki demon spawns
     * Plays an opening animation and sound, then disappears
     */
    public static final RegistryObject<EntityType<MugenDoorEntity>> MUGEN_DOOR =
        ENTITY_TYPES.register("mugen_door",
            () -> EntityType.Builder.of(MugenDoorEntity::new, MobCategory.MISC)
                .sized(1.0F, 2.0F) // Door-sized
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("mugen_door"));

    /**
     * Love Sword Slashes - Visual effect entity for Love Breathing forms
     * Spawns, plays specified animation, and despawns after lifetime
     */
    public static final RegistryObject<EntityType<LoveSwordSlashesEntity>> LOVE_SWORD_SLASHES =
        ENTITY_TYPES.register("love_sword_slashes",
            () -> EntityType.Builder.of(LoveSwordSlashesEntity::new, MobCategory.MISC)
                .sized(2.0F, 2.0F) // Slash effect size
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("love_sword_slashes"));
    

    /**
     * Love Sword Slashes - Visual effect entity for Love Breathing forms
     * Spawns, plays specified animation, and despawns after lifetime
     */
    public static final RegistryObject<EntityType<LoveTornadoEntity>> LOVE_TORNADO =
        ENTITY_TYPES.register("love_tornado",
            () -> EntityType.Builder.of(LoveTornadoEntity::new, MobCategory.MISC)
                .sized(20.0F, 20.0F) // Tornado effect size (doubled for bigger visual impact)
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("love_tornado"));

    /**
     * Register entity types to the mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    /**
     * Event handler for registering entity attributes
     */
    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EntityAttributeRegistry {
        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        	if (Config.logDebug)
            Log.info("Registering entity attributes");

            // Register attributes for our GeckoLib crow entity
            event.put(GECKOLIB_CROW.get(), GeckolibCrowEntity.createAttributes().build());

            // Register attributes for ghostly clone (visual-only entity)
            event.put(GHOSTLY_CLONE.get(), GhostlyCloneEntity.createAttributes().build());

            // Register attributes for Muichiro Tokito
            event.put(MUICHIRO.get(), MuichiroEntity.createAttributes().build());

            // Register attributes for Mitsuri Kanroji
            event.put(KANROJI.get(), KanrojiEntity.createAttributes().build());

            // Register attributes for Mugen Door (visual-only entity)
            event.put(MUGEN_DOOR.get(), MugenDoorEntity.createAttributes().build());

            // Register attributes for Love Sword Slashes (visual-only entity)
            event.put(LOVE_SWORD_SLASHES.get(), LoveSwordSlashesEntity.createAttributes().build());

            // Register attributes for Love Tornado (visual-only entity)
            event.put(LOVE_TORNADO.get(), LoveTornadoEntity.createAttributes().build());

            if (Config.logDebug)
            Log.info("Entity attributes registered successfully");
        }
    }
}
