package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders crow quest markers as persistent emissive line geometry. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CrowQuestMarkerRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final RenderType RENDER_TYPE = WorldLineRenderUtil.renderType();
    private static final int COMPLETION_TICKS = 20;
    private static CompletionEffect completionEffect;
    private static BossArrowEffect bossArrowEffect;

    private CrowQuestMarkerRenderer() {
    }

    public static void showCompletion(Vec3 location) {
        if (MINECRAFT.level != null) {
            completionEffect = new CompletionEffect(location, MINECRAFT.level.getGameTime());
        }
    }

    public static void setBossArrow(Vec3 target, long durationTicks) {
        if (MINECRAFT.level != null && durationTicks > 0L) {
            bossArrowEffect = new BossArrowEffect(target,
                MINECRAFT.level.getGameTime() + durationTicks, MINECRAFT.level);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MINECRAFT.level == null || MINECRAFT.player == null
            || !EntityConfig.crowEnhancementsEnabled) {
            return;
        }

        CrowQuestMarkerHandler.QuestMarker marker = CrowQuestMarkerHandler.getQuestMarker(
            MINECRAFT.player.getUUID());
        long gameTime = MINECRAFT.level.getGameTime();
        float partialTick = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
        boolean rendered = false;

        if (marker != null && !marker.isExpired(gameTime)) {
            Player player = MINECRAFT.player;
            if (EntityConfig.crowQuestArrowEnabled) {
                renderQuestArrow(buffer, matrix, normal, cameraPosition, player, marker.targetLocation);
                rendered = true;
            }
            if (EntityConfig.crowWaypointEnabled) {
                renderWaypoint(buffer, matrix, normal, cameraPosition, marker.targetLocation,
                    gameTime + partialTick);
                rendered = true;
            }
        }

        if (bossArrowEffect != null) {
            if (MINECRAFT.level != bossArrowEffect.level || gameTime >= bossArrowEffect.expirationTime) {
                bossArrowEffect = null;
            } else {
                renderQuestArrow(buffer, matrix, normal, cameraPosition, MINECRAFT.player,
                    bossArrowEffect.target);
                rendered = true;
            }
        }

        if (completionEffect != null) {
            double progress = (gameTime + partialTick - completionEffect.startTime) / COMPLETION_TICKS;
            if (progress >= 1.0D || MINECRAFT.level != completionEffect.level) {
                completionEffect = null;
            } else if (progress >= 0.0D) {
                renderCompletion(buffer, matrix, normal, cameraPosition,
                    completionEffect.location, progress);
                rendered = true;
            }
        }

        if (rendered) {
            buffers.endBatch(RENDER_TYPE);
        }
    }

    private static void renderQuestArrow(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                         Vec3 camera, Player player, Vec3 target) {
        Vec3 eye = player.getEyePosition(MINECRAFT.getFrameTime());
        Vec3 toTarget = target.subtract(eye);
        if (toTarget.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 direction = toTarget.normalize();
        Vec3 start = eye.add(direction.scale(0.5D));
        double length = EntityConfig.crowArrowLength;
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera, start, direction, length,
            WorldLineRenderUtil.WHITE, 0.22F, 0.09F, 0.28D, 0.2D);
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera, start, direction, length,
            WorldLineRenderUtil.WHITE, 0.95F, 0.025F, 0.28D, 0.2D);
    }

    private static void renderWaypoint(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                       Vec3 camera, Vec3 target, double time) {
        Vec3 base = target.add(0.0D, 0.5D, 0.0D);
        WorldLineRenderUtil.drawCircle(buffer, matrix, normal, camera, base, 0.8D, 32,
            WorldLineRenderUtil.GLOW, 0.28F, 0.13F);
        WorldLineRenderUtil.drawCircle(buffer, matrix, normal, camera, base, 0.8D, 32,
            WorldLineRenderUtil.WHITE, 0.9F, 0.035F);

        Vec3 previousHelix = null;
        for (int i = 0; i <= 20; i++) {
            double y = target.y + i * 0.5D;
            double angle = (time + i * 18.0D) * 0.1D;
            Vec3 point = new Vec3(target.x + Math.cos(angle) * 0.5D, y,
                target.z + Math.sin(angle) * 0.5D);
            if (previousHelix != null) {
                WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, previousHelix, point,
                    WorldLineRenderUtil.WHITE, 0.22F, 0.12F, 0.13F, 0.08F);
                WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, previousHelix, point,
                    WorldLineRenderUtil.WHITE, 0.95F, 0.75F, 0.035F, 0.025F);
            }
            previousHelix = point;
        }

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2.0D * i / 10.0D;
            Vec3 start = new Vec3(target.x + Math.cos(angle) * 0.42D, target.y + 0.5D,
                target.z + Math.sin(angle) * 0.42D);
            Vec3 end = new Vec3(start.x, target.y + 10.0D, start.z);
            WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, start, end,
                WorldLineRenderUtil.FLAME, 0.28F, 0.0F, 0.09F, 0.02F);
            WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, start, end,
                WorldLineRenderUtil.FLAME, 0.78F, 0.0F, 0.025F, 0.01F);
        }

        Vec3 coreStart = target.add(0.0D, 0.5D, 0.0D);
        Vec3 coreEnd = target.add(0.0D, 10.0D, 0.0D);
        WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, coreStart, coreEnd,
            WorldLineRenderUtil.FLAME, 0.42F, 0.0F, 0.12F, 0.01F);
    }

    private static void renderCompletion(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                         Vec3 camera, Vec3 location, double progress) {
        float alpha = (float) (1.0D - progress);
        double radius = 0.35D + progress * 2.0D;
        Vec3 center = location.add(0.0D, 0.35D + progress * 1.2D, 0.0D);
        WorldLineRenderUtil.drawCircle(buffer, matrix, normal, camera, center, radius, 32,
            WorldLineRenderUtil.HAPPY, alpha * 0.35F, 0.12F);
        WorldLineRenderUtil.drawCircle(buffer, matrix, normal, camera, center, radius, 32,
            WorldLineRenderUtil.WHITE, alpha, 0.035F);

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            Vec3 start = location.add(Math.cos(angle) * 0.25D, 0.5D, Math.sin(angle) * 0.25D);
            Vec3 end = location.add(Math.cos(angle) * (0.5D + progress * 1.5D),
                0.5D + progress * 2.0D, Math.sin(angle) * (0.5D + progress * 1.5D));
            WorldLineRenderUtil.drawSegment(buffer, matrix, normal, camera, start, end,
                WorldLineRenderUtil.HAPPY, alpha, alpha * 0.1F, 0.06F, 0.01F);
        }
    }

    private record CompletionEffect(Vec3 location, long startTime, net.minecraft.client.multiplayer.ClientLevel level) {
        private CompletionEffect(Vec3 location, long startTime) {
            this(location, startTime, MINECRAFT.level);
        }
    }

    private record BossArrowEffect(Vec3 target, long expirationTime,
                                   net.minecraft.client.multiplayer.ClientLevel level) {
    }
}
