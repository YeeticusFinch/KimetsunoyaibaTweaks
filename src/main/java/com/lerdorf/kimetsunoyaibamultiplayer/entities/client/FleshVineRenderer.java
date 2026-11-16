package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FleshVineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Renders a thick, irregular flesh vine and the baked model at its tip. */
public final class FleshVineRenderer extends EntityRenderer<FleshVineEntity> {
    private static final ResourceLocation BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/item/flesh_vine.png");
    private static final RenderType BODY_RENDER_TYPE = RenderType.entityTranslucent(BODY_TEXTURE);
    private static final int RING_SIDES = 8;
    private static final int MAX_POINTS = 96;
    private static final double TEXTURE_REPEAT_LENGTH = 0.85D;
    private static final double TIP_MODEL_DIAMETER = 0.33D;
    public FleshVineRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(FleshVineEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        List<Vec3> path = buildPath(entity, partialTick);
        if (path.size() < 2) {
            return;
        }

        Vec3 entityPosition = entity.getPosition(partialTick);
        poseStack.pushPose();
        poseStack.translate(-entityPosition.x, -entityPosition.y, -entityPosition.z);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        VertexConsumer buffer = bufferSource.getBuffer(BODY_RENDER_TYPE);
        renderTube(path, entity, matrix, normal, buffer);
        renderTip(path, entity, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    private static List<Vec3> buildPath(FleshVineEntity entity, float partialTick) {
        List<Vec3> path = new ArrayList<>();
        int seed = entity.getSeed();
        double time = entity.tickCount + partialTick;
        if (entity.getMode() == FleshVineEntity.MODE_TRANSITION) {
            List<Vec3> transitionPath = entity.getTransitionPath();
            return transitionPath.size() >= 2
                ? transitionPath : List.of(entity.getStart(), entity.getEnd());
        }
        if (entity.getMode() == FleshVineEntity.MODE_CHARGE) {
            Vec3 start = entity.getStart();
            Vec3 end = entity.getEnd();
            double distance = start.distanceTo(end);
            int count = Mth.clamp((int) Math.ceil(distance * 2.5D), 4, 24);
            Vec3 axis = entity.getUp();
            Vec3 side = perpendicular(axis, new Vec3(1.0D, 0.0D, 0.0D));
            Vec3 otherSide = axis.cross(side).normalize();
            for (int i = 0; i <= count; i++) {
                double progress = i / (double) count;
                Vec3 point = start.lerp(end, progress);
                double envelope = Math.sin(progress * Math.PI);
                double angle = seed * 0.071D + time * 0.18D + progress * Math.PI * 2.5D;
                double radius = 0.22D * envelope;
                path.add(point.add(side.scale(Math.cos(angle) * radius))
                    .add(otherSide.scale(Math.sin(angle) * radius)));
            }
            return path;
        }

        double distance = Math.min(FleshVineEntity.SURGE_MAX_DISTANCE,
            Math.max(0.0D, entity.getProgress() + partialTick * FleshVineEntity.SURGE_SPEED));
        int count = Mth.clamp((int) Math.ceil(Math.max(1.0D, distance) * 2.0D), 4, MAX_POINTS);
        for (int i = 0; i <= count; i++) {
            double pointDistance = distance * i / count;
            Vec3 point = entity.getSurgePoint(pointDistance);
            Vec3 tangent = entity.getSurgePoint(Math.min(distance, pointDistance + 0.1D)).subtract(point);
            if (tangent.lengthSqr() < 1.0E-6D) {
                tangent = entity.getVineDirection();
            }
            tangent = tangent.normalize();
            Vec3 side = perpendicular(tangent, entity.getUp());
            Vec3 otherSide = tangent.cross(side).normalize();
            double envelope = Math.sin(Math.PI * i / count);
            double lump = Math.sin(seed * 0.043D + pointDistance * 1.47D + time * 0.13D)
                * 0.12D * envelope;
            double twist = Math.cos(seed * 0.097D + pointDistance * 2.1D - time * 0.09D)
                * 0.08D * envelope;
            path.add(point.add(side.scale(lump)).add(otherSide.scale(twist)));
        }
        // The server entity position is the authoritative endpoint. Always use it for
        // the final point so client interpolation cannot disagree with the sine path.
        path.set(path.size() - 1, entity.getPosition(partialTick));
        return path;
    }

    private static Vec3 perpendicular(Vec3 direction, Vec3 reference) {
        Vec3 side = direction.cross(reference);
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return side.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : side.normalize();
    }

    private static void renderTube(List<Vec3> path, FleshVineEntity entity, Matrix4f matrix,
                                   Matrix3f normal, VertexConsumer buffer) {
        float diameter = entity.getWidth();
        int seed = entity.getSeed();
        double distanceAlongPath = 0.0D;
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 start = path.get(i);
            Vec3 end = path.get(i + 1);
            Vec3 tangent = end.subtract(start).normalize();
            Vec3 reference = Math.abs(tangent.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 side = tangent.cross(reference).normalize();
            Vec3 binormal = tangent.cross(side).normalize();
            float progress = i / (float) Math.max(1, path.size() - 2);
            float lump = 0.84F + 0.16F * Mth.sin(seed * 0.13F + i * 1.71F);
            float taper = 1.0F - Math.max(0.0F, progress - 0.82F) * 0.9F;
            float radius = diameter * 0.5F * lump * taper;
            float nextRadius = diameter * 0.5F
                * (0.84F + 0.16F * Mth.sin(seed * 0.13F + (i + 1) * 1.71F))
                * (1.0F - Math.max(0.0F, progress + 0.08F - 0.82F) * 0.9F);

            for (int sideIndex = 0; sideIndex < RING_SIDES; sideIndex++) {
                int nextSide = (sideIndex + 1) % RING_SIDES;
                double angle0 = sideIndex * Math.PI * 2.0D / RING_SIDES;
                double angle1 = nextSide * Math.PI * 2.0D / RING_SIDES;
                Vec3 startA = ringPoint(start, side, binormal, radius, angle0);
                Vec3 startB = ringPoint(start, side, binormal, radius, angle1);
                Vec3 endA = ringPoint(end, side, binormal, nextRadius, angle0);
                Vec3 endB = ringPoint(end, side, binormal, nextRadius, angle1);
                float u0 = (float) (distanceAlongPath / TEXTURE_REPEAT_LENGTH);
                float u1 = (float) ((distanceAlongPath + start.distanceTo(end)) / TEXTURE_REPEAT_LENGTH);
                float v0 = sideIndex / (float) RING_SIDES;
                float v1 = nextSide / (float) RING_SIDES;
                 Vec3 faceNormal = endA.subtract(startA).cross(startB.subtract(startA)).normalize();
                 quad(startA, endA, endB, startB, u0, u1, v0, v1, faceNormal,
                     entity.level(), matrix, normal, buffer, entity.getFadeAlpha());
            }
            distanceAlongPath += start.distanceTo(end);
        }
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 side, Vec3 binormal, float radius, double angle) {
        return center.add(side.scale(Math.cos(angle) * radius)).add(binormal.scale(Math.sin(angle) * radius));
    }

    private void renderTip(List<Vec3> path, FleshVineEntity entity, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(
            new net.minecraft.world.item.ItemStack(ModItems.FLESH_VINE_TIP.get()),
            entity.level(), null, entity.getId());
        if (model == null || model == minecraft.getModelManager().getMissingModel()) {
            return;
        }
        Vec3 tip = path.get(path.size() - 1);
        Vec3 previous = path.get(path.size() - 2);
        Vec3 direction = tip.subtract(previous);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = entity.getMode() == FleshVineEntity.MODE_SURGE
                ? entity.getVineDirection() : entity.getEnd().subtract(entity.getStart());
        }
        direction = direction.normalize();

        poseStack.pushPose();
        poseStack.translate(tip.x, tip.y, tip.z);
        poseStack.mulPose(rotationBetween(new Vec3(0.0D, 0.0D, 1.0D), direction));
        float scale = entity.getWidth() / (float) TIP_MODEL_DIAMETER;
        poseStack.scale(scale, scale, scale);
        int light = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(tip));
        VertexConsumer buffer = bufferSource.getBuffer(
            RenderType.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        renderBakedTip(model, poseStack, buffer, light, entity.getFadeAlpha(), entity.getSeed());
        poseStack.popPose();
    }

    private static void renderBakedTip(BakedModel model, PoseStack poseStack, VertexConsumer buffer,
                                       int packedLight, float alpha, int seed) {
        RandomSource random = RandomSource.create(seed);
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            renderTipQuads(model.getQuads(null, direction, random), poseStack, buffer,
                packedLight, alpha);
        }
        random.setSeed(42L);
        renderTipQuads(model.getQuads(null, null, random), poseStack, buffer, packedLight, alpha);
    }

    private static void renderTipQuads(List<BakedQuad> quads, PoseStack poseStack,
                                       VertexConsumer buffer, int packedLight, float alpha) {
        for (BakedQuad quad : quads) {
            buffer.putBulkData(poseStack.last(), quad, 1.0F, 1.0F, 1.0F, alpha,
                packedLight, OverlayTexture.NO_OVERLAY, true);
        }
    }

    private static Quaternionf rotationBetween(Vec3 from, Vec3 to) {
        Vector3f source = new Vector3f((float) from.x, (float) from.y, (float) from.z).normalize();
        Vector3f target = new Vector3f((float) to.x, (float) to.y, (float) to.z).normalize();
        float dot = source.dot(target);
        if (dot > 0.9999F) {
            return new Quaternionf();
        }
        if (dot < -0.9999F) {
            return new Quaternionf().rotateY((float) Math.PI);
        }
        Vector3f axis = source.cross(target, new Vector3f()).normalize();
        return new Quaternionf().fromAxisAngleRad(axis, (float) Math.acos(dot));
    }

    private static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1,
                             float v0, float v1, Vec3 faceNormal,
                             net.minecraft.world.level.Level level, Matrix4f matrix,
                             Matrix3f normal, VertexConsumer buffer, float alpha) {
        vertex(a, u0, v0, faceNormal, level, matrix, normal, buffer, alpha);
        vertex(b, u1, v0, faceNormal, level, matrix, normal, buffer, alpha);
        vertex(c, u1, v1, faceNormal, level, matrix, normal, buffer, alpha);
        vertex(d, u0, v1, faceNormal, level, matrix, normal, buffer, alpha);
    }

    private static void vertex(Vec3 position, float u, float v, Vec3 faceNormal,
                               net.minecraft.world.level.Level level, Matrix4f matrix,
                                Matrix3f normal, VertexConsumer buffer, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(1.0F, 1.0F, 1.0F, alpha)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LevelRenderer.getLightColor(level, BlockPos.containing(position)))
            .normal(normal, (float) faceNormal.x, (float) faceNormal.y, (float) faceNormal.z)
            .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FleshVineEntity entity) {
        return BODY_TEXTURE;
    }
}
