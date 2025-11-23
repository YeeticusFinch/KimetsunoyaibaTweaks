package com.lerdorf.kimetsunoyaibamultiplayer.client.events;

import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DualLayerSlashRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSlashRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker.SlashRenderRequest;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(
	    modid = "kimetsunoyaibamultiplayer",
	    bus = Mod.EventBusSubscriber.Bus.FORGE,
	    value = Dist.CLIENT
	)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        // Get common resources
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        // Render slash models if any exist
        List<SlashRenderRequest> queue = BonePositionTracker.getRenderQueue();
        if (queue.isEmpty())
            return;

        int packedLight = 0xF000F0;

        // Render all active slash models
        for (SlashRenderRequest req : queue) {
            // Calculate current progress based on elapsed time (smooth animation)
            float progress = req.getCurrentProgress();

            // Calculate position and rotation dynamically based on animation type
            Vec3 worldPos;
            float[] rotation;

            if (req.isRawSlash) {
                // Raw slash with custom angle control
                worldPos = BonePositionTracker.calculateRawSlashPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawSlashRotation(req.entity, progress, req);
            } else if (req.isRawHorizontal) {
                // Raw horizontal slash with custom vert parameter
                worldPos = BonePositionTracker.calculateRawHorizontalPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawHorizontalRotation(req.entity, progress, req);
            } else if (req.isRawVertical) {
                // Raw vertical slash with custom hor parameter
                worldPos = BonePositionTracker.calculateRawVerticalPosition(req.entity, progress, req);
                rotation = BonePositionTracker.calculateRawVerticalRotation(req.entity, progress, req);
            } else if (req.isHorizontal) {
                worldPos = BonePositionTracker.calculateHorizontalPosition(req.entity, progress, req.leftToRight);
                rotation = BonePositionTracker.calculateHorizontalRotation(req.entity, progress, req.leftToRight);
            } else if (req.isVertical) {
                worldPos = BonePositionTracker.calculateVerticalPosition(req.entity, progress, req.upward);
                rotation = BonePositionTracker.calculateVerticalRotation(req.entity, progress, req.upward);
            } else if (req.isSpin) {
                worldPos = BonePositionTracker.calculateSpinPosition(req.entity, progress);
                rotation = BonePositionTracker.calculateSpinRotation(req.entity, progress);
            } else {
                continue; // Unknown animation type
            }

            // Convert world coordinates to camera-relative coordinates
            Vec3 cameraRelative = worldPos.subtract(camera);

            // Calculate scale (use sizeScaler for raw slashes, default 2.5f for standard slashes)
            float scale = (req.isRawSlash || req.isRawHorizontal || req.isRawVertical) ? (2.5f * req.sizeScaler) : 2.5f;

            // Render model with dual-layer system (base + emissive)
            DualLayerSlashRenderer.renderDualLayer(
                poseStack,
                bufferSource,
                cameraRelative,
                rotation[0],  // yaw
                rotation[1],  // pitch
                rotation[2],  // roll
                scale,        // Scale (adjusted by sizeScaler for raw slashes)
                progress,
                req.modelKey,
                packedLight
            );
        }

        // flush draw calls
        bufferSource.endBatch();

        // Remove old models (instead of clearing everything)
        queue.removeIf(req -> req.shouldRemove());
    }


}
