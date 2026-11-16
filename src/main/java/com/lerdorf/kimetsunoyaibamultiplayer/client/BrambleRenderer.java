package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Renders the opaque, non-glowing split-color beams used by Biokineses. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BrambleRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final RenderType BRAMBLE_RENDER_TYPE = CustomRenderTypes.geoEntityTranslucent(BLANK_TEXTURE);
    private static final Map<UUID, Bramble> BRAMBLES = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private BrambleRenderer() {
    }

    public static synchronized void update(UUID id, Vec3 start, Vec3 end, Vec3 velocity, Vec3 arcing,
                                           Vec3 redColor, Vec3 blackColor, float width, int lifetime,
                                           boolean resetLifetime) {
        if (id == null || start == null || end == null || velocity == null || arcing == null
            || redColor == null || blackColor == null || width <= 0.0F) {
            return;
        }
        long createdAt = MINECRAFT.level == null ? 0L : MINECRAFT.level.getGameTime();
        long expiresAt = MINECRAFT.level == null ? Long.MAX_VALUE : createdAt + Math.max(1, lifetime);
        Bramble old = BRAMBLES.get(id);
        if (old != null && !resetLifetime) {
            createdAt = old.createdAt;
            expiresAt = old.expiresAt;
        }
        BRAMBLES.put(id, new Bramble(id, start, end, velocity, arcing, redColor, blackColor,
            width, expiresAt, createdAt));
    }

    public static synchronized void clear(UUID id) {
        if (id != null) {
            BRAMBLES.remove(id);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MINECRAFT.level == null || MINECRAFT.player == null) {
            return;
        }

        synchronized (BrambleRenderer.class) {
            if (lastLevel != MINECRAFT.level) {
                BRAMBLES.clear();
                lastLevel = MINECRAFT.level;
            }
            long gameTime = MINECRAFT.level.getGameTime();
            BRAMBLES.values().removeIf(bramble -> bramble.expiresAt <= gameTime);
        }

        Map<UUID, Bramble> brambles;
        synchronized (BrambleRenderer.class) {
            if (BRAMBLES.isEmpty()) {
                return;
            }
            brambles = new HashMap<>(BRAMBLES);
        }

        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        MultiBufferSource.BufferSource bufferSource = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(BRAMBLE_RENDER_TYPE);
        long gameTime = MINECRAFT.level.getGameTime();

        for (Bramble bramble : brambles.values()) {
            render(bramble, bramble.path(gameTime), matrix, normal, buffer);
        }
        bufferSource.endBatch(BRAMBLE_RENDER_TYPE);
        poseStack.popPose();
    }

    private static void render(Bramble bramble, Vec3[] path, Matrix4f matrix,
                               Matrix3f normal, VertexConsumer buffer) {
        for (int i = 0; i < path.length - 1; i++) {
            Vec3 start = path[i];
            Vec3 end = path[i + 1];
            Vec3 direction = end.subtract(start);
            if (direction.lengthSqr() < 1.0E-8D) {
                continue;
            }
            direction = direction.normalize();
            double overlap = bramble.width * 0.35D;
            drawSegment(start.subtract(direction.scale(overlap)), end.add(direction.scale(overlap)),
                bramble.width, bramble.redColor, bramble.blackColor, matrix, normal, buffer);
        }
        for (int i = 1; i < path.length - 1; i++) {
            drawCornerThorn(path[i - 1], path[i], path[i + 1], bramble.width,
                bramble.redColor, bramble.blackColor, matrix, normal, buffer);
        }
    }

    private static void drawCornerThorn(Vec3 previous, Vec3 corner, Vec3 next, float width,
                                        Vec3 redColor, Vec3 blackColor, Matrix4f matrix,
                                        Matrix3f normal, VertexConsumer buffer) {
        Vec3 incoming = corner.subtract(previous);
        Vec3 outgoing = next.subtract(corner);
        if (incoming.lengthSqr() < 1.0E-8D || outgoing.lengthSqr() < 1.0E-8D) {
            return;
        }
        incoming = incoming.normalize();
        outgoing = outgoing.normalize();
        Vec3 direction = incoming.subtract(outgoing);
        if (direction.lengthSqr() < 0.01D) {
            return;
        }
        direction = direction.normalize();

        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        double baseRadius = width * 0.9D;
        Vec3 basePositivePositive = corner.add(side.scale(baseRadius)).add(otherSide.scale(baseRadius));
        Vec3 baseNegativePositive = corner.subtract(side.scale(baseRadius)).add(otherSide.scale(baseRadius));
        Vec3 baseNegativeNegative = corner.subtract(side.scale(baseRadius)).subtract(otherSide.scale(baseRadius));
        Vec3 basePositiveNegative = corner.add(side.scale(baseRadius)).subtract(otherSide.scale(baseRadius));
        Vec3 tip = corner.add(direction.scale(width * 5.0D));

        drawTriangle(buffer, matrix, normal, tip, basePositivePositive, baseNegativePositive, redColor);
        drawTriangle(buffer, matrix, normal, tip, baseNegativePositive, baseNegativeNegative, blackColor);
        drawTriangle(buffer, matrix, normal, tip, baseNegativeNegative, basePositiveNegative, redColor);
        drawTriangle(buffer, matrix, normal, tip, basePositiveNegative, basePositivePositive, blackColor);
    }

    private static void drawSegment(Vec3 start, Vec3 end, float width, Vec3 redColor, Vec3 blackColor,
                                    Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        double halfWidth = width * 0.5D;

        Vec3 startCenter = start;
        Vec3 endCenter = end;
        Vec3 startPositive = startCenter.add(side.scale(halfWidth));
        Vec3 startNegative = startCenter.subtract(side.scale(halfWidth));
        Vec3 endPositive = endCenter.add(side.scale(halfWidth));
        Vec3 endNegative = endCenter.subtract(side.scale(halfWidth));
        Vec3 startOtherPositive = startCenter.add(otherSide.scale(halfWidth));
        Vec3 startOtherNegative = startCenter.subtract(otherSide.scale(halfWidth));
        Vec3 endOtherPositive = endCenter.add(otherSide.scale(halfWidth));
        Vec3 endOtherNegative = endCenter.subtract(otherSide.scale(halfWidth));
        Vec3 startPositiveOtherPositive = startPositive.add(otherSide.scale(halfWidth));
        Vec3 startNegativeOtherPositive = startNegative.add(otherSide.scale(halfWidth));
        Vec3 endPositiveOtherPositive = endPositive.add(otherSide.scale(halfWidth));
        Vec3 endNegativeOtherPositive = endNegative.add(otherSide.scale(halfWidth));
        Vec3 startPositiveOtherNegative = startPositive.subtract(otherSide.scale(halfWidth));
        Vec3 startNegativeOtherNegative = startNegative.subtract(otherSide.scale(halfWidth));
        Vec3 endPositiveOtherNegative = endPositive.subtract(otherSide.scale(halfWidth));
        Vec3 endNegativeOtherNegative = endNegative.subtract(otherSide.scale(halfWidth));

        // Each half of the square beam gets its own color, producing the split down its length.
        drawQuad(buffer, matrix, normal, startOtherPositive, endOtherPositive,
            endPositiveOtherPositive, startPositiveOtherPositive,
            redColor);
        drawQuad(buffer, matrix, normal, startNegativeOtherPositive, endNegativeOtherPositive,
            endOtherPositive, startOtherPositive, blackColor);
        drawQuad(buffer, matrix, normal, startOtherNegative, endOtherNegative,
            endPositiveOtherNegative, startPositiveOtherNegative, redColor);
        drawQuad(buffer, matrix, normal, startNegativeOtherNegative, endNegativeOtherNegative,
            endOtherNegative, startOtherNegative,
            blackColor);
        drawQuad(buffer, matrix, normal, startPositiveOtherNegative, endPositiveOtherNegative,
            endPositiveOtherPositive, startPositiveOtherPositive, redColor);
        drawQuad(buffer, matrix, normal, startNegativeOtherPositive, endNegativeOtherPositive,
            endNegativeOtherNegative, startNegativeOtherNegative, blackColor);
    }

    private static void drawQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                 Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Vec3 color) {
        vertex(buffer, matrix, normal, first, color);
        vertex(buffer, matrix, normal, second, color);
        vertex(buffer, matrix, normal, third, color);
        vertex(buffer, matrix, normal, fourth, color);
    }

    private static void drawTriangle(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                     Vec3 first, Vec3 second, Vec3 third, Vec3 color) {
        vertex(buffer, matrix, normal, first, color);
        vertex(buffer, matrix, normal, second, color);
        vertex(buffer, matrix, normal, third, color);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, Vec3 color) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color((float) color.x, (float) color.y, (float) color.z, 1.0F)
            .uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LevelRenderer.getLightColor(MINECRAFT.level, BlockPos.containing(position)))
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private static final class Bramble {
        private final UUID id;
        private final Vec3 start;
        private final Vec3 end;
        private final Vec3 velocity;
        private final Vec3 arcing;
        private final Vec3 redColor;
        private final Vec3 blackColor;
        private final float width;
        private final long expiresAt;
        private final long createdAt;
        private Vec3[] offsets = new Vec3[0];
        private Vec3[] cachedPath = new Vec3[0];
        private int cachedSegments;
        private long cachedTick = Long.MIN_VALUE;

        private Bramble(UUID id, Vec3 start, Vec3 end, Vec3 velocity, Vec3 arcing,
                        Vec3 redColor, Vec3 blackColor, float width, long expiresAt, long createdAt) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.velocity = velocity;
            this.arcing = arcing;
            this.redColor = redColor;
            this.blackColor = blackColor;
            this.width = width;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
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
                    .add(arcing.scale(arcFactor)).add(offsets[i]);
            }
            cachedPath = path;
            cachedTick = gameTime;
            return path;
        }

        private Vec3 randomOffset(long cycle, int index) {
            long seed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), index)
                ^ (cycle * 0x9E3779B97F4A7C15L) ^ (index * 0xC2B2AE3D27D4EB4FL);
            Random random = new Random(seed);
            Vec3 direction = new Vec3(random.nextDouble() * 2.0D - 1.0D,
                random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 1.0D);
            if (direction.lengthSqr() < 1.0E-8D) {
                direction = new Vec3(1.0D, 0.0D, 0.0D);
            }
            return direction.normalize().scale(random.nextDouble() * 5.0D * width);
        }
    }
}
