package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
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

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(TickEvent.ClientTickEvent event)
        {
            if (Minecraft.getInstance().level == null && event.phase == TickEvent.Phase.END) {
                AnimationTracker.clearTrackedAnimations();
                AnimationSyncHandler.clearAllAnimations();
            }
        }
    }
}
