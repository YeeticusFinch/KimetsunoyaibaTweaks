package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.FlowerPetalSlashModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlowerPetalSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Flower Petal Slash entity.
 *
 * Features:
 * - Applies yaw, pitch, and roll rotations for full 3D orientation
 * - Uses translucent emissive rendering for glowing petal effect
 * - Texture changes each tick for animation effect
 */
public class FlowerPetalSlashRenderer extends GeoEntityRenderer<FlowerPetalSlashEntity> {
    public FlowerPetalSlashRenderer(EntityRendererProvider.Context context) {
        super(context, new FlowerPetalSlashModel());
    }

    @Override
    public RenderType getRenderType(FlowerPetalSlashEntity animatable, ResourceLocation texture,
                                     MultiBufferSource bufferSource, float partialTick) {
        // Use translucent emissive blending for proper partial transparency/alpha support
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    @Override
    public int getPackedOverlay(FlowerPetalSlashEntity animatable, float u, float partialTick) {
        // No overlay effects (damage/hurt overlay)
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void preRender(PoseStack poseStack, FlowerPetalSlashEntity animatable, BakedGeoModel model,
                         MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                         float partialTick, int packedLight, int packedOverlay, float red, float green,
                         float blue, float alpha) {
        float baseScale = animatable.getScale();
        float progress = Mth.clamp(animatable.tickCount + partialTick, 0.0f, (float) FlowerPetalSlashEntity.TOTAL_FRAMES);
        float pulse = Mth.sin((float) Math.PI * (progress / FlowerPetalSlashEntity.TOTAL_FRAMES));
        float peakBoost = 0.15f;
        float renderScale = baseScale * (1.0f + peakBoost * pulse);
        poseStack.scale(renderScale, renderScale, renderScale);

        // Use full bright lighting for emissive/glowing effect
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                       partialTick, 0xF000F0, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected void applyRotations(FlowerPetalSlashEntity animatable, PoseStack poseStack,
                                   float ageInTicks, float rotationYaw, float partialTick) {
        // Get the entity's actual rotation values (interpolated for smooth rendering)
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.rotLerp(partialTick, animatable.xRotO, animatable.getXRot());
        float roll = animatable.getRoll();

        // Apply yaw rotation (horizontal) - 180.0F offset is standard for entity rendering
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

        // Apply pitch rotation (vertical tilt)
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Apply roll rotation (rotation around the direction/forward axis)
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        // Don't call super.applyRotations() as we're handling rotation manually
    }
}
