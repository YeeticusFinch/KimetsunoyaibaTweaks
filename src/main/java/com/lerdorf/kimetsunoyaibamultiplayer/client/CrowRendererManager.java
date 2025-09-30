package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.CrowGeoRenderer;
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
 * Manages replacing the kasugai_crow renderer with our GeckoLib renderer
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CrowRendererManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("Registering custom crow renderer...");

        // Find the kasugai_crow entity type
        EntityType<?> crowType = ForgeRegistries.ENTITY_TYPES.getValue(
            net.minecraft.resources.ResourceLocation.tryBuild("kimetsunoyaiba", "kasugai_crow")
        );

        if (crowType != null) {
            LOGGER.info("Found kasugai_crow entity type, registering GeckoLib renderer");
            // Register our GeckoLib renderer for the crow
            // We need to create a renderer that wraps entities in CrowAnimatableWrapper
            event.registerEntityRenderer(crowType, context -> new CrowGeoRenderer(context));
            LOGGER.info("GeckoLib renderer registered successfully for kasugai_crow");
        } else {
            LOGGER.warn("Could not find kasugai_crow entity type - renderer not registered");
        }
    }

}

// Separate event bus subscriber for forcing crow renderer override after all mods load
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
class CrowRendererForcer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // Queue this to run after all mod setup is complete
        event.enqueueWork(() -> {
            LOGGER.info("Forcing crow renderer override after client setup...");

            // Find the kasugai_crow entity type
            EntityType<?> crowType = ForgeRegistries.ENTITY_TYPES.getValue(
                net.minecraft.resources.ResourceLocation.tryBuild("kimetsunoyaiba", "kasugai_crow")
            );

            if (crowType != null) {
                try {
                    // Force override the renderer to use our GeckoLib version
                    EntityRenderers.register(crowType, CrowGeoRenderer::new);
                    LOGGER.info("Successfully forced crow renderer override to GeckoLib renderer");
                } catch (Exception e) {
                    LOGGER.error("Failed to force crow renderer override", e);
                }
            } else {
                LOGGER.warn("Could not find kasugai_crow entity type for forced override");
            }
        });
    }
}