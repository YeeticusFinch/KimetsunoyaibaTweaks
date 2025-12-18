package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LoveTornadoModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for the Love Tornado entity.
 *
 * The love tornado is a decorative entity that displays tornado animations
 * for Love Breathing forms (particularly Fifth Form: Swaying Love, Wildclaw).
 *
 * Features:
 * - Ultra-bright rendering with full bright lighting (0xF000F0)
 * - Additive blending for shimmer/bloom effect compatibility
 * - Emissive shader for glowing appearance
 * - Custom rotation handling for precise orientation
 */
public class LoveTornadoRenderer extends GeoEntityRenderer<LoveTornadoEntity> {
    public LoveTornadoRenderer(EntityRendererProvider.Context context) {
        super(context, new LoveTornadoModel());
    }

    @Override
    public RenderType getRenderType(LoveTornadoEntity animatable, ResourceLocation texture,
                                     MultiBufferSource bufferSource, float partialTick) {
        // Use translucent emissive blending for proper color preservation
        // Combined with full bright lighting for glowing appearance
        // Same as Love Sword Slashes - bright and glowing while preserving texture colors
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    @Override
    public int getPackedOverlay(LoveTornadoEntity animatable, float u, float partialTick) {
        // No overlay effects (damage/hurt overlay)
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }

    @Override
    public void preRender(PoseStack poseStack, LoveTornadoEntity animatable, BakedGeoModel model,
                         MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                         float partialTick, int packedLight, int packedOverlay, float red, float green,
                         float blue, float alpha) {
        // Use FULL BRIGHT lighting for maximum glow effect
        // 0xF000F0 = maximum brightness (15 in both sky and block light)
        // This makes the tornado render at full brightness regardless of surrounding lighting
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                       partialTick, 0xF000F0, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected void applyRotations(LoveTornadoEntity animatable, PoseStack poseStack,
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
