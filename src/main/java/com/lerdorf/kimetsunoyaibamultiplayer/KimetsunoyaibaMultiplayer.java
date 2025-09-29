package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientCommandHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimationCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestCrowQuestCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugCrowCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KimetsunoyaibaMultiplayer.MODID)
public class KimetsunoyaibaMultiplayer
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "kimetsunoyaibamultiplayer";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public KimetsunoyaibaMultiplayer(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register config event handlers on the mod event bus
        modEventBus.register(Config.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "kimetsunoyaibamultiplayer/common.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.SPEC, "kimetsunoyaibamultiplayer/particles.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.SPEC, "kimetsunoyaibamultiplayer/entities.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Initializing Kimetsunoyaiba Multiplayer animation sync...");

        // Register network messages
        ModNetworking.register();
        LOGGER.info("Network messages registered");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Kimetsunoyaiba Multiplayer server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        LOGGER.info("Registering test commands");
        TestAnimationCommand.register(event.getDispatcher());
        TestParticlesCommand.register(event.getDispatcher());
        DebugParticlesCommand.register(event.getDispatcher());
        TestAnimCommand.register(event.getDispatcher());
        TestCrowQuestCommand.register(event.getDispatcher());
        DebugCrowCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        // Handle attack-based particle triggering (for server-side events)
        if (com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleTriggerMode ==
            com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.ParticleTriggerMode.ATTACK_ONLY) {
            LivingEntity target = event.getEntity();
            if (event.getSource().getEntity() instanceof Player attacker) {
                ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
                if (SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                    LOGGER.debug("Attack detected with kimetsunoyaiba sword: {} -> {}",
                        attacker.getName().getString(),
                        target.getName().getString());
                    // Note: Particle spawning will be handled client-side via animation tracking
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) {
            // Update flying crows on each server level
            if (event.getServer() != null) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    CrowEnhancementHandler.tick(level);
                }
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("Animation sync system initialized for client");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        private static int debugTickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END) {
                debugTickCounter++;
                if (Config.logDebug && debugTickCounter % 100 == 0) { // Log every 5 seconds
                    LOGGER.info("Client tick event handler is working, tick: {}", debugTickCounter);
                }
                AnimationTracker.tick();
                CrowQuestMarkerHandler.clientTick();
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(TickEvent.ClientTickEvent event)
        {
            if (Minecraft.getInstance().level == null && event.phase == TickEvent.Phase.END) {
                AnimationTracker.clearTrackedAnimations();
                AnimationSyncHandler.clearAllAnimations();
                CrowQuestMarkerHandler.clearAllMarkers();
                CrowEnhancementHandler.clearFlyingCrows();
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(net.minecraftforge.client.event.RegisterClientCommandsEvent event)
        {
            ClientCommandHandler.onRegisterClientCommands(event);
        }

        @SubscribeEvent
        public static void onClientLivingAttack(LivingAttackEvent event)
        {
            // Client-side attack detection for immediate particle response
            if (!com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.swordParticlesEnabled ||
                com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleTriggerMode !=
                com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.ParticleTriggerMode.ATTACK_ONLY) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }

            // Check if the local player is attacking with a kimetsunoyaiba sword
            if (event.getSource().getEntity() instanceof AbstractClientPlayer attacker) {
                if (attacker.getUUID().equals(mc.player.getUUID())) {
                    ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
                    if (SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                        // Force spawn particles on attack
                        SwordParticleHandler.forceSpawnParticles(attacker, weapon, "attack");

                        if (Config.logDebug) {
                            LOGGER.debug("Triggered attack-based particles for local player");
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event)
        {
            // Monitor chat for crow quest messages
            String message = event.getMessage().getString();
            CrowQuestMarkerHandler.onChatMessage(message);
        }
    }
}
