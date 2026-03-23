package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.CrowGeoRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.AfterImageRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.CushionSeatRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonSlayerFemaleRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.DemonSlayerRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.FlowerPetalSlashRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.GhostlyCloneRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanataRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanaeRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanawoRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KiriyaRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanrojiRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.MuichiroRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.MuichiroFPRenderer;
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

        // Register renderer for ghostly clone entity (Mist Breathing 7th Form effect)
        event.registerEntityRenderer(ModEntities.GHOSTLY_CLONE.get(), GhostlyCloneRenderer::new);

        // Register renderer for Muichiro Tokito
        event.registerEntityRenderer(ModEntities.MUICHIRO.get(), MuichiroRenderer::new);
        event.registerEntityRenderer(ModEntities.MUICHIRO_FP.get(), MuichiroFPRenderer::new);

        // Register renderer for Mitsuri Kanroji
        event.registerEntityRenderer(ModEntities.KANROJI.get(), KanrojiRenderer::new);

        // Register renderers for Kanae and Kanawo (Flower Breathing)
        event.registerEntityRenderer(ModEntities.KANAE.get(), KanaeRenderer::new);
        event.registerEntityRenderer(ModEntities.KANAWO.get(), KanawoRenderer::new);
        event.registerEntityRenderer(ModEntities.KANATA.get(), KanataRenderer::new);
        event.registerEntityRenderer(ModEntities.KIRIYA.get(), KiriyaRenderer::new);

        // Register renderer for after image entity (Flower Breathing 7th Form effect)
        event.registerEntityRenderer(ModEntities.AFTER_IMAGE.get(), AfterImageRenderer::new);

        // Register renderer for flower petal slash entity (Flower Breathing slash effect)
        event.registerEntityRenderer(ModEntities.FLOWER_PETAL_SLASH.get(), FlowerPetalSlashRenderer::new);

        // Register no-op renderer for invisible cushion seat mount
        event.registerEntityRenderer(ModEntities.CUSHION_SEAT.get(), CushionSeatRenderer::new);

        // Register renderers for generic demon slayers
        event.registerEntityRenderer(ModEntities.DEMON_SLAYER.get(), DemonSlayerRenderer::new);
        event.registerEntityRenderer(ModEntities.DEMON_SLAYER_FEMALE.get(), DemonSlayerFemaleRenderer::new);

        if (Config.logDebug)
        	Log.info("Entity renderers registered successfully");
    }
}
