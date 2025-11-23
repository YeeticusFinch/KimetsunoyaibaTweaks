package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordAnimationHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side tick events.
 * (Whip physics and particles have been removed - Kanroji sword now uses GeckoLib animations)
 */
@Mod.EventBusSubscriber(
    modid = "kimetsunoyaibamultiplayer",
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT
)
public class ClientTickEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Update Kanroji sword animations based on player/entity movement
            KanrojiSwordAnimationHandler.tick();
        }
    }
}
