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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Renders the layered, glowing lightning beams used by enhanced Thunder Breathing. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ThunderLightningRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final RenderType BEAM_RENDER_TYPE =
        CustomRenderTypes.geoEntityTranslucentEmissive(BLANK_TEXTURE);

    private static final Map<UUID, Bolt> BOLTS = new HashMap<>();
    private static final Map<UUID, Spark> SPARKS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private ThunderLightningRenderer() {
    }

    public static synchronized void updateBolt(UUID id, Vec3[] positions, Vec3 primary, Vec3 secondary,
                                               boolean whiteMiddle, int lifetime, boolean resetLifetime) {
        if (id == null || positions == null || positions.length < 2) {
            return;
        }
        long expiresAt = MINECRAFT.level == null ? Long.MAX_VALUE
            : MINECRAFT.level.getGameTime() + Math.max(1, lifetime);
        Bolt old = BOLTS.get(id);
        if (old != null && !resetLifetime) {
            expiresAt = old.expiresAt;
        }
        BOLTS.put(id, new Bolt(positions.clone(), primary, secondary, whiteMiddle, expiresAt));
    }

    public static synchronized void clearBolt(UUID id) {
        if (id != null) {
            BOLTS.remove(id);
            ShimmerCompat.removeLightningLight(id);
        }
    }

    public static synchronized void updateSpark(UUID id, Vec3 start, Vec3 end, Vec3 velocity, Vec3 arcing,
                                                Vec3 outerColor, Vec3 innerColor, float width, int lifetime,
                                                boolean flickering, boolean resetLifetime) {
        if (id == null || start == null || end == null || velocity == null || arcing == null
            || outerColor == null || innerColor == null || width <= 0.0F) {
            return;
        }
        long createdAt = MINECRAFT.level == null ? 0L : MINECRAFT.level.getGameTime();
        long expiresAt = MINECRAFT.level == null ? Long.MAX_VALUE : createdAt + Math.max(1, lifetime);
        Spark old = SPARKS.get(id);
        if (old != null && !resetLifetime) {
            expiresAt = old.expiresAt;
            createdAt = old.createdAt;
        }
        SPARKS.put(id, new Spark(id, start, end, velocity, arcing, outerColor, innerColor,
            width, flickering, expiresAt, createdAt));
    }

    public static synchronized void clearSpark(UUID id) {
        if (id != null) {
            SPARKS.remove(id);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MINECRAFT.level == null || MINECRAFT.player == null) {
            return;
        }

        synchronized (ThunderLightningRenderer.class) {
            if (lastLevel != MINECRAFT.level) {
                BOLTS.clear();
                SPARKS.clear();
                ShimmerCompat.cleanupDynamicLights();
                lastLevel = MINECRAFT.level;
            }
            long gameTime = MINECRAFT.level.getGameTime();
            BOLTS.entrySet().removeIf(entry -> {
                if (entry.getValue().expiresAt <= gameTime) {
                    ShimmerCompat.removeLightningLight(entry.getKey());
                    return true;
                }
                return false;
            });
            SPARKS.values().removeIf(spark -> spark.expiresAt <= gameTime);
        }

        Map<UUID, Bolt> bolts;
        Map<UUID, Spark> sparks;
        synchronized (ThunderLightningRenderer.class) {
            if (BOLTS.isEmpty() && SPARKS.isEmpty()) {
                return;
            }
            bolts = new HashMap<>(BOLTS);
            sparks = new HashMap<>(SPARKS);
        }

        long gameTime = MINECRAFT.level.getGameTime();

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource bufferSource = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(BEAM_RENDER_TYPE);

        for (Map.Entry<UUID, Bolt> entry : bolts.entrySet()) {
            Bolt bolt = entry.getValue();
            ShimmerCompat.updateLightningLight(entry.getKey(), lightPosition(bolt), lightColor(bolt), 8.0F);
            renderBolt(entry.getKey(), bolt, cameraPosition, matrix, normal, buffer, false);
        }
        for (Spark spark : sparks.values()) {
            Vec3[] path = spark.path(gameTime);
            if (spark.shouldRender(gameTime)) {
                renderSpark(spark, path, cameraPosition, matrix, normal, buffer, false);
            }
        }
        bufferSource.endBatch(BEAM_RENDER_TYPE);

        ShimmerCompat.postWorldBloom((bloomPoseStack, bloomBufferSource) -> {
            Vec3 bloomCameraPosition = MINECRAFT.gameRenderer.getMainCamera().getPosition();
            Matrix4f bloomMatrix = bloomPoseStack.last().pose();
            Matrix3f bloomNormal = bloomPoseStack.last().normal();
            VertexConsumer bloomBuffer = bloomBufferSource.getBuffer(BEAM_RENDER_TYPE);
            for (Map.Entry<UUID, Bolt> entry : bolts.entrySet()) {
                renderBolt(entry.getKey(), entry.getValue(), bloomCameraPosition,
                    bloomMatrix, bloomNormal, bloomBuffer, true);
            }
            for (Spark spark : sparks.values()) {
                Vec3[] path = spark.path(gameTime);
                if (spark.shouldRender(gameTime)) {
                    renderSpark(spark, path, bloomCameraPosition, bloomMatrix, bloomNormal, bloomBuffer, true);
                }
            }
        });
    }

    private static void renderBolt(UUID id, Bolt bolt, Vec3 cameraPosition, Matrix4f matrix,
                                   Matrix3f normal, VertexConsumer buffer, boolean bloomPass) {
        List<Vec3> path = new ArrayList<>();
        for (int i = 0; i < bolt.positions.length - 1; i++) {
            Vec3 start = bolt.positions[i];
            Vec3 end = bolt.positions[i + 1];
            int subdivisions = 5;
            Vec3 direction = end.subtract(start);
            if (direction.lengthSqr() < 1.0E-8D) {
                continue;
            }
            direction = direction.normalize();
            Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-8D) {
                side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
            side = side.normalize();
            Vec3 otherSide = direction.cross(side).normalize();
            if (path.isEmpty()) {
                path.add(start);
            }
            for (int step = 1; step < subdivisions; step++) {
                double progress = step / (double) subdivisions;
                double amount = 0.18D * Math.sin((id.getLeastSignificantBits() + i * 31L + step * 17L) * 0.73D);
                double amount2 = 0.14D * Math.cos((id.getMostSignificantBits() + i * 13L + step * 29L) * 0.61D);
                path.add(start.lerp(end, progress).add(side.scale(amount)).add(otherSide.scale(amount2)));
            }
            path.add(end);
        }

        for (int i = 0; i < path.size() - 1; i++) {
            double startProgress = i / (double) Math.max(1, path.size() - 1);
            double endProgress = (i + 1) / (double) Math.max(1, path.size() - 1);
            for (BeamLayer layer : BeamLayer.values()) {
                drawBeamSegment(path.get(i), path.get(i + 1), startProgress, endProgress, layer,
                    bolt, cameraPosition, matrix, normal, buffer, bloomPass);
            }
        }
    }

    private static void drawBeamSegment(Vec3 start, Vec3 end, double startProgress, double endProgress,
                                         BeamLayer layer, Bolt bolt, Vec3 cameraPosition, Matrix4f matrix,
                                         Matrix3f normal, VertexConsumer buffer, boolean bloomPass) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        float halfWidth = layer.width * 0.5F;
        Vec3[] startCorners = corners(start, side, otherSide, halfWidth).stream()
            .map(point -> point.subtract(cameraPosition)).toArray(Vec3[]::new);
        Vec3[] endCorners = corners(end, side, otherSide, halfWidth).stream()
            .map(point -> point.subtract(cameraPosition)).toArray(Vec3[]::new);

        float[] startColor = layer == BeamLayer.CORE
            ? new float[] {1.0F, 1.0F, 1.0F} : gradient(bolt, startProgress, bloomPass);
        float[] endColor = layer == BeamLayer.CORE
            ? new float[] {1.0F, 1.0F, 1.0F} : gradient(bolt, endProgress, bloomPass);
        float startAlpha = layer.alpha;
        float endAlpha = layer.alpha;

        for (int face = 0; face < 4; face++) {
            int next = (face + 1) % 4;
            vertex(buffer, matrix, normal, startCorners[face], startColor, startAlpha);
            vertex(buffer, matrix, normal, endCorners[face], endColor, endAlpha);
            vertex(buffer, matrix, normal, endCorners[next], endColor, endAlpha);
            vertex(buffer, matrix, normal, startCorners[next], startColor, startAlpha);
        }
    }

    private static List<Vec3> corners(Vec3 center, Vec3 side, Vec3 otherSide, float halfWidth) {
        return List.of(
            center.add(side.scale(halfWidth)).add(otherSide.scale(halfWidth)),
            center.add(side.scale(halfWidth)).subtract(otherSide.scale(halfWidth)),
            center.subtract(side.scale(halfWidth)).subtract(otherSide.scale(halfWidth)),
            center.subtract(side.scale(halfWidth)).add(otherSide.scale(halfWidth)));
    }

    private static float[] gradient(Bolt bolt, double progress, boolean bloomPass) {
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        float[] color;
        if (bolt.whiteMiddle) {
            if (clamped < 0.5D) {
                color = mix(bolt.primary, new Vec3(1.0D, 1.0D, 1.0D), clamped * 2.0D);
            } else {
                color = mix(new Vec3(1.0D, 1.0D, 1.0D), bolt.secondary, (clamped - 0.5D) * 2.0D);
            }
        } else {
            color = mix(bolt.primary, bolt.secondary, clamped);
        }
        if (bloomPass) {
            color[0] = color[0] + (1.0F - color[0]) * 0.35F;
            color[1] = color[1] + (1.0F - color[1]) * 0.35F;
            color[2] = color[2] + (1.0F - color[2]) * 0.35F;
        }
        return color;
    }

    private static Vec3 lightPosition(Bolt bolt) {
        Vec3 position = Vec3.ZERO;
        for (Vec3 point : bolt.positions) {
            position = position.add(point);
        }
        return position.scale(1.0D / bolt.positions.length);
    }

    private static int lightColor(Bolt bolt) {
        int red = (int) (Math.max(0.0D, Math.min(1.0D, bolt.primary.x)) * 255.0D);
        int green = (int) (Math.max(0.0D, Math.min(1.0D, bolt.primary.y)) * 255.0D);
        int blue = (int) (Math.max(0.0D, Math.min(1.0D, bolt.primary.z)) * 255.0D);
        return (red << 16) | (green << 8) | blue;
    }

    private static float[] mix(Vec3 first, Vec3 second, double progress) {
        return new float[] {
            (float) (first.x + (second.x - first.x) * progress),
            (float) (first.y + (second.y - first.y) * progress),
            (float) (first.z + (second.z - first.z) * progress)
        };
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, float[] color, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(color[0], color[1], color[2], alpha)
            .uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private enum BeamLayer {
        OUTER(1.0F, 0.20F),
        MIDDLE(0.8F, 0.50F),
        INNER(0.6F, 0.75F),
        CORE(0.5F, 1.0F);

        private final float width;
        private final float alpha;

        BeamLayer(float width, float alpha) {
            this.width = width;
            this.alpha = alpha;
        }
    }

    private record Bolt(Vec3[] positions, Vec3 primary, Vec3 secondary, boolean whiteMiddle, long expiresAt) {
    }

    private static void renderSpark(Spark spark, Vec3[] path, Vec3 cameraPosition, Matrix4f matrix,
                                    Matrix3f normal, VertexConsumer buffer, boolean bloomPass) {
        if (path.length < 2) {
            return;
        }
        float[] outerColor = spark.color(spark.outerColor, bloomPass);
        float[] innerColor = spark.color(spark.innerColor, bloomPass);
        float outerHalfWidth = spark.width * 0.5F;
        float innerHalfWidth = outerHalfWidth * 0.5F;
        for (int i = 0; i < path.length - 1; i++) {
            drawSparkSegment(path[i], path[i + 1], outerHalfWidth, 0.50F, outerColor,
                cameraPosition, matrix, normal, buffer);
            drawSparkSegment(path[i], path[i + 1], innerHalfWidth, 1.0F, innerColor,
                cameraPosition, matrix, normal, buffer);
        }
    }

    private static void drawSparkSegment(Vec3 start, Vec3 end, float halfWidth, float alpha, float[] color,
                                         Vec3 cameraPosition, Matrix4f matrix, Matrix3f normal,
                                         VertexConsumer buffer) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        Vec3[] startCorners = corners(start, side, otherSide, halfWidth).stream()
            .map(point -> point.subtract(cameraPosition)).toArray(Vec3[]::new);
        Vec3[] endCorners = corners(end, side, otherSide, halfWidth).stream()
            .map(point -> point.subtract(cameraPosition)).toArray(Vec3[]::new);
        for (int face = 0; face < 4; face++) {
            int next = (face + 1) % 4;
            vertex(buffer, matrix, normal, startCorners[face], color, alpha);
            vertex(buffer, matrix, normal, endCorners[face], color, alpha);
            vertex(buffer, matrix, normal, endCorners[next], color, alpha);
            vertex(buffer, matrix, normal, startCorners[next], color, alpha);
        }
    }

    private static final class Spark {
        private final UUID id;
        private final Vec3 start;
        private final Vec3 end;
        private final Vec3 velocity;
        private final Vec3 arcing;
        private final Vec3 outerColor;
        private final Vec3 innerColor;
        private final float width;
        private final boolean flickering;
        private final long expiresAt;
        private final long createdAt;
        private Vec3[] offsets = new Vec3[0];
        private Vec3[] cachedPath = new Vec3[0];
        private int cachedSegments;
        private long cachedTick = Long.MIN_VALUE;

        private Spark(UUID id, Vec3 start, Vec3 end, Vec3 velocity, Vec3 arcing,
                      Vec3 outerColor, Vec3 innerColor, float width, boolean flickering,
                      long expiresAt, long createdAt) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.velocity = velocity;
            this.arcing = arcing;
            this.outerColor = outerColor;
            this.innerColor = innerColor;
            this.width = width;
            this.flickering = flickering;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }

        private boolean shouldRender(long gameTime) {
            return !flickering || ((gameTime - createdAt) & 1L) == 0L;
        }

        private Vec3[] path(long gameTime) {
            if (cachedTick == gameTime) {
                return cachedPath;
            }

            Vec3 currentStart = start.add(velocity.scale(gameTime - createdAt));
            Vec3 currentEnd = end.add(velocity.scale(gameTime - createdAt));
            Vec3 direction = currentEnd.subtract(currentStart);
            double denominator = 25.0D * width;
            int segments = denominator <= 0.0D
                ? 1 : Math.max(1, Math.min(50, (int) (direction.lengthSqr() / denominator)));
            if (segments != cachedSegments) {
                cachedSegments = segments;
                offsets = new Vec3[segments + 1];
                for (int i = 0; i <= segments; i++) {
                    offsets[i] = Vec3.ZERO;
                }
            }

            long cycle = gameTime / 2L;
            for (int i = 1; i < segments; i++) {
                if (i % 2 == Math.floorMod(gameTime, 2L)) {
                    offsets[i] = randomOffset(cycle, i);
                }
            }

            Vec3[] path = new Vec3[segments + 1];
            for (int i = 0; i <= segments; i++) {
                double progress = i / (double) segments;
                double arcFactor = 1.0D - Math.abs(progress * 2.0D - 1.0D);
                path[i] = currentStart.lerp(currentEnd, progress)
                    .add(arcing.scale(arcFactor))
                    .add(offsets[i]);
            }
            cachedPath = path;
            cachedTick = gameTime;
            return path;
        }

        private Vec3 randomOffset(long cycle, int index) {
            long seed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), index)
                ^ (cycle * 0x9E3779B97F4A7C15L) ^ (index * 0xC2B2AE3D27D4EB4FL);
            java.util.Random random = new java.util.Random(seed);
            Vec3 direction = new Vec3(random.nextDouble() * 2.0D - 1.0D,
                random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 1.0D);
            if (direction.lengthSqr() < 1.0E-8D) {
                direction = new Vec3(1.0D, 0.0D, 0.0D);
            }
            return direction.normalize().scale(random.nextDouble() * 5.0D * width);
        }

        private float[] color(Vec3 source, boolean bloomPass) {
            float[] color = {(float) source.x, (float) source.y, (float) source.z};
            if (bloomPass) {
                color[0] += (1.0F - color[0]) * 0.35F;
                color[1] += (1.0F - color[1]) * 0.35F;
                color[2] += (1.0F - color[2]) * 0.35F;
            }
            return color;
        }
    }
}
