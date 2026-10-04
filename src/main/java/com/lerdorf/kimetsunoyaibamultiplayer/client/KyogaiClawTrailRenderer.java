package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiClawEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders the black, tapered Bezier line left behind each Kyogai claw. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KyogaiClawTrailRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final RenderType TRAIL_RENDER_TYPE = RenderType.entityTranslucentEmissive(BLANK_TEXTURE);
    private static final int MAX_POINTS = 40;
    private static final int BEZIER_SEGMENTS_PER_POINT = 3;
    // Keep the line fully visible for ten seconds after its claw disappears,
    // then fade it out over the following two seconds.
    private static final int LINGER_TICKS = 20 * 10;
    private static final int FADE_TICKS = 40;
    private static final float MAX_HALF_WIDTH = 0.075F;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();

    private KyogaiClawTrailRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (MINECRAFT.level == null) {
            TRAILS.clear();
            return;
        }

        for (var entity : MINECRAFT.level.entitiesForRendering()) {
            if (!(entity instanceof KyogaiClawEntity claw)) {
                continue;
            }
            Trail trail = TRAILS.computeIfAbsent(claw.getId(), ignored -> new Trail());
            trail.detachedTicks = -1;
            Vec3 position = claw.position();
            if (trail.points.isEmpty() || trail.points.getLast().position.distanceToSqr(position) > 1.0E-5D) {
                trail.points.addLast(new TrailPoint(position, claw.getSurfaceDirection()));
                while (trail.points.size() > MAX_POINTS) {
                    trail.points.removeFirst();
                }
            }
        }

        TRAILS.entrySet().removeIf(entry -> {
            var entity = MINECRAFT.level.getEntity(entry.getKey());
            Trail trail = entry.getValue();
            if (entity != null && !entity.isRemoved()) {
                return false;
            }
            if (trail.detachedTicks < 0) {
                trail.detachedTicks = LINGER_TICKS + FADE_TICKS;
            } else {
                trail.detachedTicks--;
            }
            return trail.detachedTicks <= 0;
        });
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
            || MINECRAFT.level == null || TRAILS.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(TRAIL_RENDER_TYPE);
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        boolean rendered = false;
        for (Trail trail : TRAILS.values()) {
            if (trail.points.size() < 2) {
                continue;
            }
            renderTrail(trail, cameraPosition, matrix, normal, buffer, trail.opacity());
            rendered = true;
        }

        if (rendered) {
            bufferSource.endBatch(TRAIL_RENDER_TYPE);
        }
    }

    private static void renderTrail(Trail trail, Vec3 cameraPosition, Matrix4f matrix,
                                    Matrix3f normal, VertexConsumer buffer, float opacity) {
        List<TrailPoint> points = new ArrayList<>(trail.points);

        int segmentCount = points.size() - 1;
        for (int segment = 0; segment < segmentCount; segment++) {
            Vec3 p0 = points.get(Math.max(0, segment - 1)).position;
            TrailPoint point1 = points.get(segment);
            TrailPoint point2 = points.get(segment + 1);
            Vec3 p1 = point1.position;
            Vec3 p2 = point2.position;
            Vec3 p3 = points.get(Math.min(points.size() - 1, segment + 2)).position;
            Vec3 control1 = p1.add(p2.subtract(p0).scale(1.0D / 6.0D));
            Vec3 control2 = p2.subtract(p3.subtract(p1).scale(1.0D / 6.0D));
            Vec3 surfaceNormal = Vec3.atLowerCornerOf(point1.surfaceDirection.getNormal());

            for (int subSegment = 0; subSegment < BEZIER_SEGMENTS_PER_POINT; subSegment++) {
                double startT = subSegment / (double) BEZIER_SEGMENTS_PER_POINT;
                double endT = (subSegment + 1.0D) / BEZIER_SEGMENTS_PER_POINT;
                Vec3 start = cubicBezier(p1, control1, control2, p2, startT);
                Vec3 end = cubicBezier(p1, control1, control2, p2, endT);
                double startProgress = (segment + startT) / segmentCount;
                double endProgress = (segment + endT) / segmentCount;
                drawRibbonSegment(start, end, surfaceNormal, startProgress, endProgress,
                    cameraPosition, matrix, normal, buffer, opacity);
            }
        }
    }

    private static void drawRibbonSegment(Vec3 start, Vec3 end, Vec3 surfaceNormal,
                                           double startProgress, double endProgress,
                                           Vec3 cameraPosition, Matrix4f matrix, Matrix3f normal,
                                           VertexConsumer buffer, float opacity) {
        Vec3 tangent = end.subtract(start);
        if (tangent.lengthSqr() < 1.0E-8D) {
            return;
        }
        tangent = tangent.subtract(surfaceNormal.scale(tangent.dot(surfaceNormal)));
        if (tangent.lengthSqr() < 1.0E-8D) {
            return;
        }
        tangent = tangent.normalize();
        Vec3 side = tangent.cross(surfaceNormal);
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();

        float startWidth = MAX_HALF_WIDTH * (float) Math.pow(1.0D - startProgress, 0.8D);
        float endWidth = MAX_HALF_WIDTH * (float) Math.pow(1.0D - endProgress, 0.8D);
        float startAlpha = (0.95F * (1.0F - (float) startProgress) + 0.05F) * opacity;
        float endAlpha = (0.95F * (1.0F - (float) endProgress) + 0.05F) * opacity;

        Vec3 leftStart = start.add(side.scale(startWidth)).subtract(cameraPosition);
        Vec3 rightStart = start.subtract(side.scale(startWidth)).subtract(cameraPosition);
        Vec3 leftEnd = end.add(side.scale(endWidth)).subtract(cameraPosition);
        Vec3 rightEnd = end.subtract(side.scale(endWidth)).subtract(cameraPosition);

        vertex(buffer, matrix, normal, leftStart, 0.0F, 0.0F, startAlpha, surfaceNormal);
        vertex(buffer, matrix, normal, leftEnd, 0.0F, 1.0F, endAlpha, surfaceNormal);
        vertex(buffer, matrix, normal, rightEnd, 1.0F, 1.0F, endAlpha, surfaceNormal);
        vertex(buffer, matrix, normal, rightStart, 1.0F, 0.0F, startAlpha, surfaceNormal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, float u, float v, float alpha, Vec3 surfaceNormal) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(0.0F, 0.0F, 0.0F, alpha)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, (float) surfaceNormal.x, (float) surfaceNormal.y, (float) surfaceNormal.z)
            .endVertex();
    }

    private static Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inverse = 1.0D - t;
        return p0.scale(inverse * inverse * inverse)
            .add(p1.scale(3.0D * inverse * inverse * t))
            .add(p2.scale(3.0D * inverse * t * t))
            .add(p3.scale(t * t * t));
    }

    private static final class Trail {
        private final Deque<TrailPoint> points = new ArrayDeque<>();
        private int detachedTicks = -1;

        private float opacity() {
            if (detachedTicks < 0 || detachedTicks > FADE_TICKS) {
                return 1.0F;
            }
            return detachedTicks / (float) FADE_TICKS;
        }
    }

    private record TrailPoint(Vec3 position, Direction surfaceDirection) {
    }
}
