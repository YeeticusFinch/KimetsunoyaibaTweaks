package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesAuraTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Renders Biokineses textures projected onto nearby terrain columns. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BiokinesesAuraRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final double EPSILON = 1.0E-4D;
    private static final double SURFACE_OFFSET = 0.01D;
    private static final double SIDE_EDGE_UV = 1.0D / 256.0D;
    private static final Map<UUID, Aura> AURAS = new HashMap<>();
    private static final Map<UUID, ProjectionCache> PROJECTION_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> RENDER_TYPES = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private BiokinesesAuraRenderer() {
    }

    public static synchronized void update(UUID id, BiokinesesAuraTexture texture, Vec3 origin,
                                           int entityId, Direction gravityDirection, float opacity,
                                           float radius, double maxDepth, double upOffset,
                                           float rotation, float rotationSpeed, int lifetime,
                                           boolean resetLifetime) {
        if (id == null || texture == null || origin == null || gravityDirection == null
            || radius <= 0.0F || maxDepth < 0.0D) {
            return;
        }
        long gameTime = MINECRAFT.level == null ? 0L : MINECRAFT.level.getGameTime();
        Aura old = AURAS.get(id);
        long createdAt = gameTime;
        long expiresAt = gameTime + Math.max(1, lifetime);
        if (old != null && !resetLifetime) {
            createdAt = old.createdAt;
            expiresAt = old.expiresAt;
        }
        AURAS.put(id, new Aura(id, texture, origin, entityId, gravityDirection,
            clamp(opacity, 0.0F, 1.0F), radius, maxDepth, upOffset, rotation,
            rotationSpeed, createdAt, gameTime, expiresAt));
    }

    public static synchronized void clear(UUID id) {
        if (id != null) {
            AURAS.remove(id);
            PROJECTION_CACHE.remove(id);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MINECRAFT.level == null || MINECRAFT.player == null) {
            return;
        }

        List<Aura> auras;
        long gameTime = MINECRAFT.level.getGameTime();
        synchronized (BiokinesesAuraRenderer.class) {
            if (lastLevel != MINECRAFT.level) {
                AURAS.clear();
                PROJECTION_CACHE.clear();
                lastLevel = MINECRAFT.level;
            }
            Iterator<Map.Entry<UUID, Aura>> iterator = AURAS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Aura> entry = iterator.next();
                if (entry.getValue().expiresAt <= gameTime) {
                    PROJECTION_CACHE.remove(entry.getKey());
                    iterator.remove();
                }
            }
            if (AURAS.isEmpty()) {
                return;
            }
            auras = AURAS.values().stream().map(Aura::copy).toList();
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        Map<RenderType, VertexConsumer> vertices = new HashMap<>();

        for (Aura aura : auras) {
            Vec3 origin = resolveOrigin(aura, event.getPartialTick());
            double renderDistance = aura.radius + aura.maxDepth + 4.0D;
            if (cameraPosition.distanceToSqr(origin) > renderDistance * renderDistance) {
                continue;
            }
            ResourceLocation texture = texture(aura.texture, gameTime, event.getPartialTick());
            RenderType renderType = RENDER_TYPES.computeIfAbsent(texture, CustomRenderTypes::auraTranslucent);
            VertexConsumer buffer = vertices.computeIfAbsent(renderType, buffers::getBuffer);
            renderAura(aura, origin, buffer, matrix, normalMatrix, gameTime, event.getPartialTick());
        }

        for (RenderType renderType : vertices.keySet()) {
            buffers.endBatch(renderType);
        }
        poseStack.popPose();
    }

    private static void renderAura(Aura aura, Vec3 origin, VertexConsumer buffer, Matrix4f matrix,
                                   Matrix3f normalMatrix, long gameTime, float partialTick) {
        Basis basis = Basis.forGravity(aura.gravityDirection);
        double rotation = aura.rotation
            + aura.rotationSpeed * (gameTime + partialTick - aura.updatedAt);
        Projection projection = cachedProjection(aura, origin, basis, gameTime);

        drawFallback(buffer, matrix, normalMatrix, origin, basis, aura.radius, aura.maxDepth,
            rotation, aura.opacity);
        for (Surface surface : projection.surfaces) {
            drawTop(buffer, matrix, normalMatrix, origin, basis, aura.radius, rotation,
                aura.opacity, surface);
        }
        for (Surface surface : projection.surfaces) {
            drawSides(buffer, matrix, normalMatrix, origin, basis, aura.radius, aura.maxDepth,
                rotation, aura.opacity, projection.columns, surface);
        }
    }

    private static Projection cachedProjection(Aura aura, Vec3 origin, Basis basis, long gameTime) {
        BlockPos originBlock = BlockPos.containing(origin);
        ProjectionCache cached = PROJECTION_CACHE.get(aura.id);
        if (cached != null && cached.gameTime == gameTime && cached.originBlock.equals(originBlock)
            && cached.radius == aura.radius && cached.maxDepth == aura.maxDepth
            && cached.upOffset == aura.upOffset && cached.gravityDirection == aura.gravityDirection) {
            return cached.projection;
        }
        Projection projection = project(aura, origin, basis);
        PROJECTION_CACHE.put(aura.id, new ProjectionCache(gameTime, originBlock, aura.radius,
            aura.maxDepth, aura.upOffset, aura.gravityDirection, projection));
        return projection;
    }

    private static Projection project(Aura aura, Vec3 origin, Basis basis) {
        net.minecraft.client.multiplayer.ClientLevel level = MINECRAFT.level;
        BlockPos start = BlockPos.containing(origin.add(basis.upVec.scale(aura.upOffset)));
        int range = (int) Math.ceil(aura.radius) + 1;
        int verticalRange = (int) Math.ceil(aura.maxDepth + Math.max(0.0D, aura.upOffset)) + 3;
        Map<ColumnKey, Surface> columns = new HashMap<>();
        List<Surface> surfaces = new ArrayList<>();
        double maxRadius = aura.radius + 0.8D;

        for (int a = -range; a <= range; a++) {
            for (int b = -range; b <= range; b++) {
                BlockPos columnPos = start.relative(basis.axisA, a).relative(basis.axisB, b);
                Vec3 columnCenter = blockCenter(columnPos);
                Vec3 horizontal = columnCenter.subtract(origin);
                double localA = horizontal.dot(basis.axisAVec);
                double localB = horizontal.dot(basis.axisBVec);
                if (localA * localA + localB * localB > maxRadius * maxRadius) {
                    continue;
                }
                Surface surface = findSurface(level, columnPos, basis, origin, verticalRange,
                    aura.maxDepth, aura.upOffset);
                if (surface != null) {
                    ColumnKey key = new ColumnKey(a, b);
                    surface.a = a;
                    surface.b = b;
                    columns.put(key, surface);
                    surfaces.add(surface);
                }
            }
        }
        return new Projection(columns, surfaces);
    }

    private static Surface findSurface(net.minecraft.client.multiplayer.ClientLevel level,
                                       BlockPos columnPos, Basis basis, Vec3 origin,
                                       int verticalRange, double maxDepth, double upOffset) {
        for (int depth = 0; depth <= verticalRange; depth++) {
            BlockPos pos = columnPos.relative(basis.down, depth);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            Double offset = surfaceOffset(level, state, pos, basis.up);
            if (offset == null) {
                continue;
            }
            Vec3 surfaceColumnCenter = blockCenter(pos);
            double height = surfaceColumnCenter.add(basis.upVec.scale(offset))
                .subtract(origin).dot(basis.upVec);
            if (height >= -maxDepth - EPSILON && height <= upOffset + EPSILON) {
                return new Surface(surfaceColumnCenter, height, offset);
            }
        }
        return null;
    }

    private static Double surfaceOffset(net.minecraft.client.multiplayer.ClientLevel level,
                                        BlockState state, BlockPos pos, Direction up) {
        if (state.isCollisionShapeFullBlock(level, pos)) {
            return 0.5D;
        }
        Block block = state.getBlock();
        if (!(block instanceof SlabBlock) || up.getAxis() != Direction.Axis.Y) {
            return null;
        }
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (up == Direction.UP) {
            double top = type == SlabType.BOTTOM ? 0.5D : 1.0D;
            return top - 0.5D;
        }
        double bottom = type == SlabType.TOP ? 0.5D : 0.0D;
        return 0.5D - bottom;
    }

    private static Vec3 blockCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static void drawFallback(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                                     Vec3 origin, Basis basis, double radius, double maxDepth,
                                     double rotation, float opacity) {
        Vec3 center = origin.subtract(basis.upVec.scale(maxDepth));
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        Vec3 rotatedA = basis.axisAVec.scale(cos).add(basis.axisBVec.scale(sin));
        Vec3 rotatedB = basis.axisAVec.scale(-sin).add(basis.axisBVec.scale(cos));
        Vec3 p00 = center.subtract(rotatedA.scale(radius)).subtract(rotatedB.scale(radius));
        Vec3 p10 = center.add(rotatedA.scale(radius)).subtract(rotatedB.scale(radius));
        Vec3 p11 = center.add(rotatedA.scale(radius)).add(rotatedB.scale(radius));
        Vec3 p01 = center.subtract(rotatedA.scale(radius)).add(rotatedB.scale(radius));
        drawQuad(buffer, matrix, normalMatrix, p00, p10, p11, p01,
            new float[] {0.0F, 0.0F}, new float[] {1.0F, 0.0F},
            new float[] {1.0F, 1.0F}, new float[] {0.0F, 1.0F}, basis.upVec, opacity);
    }

    private static void drawTop(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                                Vec3 origin, Basis basis, double radius, double rotation,
                                float opacity, Surface surface) {
        Vec3 center = surface.columnCenter.add(basis.upVec.scale(surface.offset + SURFACE_OFFSET));
        Vec3 p00 = center.subtract(basis.axisAVec.scale(0.5D)).subtract(basis.axisBVec.scale(0.5D));
        Vec3 p10 = center.add(basis.axisAVec.scale(0.5D)).subtract(basis.axisBVec.scale(0.5D));
        Vec3 p11 = center.add(basis.axisAVec.scale(0.5D)).add(basis.axisBVec.scale(0.5D));
        Vec3 p01 = center.subtract(basis.axisAVec.scale(0.5D)).add(basis.axisBVec.scale(0.5D));
        drawQuad(buffer, matrix, normalMatrix, p00, p10, p11, p01,
            uv(p00, origin, basis, radius, rotation), uv(p10, origin, basis, radius, rotation),
            uv(p11, origin, basis, radius, rotation), uv(p01, origin, basis, radius, rotation),
            basis.upVec, opacity);
    }

    private static void drawSides(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                                  Vec3 origin, Basis basis, double radius, double maxDepth,
                                  double rotation, float opacity, Map<ColumnKey, Surface> columns,
                                  Surface surface) {
        for (Side side : Side.values()) {
            Surface neighbor = columns.get(new ColumnKey(surface.a + side.a, surface.b + side.b));
            if (neighbor != null && neighbor.height >= surface.height - EPSILON) {
                continue;
            }
            double lower = neighbor == null ? -maxDepth : Math.max(-maxDepth, neighbor.height);
            if (lower >= surface.height - EPSILON) {
                continue;
            }
            Vec3 edge = surface.columnCenter.add(side.axis(basis).scale(side.sign * 0.5D))
                .add(side.normal(basis).scale(SURFACE_OFFSET));
            Vec3 top = edge.add(basis.upVec.scale(surface.offset));
            double edgeHeight = edge.subtract(origin).dot(basis.upVec);
            Vec3 bottom = edge.add(basis.upVec.scale(lower - edgeHeight));
            Vec3 first = top.subtract(side.otherAxis(basis).scale(0.5D));
            Vec3 second = top.add(side.otherAxis(basis).scale(0.5D));
            Vec3 third = bottom.add(side.otherAxis(basis).scale(0.5D));
            Vec3 fourth = bottom.subtract(side.otherAxis(basis).scale(0.5D));
            drawQuad(buffer, matrix, normalMatrix, first, second, third, fourth,
                side.uv(first, origin, basis, radius, rotation),
                side.uv(second, origin, basis, radius, rotation),
                side.uv(third, origin, basis, radius, rotation),
                side.uv(fourth, origin, basis, radius, rotation),
                side.normal(basis), opacity);
        }
    }

    private static float[] uv(Vec3 point, Vec3 origin, Basis basis, double radius, double rotation) {
        Vec3 difference = point.subtract(origin);
        double a = difference.dot(basis.axisAVec);
        double b = difference.dot(basis.axisBVec);
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        double rotatedA = a * cos - b * sin;
        double rotatedB = a * sin + b * cos;
        return new float[] {
            (float) clamp(0.5D + rotatedA / (radius * 2.0D), 0.0D, 1.0D),
            (float) clamp(0.5D + rotatedB / (radius * 2.0D), 0.0D, 1.0D)
        };
    }

    private static void drawQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                                 Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
                                 float[] firstUv, float[] secondUv, float[] thirdUv, float[] fourthUv,
                                 Vec3 normal, float opacity) {
        vertex(buffer, matrix, normalMatrix, first, firstUv, normal, opacity);
        vertex(buffer, matrix, normalMatrix, second, secondUv, normal, opacity);
        vertex(buffer, matrix, normalMatrix, third, thirdUv, normal, opacity);
        vertex(buffer, matrix, normalMatrix, fourth, fourthUv, normal, opacity);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                               Vec3 position, float[] uv, Vec3 normal, float opacity) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(1.0F, 1.0F, 1.0F, opacity)
            .uv(uv[0], uv[1])
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
    }

    private static Vec3 resolveOrigin(Aura aura, float partialTick) {
        if (aura.entityId >= 0 && MINECRAFT.level != null) {
            Entity entity = MINECRAFT.level.getEntity(aura.entityId);
            if (entity != null) {
                return entity.getPosition(partialTick);
            }
        }
        return aura.origin;
    }

    private static ResourceLocation texture(BiokinesesAuraTexture texture, long gameTime, float partialTick) {
        String path;
        if (texture == BiokinesesAuraTexture.CRACKED) {
            path = "cracked_aura";
        } else {
            path = switch (texture) {
                case DARK -> "dark_aura";
                case RED_CIRCLES -> "red_circles_aura";
                case WHITE -> "white_aura";
                case CRACKED -> "scratch_aura_0";
            };
        }
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "textures/misc/" + path + ".png");
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Projection(Map<ColumnKey, Surface> columns, List<Surface> surfaces) {
    }

    private record ProjectionCache(long gameTime, BlockPos originBlock, float radius,
                                   double maxDepth, double upOffset, Direction gravityDirection,
                                   Projection projection) {
    }

    private record ColumnKey(int a, int b) {
    }

    private static final class Surface {
        private final Vec3 columnCenter;
        private final double height;
        private final double offset;
        private int a;
        private int b;

        private Surface(Vec3 columnCenter, double height, double offset) {
            this.columnCenter = columnCenter;
            this.height = height;
            this.offset = offset;
        }
    }

    private enum Side {
        POSITIVE_A(1, 0, 1),
        NEGATIVE_A(-1, 0, -1),
        POSITIVE_B(0, 1, 1),
        NEGATIVE_B(0, -1, -1);

        private final int a;
        private final int b;
        private final int sign;

        Side(int a, int b, int sign) {
            this.a = a;
            this.b = b;
            this.sign = sign;
        }

        private Vec3 axis(Basis basis) {
            return a != 0 ? basis.axisAVec : basis.axisBVec;
        }

        private Vec3 otherAxis(Basis basis) {
            return a != 0 ? basis.axisBVec : basis.axisAVec;
        }

        private Vec3 normal(Basis basis) {
            return axis(basis).scale(sign);
        }

        private float[] uv(Vec3 point, Vec3 origin, Basis basis, double radius, double rotation) {
            float[] projected = BiokinesesAuraRenderer.uv(point, origin, basis, radius, rotation);
            if (a != 0) {
                projected[0] = sign > 0 ? (float) (1.0D - SIDE_EDGE_UV) : (float) SIDE_EDGE_UV;
            } else {
                projected[1] = sign > 0 ? (float) (1.0D - SIDE_EDGE_UV) : (float) SIDE_EDGE_UV;
            }
            return projected;
        }
    }

    private static final class Basis {
        private final Direction up;
        private final Direction down;
        private final Direction axisA;
        private final Direction axisB;
        private final Vec3 upVec;
        private final Vec3 axisAVec;
        private final Vec3 axisBVec;

        private Basis(Direction up, Direction axisA, Direction axisB) {
            this.up = up;
            this.down = up.getOpposite();
            this.axisA = axisA;
            this.axisB = axisB;
            this.upVec = Vec3.atLowerCornerOf(up.getNormal());
            this.axisAVec = Vec3.atLowerCornerOf(axisA.getNormal());
            this.axisBVec = Vec3.atLowerCornerOf(axisB.getNormal());
        }

        private static Basis forGravity(Direction gravity) {
            Direction up = gravity.getOpposite();
            Direction axisA = up.getAxis() == Direction.Axis.X ? Direction.UP : Direction.EAST;
            Direction axisB = switch (up) {
                case UP -> Direction.NORTH;
                case DOWN -> Direction.SOUTH;
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                case SOUTH -> Direction.UP;
                case NORTH -> Direction.DOWN;
            };
            return new Basis(up, axisA, axisB);
        }
    }

    private static final class Aura {
        private final UUID id;
        private final BiokinesesAuraTexture texture;
        private final Vec3 origin;
        private final int entityId;
        private final Direction gravityDirection;
        private final float opacity;
        private final float radius;
        private final double maxDepth;
        private final double upOffset;
        private final float rotation;
        private final float rotationSpeed;
        private final long createdAt;
        private final long updatedAt;
        private final long expiresAt;

        private Aura(UUID id, BiokinesesAuraTexture texture, Vec3 origin, int entityId,
                     Direction gravityDirection, float opacity, float radius, double maxDepth,
                     double upOffset, float rotation, float rotationSpeed, long createdAt,
                     long updatedAt, long expiresAt) {
            this.id = id;
            this.texture = texture;
            this.origin = origin;
            this.entityId = entityId;
            this.gravityDirection = gravityDirection;
            this.opacity = opacity;
            this.radius = radius;
            this.maxDepth = maxDepth;
            this.upOffset = upOffset;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.expiresAt = expiresAt;
        }

        private Aura copy() {
            return new Aura(id, texture, origin, entityId, gravityDirection, opacity, radius,
                maxDepth, upOffset, rotation, rotationSpeed, createdAt, updatedAt, expiresAt);
        }
    }
}
