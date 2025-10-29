package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModel;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for sword slash effects using model textures
 * Renders models at specified positions with rotation and scaling
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashRenderer {

    // Cache of loaded models by model key
    private static final Map<String, SwordSlashModel> MODEL_CACHE = new HashMap<>();

    /**
     * Renders a sword slash model at the specified position with rotation
     *
     * @param poseStack The pose stack for transformations
     * @param bufferSource The buffer source for rendering
     * @param position World position to render at
     * @param yaw Yaw rotation in degrees
     * @param pitch Pitch rotation in degrees
     * @param roll Roll rotation in degrees
     * @param scale Scale factor
     * @param progress Animation progress (0.0 to 1.0)
     * @param modelKey Model key (e.g., "mist", "generic")
     * @param packedLight Light value
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 position,
                            float yaw, float pitch, float roll, float scale, float progress,
                            String modelKey, int packedLight) {

        if (!SwordSwingConfig.useSwordSwingModel) {
            return;
        }

        try {
            // Get or create model
            SwordSlashModel model = getModel(modelKey);
            if (model == null) {
                Log.warn("Failed to get model for key: " + modelKey);
                return;
            }

            poseStack.pushPose();

            // Translate to world position
            poseStack.translate(position.x, position.y, position.z);

            // Apply rotations (order: yaw -> pitch -> roll)
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw+SwordSwingConfig.globalYawOffset)); // Yaw (around Y axis)
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch+SwordSwingConfig.globalPitchOffset)); // Pitch (around X axis)
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll+SwordSwingConfig.globalRollOffset)); // Roll (around Z axis)

            // Apply scale
            poseStack.scale(scale*SwordSwingConfig.modelScale, scale*SwordSwingConfig.modelScale, scale*SwordSwingConfig.modelScale);

            // Calculate alpha based on progress for fade in/out
            float alpha = calculateAlpha(progress);

            // Get the texture
            ResourceLocation texture = model.getTextureResource();

            // Render using GeckoLib's rendering system
            renderModel(poseStack, bufferSource, model, texture, alpha, packedLight);

            poseStack.popPose();

        } catch (Exception e) {
            Log.error("Error rendering sword slash model: " + e.getMessage());
            if (Config.logDebug) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Renders the actual model
     */
    private static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource,
                                   SwordSlashModel model, ResourceLocation texture,
                                   float alpha, int packedLight) {
        // Get the render type with transparency
        //RenderType renderType = RenderType.entityTranslucent(texture);
        RenderType renderType = RenderType.entityTranslucentEmissive(texture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Note: This is a simplified rendering approach
        // For full GeckoLib rendering, you'd need to implement a complete GeoRenderer
        // or use an entity-based approach

        // For now, log that we're attempting to render
        if (Config.logDebug) {
            Log.debug("Rendering sword slash model: " + model.getModelKey() +
                     " at alpha: " + alpha);
        }

        // TODO: Implement actual model rendering
        // This requires accessing GeckoLib's internal rendering pipeline
        // or creating a temporary entity to render through
        
        // Use GeckoLib or your own model render
        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            0xF000F0,                   // full bright light
            OverlayTexture.NO_OVERLAY,
            1f, 1f, 1f, alpha           // color + transparency
        );
    }

    /**
     * Gets or creates a cached model
     */
    private static SwordSlashModel getModel(String modelKey) {
        return MODEL_CACHE.computeIfAbsent(modelKey, key -> {
            try {
                return new SwordSlashModel(key);
            } catch (Exception e) {
                Log.error("Failed to create model for key " + key + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Calculates alpha value based on animation progress
     * Stays at full opacity for maximum visibility
     */
    private static float calculateAlpha(float progress) {
        // Keep at full opacity - no fading
        // The brief display time is enough, we don't need fading
        return 1.0f;
    }

    /**
     * Clears the model cache
     */
    public static void clearCache() {
        MODEL_CACHE.clear();
        Log.info("Cleared sword slash model cache");
    }
}
