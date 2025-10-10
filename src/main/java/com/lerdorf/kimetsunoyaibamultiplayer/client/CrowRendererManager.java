package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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
/**
 * Manages renderer registration for custom entities
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CrowRendererManager {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    	if (Config.logDebug)
    		Log.info("Registering entity renderers...");

        // Register renderer for our GeckolibCrowEntity
        event.registerEntityRenderer(ModEntities.GECKOLIB_CROW.get(), CrowGeoRenderer::new);

        if (Config.logDebug)
        	Log.info("Entity renderers registered successfully");
    }
}