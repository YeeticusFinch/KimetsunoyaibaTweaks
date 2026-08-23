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
     * Muichiro Tokito (Full Potential) - starts in demon slayer mark state.
     */
    public static final RegistryObject<EntityType<MuichiroFullPotentialEntity>> MUICHIRO_FP =
        ENTITY_TYPES.register("muichiro_fp",
            () -> EntityType.Builder.of(MuichiroFullPotentialEntity::new, MobCategory.MISC)
                .sized(0.54F, 1.62F) // Slightly larger than base Muichiro
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("muichiro_fp"));

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
     * Kanae Kocho - Flower Hashira
     * Wields nichirinsword_kanae and uses Hashira-tier Flower Breathing.
     */
    public static final RegistryObject<EntityType<KanaeEntity>> KANAE =
        ENTITY_TYPES.register("kanae",
            () -> EntityType.Builder.of(KanaeEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kanae"));

    /**
     * Kanao Tsuyuri (Kanawo) - Kamaboko Flower Breathing user
     * Wields nichirinsword_kanawo and uses Kamaboko-tier Flower Breathing.
     */
    public static final RegistryObject<EntityType<KanawoEntity>> KANAWO =
        ENTITY_TYPES.register("kanawo",
            () -> EntityType.Builder.of(KanawoEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kanawo"));

    /**
     * Kanata Ubuyashiki - passive child civilian.
     */
    public static final RegistryObject<EntityType<KanataEntity>> KANATA =
        ENTITY_TYPES.register("kanata",
            () -> EntityType.Builder.of(KanataEntity::new, MobCategory.MISC)
                .sized(0.42F, 1.26F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kanata"));

    /**
     * Kiriya Ubuyashiki - passive child civilian.
     */
    public static final RegistryObject<EntityType<KiriyaEntity>> KIRIYA =
        ENTITY_TYPES.register("kiriya",
            () -> EntityType.Builder.of(KiriyaEntity::new, MobCategory.MISC)
                .sized(0.42F, 1.26F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kiriya"));

    public static final RegistryObject<EntityType<KazumiEntity>> KAZUMI =
        ENTITY_TYPES.register("kazumi",
            () -> EntityType.Builder.of(KazumiEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("kazumi"));

    /**
     * Princess - Tanzanite's poodle companion.
     * Egg-spawned, self-taming GeckoLib dog that follows and defends its owner.
     */
    public static final RegistryObject<EntityType<PrincessEntity>> PRINCESS =
        ENTITY_TYPES.register("princess",
            () -> EntityType.Builder.of(PrincessEntity::new, MobCategory.CREATURE)
                .sized(0.8F, 1.0F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("princess"));

    /**
     * Orochi - demon quest companion for the Permanence storyline.
     */
    public static final RegistryObject<EntityType<OrochiEntity>> OROCHI =
        ENTITY_TYPES.register("orochi",
            () -> EntityType.Builder.of(OrochiEntity::new, MobCategory.CREATURE)
                .sized(0.45F, 0.35F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("orochi"));

    /**
     * Eye Familiar - demon quest companion.
     */
    public static final RegistryObject<EntityType<EyeFamiliarEntity>> EYE_FAMILIAR =
        ENTITY_TYPES.register("eye_familiar",
            () -> EntityType.Builder.of(EyeFamiliarEntity::new, MobCategory.CREATURE)
                .sized(0.45F, 0.45F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("eye_familiar"));

    /**
     * Mugen Door - Decorative entity for kizuki demon spawns
     * Plays an opening animation and sound, then disappears
     */
    public static final RegistryObject<EntityType<MugenDoorEntity>> MUGEN_DOOR =
        ENTITY_TYPES.register("mugen_door",
            () -> EntityType.Builder.of(MugenDoorEntity::new, MobCategory.MISC)
                .sized(3.0F, 3.0F)
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
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

    public static final RegistryObject<EntityType<WhiteSlashesEntity>> WHITE_SLASHES =
        ENTITY_TYPES.register("white_slashes",
            () -> EntityType.Builder.of(WhiteSlashesEntity::new, MobCategory.MISC)
                .sized(2.0F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .noSave()
                .fireImmune()
                .build("white_slashes"));

    public static final RegistryObject<EntityType<BeastSlashesEntity>> BEAST_SLASHES =
        ENTITY_TYPES.register("beast_slashes",
            () -> EntityType.Builder.of(BeastSlashesEntity::new, MobCategory.MISC)
                .sized(2.0F, 2.0F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .noSave()
                .fireImmune()
                .build("beast_slashes"));
    

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
     * After Image - Visual effect for Flower Breathing 7th Form and other speed techniques
     * Creates ghostly afterimages that fade out, giving the illusion of blinding speed
     * Cannot be attacked or damaged, purely decorative
     */
    public static final RegistryObject<EntityType<AfterImageEntity>> AFTER_IMAGE =
        ENTITY_TYPES.register("after_image",
            () -> EntityType.Builder.<AfterImageEntity>of(AfterImageEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Player-sized
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth fade
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("after_image"));

    /**
     * Dark Star - Temporary visual entity for the Dark Star blood demon art.
     * The server spawns it and the client renderer draws the dark_star item model.
     */
    public static final RegistryObject<EntityType<DarkStarVisualEntity>> DARK_STAR_VISUAL =
        ENTITY_TYPES.register("dark_star_visual",
            () -> EntityType.Builder.<DarkStarVisualEntity>of(DarkStarVisualEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .noSave()
                .fireImmune()
                .build("dark_star_visual"));

    /**
     * Generic Demon Slayer (male) - Spawns with random nichirin sword and power level.
     * Can use any registered breathing style (base mod, this mod, or addons).
     */
    public static final RegistryObject<EntityType<DemonSlayerEntity>> DEMON_SLAYER =
        ENTITY_TYPES.register("demon_slayer",
            () -> EntityType.Builder.of(DemonSlayerEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("demon_slayer"));

    /**
     * Generic Demon Slayer (female) - Same behavior as demon_slayer but uses
     * biped_female model and slayer_female textures. Tagged with forge:woman.
     */
    public static final RegistryObject<EntityType<DemonSlayerEntity>> DEMON_SLAYER_FEMALE =
        ENTITY_TYPES.register("demon_slayer_female",
            () -> EntityType.Builder.of(DemonSlayerEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .updateInterval(3)
                .build("demon_slayer_female"));

    public static final RegistryObject<EntityType<DemonCreeperEntity>> DEMON_CREEPER =
        ENTITY_TYPES.register("demon_creeper",
            () -> EntityType.Builder.of(DemonCreeperEntity::new, MobCategory.MONSTER)
                .sized(0.8F, 1.7F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("demon_creeper"));

    public static final RegistryObject<EntityType<DemonVillagerEntity>> DEMON_VILLAGER =
        ENTITY_TYPES.register("demon_villager",
            () -> EntityType.Builder.of(DemonVillagerEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("demon_villager"));

    public static final RegistryObject<EntityType<DemonPillagerEntity>> DEMON_PILLAGER =
        ENTITY_TYPES.register("demon_pillager",
            () -> EntityType.Builder.of(DemonPillagerEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("demon_pillager"));

    public static final RegistryObject<EntityType<DemonVindicatorEntity>> DEMON_VINDICATOR =
        ENTITY_TYPES.register("demon_vindicator",
            () -> EntityType.Builder.of(DemonVindicatorEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("demon_vindicator"));

    public static final RegistryObject<EntityType<SwampDemonEntity>> SWAMP_DEMON =
        ENTITY_TYPES.register("swamp_demon",
            () -> EntityType.Builder.of(SwampDemonEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("swamp_demon"));

    public static final RegistryObject<EntityType<NezukoEntity>> NEZUKO =
        ENTITY_TYPES.register("nezuko",
            () -> EntityType.Builder.of(NezukoEntity::new, MobCategory.MISC)
                .sized(0.6F, 2.0F)
                .clientTrackingRange(10)
                .updateInterval(2)
                .build("nezuko"));

    public static final RegistryObject<EntityType<SwampPuddleEntity>> SWAMP_PUDDLE =
        ENTITY_TYPES.register("swamp_puddle",
            () -> EntityType.Builder.<SwampPuddleEntity>of(SwampPuddleEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.1F)
                .clientTrackingRange(32)
                .updateInterval(1)
                .noSave()
                .fireImmune()
                .build("swamp_puddle"));

    /**
     * Swamp Hand - Temporary attack effect for Swamp Demon Art
     * Spawns, plays attack animation, deals damage at tick 10, despawns at tick 20
     */
    public static final RegistryObject<EntityType<SwampHandEntity>> SWAMP_HAND =
        ENTITY_TYPES.register("swamp_hand",
            () -> EntityType.Builder.<SwampHandEntity>of(SwampHandEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F) // Hand effect size
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("swamp_hand"));

    /**
     * Flower Petal Slash - Visual effect for Flower Breathing slash attacks
     * Animated texture cycling through 18 frames (sword_loop_flower0-17.png)
     * Despawns after one full animation loop (18 ticks)
     */
    public static final RegistryObject<EntityType<FlowerPetalSlashEntity>> FLOWER_PETAL_SLASH =
        ENTITY_TYPES.register("flower_petal_slash",
            () -> EntityType.Builder.<FlowerPetalSlashEntity>of(FlowerPetalSlashEntity::new, MobCategory.MISC)
                .sized(2.0F, 2.0F) // Slash effect size
                .clientTrackingRange(64) // Visible from distance
                .updateInterval(1) // Update every tick for smooth animation
                .noSave() // Don't save to world (temporary entity)
                .fireImmune()
                .build("flower_petal_slash"));

    public static final RegistryObject<EntityType<SpineEntity>> SPINE =
        ENTITY_TYPES.register("spine",
            () -> EntityType.Builder.<SpineEntity>of(SpineEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .noSave()
                .fireImmune()
                .build("spine"));

    public static final RegistryObject<EntityType<KunaiEntity>> KUNAI =
        ENTITY_TYPES.register("kunai",
            () -> EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .noSave()
                .build("kunai"));

    /**
     * Invisible temporary mount used so players can sit on cushion blocks from the base mod.
     */
    public static final RegistryObject<EntityType<CushionSeatEntity>> CUSHION_SEAT =
        ENTITY_TYPES.register("cushion_seat",
            () -> EntityType.Builder.of(CushionSeatEntity::new, MobCategory.MISC)
                .sized(0.01F, 0.01F)
                .clientTrackingRange(16)
                .updateInterval(1)
                .noSave()
                .build("cushion_seat"));

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
            event.put(MUICHIRO_FP.get(), MuichiroFullPotentialEntity.createAttributes().build());

            // Register attributes for Mitsuri Kanroji
            event.put(KANROJI.get(), KanrojiEntity.createAttributes().build());

            event.put(DEMON_CREEPER.get(), DemonCreeperEntity.createAttributes().build());
            event.put(DEMON_VILLAGER.get(), DemonVillagerEntity.createAttributes().build());
            event.put(DEMON_PILLAGER.get(), DemonPillagerEntity.createAttributes().build());
            event.put(DEMON_VINDICATOR.get(), DemonVindicatorEntity.createAttributes().build());
            event.put(SWAMP_DEMON.get(), SwampDemonEntity.createAttributes().build());
            event.put(NEZUKO.get(), NezukoEntity.createAttributes().build());

            // Register attributes for Kanae and Kanawo
            event.put(KANAE.get(), KanaeEntity.createAttributes().build());
            event.put(KANAWO.get(), KanawoEntity.createAttributes().build());
            event.put(KANATA.get(), KanataEntity.createAttributes().build());
            event.put(KIRIYA.get(), KiriyaEntity.createAttributes().build());
            event.put(KAZUMI.get(), KazumiEntity.createAttributes().build());
            event.put(PRINCESS.get(), PrincessEntity.createAttributes().build());
            event.put(OROCHI.get(), OrochiEntity.createAttributes().build());
            event.put(EYE_FAMILIAR.get(), EyeFamiliarEntity.createAttributes().build());

            // Register attributes for Mugen Door (visual-only entity)
            event.put(MUGEN_DOOR.get(), MugenDoorEntity.createAttributes().build());

            // Register attributes for Love Sword Slashes (visual-only entity)
            event.put(LOVE_SWORD_SLASHES.get(), LoveSwordSlashesEntity.createAttributes().build());
            event.put(WHITE_SLASHES.get(), WhiteSlashesEntity.createAttributes().build());
            event.put(BEAST_SLASHES.get(), BeastSlashesEntity.createAttributes().build());

            // Register attributes for Love Tornado (visual-only entity)
            event.put(LOVE_TORNADO.get(), LoveTornadoEntity.createAttributes().build());

            // Register attributes for After Image (visual-only entity)
            event.put(AFTER_IMAGE.get(), AfterImageEntity.createAttributes().build());

            // Register attributes for Flower Petal Slash (visual-only entity)
            event.put(FLOWER_PETAL_SLASH.get(), FlowerPetalSlashEntity.createAttributes().build());

            // Register attributes for Swamp Hand (temporary attack effect)
            event.put(SWAMP_HAND.get(), SwampHandEntity.createAttributes().build());

            // Register attributes for invisible cushion seat mount
            event.put(CUSHION_SEAT.get(), CushionSeatEntity.createAttributes().build());

            // Register attributes for generic demon slayers (male and female)
            event.put(DEMON_SLAYER.get(), DemonSlayerEntity.createAttributes().build());
            event.put(DEMON_SLAYER_FEMALE.get(), DemonSlayerEntity.createAttributes().build());

            if (Config.logDebug)
            Log.info("Entity attributes registered successfully");
        }
    }
}
