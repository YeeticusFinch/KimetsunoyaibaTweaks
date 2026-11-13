package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the Kyogai field preview only for the targeted player client. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KyogaiGravityFieldRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final RenderType RENDER_TYPE = WorldLineRenderUtil.renderType();
    private static Preview preview;

    private KyogaiGravityFieldRenderer() {
    }

    public static void setPreview(AABB box, BlockPos sourcePos, Direction gravityDirection,
                                  int durationTicks) {
        if (MINECRAFT.level == null || durationTicks <= 0) {
            return;
        }
        preview = new Preview(box, sourcePos, gravityDirection,
            MINECRAFT.level.getGameTime() + durationTicks, MINECRAFT.level);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || preview == null || MINECRAFT.level == null
            || MINECRAFT.level != preview.level
            || MINECRAFT.level.getGameTime() >= preview.expirationTime) {
            if (preview != null && (MINECRAFT.level == null || MINECRAFT.level != preview.level
                || MINECRAFT.level.getGameTime() >= preview.expirationTime)) {
                preview = null;
            }
            return;
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);

        WorldLineRenderUtil.drawBox(buffer, matrix, normal, camera.getPosition(), preview.box,
            WorldLineRenderUtil.GRAVITY_OUTLINE, 0.3F, 0.11F);
        WorldLineRenderUtil.drawBox(buffer, matrix, normal, camera.getPosition(), preview.box,
            WorldLineRenderUtil.GRAVITY_OUTLINE, 1.0F, 0.028F);

        Vec3 center = Vec3.atCenterOf(preview.sourcePos);
        Vec3 direction = Vec3.atLowerCornerOf(preview.gravityDirection.getNormal());
        Vec3 start = center.subtract(direction.scale(0.18D));
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera.getPosition(), start, direction,
            0.56D, WorldLineRenderUtil.GRAVITY_ARROW, 0.28F, 0.09F, 0.14D, 0.12D);
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera.getPosition(), start, direction,
            0.56D, WorldLineRenderUtil.GRAVITY_ARROW, 1.0F, 0.025F, 0.14D, 0.12D);

        buffers.endBatch(RENDER_TYPE);
    }

    private record Preview(AABB box, BlockPos sourcePos, Direction gravityDirection,
                           long expirationTime, net.minecraft.client.multiplayer.ClientLevel level) {
    }
}
