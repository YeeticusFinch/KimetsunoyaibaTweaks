package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ShimmerCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.ThunderDragonModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ThunderDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Emissive GeckoLib renderer for the Flaming Thunder God dragon. */
public final class ThunderDragonRenderer extends GeoEntityRenderer<ThunderDragonEntity> {
    private boolean renderingBloomPass;

    public ThunderDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new ThunderDragonModel());
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ThunderDragonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        ShimmerCompat.updateTornadoLight(entity, partialTick, 0xFFD21A, 12.0F);
        ShimmerCompat.postEntityBloom(poseStack, (bloomPoseStack, bloomBufferSource) -> {
            renderingBloomPass = true;
            try {
                super.render(entity, entityYaw, partialTick, bloomPoseStack, bloomBufferSource, packedLight);
            } finally {
                renderingBloomPass = false;
            }
        });
    }

    @Override
    public RenderType getRenderType(ThunderDragonEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    @Override
    public int getPackedOverlay(ThunderDragonEntity animatable, float u, float partialTick) {
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void preRender(PoseStack poseStack, ThunderDragonEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, float red, float green,
                          float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
            partialTick, 0xF000F0, packedOverlay,
            renderingBloomPass ? 1.0F : red,
            renderingBloomPass ? 0.82F : green,
            renderingBloomPass ? 0.08F : blue,
            alpha);
    }
}
