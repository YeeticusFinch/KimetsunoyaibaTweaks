package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.model.CrowGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import org.slf4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for GeckolibCrowEntity
 */
public class CrowGeoRenderer extends GeoEntityRenderer<GeckolibCrowEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean hasLoggedRender = false;

    public CrowGeoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrowGeoModel());
        this.shadowRadius = 0.3f;
        LOGGER.info("CrowGeoRenderer initialized with GeckoLib model");
    }

    @Override
    public void render(GeckolibCrowEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!hasLoggedRender) {
            LOGGER.info("=== CrowGeoRenderer.render() CALLED ===");
            LOGGER.info("Entity: {}", entity.getName().getString());
            LOGGER.info("Entity type: {}", entity.getType());
            LOGGER.info("Entity position: {}, {}, {}", entity.getX(), entity.getY(), entity.getZ());
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