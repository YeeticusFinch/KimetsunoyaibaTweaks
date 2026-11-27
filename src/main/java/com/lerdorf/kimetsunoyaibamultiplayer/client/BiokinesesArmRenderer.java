package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.BiokinesesTentacleConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesArmController;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders the six flesh-and-bone Biokinesis tentacles and their sharpened tips. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BiokinesesArmRenderer {
    private static final Minecraft MINECRAFT = Minecraft.getInstance();
    private static final ResourceLocation BONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "minecraft", "textures/block/bone_block_side.png");
    private static final ResourceLocation FLESH_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/biokineses_flesh.png");
    private static final RenderType ARM_RENDER_TYPE = RenderType.entityCutoutNoCull(FLESH_TEXTURE);
    private static final RenderType BONE_RENDER_TYPE = RenderType.entityCutoutNoCull(BONE_TEXTURE);
    private static final int ARM_FLESH = 0x9E2634;
    private static final int ARM_FLESH_HIGHLIGHT = 0xB53A48;
    private static final int BONE_TINT = 0xE2DCCF;
    private static final int SPIKE_WHITE = 0xF5F2E9;
    private static final int PATH_SEGMENTS = 24;
    private static final float SMOOTH_FACTOR = 0.32F;
    private static final Map<Integer, TorsoTransform> GEO_TORSO_TRANSFORMS = new HashMap<>();
    private static final Map<String, SmoothedArmState> SMOOTHED_ARMS = new HashMap<>();
    private static final Matrix4f WORLD_FROM_VIEW = new Matrix4f();
    private static boolean hasViewTransform;

    private BiokinesesArmRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            GEO_TORSO_TRANSFORMS.clear();
            WORLD_FROM_VIEW.set(event.getPoseStack().last().pose())
                .setTranslation(0.0F, 0.0F, 0.0F)
                .invert();
            hasViewTransform = true;
            return;
        }
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
        MultiBufferSource.BufferSource buffers = MINECRAFT.renderBuffers().bufferSource();
        VertexConsumer armBuffer = buffers.getBuffer(ARM_RENDER_TYPE);
        VertexConsumer boneBuffer = buffers.getBuffer(BONE_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (LivingEntity entity : entities) {
            renderEntityArms(entity, event.getPartialTick(), cameraPosition, matrix, normal,
                armBuffer, boneBuffer);
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

    private static void renderEntityArms(LivingEntity entity, float partialTick, Vec3 cameraPosition,
                                         Matrix4f matrix, Matrix3f normal, VertexConsumer armBuffer,
                                         VertexConsumer boneBuffer) {
        IBiokinesesArmData data = entity.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS).orElse(null);
        if (data == null || !data.isActive()) {
            return;
        }

        TorsoTransform torso = torsoTransform(entity, partialTick);
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            SmoothedArmState smoothed = smoothState(entity, arm, data);
            float progress = smoothed.progress();
            if (progress <= 0.01F || (data.getMode(arm) == IBiokinesesArmData.MODE_IDLE
                && !data.isRetracting())) {
                continue;
            }
            Vec3 source = armSource(torso, arm);
            Vec3 target = smoothed.target();
            if (data.getHeldEntityId(arm) >= 0) {
                Entity held = entity.level().getEntity(data.getHeldEntityId(arm));
                if (held != null) {
                    target = held.position().add(0.0D, held.getBbHeight() * 0.5D, 0.0D);
                }
            }
            Vec3 tip = source.lerp(target, Mth.clamp(progress, 0.0D, 1.0D));
            Vec3 terminalDirection = anchorTerminalDirection(entity, data, arm, target, torso);
            double maxReach = data.getMode(arm) == IBiokinesesArmData.MODE_ABILITY
                ? Math.max(BiokinesesArmController.TENTACLE_REACH,
                    source.distanceTo(target) + 0.8D)
                : BiokinesesArmController.TENTACLE_REACH;
            List<Vec3> path = createIkPath(source, tip, torso, arm, terminalDirection, maxReach);
            renderArmPath(path, BiokinesesTentacleConfig.spikeLength, entity, cameraPosition, matrix, normal,
                armBuffer, boneBuffer, arm);
            renderSpike(path.get(path.size() - 2), path.get(path.size() - 1), entity,
                cameraPosition, matrix, normal, boneBuffer, BiokinesesTentacleConfig.spikeLength,
                BiokinesesTentacleConfig.spikeWidth, BiokinesesTentacleConfig.spikeDepth);
        }
    }

    /** Renders a configuration-only preview using the same path and geometry as live arms. */
    public static void renderPreviewArms(GuiGraphics graphics, int centerX, int baseY, float scale,
                                         float orbitYaw, float orbitPitch) {
        if (MINECRAFT.level == null) {
            return;
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        // Entity render types use model/world coordinates, unlike GuiGraphics fills and text.
        // Match InventoryScreen's translated preview space instead of submitting pixel positions.
        poseStack.translate(centerX, baseY, 50.0D);
        poseStack.scale(scale, -scale, scale);
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        VertexConsumer armBuffer = buffers.getBuffer(ARM_RENDER_TYPE);
        VertexConsumer boneBuffer = buffers.getBuffer(BONE_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        TorsoTransform torso = new TorsoTransform(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D),
            new Vec3(0.0D, 1.0D, 0.0D), new Vec3(1.0D, 0.0D, 0.0D));

        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            Vec3 source = armSource(torso, arm);
            Vec3 target = torso.origin().add(torsoVector(torso, new Vec3(
                BiokinesesTentacleConfig.targetLeft(arm),
                BiokinesesTentacleConfig.targetUp(arm),
                BiokinesesTentacleConfig.targetForward(arm))));
            Vec3 terminalDirection = configuredTargetDirection(torso, arm);
            double maxReach = Math.max(BiokinesesArmController.TENTACLE_REACH,
                source.distanceTo(target) + 0.8D);
            List<Vec3> path = createIkPath(source, target, torso, arm, terminalDirection, maxReach);
            List<Vec3> previewPath = path.stream()
                .map(point -> previewPoint(point, orbitYaw, orbitPitch))
                .toList();
            renderPreviewArmPath(previewPath, BiokinesesTentacleConfig.spikeLength, matrix, normal,
                armBuffer, boneBuffer, arm);
            renderPreviewSpike(previewPath.get(previewPath.size() - 2), previewPath.get(previewPath.size() - 1),
                matrix, normal, boneBuffer, BiokinesesTentacleConfig.spikeLength,
                BiokinesesTentacleConfig.spikeWidth, BiokinesesTentacleConfig.spikeDepth);
        }

        buffers.endBatch(ARM_RENDER_TYPE);
        buffers.endBatch(BONE_RENDER_TYPE);
        poseStack.popPose();
    }

    private static Vec3 previewPoint(Vec3 point, float orbitYaw, float orbitPitch) {
        Vec3 rotated = rotateAround(point, new Vec3(0.0D, 1.0D, 0.0D), Math.toRadians(orbitYaw));
        rotated = rotateAround(rotated, new Vec3(1.0D, 0.0D, 0.0D), Math.toRadians(orbitPitch));
        return rotated;
    }

    private static void renderPreviewArmPath(List<Vec3> path, double tailDistance, Matrix4f matrix,
                                             Matrix3f normal, VertexConsumer armBuffer,
                                             VertexConsumer boneBuffer, int arm) {
        List<Vec3> fleshPath = trimTail(path, tailDistance);
        double distanceFromStart = 0.0D;
        for (int i = 0; i < fleshPath.size() - 1; i++) {
            Vec3 start = fleshPath.get(i);
            Vec3 end = fleshPath.get(i + 1);
            boolean bone = distanceFromStart < BiokinesesTentacleConfig.boneLength;
            float width = (float) ((bone ? BiokinesesTentacleConfig.boneWidth
                : BiokinesesTentacleConfig.fleshWidth) * 0.5D);
            int color = bone ? BONE_TINT
                : ((i + arm) % 4 == 0 ? ARM_FLESH_HIGHLIGHT : ARM_FLESH);
            drawPreviewBeam(start, end, width, color, matrix, normal, bone ? boneBuffer : armBuffer);
            distanceFromStart += start.distanceTo(end);
        }
    }

    private static void renderPreviewSpike(Vec3 previous, Vec3 tip, Matrix4f matrix, Matrix3f normal,
                                           VertexConsumer buffer, double spikeLength, double spikeWidth,
                                           double spikeDepth) {
        Vec3 direction = tip.subtract(previous);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(spikeWidth * 0.5D);
        Vec3 otherSide = direction.cross(side).normalize().scale(spikeDepth * 0.5D);
        Vec3 base = tip.subtract(direction.scale(spikeLength));
        Vec3 pointA = base.add(side).add(otherSide);
        Vec3 pointB = base.subtract(side).add(otherSide);
        Vec3 pointC = base.subtract(side).subtract(otherSide);
        Vec3 pointD = base.add(side).subtract(otherSide);
        drawPreviewTriangle(tip, pointA, pointB, matrix, normal, buffer, 0.5F, 0.0F, 0.0F,
            1.0F, 1.0F, 1.0F);
        drawPreviewTriangle(tip, pointB, pointC, matrix, normal, buffer, 0.5F, 0.0F, 1.0F,
            1.0F, 0.5F, 1.0F);
        drawPreviewTriangle(tip, pointC, pointD, matrix, normal, buffer, 0.5F, 0.0F, 0.5F,
            1.0F, 0.0F, 1.0F);
        drawPreviewTriangle(tip, pointD, pointA, matrix, normal, buffer, 0.5F, 0.0F, 0.0F,
            1.0F, 1.0F, 1.0F);
    }

    private static void drawPreviewBeam(Vec3 start, Vec3 end, float width, int color,
                                        Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
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
        drawPreviewQuad(corners[0], corners[4], corners[5], corners[1], color, matrix, normal, buffer);
        drawPreviewQuad(corners[1], corners[5], corners[6], corners[2], color, matrix, normal, buffer);
        drawPreviewQuad(corners[2], corners[6], corners[7], corners[3], color, matrix, normal, buffer);
        drawPreviewQuad(corners[3], corners[7], corners[4], corners[0], color, matrix, normal, buffer);
        drawPreviewQuad(corners[0], corners[4], corners[7], corners[3], color, matrix, normal, buffer);
        drawPreviewQuad(corners[1], corners[2], corners[6], corners[5], color, matrix, normal, buffer);
    }

    private static void drawPreviewQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
                                        Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
        previewVertex(a, color, matrix, normal, buffer, 0.0F, 0.0F);
        previewVertex(b, color, matrix, normal, buffer, 1.0F, 0.0F);
        previewVertex(c, color, matrix, normal, buffer, 1.0F, 1.0F);
        previewVertex(d, color, matrix, normal, buffer, 0.0F, 1.0F);
    }

    private static void drawPreviewTriangle(Vec3 a, Vec3 b, Vec3 c, Matrix4f matrix,
                                            Matrix3f normal, VertexConsumer buffer,
                                            float au, float av, float bu, float bv, float cu, float cv) {
        previewVertex(a, SPIKE_WHITE, matrix, normal, buffer, au, av);
        previewVertex(b, SPIKE_WHITE, matrix, normal, buffer, bu, bv);
        previewVertex(c, SPIKE_WHITE, matrix, normal, buffer, cu, cv);
    }

    private static void previewVertex(Vec3 position, int color, Matrix4f matrix, Matrix3f normal,
                                      VertexConsumer buffer, float u, float v) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(red, green, blue, 1.0F)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
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

    private static TorsoTransform torsoTransform(LivingEntity entity, float partialTick) {
        TorsoTransform geoTransform = GEO_TORSO_TRANSFORMS.get(entity.getId());
        if (geoTransform != null) {
            return geoTransform;
        }
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.58D;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 left = forward.cross(up).normalize();
        return new TorsoTransform(new Vec3(x, y, z).subtract(forward.scale(0.24D)),
            forward.scale(-1.0D), up, left);
    }

    private static Vec3 armSource(TorsoTransform torso, int arm) {
        Vec3 localOffset = new Vec3(BiokinesesTentacleConfig.startLeft(arm),
            BiokinesesTentacleConfig.startUp(arm), BiokinesesTentacleConfig.startForward(arm));
        return torso.origin().add(torsoVector(torso, localOffset));
    }

    private static Vec3 abilityHoverTarget(TorsoTransform torso, int arm) {
        boolean leftArm = isLeftArm(arm);
        double side = leftArm ? 1.0D : -1.0D;
        double height = arm == BiokinesesArmController.ARM_LEFT_UPPER
            || arm == BiokinesesArmController.ARM_RIGHT_UPPER ? 0.42D
            : arm == BiokinesesArmController.ARM_LEFT_LOWER
                || arm == BiokinesesArmController.ARM_RIGHT_LOWER ? -0.42D : 0.0D;
        double radius = isCombatArm(arm) ? 1.15D : 1.35D;
        return torso.origin().add(torso.left().scale(side * radius)).add(torso.up().scale(height))
            .add(torso.backward().scale(0.2D));
    }

    private static Vec3 anchorTerminalDirection(LivingEntity entity, IBiokinesesArmData data,
                                                 int arm, Vec3 anchor, TorsoTransform torso) {
        if (data.getMode(arm) != IBiokinesesArmData.MODE_ANCHOR || !data.hasAnchor(arm)) {
            return configuredTargetDirection(torso, arm);
        }
        Direction face = Direction.from3DDataValue(data.getAnchorFace(arm));
        Vec3 intoFace = new Vec3(-face.getStepX(), -face.getStepY(), -face.getStepZ()).normalize();
        Vec3 towardUser = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)
            .subtract(anchor);
        if (towardUser.lengthSqr() < 1.0E-6D) {
            return intoFace;
        }
        Vec3 configured = intoFace.scale(BiokinesesTentacleConfig.faceInwardWeight)
            .add(towardUser.normalize().scale(BiokinesesTentacleConfig.faceTowardUserWeight));
        return configured.lengthSqr() < 1.0E-6D ? intoFace : configured.normalize();
    }

    private static Vec3 configuredTargetDirection(TorsoTransform torso, int arm) {
        if (Math.abs(BiokinesesTentacleConfig.targetPitch(arm)) < 1.0E-6D
            && Math.abs(BiokinesesTentacleConfig.targetYaw(arm)) < 1.0E-6D
            && Math.abs(BiokinesesTentacleConfig.targetRoll(arm)) < 1.0E-6D) {
            return null;
        }
        return torso == null ? null : torsoVector(torso, rotateLocal(new Vec3(0.0D, 0.0D, 1.0D),
            BiokinesesTentacleConfig.targetPitch(arm), BiokinesesTentacleConfig.targetYaw(arm),
            BiokinesesTentacleConfig.targetRoll(arm))).normalize();
    }

    /** Called by the model layers after they resolve the animated torso. */
    public static void captureGeoTorsoTransform(LivingEntity entity, PoseStack poseStack, Vec3 cameraPosition,
                                                float partialTick) {
        if (entity == null || poseStack == null || cameraPosition == null) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Vector3f origin = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f backward = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f left = new Vector3f(-1.0F, 0.0F, 0.0F);
        pose.transformPosition(origin);
        pose.transformPosition(backward);
        pose.transformPosition(up);
        pose.transformPosition(left);
        Matrix4f worldFromView = hasViewTransform ? new Matrix4f(WORLD_FROM_VIEW) : new Matrix4f();
        Vector3f worldOriginVector = new Vector3f(origin);
        worldFromView.transformPosition(worldOriginVector);
        Vec3 worldOrigin = cameraPosition.add(worldOriginVector.x, worldOriginVector.y, worldOriginVector.z);
        GEO_TORSO_TRANSFORMS.put(entity.getId(), new TorsoTransform(worldOrigin,
            transformDirection(origin, backward, worldFromView),
            transformDirection(origin, up, worldFromView),
            transformDirection(origin, left, worldFromView)));
    }

    private static Vec3 transformDirection(Vector3f origin, Vector3f point, Matrix4f worldFromView) {
        Vector3f direction = new Vector3f(point).sub(origin);
        if (direction.lengthSquared() < 1.0E-6F) {
            return Vec3.ZERO;
        }
        worldFromView.transformDirection(direction).normalize();
        return new Vec3(direction.x, direction.y, direction.z);
    }

    private static List<Vec3> createIkPath(Vec3 start, Vec3 target, TorsoTransform torso, int arm,
                                           Vec3 terminalDirection, double maxReach) {
        Vec3 difference = target.subtract(start);
        double distance = difference.length();
        if (distance < 1.0E-5D) {
            return List.of(start, target);
        }

        Vec3 direction = difference.scale(1.0D / distance);
        double totalLength = Mth.clamp(distance + 0.8D, 2.4D, maxReach);
        double upperLength = totalLength * 0.53D;
        double lowerLength = totalLength - upperLength;
        Vec3 reachableTarget = distance > totalLength
            ? start.add(direction.scale(totalLength - 0.04D)) : target;

        Vec3 startDirection = startDirection(torso, arm);
        Vec3 pole = startDirection.scale(0.85D);
        Vec3 bendDirection = pole.subtract(direction.scale(pole.dot(direction)));
        if (bendDirection.lengthSqr() < 1.0E-6D) {
            bendDirection = torso.up().subtract(direction.scale(torso.up().dot(direction)));
        }
        bendDirection = bendDirection.normalize();

        double jointDistance = reachableTarget.distanceTo(start);
        double cosine = Mth.clamp((upperLength * upperLength + jointDistance * jointDistance
            - lowerLength * lowerLength) / (2.0D * upperLength * Math.max(jointDistance, 1.0E-5D)), -1.0D, 1.0D);
        double jointHeight = Math.sqrt(Math.max(0.0D, upperLength * upperLength
            - (cosine * upperLength) * (cosine * upperLength)));
        Vec3 elbow = start.add(direction.scale(cosine * upperLength)).add(bendDirection.scale(jointHeight));

        Vec3 firstControl = start.add(startDirection.scale(Math.min(0.75D, upperLength * 0.42D)));
        double terminalControlDistance = Math.min(1.1D, Math.max(0.65D, totalLength * 0.22D));
        Vec3 secondControl = terminalDirection == null
            ? reachableTarget.lerp(elbow, 0.78D)
            : reachableTarget.subtract(terminalDirection.scale(terminalControlDistance));
        List<Vec3> path = new ArrayList<>(PATH_SEGMENTS + 1);
        for (int i = 0; i <= PATH_SEGMENTS; i++) {
            double progress = i / (double) PATH_SEGMENTS;
            double inverse = 1.0D - progress;
            Vec3 point = start.scale(inverse * inverse * inverse)
                .add(firstControl.scale(3.0D * inverse * inverse * progress))
                .add(secondControl.scale(3.0D * inverse * progress * progress))
                .add(reachableTarget.scale(progress * progress * progress));
            Vec3 arc = torsoVector(torso, new Vec3(BiokinesesTentacleConfig.arcLeft(arm),
                BiokinesesTentacleConfig.arcUp(arm), BiokinesesTentacleConfig.arcForward(arm)));
            point = point.add(arc.scale(2.0D * Math.min(progress, 1.0D - progress)));
            double sway = Math.sin(progress * Math.PI * 2.0D + arm * 0.85D) * 0.065D
                * Math.sin(progress * Math.PI);
            path.add(point.add(bendDirection.scale(sway)));
        }
        if (terminalDirection != null && path.size() > 1) {
            Vec3 tip = path.get(path.size() - 1);
            double terminalLength = Math.min(BiokinesesTentacleConfig.spikeLength,
                Math.max(0.12D, tip.distanceTo(start) * 0.25D));
            path.set(path.size() - 2, tip.subtract(terminalDirection.scale(terminalLength)));
        }
        return path;
    }

    private static Vec3 startDirection(TorsoTransform torso, int arm) {
        double vertical = isTopArm(arm) ? 0.35D : isBottomArm(arm) ? -0.35D : 0.0D;
        Vec3 localDirection = rotateLocal(new Vec3(0.0D, vertical, -1.0D),
            BiokinesesTentacleConfig.startPitch(arm), BiokinesesTentacleConfig.startYaw(arm),
            BiokinesesTentacleConfig.startRoll(arm));
        return torsoVector(torso, localDirection).normalize();
    }

    private static Vec3 torsoVector(TorsoTransform torso, Vec3 local) {
        return torso.left().scale(local.x).add(torso.up().scale(local.y))
            .subtract(torso.backward().scale(local.z));
    }

    private static Vec3 rotateLocal(Vec3 vector, double pitch, double yaw, double roll) {
        Vec3 result = rotateAround(vector, new Vec3(1.0D, 0.0D, 0.0D), pitch);
        result = rotateAround(result, new Vec3(0.0D, 1.0D, 0.0D), yaw);
        return rotateAround(result, new Vec3(0.0D, 0.0D, 1.0D), roll);
    }

    private static Vec3 rotateAround(Vec3 vector, Vec3 axis, double angle) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return vector.scale(cosine).add(axis.cross(vector).scale(sine))
            .add(axis.scale(axis.dot(vector) * (1.0D - cosine)));
    }

    private static boolean isLeftArm(int arm) {
        return arm == BiokinesesArmController.ARM_LEFT_UPPER
            || arm == BiokinesesArmController.ARM_LEFT_LOWER
            || arm == BiokinesesArmController.ARM_LEFT_COMBAT;
    }

    private static boolean isCombatArm(int arm) {
        return arm == BiokinesesArmController.ARM_LEFT_COMBAT
            || arm == BiokinesesArmController.ARM_RIGHT_COMBAT;
    }

    private static boolean isTopArm(int arm) {
        return arm == BiokinesesArmController.ARM_LEFT_UPPER
            || arm == BiokinesesArmController.ARM_RIGHT_UPPER;
    }

    private static boolean isBottomArm(int arm) {
        return arm == BiokinesesArmController.ARM_LEFT_LOWER
            || arm == BiokinesesArmController.ARM_RIGHT_LOWER;
    }

    private record TorsoTransform(Vec3 origin, Vec3 backward, Vec3 up, Vec3 left) {
    }

    private static void renderArmPath(List<Vec3> path, double tailDistance, LivingEntity entity,
                                      Vec3 cameraPosition, Matrix4f matrix, Matrix3f normal,
                                      VertexConsumer armBuffer,
                                      VertexConsumer boneBuffer, int arm) {
        List<Vec3> fleshPath = trimTail(path, tailDistance);
        double distanceFromStart = 0.0D;
        for (int i = 0; i < fleshPath.size() - 1; i++) {
            Vec3 start = fleshPath.get(i);
            Vec3 end = fleshPath.get(i + 1);
            boolean bone = distanceFromStart < BiokinesesTentacleConfig.boneLength;
            float width = (float) ((bone ? BiokinesesTentacleConfig.boneWidth
                : BiokinesesTentacleConfig.fleshWidth) * 0.5D);
            int color = bone ? BONE_TINT
                : ((i + arm) % 4 == 0 ? ARM_FLESH_HIGHLIGHT : ARM_FLESH);
            drawBeam(start, end, width, color, entity.level(), cameraPosition, matrix, normal,
                bone ? boneBuffer : armBuffer);
            distanceFromStart += start.distanceTo(end);
        }
    }

    private static List<Vec3> trimTail(List<Vec3> path, double distance) {
        List<Vec3> trimmed = new ArrayList<>(path);
        double remaining = distance;
        while (trimmed.size() > 1 && remaining > 0.0D) {
            int last = trimmed.size() - 1;
            Vec3 start = trimmed.get(last - 1);
            Vec3 end = trimmed.get(last);
            double segmentLength = start.distanceTo(end);
            if (segmentLength <= remaining) {
                trimmed.remove(last);
                remaining -= segmentLength;
                continue;
            }
            trimmed.set(last, end.lerp(start, remaining / segmentLength));
            break;
        }
        return trimmed;
    }

    private static void renderSpike(Vec3 previous, Vec3 tip, LivingEntity entity, Vec3 cameraPosition,
                                    Matrix4f matrix, Matrix3f normal, VertexConsumer buffer,
                                    double spikeLength, double spikeWidth, double spikeDepth) {
        Vec3 direction = tip.subtract(previous);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize().scale(spikeWidth * 0.5D);
        Vec3 otherSide = direction.cross(side).normalize().scale(spikeDepth * 0.5D);
        Vec3 base = tip.subtract(direction.scale(spikeLength));
        Vec3 pointA = base.add(side).add(otherSide);
        Vec3 pointB = base.subtract(side).add(otherSide);
        Vec3 pointC = base.subtract(side).subtract(otherSide);
        Vec3 pointD = base.add(side).subtract(otherSide);
        drawTriangle(tip, pointA, pointB, SPIKE_WHITE, entity.level(), cameraPosition, matrix, normal, buffer,
            0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        drawTriangle(tip, pointB, pointC, SPIKE_WHITE, entity.level(), cameraPosition, matrix, normal, buffer,
            0.5F, 0.0F, 1.0F, 1.0F, 0.5F, 1.0F);
        drawTriangle(tip, pointC, pointD, SPIKE_WHITE, entity.level(), cameraPosition, matrix, normal, buffer,
            0.5F, 0.0F, 0.5F, 1.0F, 0.0F, 1.0F);
        drawTriangle(tip, pointD, pointA, SPIKE_WHITE, entity.level(), cameraPosition, matrix, normal, buffer,
            0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawBeam(Vec3 start, Vec3 end, float width, int color,
                                 net.minecraft.world.level.Level level, Vec3 cameraPosition,
                                 Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
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
        drawQuad(corners[0], corners[4], corners[5], corners[1], color, level, cameraPosition,
            matrix, normal, buffer);
        drawQuad(corners[1], corners[5], corners[6], corners[2], color, level, cameraPosition,
            matrix, normal, buffer);
        drawQuad(corners[2], corners[6], corners[7], corners[3], color, level, cameraPosition,
            matrix, normal, buffer);
        drawQuad(corners[3], corners[7], corners[4], corners[0], color, level, cameraPosition,
            matrix, normal, buffer);
        drawQuad(corners[0], corners[4], corners[7], corners[3], color, level, cameraPosition,
            matrix, normal, buffer);
        drawQuad(corners[1], corners[2], corners[6], corners[5], color, level, cameraPosition,
            matrix, normal, buffer);
    }

    private static void drawQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
                                 net.minecraft.world.level.Level level, Vec3 cameraPosition,
                                 Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
        vertex(a, color, level, cameraPosition, matrix, normal, buffer, 0.0F, 0.0F);
        vertex(b, color, level, cameraPosition, matrix, normal, buffer, 1.0F, 0.0F);
        vertex(c, color, level, cameraPosition, matrix, normal, buffer, 1.0F, 1.0F);
        vertex(d, color, level, cameraPosition, matrix, normal, buffer, 0.0F, 1.0F);
    }

    private static void drawTriangle(Vec3 a, Vec3 b, Vec3 c, int color,
                                     net.minecraft.world.level.Level level, Vec3 cameraPosition,
                                     Matrix4f matrix, Matrix3f normal, VertexConsumer buffer,
                                     float au, float av, float bu, float bv, float cu, float cv) {
        vertex(a, color, level, cameraPosition, matrix, normal, buffer, au, av);
        vertex(b, color, level, cameraPosition, matrix, normal, buffer, bu, bv);
        vertex(c, color, level, cameraPosition, matrix, normal, buffer, cu, cv);
    }

    private static void vertex(Vec3 position, int color, net.minecraft.world.level.Level level,
                               Vec3 cameraPosition, Matrix4f matrix, Matrix3f normal,
                               VertexConsumer buffer,
                               float u, float v) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        Vec3 cameraRelative = position.subtract(cameraPosition);
        buffer.vertex(matrix, (float) cameraRelative.x, (float) cameraRelative.y, (float) cameraRelative.z)
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
