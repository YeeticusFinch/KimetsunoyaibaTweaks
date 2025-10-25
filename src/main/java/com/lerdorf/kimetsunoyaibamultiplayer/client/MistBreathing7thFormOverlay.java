package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for making the player/entity fade in and out during Mist Breathing 7th Form.
 * This makes the player indistinguishable from the ghostly clones.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public class MistBreathing7thFormOverlay {

    /**
     * Modifies the rendering alpha of entities using Mist Breathing 7th Form
     * to make them fade in and out like the ghostly clones
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // Check if entity is using Mist Breathing 7th Form
        if (!entity.getPersistentData().getBoolean("mist_7th_teleport_active")) {
            return;
        }

        // Calculate fade in/out opacity (same logic as ghostly clones)
        int tickCount = entity.tickCount;
        float fadeSpeed = 0.08f;
        int fadeCycle = (int) (1.0f / fadeSpeed) * 2; // Full fade in + fade out cycle

        // Calculate opacity based on time
        int cyclePosition = tickCount % fadeCycle;
        float opacity;

        if (cyclePosition < fadeCycle / 2) {
            // Fading in
            opacity = (cyclePosition / (float) (fadeCycle / 2)) * 0.4f;
        } else {
            // Fading out
            opacity = ((fadeCycle - cyclePosition) / (float) (fadeCycle / 2)) * 0.4f;
        }

        // Clamp opacity between 0.0 and 0.5 (same as clones)
        opacity = Math.max(0.0f, Math.min(0.5f, opacity));

        // Store the opacity for use in the render
        entity.getPersistentData().putFloat("mist_7th_render_opacity", opacity);

        // If opacity is very low, we could cancel rendering entirely
        // but it's better to let it fade to match clone behavior
        if (opacity < 0.05f) {
            // Very faint - almost invisible
            // Note: We don't cancel the event to maintain consistent behavior
        }
    }

    /**
     * Post-render to reset any rendering state
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // Clean up opacity data after rendering
        if (entity.getPersistentData().contains("mist_7th_render_opacity")) {
            entity.getPersistentData().remove("mist_7th_render_opacity");
        }
    }
}
