package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ShimmerCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LoveTornadoModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    private static final ThreadLocal<Boolean> LATE_RENDER_PASS = ThreadLocal.withInitial(() -> false);
    private boolean renderingBloomPass;
    private static final float PINK_RED = 1.0F;
    private static final float PINK_GREEN = 0.28F;
    private static final float PINK_BLUE = 0.65F;
    private static final float BLOOM_GREEN = 0.41F;
    private static final float BLOOM_BLUE = 0.71F;

    public LoveTornadoRenderer(EntityRendererProvider.Context context) {
        super(context, new LoveTornadoModel());
        this.shadowRadius = 0.0F;
    }

    /**
     * Render after translucent world geometry, including water. The regular entity pass
     * is skipped so the tornado is not drawn twice.
     */
    public static void renderAfterTranslucent(LoveTornadoEntity entity, float partialTick,
                                              PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                              Vec3 cameraPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        LATE_RENDER_PASS.set(true);
        try {
            dispatcher.render(entity, x - cameraPosition.x, y - cameraPosition.y, z - cameraPosition.z,
                    yaw, partialTick, poseStack, bufferSource,
                    dispatcher.getPackedLightCoords(entity, partialTick));
        } finally {
            LATE_RENDER_PASS.set(false);
        }
    }

    @Override
    public void render(LoveTornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (!LATE_RENDER_PASS.get()) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        ShimmerCompat.updateTornadoLight(entity, partialTick, 0xFF3FA3, 8.0F);
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
                        partialTick, 0xF000F0, packedOverlay,
                        PINK_RED,
                        renderingBloomPass ? BLOOM_GREEN : PINK_GREEN,
                        renderingBloomPass ? BLOOM_BLUE : PINK_BLUE,
                        alpha);
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
