package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Keeps Kyogai's gravity indicator visible only to the player who used the drum. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class KyogaiGravityArrowRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final RenderType RENDER_TYPE = WorldLineRenderUtil.renderType();
    private static Direction activeGravity;
    private static int activeColor;
    private static long expirationTime;

    private KyogaiGravityArrowRenderer() {
    }

    public static void show(Direction gravity, int durationTicks, int color) {
        if (MINECRAFT.level == null || durationTicks <= 0) {
            return;
        }
        activeGravity = gravity;
        activeColor = color;
        expirationTime = MINECRAFT.level.getGameTime() + durationTicks;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        Level level = MINECRAFT.level;
        if (player == null || level == null || activeGravity == null
            || level.getGameTime() >= expirationTime) {
            activeGravity = null;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || activeGravity == null || MINECRAFT.level == null || MINECRAFT.player == null
            || MINECRAFT.level.getGameTime() >= expirationTime) {
            return;
        }

        LocalPlayer player = MINECRAFT.player;
        Vec3 look = GravityDirectionHelper.getLookDirection(player);
        if (look.lengthSqr() < 1.0E-6D) {
            look = player.getLookAngle();
        }

        Vec3 center = player.getEyePosition(event.getPartialTick()).add(look.normalize().scale(3.0D));
        Vec3 direction = Vec3.atLowerCornerOf(activeGravity.getNormal());
        float[] color = new float[] {
            ((activeColor >> 16) & 0xFF) / 255.0F,
            ((activeColor >> 8) & 0xFF) / 255.0F,
            (activeColor & 0xFF) / 255.0F
        };

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
        Vec3 start = center.subtract(direction.normalize().scale(0.3D));
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera.getPosition(), start, direction,
            0.9D, color, 0.3F, 0.11F, 0.25D, 0.16D);
        WorldLineRenderUtil.drawArrow(buffer, matrix, normal, camera.getPosition(), start, direction,
            0.9D, color, 1.0F, 0.03F, 0.25D, 0.16D);
        buffers.endBatch(RENDER_TYPE);
    }
}
