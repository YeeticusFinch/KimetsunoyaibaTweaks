package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanaeRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanawoRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KanrojiRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles resource reload events to clear cached model resources.
 * This ensures that when players reload resourcepacks (F3+T),
 * custom entity models are properly updated.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ResourceReloadHandler {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2) -> {
            return preparationBarrier.wait(null).thenRunAsync(() -> {
                // Clear model caches when resources are reloaded
                KanrojiRenderer.clearModelCache();
                KanaeRenderer.clearModelCache();
                KanawoRenderer.clearModelCache();
                Log.info("[ResourceReloadHandler] Cleared entity model caches after resource reload");
            }, executor2);
        });
    }
}
