package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.model.CrowGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for GeckolibCrowEntity
 */
public class CrowGeoRenderer extends GeoEntityRenderer<GeckolibCrowEntity> {
    private static boolean hasLoggedRender = false;

    public CrowGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrowGeoModel());
        this.shadowRadius = 0.3f;
        if (Config.logDebug)
        	Log.info("CrowGeoRenderer initialized with GeckoLib model");
    }

    @Override
    public void render(GeckolibCrowEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!hasLoggedRender) {
        	if (Config.logDebug) {
	            Log.info("=== CrowGeoRenderer.render() CALLED ===");
	            Log.info("Entity: {}", entity.getName().getString());
	            Log.info("Entity type: {}", entity.getType());
	            Log.info("Entity position: {}, {}, {}", entity.getX(), entity.getY(), entity.getZ());
        	}
            hasLoggedRender = true;
        }

        // Apply red tint if hurt
        if (entity.isHurt()) {
            // Red overlay handled by GeckoLib's color modification
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public int getPackedOverlay(GeckolibCrowEntity entity, float u) {
        // Apply damage flash overlay when hurt
        if (entity.isHurt()) {
            return net.minecraft.client.renderer.texture.OverlayTexture.pack(
                net.minecraft.client.renderer.texture.OverlayTexture.u(u),
                net.minecraft.client.renderer.texture.OverlayTexture.v(true) // true = hurt
            );
        }
        return super.getPackedOverlay(entity, u);
    }
}