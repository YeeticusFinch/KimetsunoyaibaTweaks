package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModel;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Dual-layer renderer for sword slash models
 * Renders both base texture and emissive layer for maximum glow effect
 */
@OnlyIn(Dist.CLIENT)
public class DualLayerSlashRenderer {

    /**
     * Render sword slash with emissive rendering for colored glow effect
     * Uses the original colored texture with emissive render type for Shimmer bloom
     *
     * @param flipHorizontal true to use "reverse" animation (flip texture horizontally), false for "base" animation
     * @param startTimeMillis Start time in milliseconds (for animated texture frame calculation)
     * @param durationMillis Duration in milliseconds (for animated texture frame calculation)
     */
    public static void renderDualLayer(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 position,
                                      float yaw, float pitch, float roll, float scale, float progress,
                                      String modelKey, int packedLight, boolean flipHorizontal,
                                      long startTimeMillis, int durationMillis) {

        if (!SwordSwingConfig.useSwordSwingModel) {
            return;
        }

        try {
            SwordSlashModel model = SwordSlashRenderer.getModel(modelKey);
            if (model == null) {
                return;
            }

            // Update animated texture frame based on current progress
            model.setProgress(progress);

            // Set timing for tick-based frame calculation
            model.setTiming(startTimeMillis, durationMillis);

            // Set flip flag for animation (base vs reverse)
            model.setFlipHorizontal(flipHorizontal);

            poseStack.pushPose();

            // Translate to world position
            poseStack.translate(position.x, position.y, position.z);

            // Apply rotations
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw + SwordSwingConfig.globalYawOffset));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch + SwordSwingConfig.globalPitchOffset));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll + SwordSwingConfig.globalRollOffset));

            // Apply scale
            float finalScale = scale * SwordSwingConfig.modelScale;
            poseStack.scale(finalScale, finalScale, finalScale);

            // Calculate alpha
            float alpha = 0.8f; // Slightly transparent for nice blend

            // Get base texture (keep original colors)
            ResourceLocation baseTexture = model.getTextureResource();

            // Render with emissive type so Shimmer/shaders can apply bloom
            // Use original texture to preserve colors and transparency
            renderLayer(poseStack, bufferSource, model, baseTexture, alpha, packedLight, false);

            poseStack.popPose();

        } catch (Exception e) {
            Log.error("Error in dual-layer sword slash rendering: " + e.getMessage());
            if (Config.logDebug) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Render a single layer (either base or emissive)
     */
    private static void renderLayer(PoseStack poseStack, MultiBufferSource bufferSource,
                                    SwordSlashModel model, ResourceLocation texture,
                                    float alpha, int packedLight, boolean isEmissive) {

        // Choose render type based on layer
        RenderType renderType;
        float colorMultiplier;

        if (isEmissive) {
            // Emissive layer: Additive blending for MAXIMUM brightness
            renderType = CustomRenderTypes.swordSlashAdditive(texture);
            // Extra brightness multiplier for emissive layer
            colorMultiplier = SwordSwingConfig.brightnessMultiplier;
        } else {
            // Base layer: Standard emissive rendering
            renderType = RenderType.entityTranslucentEmissive(texture);
            colorMultiplier = 1.0f; // Normal brightness for base
        }

        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Render model to buffer
        // Color multiplier applied inside model's renderToBuffer
        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            0xF000F0,  // Full bright
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            colorMultiplier, colorMultiplier, colorMultiplier, alpha
        );
    }

    /**
     * Get emissive texture path from base texture
     * e.g., "sword_slash_mist.png" -> "sword_slash_mist_e.png"
     */
    private static ResourceLocation getEmissiveTexture(ResourceLocation baseTexture) {
        String path = baseTexture.getPath();
        String emissivePath;

        if (path.endsWith(".png")) {
            // Insert "_e" before ".png"
            emissivePath = path.substring(0, path.length() - 4) + "_e.png";
        } else {
            // Append "_e"
            emissivePath = path + "_e";
        }

        return ResourceLocation.fromNamespaceAndPath(baseTexture.getNamespace(), emissivePath);
    }
}
