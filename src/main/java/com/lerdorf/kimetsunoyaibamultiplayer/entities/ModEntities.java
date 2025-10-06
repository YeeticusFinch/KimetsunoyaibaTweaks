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
                .clientTrackingRange(8)
                .updateInterval(3)
                .build("geckolib_crow"));

    /**
     * Ice Slayer - Wields nichirinsword_ice, uses Ice Breathing (forms 1-6)
     * Neutral entity that targets hostile mobs
     */
    public static final RegistryObject<EntityType<IceSlayerEntity>> ICE_SLAYER =
        ENTITY_TYPES.register("ice_slayer",
            () -> EntityType.Builder.of(IceSlayerEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("ice_slayer"));

    /**
     * Frost Slayer - Wields nichirinsword_frost, uses Frost Breathing (forms 1-6)
     * Neutral entity that targets hostile mobs
     */
    public static final RegistryObject<EntityType<FrostSlayerEntity>> FROST_SLAYER =
        ENTITY_TYPES.register("frost_slayer",
            () -> EntityType.Builder.of(FrostSlayerEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("frost_slayer"));

    /**
     * Kakushima Hiori - Wields nichirinsword_hiori, uses Frost Breathing (all 7 forms)
     * Neutral entity that targets hostile mobs
     */
    public static final RegistryObject<EntityType<HioriEntity>> HIORI =
        ENTITY_TYPES.register("hiori",
            () -> EntityType.Builder.of(HioriEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("hiori"));

    /**
     * Yukire Hanazawa - Wields nichirinsword_hanazawa, uses Ice Breathing (all 7 forms)
     * Neutral entity that targets hostile mobs
     */
    public static final RegistryObject<EntityType<HanazawaEntity>> HANAZAWA =
        ENTITY_TYPES.register("hanazawa",
            () -> EntityType.Builder.of(HanazawaEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("hanazawa"));

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

            // Register attributes for breathing slayer entities
            event.put(ICE_SLAYER.get(), BreathingSlayerEntity.createAttributes().build());
            event.put(FROST_SLAYER.get(), BreathingSlayerEntity.createAttributes().build());
            event.put(HIORI.get(), BreathingSlayerEntity.createAttributes().build());
            event.put(HANAZAWA.get(), BreathingSlayerEntity.createAttributes().build());

            if (Config.logDebug)
            Log.info("Entity attributes registered successfully");
        }
    }
}
