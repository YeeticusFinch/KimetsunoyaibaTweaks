package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.RuiBloodDemonArt;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiWebEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;
import java.util.UUID;

/** Renders Rui's persistent webs as thin camera-facing thread curves. */
public class RuiWebRenderer extends EntityRenderer<RuiWebEntity> {
    private static final ResourceLocation THREAD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/silk_ribbon.png");
    private static final int CIRCLE_SEGMENTS = 20;
    private static final float CAGE_THREAD_WIDTH = 0.025F;
    private static final float THIN_THREAD_WIDTH = 0.002F;
    private static final double CAGE_INITIAL_HALF = 7.5D;

    public RuiWebRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RuiWebEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        if (entity.getPattern() == RuiWebEntity.Pattern.ROTATION) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        }
        if (entity.getPattern() == RuiWebEntity.Pattern.EYE_BASKET_PULSE) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getEyePitch()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getEyeRoll()));
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getEyeYaw()));
        }
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(THREAD_TEXTURE));
        float red = entity.isRedThreads() ? 0.85F : 0.96F;
        float green = entity.isRedThreads() ? 0.06F : 0.96F;
        float blue = entity.isRedThreads() ? 0.10F : 1.0F;
        float scale = entity.getVisualScale();

        switch (entity.getPattern()) {
            case DICING_WALL -> drawDicingWall(entity, pose, buffer, scale, red, green, blue);
            case CUTTING_CAGE -> drawCage(entity, pose, buffer, red, green, blue);
            case CONTORTING -> drawContorting(pose, buffer, scale, red, green, blue);
            case EYE_BASKET -> drawEyeBasket(entity, pose, buffer, scale, red, green, blue);
            case ROTATION -> drawRotation(entity, pose, buffer, scale, red, green, blue, partialTick);
            case EYE_BASKET_PULSE -> drawEyeBasketPulse(entity, pose, buffer, red, green, blue);
            case DECAPITATION_CIRCLE -> drawCircle(pose, buffer, entity.getEyeRadius(), 0.0D,
                red, green, blue);
        }
        poseStack.popPose();
    }

    private static void drawDicingWall(RuiWebEntity entity, PoseStack.Pose pose, VertexConsumer buffer,
                                       float scale, float red, float green, float blue) {
        float half = 4.0F * scale;
        Random random = new Random(0x5EEDL ^ (long) entity.getId() * 0x9E3779B9L);
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI;
            double centerX = (random.nextDouble() * 2.0D - 1.0D) * half;
            double centerY = (random.nextDouble() * 2.0D - 1.0D) * half;
            double length = (6.0D + random.nextDouble() * 6.0D) * scale;
            double dx = Math.cos(angle) * length;
            double dy = Math.sin(angle) * length;
            line(pose, buffer,
                new Vec3(centerX - dx, centerY - dy, 0),
                new Vec3(centerX + dx, centerY + dy, 0),
                0.07F, red, green, blue);
        }
        drawDicingOwnerThreads(entity, pose, buffer, half, red, green, blue);
    }

    private static void drawDicingOwnerThreads(RuiWebEntity entity, PoseStack.Pose pose,
                                               VertexConsumer buffer, float half,
                                               float red, float green, float blue) {
        LivingEntity owner = findOwner(entity);
        if (owner == null) {
            return;
        }

        Vec3 ownerHead = worldToLocal(entity,
            RuiBloodDemonArt.getThreadAnchor(owner).subtract(entity.position()));
        Random random = new Random(0xD1C1A90L ^ (long) entity.getId() * 0x9E3779B9L);
        for (int thread = 0; thread < 3; thread++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = half * (0.35D + random.nextDouble() * 0.65D);
            Vec3 wallPoint = new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0D);
            Vec3 outward = new Vec3(wallPoint.x, wallPoint.y, 0.0D);
            if (outward.lengthSqr() < 1.0E-4D) {
                outward = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                outward = outward.normalize();
            }
            Vec3 control = wallPoint.add(outward.scale(3.0D + random.nextDouble() * 4.0D));
            drawQuadraticThread(pose, buffer, ownerHead, control, wallPoint,
                THIN_THREAD_WIDTH, red, green, blue);
        }
    }

    private static void drawCage(RuiWebEntity entity, PoseStack.Pose pose, VertexConsumer buffer,
                                 float red, float green, float blue) {
        float half = entity.getCageWidth() * 0.5F;
        if (half <= 0.01F) {
            return;
        }

        drawCageFrame(pose, buffer, half, red, green, blue);

        Random random = new Random(0xCA6E5EEDL ^ (long) entity.getId() * 0x9E3779B9L);
        for (int face = 0; face < 6; face++) {
            for (int thread = 0; thread < 10; thread++) {
                int edgeA = random.nextInt(4);
                int edgeB = (edgeA + 1 + random.nextInt(3)) % 4;
                Vec3 facePointA = cageFacePoint(face, edgeA, random.nextDouble() * 2.0D - 1.0D, half);
                Vec3 facePointB = cageFacePoint(face, edgeB, random.nextDouble() * 2.0D - 1.0D, half);
                line(pose, buffer, facePointA, facePointB,
                    CAGE_THREAD_WIDTH, red, green, blue);
            }

            for (int thread = 0; thread < 3; thread++) {
                int edgeA = random.nextInt(4);
                double positionA = random.nextDouble() * 2.0D - 1.0D;
                Vec3 facePoint = cageFacePoint(face, edgeA, positionA, half);
                Vec3 blockPoint = nearbyBlockPoint(entity, face, facePoint, half, random);
                line(pose, buffer, facePoint, blockPoint,
                    CAGE_THREAD_WIDTH, red, green, blue);
            }
        }
    }

    private static void drawCageFrame(PoseStack.Pose pose, VertexConsumer buffer, float half,
                                      float red, float green, float blue) {
        Vec3[] corners = {
            new Vec3(-half, -half, -half),
            new Vec3(half, -half, -half),
            new Vec3(half, half, -half),
            new Vec3(-half, half, -half),
            new Vec3(-half, -half, half),
            new Vec3(half, -half, half),
            new Vec3(half, half, half),
            new Vec3(-half, half, half)
        };
        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            line(pose, buffer, corners[edge[0]], corners[edge[1]],
                CAGE_THREAD_WIDTH, red, green, blue);
        }
    }

    private static Vec3 nearbyBlockPoint(RuiWebEntity entity, int face, Vec3 facePoint,
                                         double half, Random random) {
        Vec3 normal = cageFaceNormal(face);
        // Select the attachment from the cage's initial face so shrinking the
        // cage does not make the thread jump to a different block.
        double scale = CAGE_INITIAL_HALF / Math.max(0.01D, half);
        Vec3 stableFacePoint = facePoint.scale(scale);
        Vec3 candidateLocal = stableFacePoint.add(normal.scale(2.0D + random.nextDouble() * 5.0D));
        Vec3 candidateWorld = entity.position().add(localToWorld(entity, candidateLocal));
        BlockPos origin = BlockPos.containing(candidateWorld);

        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos block = origin.offset(random.nextInt(5) - 2,
                random.nextInt(5) - 2, random.nextInt(5) - 2);
            if (!entity.level().getBlockState(block).getCollisionShape(entity.level(), block).isEmpty()) {
                return worldToLocal(entity, Vec3.atCenterOf(block).subtract(entity.position()));
            }
        }
        return candidateLocal;
    }

    private static Vec3 cageFaceNormal(int face) {
        return switch (face) {
            case 0 -> new Vec3(0.0D, 0.0D, 1.0D);
            case 1 -> new Vec3(0.0D, 0.0D, -1.0D);
            case 2 -> new Vec3(1.0D, 0.0D, 0.0D);
            case 3 -> new Vec3(-1.0D, 0.0D, 0.0D);
            case 4 -> new Vec3(0.0D, 1.0D, 0.0D);
            default -> new Vec3(0.0D, -1.0D, 0.0D);
        };
    }

    private static LivingEntity findOwner(RuiWebEntity entity) {
        UUID ownerUuid = entity.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        AABB search = new AABB(entity.getX() - 256.0D, entity.getY() - 256.0D, entity.getZ() - 256.0D,
            entity.getX() + 256.0D, entity.getY() + 256.0D, entity.getZ() + 256.0D);
        for (Entity candidate : entity.level().getEntitiesOfClass(Entity.class, search,
            value -> ownerUuid.equals(value.getUUID()))) {
            if (candidate instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static Vec3 worldToLocal(RuiWebEntity entity, Vec3 worldOffset) {
        double yaw = Math.toRadians(entity.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec3(worldOffset.x * cos + worldOffset.z * sin, worldOffset.y,
            -worldOffset.x * sin + worldOffset.z * cos);
    }

    private static Vec3 localToWorld(RuiWebEntity entity, Vec3 localOffset) {
        double yaw = Math.toRadians(entity.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec3(localOffset.x * cos - localOffset.z * sin, localOffset.y,
            localOffset.x * sin + localOffset.z * cos);
    }

    private static void drawQuadraticThread(PoseStack.Pose pose, VertexConsumer buffer,
                                            Vec3 start, Vec3 control, Vec3 end, float width,
                                            float red, float green, float blue) {
        Vec3 previous = start;
        for (int index = 1; index <= 12; index++) {
            double progress = index / 12.0D;
            double inverse = 1.0D - progress;
            Vec3 current = start.scale(inverse * inverse)
                .add(control.scale(2.0D * inverse * progress))
                .add(end.scale(progress * progress));
            line(pose, buffer, previous, current, width, red, green, blue);
            previous = current;
        }
    }

    private static Vec3 cageFacePoint(int face, int edge, double position, double half) {
        return switch (face) {
            case 0 -> switch (edge) {
                case 0 -> new Vec3(position * half, -half, half);
                case 1 -> new Vec3(half, position * half, half);
                case 2 -> new Vec3(position * half, half, half);
                default -> new Vec3(-half, position * half, half);
            };
            case 1 -> switch (edge) {
                case 0 -> new Vec3(position * half, -half, -half);
                case 1 -> new Vec3(half, position * half, -half);
                case 2 -> new Vec3(position * half, half, -half);
                default -> new Vec3(-half, position * half, -half);
            };
            case 2 -> switch (edge) {
                case 0 -> new Vec3(half, -half, position * half);
                case 1 -> new Vec3(half, position * half, half);
                case 2 -> new Vec3(half, half, position * half);
                default -> new Vec3(half, position * half, -half);
            };
            case 3 -> switch (edge) {
                case 0 -> new Vec3(-half, -half, position * half);
                case 1 -> new Vec3(-half, position * half, half);
                case 2 -> new Vec3(-half, half, position * half);
                default -> new Vec3(-half, position * half, -half);
            };
            case 4 -> switch (edge) {
                case 0 -> new Vec3(position * half, half, -half);
                case 1 -> new Vec3(half, half, position * half);
                case 2 -> new Vec3(position * half, half, half);
                default -> new Vec3(-half, half, position * half);
            };
            default -> switch (edge) {
                case 0 -> new Vec3(position * half, -half, -half);
                case 1 -> new Vec3(half, -half, position * half);
                case 2 -> new Vec3(position * half, -half, half);
                default -> new Vec3(-half, -half, position * half);
            };
        };
    }

    private static void drawContorting(PoseStack.Pose pose, VertexConsumer buffer, float scale,
                                       float red, float green, float blue) {
        float radius = 3.2F * scale;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2.0D / 16.0D;
            Vec3 outer = new Vec3(Math.cos(angle) * radius,
                Math.sin(angle * 2.0D) * radius * 0.5D, Math.sin(angle) * radius);
            line(pose, buffer, outer, Vec3.ZERO, 0.06F, red, green, blue);
        }
    }

    private static void drawEyeBasket(RuiWebEntity entity, PoseStack.Pose pose, VertexConsumer buffer,
                                      float scale, float red, float green, float blue) {
        drawCircle(pose, buffer, entity.getEyeRadius() * scale, 0.0D,
            entity.getEyeProgress(), red, green, blue);
    }

    private static void drawEyeBasketPulse(RuiWebEntity entity, PoseStack.Pose pose, VertexConsumer buffer,
                                           float red, float green, float blue) {
        drawCircle(pose, buffer, entity.getEyeRadius(), 0.0D,
            Math.min(1.0D, (entity.tickCount + 1.0D) / 3.0D), red, green, blue);
    }

    private static void drawRotation(RuiWebEntity entity, PoseStack.Pose pose, VertexConsumer buffer,
                                     float scale, float red, float green, float blue, float partialTick) {
        float angle = entity.getRotationAngle();
        for (int index = 0; index < 8; index++) {
            double baseAngle = Math.PI * 0.5D + index * Math.PI * 0.25D;
            boolean clockwise = ((index + 1) & 1) == 0;
            double circleAngle = baseAngle + (clockwise ? -angle : angle);
            double centerX = Math.cos(circleAngle);
            double centerY = Math.sin(circleAngle);
            drawCanvasCircle(pose, buffer, centerX * scale, centerY * scale,
                entity.getRotationZOffset(index), 2.0D * scale, red, green, blue);
        }
        drawRotationTether(entity, pose, buffer, angle, partialTick, red, green, blue);
    }

    private static void drawCanvasCircle(PoseStack.Pose pose, VertexConsumer buffer,
                                         double centerX, double centerY, double centerZ, double radius,
                                         float red, float green, float blue) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a = i * Math.PI * 2.0D / CIRCLE_SEGMENTS;
            double b = (i + 1) * Math.PI * 2.0D / CIRCLE_SEGMENTS;
            line(pose, buffer,
                new Vec3(centerX + Math.cos(a) * radius, centerY + Math.sin(a) * radius, centerZ),
                new Vec3(centerX + Math.cos(b) * radius, centerY + Math.sin(b) * radius, centerZ),
                0.06F, red, green, blue);
        }
    }

    private static void drawRotationTether(RuiWebEntity entity, PoseStack.Pose pose,
                                           VertexConsumer buffer, float angle, float partialTick,
                                           float red, float green, float blue) {
        double firstAngle = Math.PI * 0.5D + angle;
        double radialX = Math.cos(firstAngle);
        double radialY = Math.sin(firstAngle);
        Vec3 start = new Vec3(radialX * 3.0D, radialY * 3.0D,
            entity.getRotationZOffset(0));
        double length = Math.max(0.5D, entity.tickCount + partialTick);
        LivingEntity owner = findOwner(entity);
        Vec3 end = owner != null
            ? worldToRotationLocal(entity,
                RuiBloodDemonArt.getThreadAnchor(owner).subtract(entity.position()))
            : new Vec3(0.0D, 0.0D, -length);
        Vec3 previous = start;
        for (int i = 1; i <= 24; i++) {
            double progress = i / 24.0D;
            Vec3 base = start.lerp(end, progress);
            double spiralAngle = angle * 1.5D + progress * Math.PI * 4.0D;
            double amplitude = 0.35D * Math.sin(Math.PI * progress);
            Vec3 current = base.add(Math.cos(spiralAngle) * amplitude,
                Math.sin(spiralAngle) * amplitude, 0.0D);
            line(pose, buffer, previous, current, THIN_THREAD_WIDTH, red, green, blue);
            previous = current;
        }
    }

    private static Vec3 worldToRotationLocal(RuiWebEntity entity, Vec3 worldOffset) {
        Vec3 forward = entity.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        return new Vec3(worldOffset.dot(right), worldOffset.dot(up), worldOffset.dot(forward));
    }

    private static void drawCircle(PoseStack.Pose pose, VertexConsumer buffer, double radius, double y,
                                   float red, float green, float blue) {
        drawCircle(pose, buffer, radius, y, 1.0D, red, green, blue);
    }

    private static void drawCircle(PoseStack.Pose pose, VertexConsumer buffer, double radius, double y,
                                   double progress, float red, float green, float blue) {
        int segments = Math.min(CIRCLE_SEGMENTS,
            Math.max(0, (int) Math.ceil(CIRCLE_SEGMENTS * Math.max(0.0D, Math.min(1.0D, progress)))));
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0D / CIRCLE_SEGMENTS;
            double b = (i + 1) * Math.PI * 2.0D / CIRCLE_SEGMENTS;
            line(pose, buffer, new Vec3(Math.cos(a) * radius, y, Math.sin(a) * radius),
                new Vec3(Math.cos(b) * radius, y, Math.sin(b) * radius),
                0.06F, red, green, blue);
        }
    }

    private static void line(PoseStack.Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end,
                             float width, float red, float green, float blue) {
        Vector3f a = new Vector3f((float) start.x, (float) start.y, (float) start.z);
        Vector3f b = new Vector3f((float) end.x, (float) end.y, (float) end.z);
        Vector3f direction = new Vector3f(b).sub(a);
        Vector3f side = direction.cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if (side.lengthSquared() < 1.0E-6F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize().mul(width);
        vertex(buffer, pose.pose(), pose.normal(), new Vector3f(a).add(side), red, green, blue);
        vertex(buffer, pose.pose(), pose.normal(), new Vector3f(b).add(side), red, green, blue);
        vertex(buffer, pose.pose(), pose.normal(), new Vector3f(b).sub(side), red, green, blue);
        vertex(buffer, pose.pose(), pose.normal(), new Vector3f(a).sub(side), red, green, blue);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, Vector3f point,
                               float red, float green, float blue) {
        buffer.vertex(matrix, point.x(), point.y(), point.z()).color(red, green, blue, 1.0F)
            .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(RuiWebEntity entity) {
        return THREAD_TEXTURE;
    }
}
