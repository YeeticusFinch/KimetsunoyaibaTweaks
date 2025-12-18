package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LoveSwordSlashesModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Love Sword Slashes entity.
 *
 * The love sword slashes is a decorative entity that displays sword slash animations
 * for Love Breathing forms (particularly Third Form: Catlove Shower).
 *
 * Uses translucent emissive rendering for proper alpha blending with glowing appearance.
 */
public class LoveSwordSlashesRenderer extends GeoEntityRenderer<LoveSwordSlashesEntity> {
    public LoveSwordSlashesRenderer(EntityRendererProvider.Context context) {
        super(context, new LoveSwordSlashesModel());
    }

    @Override
    public RenderType getRenderType(LoveSwordSlashesEntity animatable, ResourceLocation texture,
                                     MultiBufferSource bufferSource, float partialTick) {
        // Use translucent emissive blending for proper partial transparency/alpha support
        // Combined with emissive shader for glowing appearance
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    @Override
    public int getPackedOverlay(LoveSwordSlashesEntity animatable, float u, float partialTick) {
        // No overlay effects (damage/hurt overlay)
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void preRender(PoseStack poseStack, LoveSwordSlashesEntity animatable, BakedGeoModel model,
                         MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                         float partialTick, int packedLight, int packedOverlay, float red, float green,
                         float blue, float alpha) {
        // Use full bright lighting for emissive/glowing effect
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                       partialTick, 0xF000F0, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected void applyRotations(LoveSwordSlashesEntity animatable, PoseStack poseStack,
                                   float ageInTicks, float rotationYaw, float partialTick) {
        // Get the entity's actual rotation values (interpolated for smooth rendering)
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = net.minecraft.util.Mth.rotLerp(partialTick, animatable.xRotO, animatable.getXRot());

        // Apply yaw rotation (horizontal)
        // 180.0F offset is standard for entity rendering (entities face south by default)
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yaw));

        // Apply pitch rotation (vertical tilt)
        // Positive pitch = looking down, negative = looking up
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));

        // Don't call super.applyRotations() as we're handling rotation manually
    }
}
