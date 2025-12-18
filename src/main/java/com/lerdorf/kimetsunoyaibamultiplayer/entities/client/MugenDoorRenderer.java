package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.MugenDoorModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDoorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Mugen Door entity.
 *
 * The mugen door is a decorative entity that spawns before kizuki demons in raids.
 * It plays an "open" animation and then disappears after a short time.
 */
public class MugenDoorRenderer extends GeoEntityRenderer<MugenDoorEntity> {
    public MugenDoorRenderer(EntityRendererProvider.Context context) {
        super(context, new MugenDoorModel());
    }

    @Override
    public int getPackedOverlay(MugenDoorEntity animatable, float u, float partialTick) {
        // No overlay effects
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }
}
