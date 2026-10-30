package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Renders enhanced Love Breathing point frames as glowing smooth ribbons. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LoveLineRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final RenderType LINE_RENDER_TYPE = CustomRenderTypes.geoEntityTranslucentEmissive(BLANK_TEXTURE);

    private static final int BEZIER_SUBDIVISIONS = 6;
    private static final float GLOW_HALF_WIDTH = 0.18F;
    private static final float CORE_HALF_WIDTH = 0.075F;
    private static final float PINK_R = 1.0F;
    private static final float PINK_G = 0.25F;
    private static final float PINK_B = 0.68F;

    private static final Map<UUID, Vec3[]> PATHS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private LoveLineRenderer() {
    }

    public static synchronized void setSyncedPoints(UUID entityUuid, int entityId, Vec3[] points) {
        if (entityUuid != null && points != null && points.length >= 2) {
            PATHS.put(entityUuid, points.clone());
        }
    }

    public static synchronized void clearSyncedPoints(UUID entityUuid, int entityId) {
        if (entityUuid != null) {
            PATHS.remove(entityUuid);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MINECRAFT.level == null || MINECRAFT.player == null) {
            return;
        }

        synchronized (LoveLineRenderer.class) {
            if (lastLevel != MINECRAFT.level) {
                PATHS.clear();
                lastLevel = MINECRAFT.level;
            }
        }

        Map<UUID, Vec3[]> paths;
        synchronized (LoveLineRenderer.class) {
            if (PATHS.isEmpty()) {
                return;
            }
            paths = new HashMap<>(PATHS);
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource bufferSource = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(LINE_RENDER_TYPE);

        boolean rendered = false;
        for (Vec3[] points : paths.values()) {
            if (points.length >= 2) {
                renderPath(points, cameraPosition, matrix, normal, buffer);
                rendered = true;
            }
        }

        if (rendered) {
            bufferSource.endBatch(LINE_RENDER_TYPE);
        }
    }

    private static void renderPath(Vec3[] points, Vec3 cameraPosition, Matrix4f matrix,
                                   Matrix3f normal, VertexConsumer buffer) {
        for (int segment = 0; segment < points.length - 1; segment++) {
            Vec3 p0 = points[Math.max(0, segment - 1)];
            Vec3 p1 = points[segment];
            Vec3 p2 = points[segment + 1];
            Vec3 p3 = points[Math.min(points.length - 1, segment + 2)];
            Vec3 control1 = p1.add(p2.subtract(p0).scale(1.0D / 6.0D));
            Vec3 control2 = p2.subtract(p3.subtract(p1).scale(1.0D / 6.0D));

            for (int subdivision = 0; subdivision < BEZIER_SUBDIVISIONS; subdivision++) {
                double startT = subdivision / (double) BEZIER_SUBDIVISIONS;
                double endT = (subdivision + 1.0D) / BEZIER_SUBDIVISIONS;
                Vec3 start = cubicBezier(p1, control1, control2, p2, startT);
                Vec3 end = cubicBezier(p1, control1, control2, p2, endT);
                double startProgress = (segment + startT) / (points.length - 1);
                double endProgress = (segment + endT) / (points.length - 1);

                drawRibbonSegment(start, end, startProgress, endProgress, cameraPosition,
                    matrix, normal, buffer, GLOW_HALF_WIDTH, 0.30F);
                drawRibbonSegment(start, end, startProgress, endProgress, cameraPosition,
                    matrix, normal, buffer, CORE_HALF_WIDTH, 0.98F);
            }
        }
    }

    private static void drawRibbonSegment(Vec3 start, Vec3 end, double startProgress,
                                          double endProgress, Vec3 cameraPosition,
                                          Matrix4f matrix, Matrix3f normal, VertexConsumer buffer,
                                          float halfWidth, float opacity) {
        Vec3 tangent = end.subtract(start);
        if (tangent.lengthSqr() < 1.0E-8D) {
            return;
        }
        tangent = tangent.normalize();
        Vec3 side = tangent.cross(cameraPosition.subtract(start));
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();

        Vec3 leftStart = start.add(side.scale(halfWidth)).subtract(cameraPosition);
        Vec3 rightStart = start.subtract(side.scale(halfWidth)).subtract(cameraPosition);
        Vec3 leftEnd = end.add(side.scale(halfWidth)).subtract(cameraPosition);
        Vec3 rightEnd = end.subtract(side.scale(halfWidth)).subtract(cameraPosition);

        float[] startColor = gradientColor(startProgress);
        float[] endColor = gradientColor(endProgress);
        vertex(buffer, matrix, normal, leftStart, 0.0F, 0.0F, startColor, opacity);
        vertex(buffer, matrix, normal, leftEnd, 0.0F, 1.0F, endColor, opacity);
        vertex(buffer, matrix, normal, rightEnd, 1.0F, 1.0F, endColor, opacity);
        vertex(buffer, matrix, normal, rightStart, 1.0F, 0.0F, startColor, opacity);
    }

    private static float[] gradientColor(double progress) {
        double wave = Math.sin(Math.PI * progress);
        float white = (float) (wave * wave);
        return new float[] {
            PINK_R,
            PINK_G + (1.0F - PINK_G) * white,
            PINK_B + (1.0F - PINK_B) * white
        };
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, float u, float v, float[] color, float opacity) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(color[0], color[1], color[2], opacity)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inverse = 1.0D - t;
        return p0.scale(inverse * inverse * inverse)
            .add(p1.scale(3.0D * inverse * inverse * t))
            .add(p2.scale(3.0D * inverse * t * t))
            .add(p3.scale(t * t * t));
    }
}
