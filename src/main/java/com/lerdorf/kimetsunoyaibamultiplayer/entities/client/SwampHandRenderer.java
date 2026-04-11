package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwampHandModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampHandEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Swamp Hand entity.
 *
 * The swamp hand is a temporary attack effect entity that spawns, plays an attack animation,
 * deals damage at tick 10, and despawns at tick 20.
 */
public class SwampHandRenderer extends GeoEntityRenderer<SwampHandEntity> {
    public SwampHandRenderer(EntityRendererProvider.Context context) {
        super(context, new SwampHandModel());
    }

    @Override
    public int getPackedOverlay(SwampHandEntity animatable, float u, float partialTick) {
        // No overlay effects
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }
}
