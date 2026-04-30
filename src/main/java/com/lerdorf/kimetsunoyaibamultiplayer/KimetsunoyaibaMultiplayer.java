package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.ISwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.SwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
// import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestParticlesCommand; // REMOVED: Uses client-only SwordParticleHandler
// import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugParticlesCommand; // REMOVED: Uses client-only SwordParticleHandler and BonePositionTracker
// import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimCommand; // REMOVED: Uses client-only SwordParticleHandler, registered client-side only
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestCrowQuestCommand;
// import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.SwordParticleHandler; // REMOVED: Client-only class, causes server crash
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SpawnRateHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.proxy.IClientProxy;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KimetsunoyaibaMultiplayer.MODID)
public class KimetsunoyaibaMultiplayer
{
    private static boolean firstServerTickProbeLogged = false;
    private static boolean firstPlayerTickProbeLogged = false;
    private static final long SERVER_HANG_WATCHDOG_THRESHOLD_MS = 5000L;
    private static final AtomicBoolean SERVER_HANG_WATCHDOG_STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean SERVER_HANG_DUMPED_FOR_CURRENT_STALL = new AtomicBoolean(false);
    private static volatile Thread observedServerThread;
    private static volatile long lastServerTickHeartbeatMs = 0L;
    // Define mod id in a common place for everything to reference
    public static final String MODID = "kimetsunoyaibamultiplayer";

    // NOTE: CLIENT_PROXY removed to prevent server crashes
    // The proxy pattern was causing client-only classes to be loaded on dedicated servers
    // Instead, packet handlers now use DistExecutor.unsafeRunWhenOn() directly

    public KimetsunoyaibaMultiplayer(FMLJavaModLoadingContext context)
    {
        Log.startupProbe("KimetsunoyaibaMultiplayer.<init>.start");
        IEventBus modEventBus = context.getModEventBus();
        Log.alwaysWarn("[INIT] Acquired mod event bus");
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        Log.alwaysWarn("[INIT] Registered commonSetup listener");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SpawnRateHandler.class);
        Log.alwaysWarn("[INIT] Registered Forge event bus listeners");

        // Register entities
        ModEntities.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered entities");

        // Register sounds
        ModSounds.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered sounds");

        // Register blocks
        com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.register(modEventBus);
        com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered blocks");

        com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered block entities");

        // Register items
        ModItems.register(modEventBus);
        com.lerdorf.kimetsunoyaibamultiplayer.items.SheathItems.register(modEventBus);
        com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems.register(modEventBus);
        com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyMenus.register(modEventBus);
        com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyRecipes.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered items");
        
        // Register effects
        ModEffects.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered effects");

        // Register tree decorators
        com.lerdorf.kimetsunoyaibamultiplayer.worldgen.ModTreeDecorators.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered tree decorators");

        // Register particles
        com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.register(modEventBus);
        Log.alwaysWarn("[INIT] Registered particles");

        // Register config event handlers on the mod event bus
        modEventBus.register(Config.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedChestOfDrawersConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SwordsmithVillageConfig.class);
        Log.alwaysWarn("[INIT] Registered config event handlers");

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "kimetsunoyaibamultiplayer/common.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig.SPEC, "kimetsunoyaibamultiplayer/sword_slash.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.SPEC, "kimetsunoyaibamultiplayer/particles.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.SPEC, "kimetsunoyaibamultiplayer/entities.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.SPEC, "kimetsunoyaibamultiplayer/sword_display.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.BiomeConfig.SPEC, "kimetsunoyaibamultiplayer/biomes.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SpawnRateConfig.SPEC, "kimetsunoyaibamultiplayer/spawn_rates.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedSpawnConfig.SPEC, "kimetsunoyaibamultiplayer/enhanced_spawning.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig.SPEC, "kimetsunoyaibamultiplayer/raids.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SurvivalRaidConfig.SPEC, "kimetsunoyaibamultiplayer/survival_raids.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig.SPEC, "kimetsunoyaibamultiplayer/final_selection_raids.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.SPEC, "kimetsunoyaibamultiplayer/enhanced_breathing.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig.SPEC, "kimetsunoyaibamultiplayer/customnpcs.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.VariationConfig.SPEC, "kimetsunoyaibamultiplayer/variations.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig.SPEC, "kimetsunoyaibamultiplayer/custom_progression.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.DemonSlayerConfig.SPEC, "kimetsunoyaibamultiplayer/demon_slayer.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedChestOfDrawersConfig.SPEC, "kimetsunoyaibamultiplayer/enhanced_chest_of_drawers.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SwordsmithVillageConfig.SPEC, "kimetsunoyaibamultiplayer/swordsmith_village.toml");
        Log.alwaysWarn("[INIT] Registered config specs");
        Log.startupProbe("KimetsunoyaibaMultiplayer.<init>.end");
    }

    public static final Capability<ISwordWielderData> SWORD_WIELDER_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        Log.startupProbe("KimetsunoyaibaMultiplayer.commonSetup.start");
        Log.info("Initializing Kimetsunoyaiba Multiplayer animation sync...");

        // Register custom game rules
        ModGameRules.register();
        Log.info("Custom game rules registered");

        // Register network messages
        ModNetworking.register();
        Log.info("Network messages registered");
        Log.alwaysWarn("[INIT] commonSetup registered game rules and networking");
        
        // GeckoLib initialization moved to client setup to avoid early shader reload issues

        // Register our built-in swords in the SwordRegistry (must be done after items are registered)
        event.enqueueWork(() -> {
            Log.startupProbe("KimetsunoyaibaMultiplayer.commonSetup.enqueueWork.start");
            com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyBrewingRecipes.register();
            ((net.minecraft.world.level.block.FlowerPotBlock) net.minecraft.world.level.block.Blocks.FLOWER_POT).addPlant(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "fermented_orchid"),
                com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks.POTTED_FERMENTED_ORCHID
            );
            ((net.minecraft.world.level.block.FlowerPotBlock) net.minecraft.world.level.block.Blocks.FLOWER_POT).addPlant(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "immortal_daisy"),
                com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks.POTTED_IMMORTAL_DAISY
            );
            // Register base mod style metadata and sword metadata for color change system
            com.lerdorf.kimetsunoyaibamultiplayer.api.BaseModRegistration.registerAll();


            // Cyan mist particle for mist breathing swords (RGB: 138, 195, 194)
            DustParticleOptions mistParticle = new DustParticleOptions(
                new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
                0.5f
            );

            // Register Mist Breathing sword (generic, 6 forms)
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_mist",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_MIST.get(),
                "mist_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
                mistParticle,
                SoundEvents.AMBIENT_CAVE.value(),
                null
            );

            // Register Muichiro's Mist Breathing sword (special, 7 forms including Obscuring Clouds)
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_muichiro",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_MUICHIRO.get(),
                "mist_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.SPECIAL,
                mistParticle,
                SoundEvents.AMBIENT_CAVE.value(),
                null,
                true,
                2 // Hashira level
            );

            // Register Mitsuri Kanroji's Love Breathing sword (special, whip sword with 5 forms)
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_kanroji",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_KANROJI.get(),
                "love_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.SPECIAL,
                ParticleTypes.HEART,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                null,
                true,
                2 // Hashira level
            );

            // Register Flower Breathing swords (base, Kanawo, Kanae)
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_flower",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_FLOWER.get(),
                "flower_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
                null,
                null,
                null,
                true,
                0 // Base sword level
            );

            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_kanawo",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_KANAWO.get(),
                "flower_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.SPECIAL,
                null,
                null,
                null,
                true,
                1 // Named character level
            );

            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
                "nichirinsword_kanae",
                (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) ModItems.NICHIRINSWORD_KANAE.get(),
                "flower_breathing",
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.SPECIAL,
                null,
                null,
                null,
                true,
                2 // Hashira level
            );

            // Register breathing styles if not already registered
            if (!com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.isRegistered("mist_breathing")) {
                com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.register(
                    "mist_breathing",
                    "Mist Breathing",
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms.createGenericMistBreathing(),
                    20000,
                    mistParticle,
                    null
                );
            }

            if (!com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.isRegistered("love_breathing")) {
                com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.register(
                    "love_breathing",
                    "Love Breathing",
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms.createLoveBreathing(),
                    21000,
                    ParticleTypes.HEART,
                    null
                );
            }

            if (!com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.isRegistered("flower_breathing")) {
                com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.register(
                    "flower_breathing",
                    "Flower Breathing",
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedFlowerForms.createGenericFlowerBreathing(),
                    24000,
                    null,
                    null
                );
            }

            if (!com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.isRegistered("beast_breathing")) {
                com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.register(
                    "beast_breathing",
                    "Beast Breathing",
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedBeastForms.createBeastBreathing(),
                    200,
                    null,
                    null
                );
            }

            // Also register for base mod's 1300 range (for compatibility with base mod swords)
            if (!com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.isRegistered("flower_breathing_base")) {
                com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry.register(
                    "flower_breathing_base",
                    "Flower Breathing",
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedFlowerForms.createGenericFlowerBreathing(),
                    1300,
                    null,
                    null
                );
            }

            // Register example breathing form variations
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.WaterVariations.register();
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.LoveVariations.register();
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MistVariations.register();

            com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonCreeperEntity.registerBloodDemonArt();
            com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVindicatorEntity.registerBloodDemonArt();
            com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity.registerBloodDemonArt();
            com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI.registerDemon(
                "kimetsunoyaibamultiplayer:demon_creeper",
                com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale.MEDIUM_DEMON,
                false,
                com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonCreeperEntity.BLOOD_DEMON_ART_ID
            );
            com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI.registerDemon(
                "kimetsunoyaibamultiplayer:demon_villager",
                com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale.EASY_DEMON,
                false,
                null
            );
            com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI.registerDemon(
                "kimetsunoyaibamultiplayer:demon_pillager",
                com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale.EASY_DEMON,
                false,
                null
            );
            com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI.registerDemon(
                "kimetsunoyaibamultiplayer:demon_vindicator",
                com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale.EASY_DEMON,
                false,
                com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVindicatorEntity.BLOOD_DEMON_ART_ID
            );
            com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI.registerDemon(
                "kimetsunoyaibamultiplayer:swamp_demon",
                com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityPowerScale.MEDIUM_DEMON,
                false,
                com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity.BLOOD_DEMON_ART_ID
            );

            Log.info("Registered built-in swords in SwordRegistry");
            Log.startupProbe("KimetsunoyaibaMultiplayer.commonSetup.enqueueWork.end");
        });
        Log.startupProbe("KimetsunoyaibaMultiplayer.commonSetup.end");
    }
    
 // 4. Attach it to entities
    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player || event.getObject() instanceof LivingEntity) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(MODID, "sword_wielder_data"),
                    new ICapabilityProvider() {
                        final SwordWielderData backend = new SwordWielderData();
                        @Override
                        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                            return cap == SWORD_WIELDER_DATA ? LazyOptional.of(() -> backend).cast() : LazyOptional.empty();
                        }
                    });
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        Log.startupProbe("KimetsunoyaibaMultiplayer.onServerStarting");
        Log.info("Kimetsunoyaiba Multiplayer server starting");
    }

    /**
     * Sync breathing form and variation data when player joins the server.
     * This is critical for multiplayer functionality - ensures client has current state.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Log.startupProbeOnce("KimetsunoyaibaMultiplayer.onPlayerLogin.start");
            // Get player's current breathing data
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(serverPlayer);
            Log.startupProbeOnce("KimetsunoyaibaMultiplayer.onPlayerLogin.afterGetOrCreate");

            // Sync base mod breathes value to client (includes encoded variation)
            double breathes = serverPlayer.getPersistentData().getDouble("breathes");
            if (breathes > 0) {
                if (com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.isKnownFormId(
                        com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathes))) {
                    data.setBaseModBreathesValue(breathes);
                }
                ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket(
                        serverPlayer.getUUID(),
                        breathes
                    ),
                    serverPlayer
                );
                Log.startupProbeOnce("KimetsunoyaibaMultiplayer.onPlayerLogin.afterBreathesSync");
            }
            // Sync variation index separately
            ModNetworking.sendToPlayer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(
                    serverPlayer.getUUID(),
                    data.getCurrentVariationIndex()
                ),
                serverPlayer
            );
            Log.startupProbeOnce("KimetsunoyaibaMultiplayer.onPlayerLogin.afterVariationSync");

            if (Config.logDebug) {
                // Decode form index to show base form and variation
                int encodedFormIndex = data.getCurrentFormIndex();
                int baseFormIndex = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(encodedFormIndex);
                int variationIndex = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getVariationIndex(encodedFormIndex);

                Log.debug("Synced breathing data to joining player: " + serverPlayer.getName().getString() +
                         " (form: " + baseFormIndex + ", variation: " + variationIndex + ", breathes: " + breathes + ")");
            }
            Log.startupProbeOnce("KimetsunoyaibaMultiplayer.onPlayerLogin.end");
        }
    }

    /**
     * Handle axe stripping for wisteria logs
     */
    @SubscribeEvent
    public void onBlockToolModification(net.minecraftforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        // Check if using an axe and right-clicking
        if (event.getToolAction() == net.minecraftforge.common.ToolActions.AXE_STRIP) {
            net.minecraft.world.level.block.Block block = event.getState().getBlock();

            // Wisteria log -> Stripped wisteria log
            if (block == com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LOG.get()) {
                event.setFinalState(
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_LOG.get()
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                                 event.getState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS))
                );
            }
            // Wisteria wood -> Stripped wisteria wood
            else if (block == com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_WOOD.get()) {
                event.setFinalState(
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_WOOD.get()
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                                 event.getState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS))
                );
            }
            else if (block == com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.DARK_OAK_WALL.get()) {
                net.minecraft.world.level.block.state.BlockState state = event.getState();
                event.setFinalState(
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_DARK_OAK_WALL.get()
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.WallBlock.UP, state.getValue(net.minecraft.world.level.block.WallBlock.UP))
                        .setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, state.getValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL))
                        .setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, state.getValue(net.minecraft.world.level.block.WallBlock.EAST_WALL))
                        .setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, state.getValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL))
                        .setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, state.getValue(net.minecraft.world.level.block.WallBlock.WEST_WALL))
                        .setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED, state.getValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED))
                );
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        Log.startupProbe("KimetsunoyaibaMultiplayer.onRegisterCommands.start");
        Log.info("Registering server commands");
        // Only register server-safe commands
        // TestAnimationCommand, TestAnimCommand, TestParticlesCommand, and DebugParticlesCommand
        // all use client-only particle handlers (SwordParticleHandler, BonePositionTracker)
        // and are registered client-side only via ClientCommandHandler
        // TestParticlesCommand.register(event.getDispatcher()); // REMOVED: Uses client-only SwordParticleHandler
        // DebugParticlesCommand.register(event.getDispatcher()); // REMOVED: Uses client-only SwordParticleHandler and BonePositionTracker
        // TestAnimCommand.register(event.getDispatcher()); // REMOVED: Uses client-only SwordParticleHandler
        TestCrowQuestCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.TrainingSwordCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.GiveBlackSwordCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.SunBreathingLevelCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.SpawnDemonSlayerCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.TorilGateCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.SwordsmithVillageCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.FinalSelectionCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.OreSelectCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.SurvivalRaidCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.MeditationMenuCommand.register(event.getDispatcher());
        com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugPlayerDimensionsCommand.register(event.getDispatcher());
        Log.startupProbe("KimetsunoyaibaMultiplayer.onRegisterCommands.end");
    }

    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        if (event.player.level().isClientSide()) {
            return;
        }
        if (!firstPlayerTickProbeLogged) {
            firstPlayerTickProbeLogged = true;
            Log.startupProbe("KimetsunoyaibaMultiplayer.onPlayerTick");
        }
        net.minecraft.world.entity.player.Player player = event.player;
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedBeastForms.syncDualWieldAttackSpeed(player);
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());

        // Track breathes value changes for debugging
        double currentBreathes = player.getPersistentData().getDouble("breathes");
        if (currentBreathes > 0 && currentBreathes != data.getLastBreathesValue()) {
            if (Config.logDebug) {
                var main = player.getMainHandItem();
                net.minecraft.nbt.CompoundTag mainTag = main.getTag();
                double selectOffset = (main.isEmpty() || mainTag == null) ? 0 : (mainTag.contains("select") ? mainTag.getDouble("select") : 0);
                Log.debug("[PlayerTick] Breathes changed: " + data.getLastBreathesValue() + " -> " + currentBreathes +
                         " (sword: " + (main.isEmpty() ? "none" : main.getItem()) + ", select: " + selectOffset + ")");
            }
            data.setLastBreathesValue(currentBreathes);
        }

        var main = player.getMainHandItem();
        String key = main.isEmpty() ? "" : main.getItem().toString();
        net.minecraft.nbt.CompoundTag mainTag = main.getTag();
        if (!main.isEmpty() && mainTag != null && mainTag.contains("select")) {
            key += "#select_" + mainTag.getDouble("select");
        }

        if (!key.equals(data.getLastSwordKey())) {
            Log.debug("[PlayerTick] Sword key changed! Old: '{}', New: '{}' - RESETTING variation index", data.getLastSwordKey(), key);
            data.setLastSwordKey(key);
            data.setCurrentVariationIndex(0);
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
                    sp
                );
            }
        }

        // TRAINING SWORD FAILSAFE: If player is holding a training sword and the form is not first form, reset it
        // This catches cases where the base mod's form cycling slips through (e.g., when holding down the cycle key)
        if (!main.isEmpty() && com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.isTrainingSword(main)) {
            // Keep training sword combat profile valid (fixes legacy -1 damage training swords in-place).
            com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.ensureTrainingCombatProfile(main);
            com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.sanitizeBaseModTrainingSwordState(player, main);

            // Use getFirstFormForTrainingSword which handles black swords specially (uses water breathing)
            double firstForm = com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.getFirstFormForTrainingSword(main, currentBreathes);

            // Check if the current form is not the first form (allow for encoding - compare base form IDs)
            double currentFormId = currentBreathes;
            if (com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.isEncoded(currentBreathes)) {
                currentFormId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(currentBreathes);
            }

            boolean transientBaseCombatState =
                com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper
                    .isTransientBaseModCombatBreathes(currentFormId, firstForm);

            if (currentBreathes > 0 && currentFormId != firstForm && !transientBaseCombatState) {
                Log.debug("[TrainingSword] Detected non-first form on training sword! Current: {} (formId={}), First form: {} - RESETTING",
                    currentBreathes, currentFormId, firstForm);

                // Reset to first form using the helper
                com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.resetToFirstForm(main, player);
            }
        }

        // SPRINT ANIMATION SYNC: Detect when player starts/stops sprinting with a nichirin sword
        if (Config.enableNichirinSprintAnimation && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean holdingNichirinSword = main.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem ||
                                          (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getSword(main.getItem()) != null);
            boolean isTrainingSword = com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.isTrainingSword(main);
            boolean isSprintingWithSword = player.isSprinting() && holdingNichirinSword;
            boolean wasSprintingWithSword = data.wasSprintingWithSword();

            if (isSprintingWithSword != wasSprintingWithSword) {
                data.setWasSprintingWithSword(isSprintingWithSword);

                if (isSprintingWithSword) {
                    net.minecraft.resources.ResourceLocation sprintAnimationId = isTrainingSword
                        ? net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "sprint_noob")
                        : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint2");

                    // Player started sprinting with sword - send looping sprint animation to nearby players
                    // NOTE: Server doesn't need to look up animation length - client will handle it
                    // Using a safe default length estimate (40 ticks = 2 seconds)
                    com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket packet =
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket(
                            player.getUUID(),
                            sprintAnimationId,
                            0,
                            40, // Default animation length
                            true, // looping
                            false, // not stopping
                            null, // Animation will be looked up on client side
                            1.0f, // normal speed
                            200 // sprint layer priority
                        );
                    // Send only to nearby OTHER players. The local player uses SprintAnimationHandler
                    // and should not receive a server-side sprint override packet.
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                    for (net.minecraft.server.level.ServerPlayer nearbyPlayer : level.players()) {
                        if (nearbyPlayer.getUUID().equals(serverPlayer.getUUID())) {
                            continue;
                        }
                        if (nearbyPlayer.distanceToSqr(player) <= (64.0 * 64.0)) {
                            ModNetworking.sendToPlayer(packet, nearbyPlayer);
                        }
                    }
                } else {
                    // Player stopped sprinting or switched items - send stop packet
                    com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket stopPacket =
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket(
                            player.getUUID(),
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint2"),
                            0,
                            20,
                            false,
                            true // stopping
                        );
                    // Send only to nearby OTHER players.
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
                    for (net.minecraft.server.level.ServerPlayer nearbyPlayer : level.players()) {
                        if (nearbyPlayer.getUUID().equals(serverPlayer.getUUID())) {
                            continue;
                        }
                        if (nearbyPlayer.distanceToSqr(player) <= (64.0 * 64.0)) {
                            ModNetworking.sendToPlayer(stopPacket, nearbyPlayer);
                        }
                    }
                }
            }
        }
    }

    // Prevent infinite recursion when AOE attacks trigger more events
    private static final ThreadLocal<Boolean> IS_PROCESSING_AOE = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        try {
            // Prevent recursion from AOE attacks triggering more events
            if (IS_PROCESSING_AOE.get()) {
                return;
            }

            LivingEntity target = event.getEntity();

            // NOTE: AOE damage is now handled client-side via BreathingSwordSwingPacket
            // sent from onLeftClickEmpty and onClientLivingAttack events.
            // This server-side event is only for logging/debugging purposes.

            // Handle attack-based logging (for debugging)
            if (event.getSource().getEntity() instanceof Player attacker) {
                ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);

                if (Config.logDebug && SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                    Log.debug("Server: Attack detected with kimetsunoyaiba sword: " +
                        attacker.getName().getString() + " -> " + target.getName().getString());
                }
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
            if (Config.logDebug) {
                Log.debug("Error in onLivingAttack: " + e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) {
            if (event.getServer() != null) {
                observedServerThread = Thread.currentThread();
                lastServerTickHeartbeatMs = System.currentTimeMillis();
                SERVER_HANG_DUMPED_FOR_CURRENT_STALL.set(false);
                startServerHangWatchdogIfNeeded();
                if (!firstServerTickProbeLogged) {
                    firstServerTickProbeLogged = true;
                    Log.startupProbe("KimetsunoyaibaMultiplayer.onServerTick");
                }
                ServerLevel overworld = event.getServer().overworld();

                // Update flying crows ONCE per tick (not per dimension)
                CrowEnhancementHandler.tick(overworld);

                // Tick breathing technique ability scheduler in every loaded dimension.
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler.tickAll(event.getServer());

                // Scan for unmirrored crows every 5 seconds (100 ticks)
                // Uses batching internally to avoid freezing the server
                if (overworld.getGameTime() % 100 == 0) {
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowMirrorHandler.scanForUnmirroredCrows(overworld);
                }
            }
        }
    }

    private static void startServerHangWatchdogIfNeeded() {
        if (!SERVER_HANG_WATCHDOG_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread watchdog = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000L);
                    long heartbeatMs = lastServerTickHeartbeatMs;
                    Thread serverThread = observedServerThread;
                    if (heartbeatMs == 0L || serverThread == null) {
                        continue;
                    }

                    long stallMs = System.currentTimeMillis() - heartbeatMs;
                    if (stallMs < SERVER_HANG_WATCHDOG_THRESHOLD_MS) {
                        SERVER_HANG_DUMPED_FOR_CURRENT_STALL.set(false);
                        continue;
                    }

                    if (!SERVER_HANG_DUMPED_FOR_CURRENT_STALL.compareAndSet(false, true)) {
                        continue;
                    }

                    Log.startupProbe("KimetsunoyaibaMultiplayer.serverHangWatchdog");
                    Log.alwaysWarn("[WATCHDOG] Server thread '{}' has not advanced for {} ms", serverThread.getName(), stallMs);
                    StackTraceElement[] stack = serverThread.getStackTrace();
                    if (stack.length == 0) {
                        Log.alwaysWarn("[WATCHDOG] No stack trace available for stalled server thread");
                        continue;
                    }

                    for (int i = 0; i < stack.length; i++) {
                        Log.alwaysWarn("[WATCHDOG] at {}", stack[i]);
                        if (i >= 39) {
                            Log.alwaysWarn("[WATCHDOG] stack trace truncated after {} frames", i + 1);
                            break;
                        }
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable throwable) {
                    Log.alwaysError("[WATCHDOG] Failed to inspect server thread: {}", throwable.toString());
                }
            }
        }, "KimetsunoyaibaTweaks-ServerHangWatchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Initialize GeckoLib on client side during client setup
            try {
                GeckoLib.initialize();
            } catch (Throwable t) {
                if (Config.logDebug)
                Log.error("Failed GeckoLib.initialize on client setup: {}", t.getMessage());
            }
		if (Config.logDebug)
            Log.info("Animation sync system initialized for client");
        }
        
        @SubscribeEvent
        public static void registerParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event)
        {
            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.MIST_PARTICLE.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.MistParticle.Provider::new
            );
            
            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.SMALL_MIST_PARTICLE.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.SmallMistParticle.Provider::new
            );

            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.LOVE_IMPACT.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.LoveImpactParticle.Provider::new
            );

            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.LOVE_SLASH.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.LoveSlashParticle.Provider::new
            );

            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.BLOOD.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BloodParticle.Provider::new
            );

            event.registerSpriteSet(
                com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles.ENERGY.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.particles.EnergyParticle.Provider::new
            );
        }

        @SubscribeEvent
        public static void onKeyRegister(net.minecraftforge.client.event.RegisterKeyMappingsEvent event)
        {
            Log.debug("[KimetsunoyaibaMultiplayer] onKeyRegister() called - registering keybindings");
            //event.register(com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_BREATHING_FORM);
            event.register(com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_BREATHING_FORM_BACKWARD);
            event.register(com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_FORM_VARIATION);
            Log.debug("[KimetsunoyaibaMultiplayer] Registered breathing technique cycling key bindings");
            Log.debug("[KimetsunoyaibaMultiplayer] CYCLE_FORM_VARIATION bound to: {}",
                    com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_FORM_VARIATION.getKey().getName());
            if (Config.logDebug) {
                Log.info("Registered breathing technique cycling key bindings");
                Log.info("CYCLE_FORM_VARIATION bound to: " +
                        com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_FORM_VARIATION.getKey().getName());
            }
        }

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event)
        {
            // Register GeckoLib entity renderers
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.GHOSTLY_CLONE.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.GhostlyCloneRenderer::new);

            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.MUGEN_DOOR.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.MugenDoorRenderer::new);

            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.LOVE_SWORD_SLASHES.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.LoveSwordSlashesRenderer::new);

            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.LOVE_TORNADO.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.LoveTornadoRenderer::new);

            // Register renderers for generic demon slayers
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_SLAYER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonSlayerRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_SLAYER_FEMALE.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonSlayerFemaleRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_CREEPER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonCreeperRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_VILLAGER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonVillagerRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_PILLAGER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonPillagerRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_VINDICATOR.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonVindicatorRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SWAMP_DEMON.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.SwampDemonRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SWAMP_PUDDLE.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.SwampPuddleRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SWAMP_HAND.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.SwampHandRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.MUICHIRO_FP.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.MuichiroFPRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KAZUMI.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KazumiRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.PRINCESS.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.client.PrincessRenderer::new);

            event.registerBlockEntityRenderer(
                com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities.CHEST_OF_DRAWERS.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.ChestOfDrawersRenderer::new
            );

            if (Config.logDebug)
            Log.info("Registered entity renderers");
        }

        @SubscribeEvent
        public static void registerAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event)
        {
            // Register the static GUI model for Kanroji sword
            event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "item/kanroji_sword_static"));
            // Register the static GUI model for Love sword
            event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "item/nichirinsword_love_static"));

            if (Config.logDebug)
            Log.info("Registered additional models (kanroji_sword_static, nichirinsword_love_static)");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        // Client-only imports (safe inside Dist.CLIENT annotated class)
        // These are imported here to avoid loading client classes on dedicated server

        private static int debugTickCounter = 0;

        // Register breathing form cycle handler
        static {
            MinecraftForge.EVENT_BUS.register(com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormCycleHandler.class);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END) {
                debugTickCounter++;
                if (Config.logDebug && debugTickCounter % 100 == 0) { // Log every 5 seconds
                    Log.info("Client tick event handler is working, tick: {}", debugTickCounter);
                }
                com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.tick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.IdleWalkAnimationHandler.tick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.SprintAnimationHandler.tick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordAnimationHandler.tick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.CrowQuestMarkerHandlerClient.clientTick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.tick();

                // Update gun animations for local player
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updatePlayerGunAnimation(
                            net.minecraft.client.Minecraft.getInstance().player);
                }

                // Update gun animations for all nearby entities (including mobs)
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updateAllEntityGunAnimations();
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)
        {
            // This event fires exactly once when disconnecting from a world
            com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.clearTrackedAnimations();
            com.lerdorf.kimetsunoyaibamultiplayer.client.IdleWalkAnimationHandler.clear();
            com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler.clearAllAnimations();
            com.lerdorf.kimetsunoyaibamultiplayer.client.CrowQuestMarkerHandlerClient.clearAllMarkers();
            CrowEnhancementHandler.clearFlyingCrows();
            com.lerdorf.kimetsunoyaibamultiplayer.client.CrowAnimatableWrapper.clearAll();
            com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.clearAll();
            com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.clearAll();
            com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.clearAll();
            com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker.clearAll();
            // Don't clear mirrors from client side - they are server-side entities
            // They will be cleared when the server shuts down or dimension unloads
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(net.minecraftforge.client.event.RegisterClientCommandsEvent event)
        {
            com.lerdorf.kimetsunoyaibamultiplayer.client.ClientCommandHandler.onRegisterClientCommands(event);
        }

        @SubscribeEvent
        public static void onClientLivingAttack(LivingAttackEvent event)
        {
            try {
                // IMPORTANT: This event fires on BOTH client and server threads!
                // We MUST check that we're on the logical client side
                if (!event.getEntity().level().isClientSide()) {
                    return; // Skip server-side events
                }

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null || mc.level == null) {
                    return;
                }
                
                if (event.getEntity().getTags().contains("CheckForAttack")) {
                	event.getEntity().removeTag("CheckForAttack");
                	event.getEntity().addTag("DidAttack");
                }

                // ONLY handle attacks by the LOCAL PLAYER
                // Other players' and mobs' gun attacks should NOT trigger effects here
                // (they would be handled via network sync or other mechanisms)
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    // Only process if it's the local player attacking
                    if (!attacker.getUUID().equals(mc.player.getUUID())) {
                        return; // Not the local player - ignore
                    }

                    ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
                    boolean isBaseTrainingSword =
                        com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.isTrainingSword(weapon) &&
                        !(weapon.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem);

                    // Check for gun attacks by LOCAL PLAYER ONLY
                    if (com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.isGun(weapon)) {
                        com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType gunType =
                                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.getGunType(attacker);

                        // Play shoot animation for local player
                        com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.playShootAnimation(
                                mc.player, gunType);

                        if (Config.logDebug) {
                            Log.debug("Triggered gun shoot animation for local player: " + gunType);
                        }
                        return;
                    }

                    // Check for breathing sword attacks (entity hit)
                    if (attacker instanceof net.minecraft.client.player.AbstractClientPlayer clientAttacker) {
                        // Base-mod training swords should never trigger breathing-form execution on left-click.
                        if (isBaseTrainingSword) {
                            return;
                        }

                        // Play attack animation for breathing swords (both our mod and kimetsunoyaiba)
                        String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(clientAttacker);
                        boolean isKimetsuSword = SwordParticleMapping.isKimetsunoyaibaSword(weapon);

                        if (isKimetsuSword) {
                            String slashAnimation = (animationName != null && !animationName.isEmpty()) ? animationName : "sword_to_left";
                            ModNetworking.sendToServer(new BreathingSwordSwingPacket(slashAnimation));
                        }

                        // Set the left-click attack flag (sticky bit) for ATTACK_ONLY mode
                        if (animationName != null && isKimetsuSword) {
                            com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(clientAttacker.getUUID());

                            if (Config.logDebug) {
                                Log.debug("Set left-click attack flag for entity attack: " + animationName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Silently catch exceptions to prevent crash
                if (Config.logError) {
                    Log.error("Error in onClientLivingAttack: " + e.getMessage());
                }
            }
        }

        @SubscribeEvent
        public static void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event)
        {
            // Monitor chat for crow quest messages
            String message = event.getMessage().getString();
            com.lerdorf.kimetsunoyaibamultiplayer.client.CrowQuestMarkerHandlerClient.onChatMessage(message);

            // Check if this is a breathing form cycle message from kimetsunoyaiba mod
            boolean isFormCycleMessage = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormChatFilter.shouldSuppressMessage(message);

            // CRITICAL: Don't cache variation messages! Only cache base form messages from R key cycling.
            // Check if message contains a registered variation name OR has "(X/Y)" counter format
            boolean isVariationMessage = message.matches(".*\\(\\d+/\\d+\\).*") ||
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.containsVariationName(message);

            if (isFormCycleMessage) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    ItemStack heldSword = mc.player.getMainHandItem();
                    if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(heldSword)) {

                        if (!isVariationMessage) {
                            // BASE FORM: Cache in both sword-specific and form-specific caches
                            // Store per-sword so switching between swords remembers each sword's form
                            com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.updateDisplayTextFromChat(
                                mc.player.getUUID(), heldSword, message);

                            // CRITICAL: Parse and cache breathes value from chat message
                            // Client-side player NBT doesn't have breathes, so chat is our source of truth!
                            com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.BreathingInfo info =
                                com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.parseDisplayText(message);
                            if (info != null) {
                                // Correct out-of-range breathes (e.g., 602 → 102)
                                double displayBreathes = info.fullBreathesValue;
                                int expectedRange = com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping.getExpectedRangeForSword(heldSword);
                                int actualRange = ((int)displayBreathes / 100) * 100;

                                double trueBreathes = displayBreathes;
                                if (expectedRange > 0 && actualRange == expectedRange + 500) {
                                    trueBreathes = displayBreathes - 500;
                                }

                                int trueFormId = (int) trueBreathes;

                                // Cache BOTH the breathes value AND the display text in form-specific cache
                                if (trueFormId > 0) {
                                    com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.updateForm(
                                        mc.player.getUUID(), heldSword, trueBreathes);
                                    com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.updateDisplayTextForForm(
                                        mc.player.getUUID(), trueFormId, info.originalColoredText);

                                    if (Config.logDebug) {
                                        Log.debug("[ChatCache] Cached BASE form " + trueFormId + " (display=" + displayBreathes +
                                                 ", corrected=" + trueBreathes + "): " + info.originalColoredText);
                                    }
                                }
                            }
                        } else {
                            // VARIATION: Cache ONLY in sword-specific cache (for onscreen display)
                            // Do NOT cache in form-specific cache (so form cycling still uses base form name)
                            // Strip the counter "(X/Y)" from the message since the display overlay adds it separately
                            String messageWithoutCounter = message.replaceAll("\\s*\\(\\d+/\\d+\\)\\s*", "");
                            com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.updateDisplayTextFromChat(
                                mc.player.getUUID(), heldSword, messageWithoutCounter);

                            if (Config.logDebug) {
                                Log.debug("[ChatCache] Cached VARIATION (stripped counter) in sword-specific cache: " + messageWithoutCounter);
                            }
                        }
                    }
                }

                // THEN: Suppress chat messages based on type
                // - Base form messages: suppress if config enabled
                // - Variation messages: NEVER suppress (user always wants to see variation names)
                if (Config.suppressFormCycleChat && !isVariationMessage) {
                    event.setCanceled(true);
                    if (Config.logDebug) {
                        Log.debug("Suppressed base form chat message: " + message);
                    }
                } else if (Config.logDebug && isVariationMessage) {
                    Log.debug("Allowing variation message to show in chat: " + message);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event)
        {
            com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingDisplayOverlay.onRenderGuiOverlay(event);
        }

        /**
         * Handle left-click attacks (works for both air and entity clicks)
         * This is called BEFORE LivingAttackEvent, so we can handle air clicks here
         */
        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void onLeftClickEmpty(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event)
        {
            try {
                // Only handle attack key
                if (!event.isAttack()) {
                    return;
                }

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null || mc.level == null) {
                    return;
                }

                ItemStack heldItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean isBaseTrainingSword =
                    com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper.isTrainingSword(heldItem) &&
                    !(heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem);

                // Check for Fifth Form attack detection (CheckForAttack tag)
                if (mc.player.getTags().contains("CheckForAttack")) {
                	mc.player.addTag("DidAttack");
                	if (Config.logDebug) {
                		Log.debug("Fifth Form: Player attacked while invisible (air click)");
                	}
                }

                if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem)) {
                	// Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                    com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                    if (Config.logDebug) {
                        Log.debug("Set left-click attack flag for breathing sword left-click for kimetsynoyaibaSword");
                    }
                }
                
                // Check if holding breathing sword from our mod - play attack animation and handle AOE
                if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
                    String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(mc.player);

                    // Only send packet and set sticky bit if animation was actually played (cooldown check passed)
                    if (animationName != null) {
                        ModNetworking.sendToServer(new BreathingSwordSwingPacket(animationName)); // For AOE damage (only our mod's swords)

                        // Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                        com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                        if (Config.logDebug) {
                            Log.debug("Set left-click attack flag for breathing sword left-click: " + animationName);
                        }
                    }
                }
                // Check if holding nichirinsword from kimetsunoyaiba mod - only handle particles, no AOE
                else if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem) &&
                         !isBaseTrainingSword &&
                         heldItem.getItem().toString().contains("nichirinsword")) {

                    // Play similar attack animation for kimetsunoyaiba swords
                    String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(mc.player);
                    String slashAnimation = (animationName != null && !animationName.isEmpty()) ? animationName : "sword_to_left";

                    // Send packet so nearby clients can render this player's slash.
                    ModNetworking.sendToServer(new BreathingSwordSwingPacket(slashAnimation));

                    // Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                    if (animationName != null) {
                        com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                        if (Config.logDebug) {
                            Log.debug("Set left-click attack flag for nichirinsword left-click: " + animationName);
                        }
                    }
                }

                // Check if holding rifle
                if (heldItem.getItem().toString().contains("rifle")) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType gunType =
                            com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType.RIFLE;

                    // Play shoot animation and effects
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.playShootAnimation(
                            mc.player, gunType);

                    if (Config.logDebug) {
                        Log.debug("Triggered rifle shoot animation (air click)");
                    }
                }
            } catch (Exception e) {
                // Silently catch exceptions to prevent crash
                // The exception likely occurs due to threading or mod conflicts
                if (Config.logDebug) {
                    Log.error("Error in onLeftClickEmpty: " + e.getMessage());
                }
            }
        }
    }
}
