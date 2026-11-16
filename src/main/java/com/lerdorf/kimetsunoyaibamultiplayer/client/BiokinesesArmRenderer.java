package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesArmController;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
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

/** Renders the six pale bone-like Biokinesis arms and their blade tips. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BiokinesesArmRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final ResourceLocation BONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "minecraft", "textures/block/bone_block_side.png");
    private static final RenderType ARM_RENDER_TYPE = RenderType.entityCutoutNoCull(BLANK_TEXTURE);
    private static final RenderType BONE_RENDER_TYPE = RenderType.entityCutoutNoCull(BONE_TEXTURE);
    private static final int ARM_GREY = 0xC9C8C2;
    private static final int ARM_HIGHLIGHT = 0xE7E5DE;
    private static final int BONE_TINT = 0xE2DCCF;
    private static final int SPIKE_WHITE = 0xF5F2E9;
    private static final int BASE_BONE_SEGMENTS = 3;
    private static final double ARM_REACH = 8.0D;
    private static final float SMOOTH_FACTOR = 0.15F;
    private static final Map<Integer, Vec3> GEO_TORSO_ANCHORS = new HashMap<>();
    private static final Map<String, SmoothedArmState> SMOOTHED_ARMS = new HashMap<>();

    private BiokinesesArmRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES
            || MINECRAFT.level == null || MINECRAFT.player == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        AABB renderArea = AABB.ofSize(cameraPosition, 128.0D, 128.0D, 128.0D);
        List<LivingEntity> entities = MINECRAFT.level.getEntitiesOfClass(LivingEntity.class, renderArea,
            entity -> entity.isAlive() && BiokinesesArmController.isActive(entity));
        if (entities.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer armBuffer = buffers.getBuffer(ARM_RENDER_TYPE);
        VertexConsumer boneBuffer = buffers.getBuffer(BONE_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (LivingEntity entity : entities) {
            renderEntityArms(entity, event.getPartialTick(), matrix, normal, armBuffer, boneBuffer);
        }

        SMOOTHED_ARMS.entrySet().removeIf(entry -> {
            int separator = entry.getKey().indexOf(':');
            if (separator < 0) {
                return true;
            }
            try {
                int entityId = Integer.parseInt(entry.getKey().substring(0, separator));
                Entity entity = MINECRAFT.level.getEntity(entityId);
                return !(entity instanceof LivingEntity living) || !living.isAlive()
                    || !BiokinesesArmController.isActive(living);
            } catch (NumberFormatException exception) {
                return true;
            }
        });

        buffers.endBatch(ARM_RENDER_TYPE);
        buffers.endBatch(BONE_RENDER_TYPE);
        poseStack.popPose();
    }

    private static void renderEntityArms(LivingEntity entity, float partialTick, Matrix4f matrix,
                                         Matrix3f normal, VertexConsumer armBuffer, VertexConsumer boneBuffer) {
        IBiokinesesArmData data = entity.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }

        Vec3 anchor = torsoAnchor(entity, partialTick);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            SmoothedArmState smoothed = smoothState(entity, arm, data);
            float progress = smoothed.progress();
            if (progress <= 0.01F || (data.getMode(arm) == IBiokinesesArmData.MODE_IDLE
                && !data.isRetracting())) {
                continue;
            }
            Vec3 source = armSource(anchor, entity, arm, partialTick);
            Vec3 target = smoothed.target();
            if (data.getHeldEntityId(arm) >= 0) {
                Entity held = entity.level().getEntity(data.getHeldEntityId(arm));
                if (held != null) {
                    target = held.position().add(0.0D, held.getBbHeight() * 0.5D, 0.0D);
                }
            }
            Vec3 tip = source.lerp(target, Mth.clamp(progress, 0.0D, 1.0D));
            List<Vec3> path = createIkPath(source, tip, entity, arm);
            renderArmPath(path, entity, matrix, normal, armBuffer, boneBuffer, arm);
            renderSpike(path.get(path.size() - 2), path.get(path.size() - 1), entity,
                matrix, normal, boneBuffer);
        }
    }

    private static SmoothedArmState smoothState(LivingEntity entity, int arm, IBiokinesesArmData data) {
        String key = entity.getId() + ":" + arm;
        Vec3 target = data.getTarget(arm);
        float progress = data.getProgress(arm);
        SmoothedArmState previous = SMOOTHED_ARMS.get(key);
        if (previous == null) {
            SmoothedArmState initial = new SmoothedArmState(target, progress);
            SMOOTHED_ARMS.put(key, initial);
            return initial;
        }
        SmoothedArmState current = new SmoothedArmState(
            previous.target().lerp(target, SMOOTH_FACTOR),
            previous.progress() + (progress - previous.progress()) * SMOOTH_FACTOR);
        SMOOTHED_ARMS.put(key, current);
        return current;
    }

    private static Vec3 torsoAnchor(LivingEntity entity, float partialTick) {
        Vec3 geoAnchor = GEO_TORSO_ANCHORS.get(entity.getId());
        if (geoAnchor != null) {
            return geoAnchor;
        }
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.58D;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).normalize();
        return new Vec3(x, y, z).subtract(forward.scale(0.24D));
    }

    private static Vec3 armSource(Vec3 torso, LivingEntity entity, int arm, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 left = up.cross(forward).normalize();
        boolean leftArm = arm == BiokinesesArmController.ARM_LEFT_UPPER
            || arm == BiokinesesArmController.ARM_LEFT_LOWER
            || arm == BiokinesesArmController.ARM_LEFT_COMBAT;
        double side = leftArm ? 1.0D : -1.0D;
        double height = arm == BiokinesesArmController.ARM_LEFT_UPPER
            || arm == BiokinesesArmController.ARM_RIGHT_UPPER ? 0.34D
            : arm == BiokinesesArmController.ARM_LEFT_LOWER
                || arm == BiokinesesArmController.ARM_RIGHT_LOWER ? -0.18D : 0.18D;
        double sideOffset = arm == BiokinesesArmController.ARM_LEFT_COMBAT
            || arm == BiokinesesArmController.ARM_RIGHT_COMBAT ? 0.4D : 0.3D;
        double forwardOffset = arm == BiokinesesArmController.ARM_LEFT_COMBAT
            || arm == BiokinesesArmController.ARM_RIGHT_COMBAT ? 0.18D : -0.08D;
        return torso.add(left.scale(side * sideOffset)).add(up.scale(height)).add(forward.scale(forwardOffset));
    }

    /** Called by the GeckoLib layer after it resolves the animated body bone. */
    public static void captureGeoTorsoAnchor(int entityId, Vec3 worldPosition) {
        if (worldPosition != null) {
            GEO_TORSO_ANCHORS.put(entityId, worldPosition);
        }
    }

    private static List<Vec3> createIkPath(Vec3 start, Vec3 target, LivingEntity entity, int arm) {
        Vec3 difference = target.subtract(start);
        double distance = difference.length();
        if (distance < 1.0E-5D) {
            return List.of(start, target);
        }

        Vec3 direction = difference.scale(1.0D / distance);
        double totalLength = Mth.clamp(distance + 0.8D, 2.4D, ARM_REACH);
        double upperLength = totalLength * 0.53D;
        double lowerLength = totalLength - upperLength;
        Vec3 reachableTarget = distance > totalLength
            ? start.add(direction.scale(totalLength - 0.04D)) : target;

        Vec3 forward = Vec3.directionFromRotation(0.0F, entity.yBodyRot).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = up.cross(forward).normalize();
        if (arm == BiokinesesArmController.ARM_RIGHT_UPPER
            || arm == BiokinesesArmController.ARM_RIGHT_LOWER
            || arm == BiokinesesArmController.ARM_RIGHT_COMBAT) {
            side = side.scale(-1.0D);
        }
        Vec3 pole = side.scale(0.85D).add(up.scale(0.9D)).add(forward.scale(0.25D));
        Vec3 bendDirection = pole.subtract(direction.scale(pole.dot(direction)));
        if (bendDirection.lengthSqr() < 1.0E-6D) {
            bendDirection = up.subtract(direction.scale(up.dot(direction)));
        }
        bendDirection = bendDirection.normalize();

        double jointDistance = reachableTarget.distanceTo(start);
        double cosine = Mth.clamp((upperLength * upperLength + jointDistance * jointDistance
            - lowerLength * lowerLength) / (2.0D * upperLength * Math.max(jointDistance, 1.0E-5D)), -1.0D, 1.0D);
        double jointHeight = Math.sqrt(Math.max(0.0D, upperLength * upperLength
            - (cosine * upperLength) * (cosine * upperLength)));
        Vec3 elbow = start.add(direction.scale(cosine * upperLength)).add(bendDirection.scale(jointHeight));

        List<Vec3> path = new ArrayList<>(15);
        appendSegment(path, start, elbow, 7, arm);
        appendSegment(path, elbow, reachableTarget, 7, arm + 17);
        return path;
    }

    private static void appendSegment(List<Vec3> path, Vec3 start, Vec3 end, int count, int phase) {
        Vec3 direction = end.subtract(start);
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        for (int i = 0; i <= count; i++) {
            if (!path.isEmpty() && i == 0) {
                continue;
            }
            double progress = i / (double) count;
            double arc = Math.sin(progress * Math.PI);
            double ripple = Math.sin(phase * 0.37D + progress * Math.PI) * 0.035D;
            path.add(start.lerp(end, progress).add(side.scale(arc * ripple)));
        }
    }

    private static void renderArmPath(List<Vec3> path, LivingEntity entity, Matrix4f matrix,
                                      Matrix3f normal, VertexConsumer armBuffer,
                                      VertexConsumer boneBuffer, int arm) {
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 start = path.get(i);
            Vec3 end = path.get(i + 1);
            float segmentProgress = i / (float) Math.max(1, path.size() - 2);
            boolean bone = i < BASE_BONE_SEGMENTS;
            float width = bone
                ? Mth.lerp(segmentProgress, 0.105F, 0.072F)
                : Mth.lerp(segmentProgress, 0.080F, 0.045F);
            int color = bone ? BONE_TINT : ((i + arm) % 4 == 0 ? ARM_HIGHLIGHT : ARM_GREY);
            drawBeam(start, end, width, color, entity.level(), matrix, normal,
                bone ? boneBuffer : armBuffer);
        }
    }

    private static void renderSpike(Vec3 previous, Vec3 tip, LivingEntity entity, Matrix4f matrix,
                                    Matrix3f normal, VertexConsumer buffer) {
        Vec3 direction = tip.subtract(previous);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(0.075D);
        Vec3 otherSide = direction.cross(side).normalize().scale(0.045D);
        Vec3 base = tip.subtract(direction.scale(0.40D));
        Vec3 pointA = base.add(side).add(otherSide);
        Vec3 pointB = base.subtract(side).add(otherSide);
        Vec3 pointC = base.subtract(side).subtract(otherSide);
        Vec3 pointD = base.add(side).subtract(otherSide);
        drawTriangle(tip, pointA, pointB, SPIKE_WHITE, entity.level(), matrix, normal, buffer,
            0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        drawTriangle(tip, pointB, pointC, SPIKE_WHITE, entity.level(), matrix, normal, buffer,
            0.5F, 0.0F, 1.0F, 1.0F, 0.5F, 1.0F);
        drawTriangle(tip, pointC, pointD, SPIKE_WHITE, entity.level(), matrix, normal, buffer,
            0.5F, 0.0F, 0.5F, 1.0F, 0.0F, 1.0F);
        drawTriangle(tip, pointD, pointA, SPIKE_WHITE, entity.level(), matrix, normal, buffer,
            0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawBeam(Vec3 start, Vec3 end, float width, int color,
                                 net.minecraft.world.level.Level level, Matrix4f matrix,
                                 Matrix3f normal, VertexConsumer buffer) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(width);
        Vec3 otherSide = direction.cross(side).normalize().scale(width);
        Vec3[] corners = {
            start.add(side).add(otherSide), start.subtract(side).add(otherSide),
            start.subtract(side).subtract(otherSide), start.add(side).subtract(otherSide),
            end.add(side).add(otherSide), end.subtract(side).add(otherSide),
            end.subtract(side).subtract(otherSide), end.add(side).subtract(otherSide)
        };
        drawQuad(corners[0], corners[4], corners[5], corners[1], color, level, matrix, normal, buffer);
        drawQuad(corners[1], corners[5], corners[6], corners[2], color, level, matrix, normal, buffer);
        drawQuad(corners[2], corners[6], corners[7], corners[3], color, level, matrix, normal, buffer);
        drawQuad(corners[3], corners[7], corners[4], corners[0], color, level, matrix, normal, buffer);
        drawQuad(corners[0], corners[4], corners[7], corners[3], color, level, matrix, normal, buffer);
        drawQuad(corners[1], corners[2], corners[6], corners[5], color, level, matrix, normal, buffer);
    }

    private static void drawQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
                                 net.minecraft.world.level.Level level, Matrix4f matrix,
                                 Matrix3f normal, VertexConsumer buffer) {
        vertex(a, color, level, matrix, normal, buffer, 0.0F, 0.0F);
        vertex(b, color, level, matrix, normal, buffer, 1.0F, 0.0F);
        vertex(c, color, level, matrix, normal, buffer, 1.0F, 1.0F);
        vertex(d, color, level, matrix, normal, buffer, 0.0F, 1.0F);
    }

    private static void drawTriangle(Vec3 a, Vec3 b, Vec3 c, int color,
                                     net.minecraft.world.level.Level level, Matrix4f matrix,
                                     Matrix3f normal, VertexConsumer buffer,
                                     float au, float av, float bu, float bv, float cu, float cv) {
        vertex(a, color, level, matrix, normal, buffer, au, av);
        vertex(b, color, level, matrix, normal, buffer, bu, bv);
        vertex(c, color, level, matrix, normal, buffer, cu, cv);
    }

    private static void vertex(Vec3 position, int color, net.minecraft.world.level.Level level,
                               Matrix4f matrix, Matrix3f normal, VertexConsumer buffer,
                               float u, float v) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(red, green, blue, 1.0F)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LevelRenderer.getLightColor(level, BlockPos.containing(position)))
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private record SmoothedArmState(Vec3 target, float progress) {
    }

}
