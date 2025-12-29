package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tracks which entity is currently being rendered by listening to entity render events.
 * This provides entity context to GeoItemRenderer instances.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EntityRenderTrackerHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            EntityRenderContext.setCurrentEntity(entity);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        EntityRenderContext.clearCurrentEntity();
    }
}
