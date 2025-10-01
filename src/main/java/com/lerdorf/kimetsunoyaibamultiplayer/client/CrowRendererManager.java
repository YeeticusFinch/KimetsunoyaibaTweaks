package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.CrowGeoRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Manages renderer registration for our GeckoLib crow entity
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CrowRendererManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    	if (Config.logDebug)
    		LOGGER.info("Registering GeckoLib crow renderer...");

        // Register renderer for our GeckolibCrowEntity
        event.registerEntityRenderer(ModEntities.GECKOLIB_CROW.get(), CrowGeoRenderer::new);
        if (Config.logDebug)
        	LOGGER.info("GeckoLib crow renderer registered successfully");
    }
}