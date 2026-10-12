package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BeastBezierCurveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders a fading cyan 270-degree quadratic-Bezier arc as a thin ribbon. */
public class BeastBezierCurveRenderer extends EntityRenderer<BeastBezierCurveEntity> {
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/misc/blank.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(BLANK_TEXTURE);
    private static final float HALF_WIDTH = 0.035F;
    private static final int ARC_SEGMENTS = 4;
    private static final int BEZIER_SAMPLES = 4;
    private static final double ARC_RADIANS = Math.toRadians(270.0D);

    public BeastBezierCurveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BeastBezierCurveEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 axis = entity.getAxis();
        Vec3 basis = axis.cross(new Vec3(0, 1, 0));
        if (basis.lengthSqr() < 1.0E-6D) {
            basis = axis.cross(new Vec3(1, 0, 0));
        }
        basis = basis.normalize();
        Vec3 perpendicular = axis.cross(basis).normalize();
        Vec3 cameraPosition = getCameraPosition();
        Vec3 center = entity.getPosition(partialTicks);
        Vec3 cameraLocal = cameraPosition.subtract(center);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
        Vec3 previous = null;
        int totalSamples = ARC_SEGMENTS * BEZIER_SAMPLES;

        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            double start = entity.getPhase() + ARC_RADIANS * segment / ARC_SEGMENTS;
            double end = entity.getPhase() + ARC_RADIANS * (segment + 1) / ARC_SEGMENTS;
            double midpoint = (start + end) * 0.5D;
            double controlRadius = entity.getRadius() / Math.cos((end - start) * 0.5D);
            Vec3 p0 = arcPoint(basis, perpendicular, start, entity.getRadius());
            Vec3 p2 = arcPoint(basis, perpendicular, end, entity.getRadius());
            Vec3 control = arcPoint(basis, perpendicular, midpoint, controlRadius);

            for (int sample = 0; sample < BEZIER_SAMPLES; sample++) {
                double t = sample / (double) BEZIER_SAMPLES;
                Vec3 current = quadraticBezier(p0, control, p2, t);
                if (previous != null) {
                    double endProgress = (segment * BEZIER_SAMPLES + sample) / (double) totalSamples;
                    double startProgress = Math.max(0.0D, endProgress - 1.0D / totalSamples);
                    drawSegment(buffer, matrix, normal, previous, current, cameraLocal,
                        1.0D - startProgress, 1.0D - endProgress, entity.usesWhiteColor());
                }
                previous = current;
            }
        }

        Vec3 endPoint = arcPoint(basis, perpendicular, entity.getPhase() + ARC_RADIANS, entity.getRadius());
        if (previous != null) {
            drawSegment(buffer, matrix, normal, previous, endPoint, cameraLocal,
                1.0D / totalSamples, 0.0D, entity.usesWhiteColor());
        }
    }

    private static Vec3 arcPoint(Vec3 basis, Vec3 perpendicular, double angle, double radius) {
        return basis.scale(Math.cos(angle) * radius)
            .add(perpendicular.scale(Math.sin(angle) * radius));
    }

    private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double t) {
        double inverse = 1.0D - t;
        return start.scale(inverse * inverse)
            .add(control.scale(2.0D * inverse * t))
            .add(end.scale(t * t));
    }

    private static void drawSegment(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                    Vec3 start, Vec3 end, Vec3 cameraPosition,
                                    double startOpacity, double endOpacity, boolean white) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 toCamera = cameraPosition.subtract(start);
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(0, 1, 0));
        }
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1, 0, 0));
        }
        side = side.normalize();
        Vec3 startSide = side.scale(HALF_WIDTH * clampOpacity(startOpacity));
        Vec3 endSide = side.scale(HALF_WIDTH * clampOpacity(endOpacity));

        vertex(buffer, matrix, normal, start.add(startSide), startOpacity, white);
        vertex(buffer, matrix, normal, end.add(endSide), endOpacity, white);
        vertex(buffer, matrix, normal, end.subtract(endSide), endOpacity, white);
        vertex(buffer, matrix, normal, start.subtract(startSide), startOpacity, white);
    }

    private static float clampOpacity(double opacity) {
        return (float) Math.max(0.0D, Math.min(1.0D, opacity));
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 point, double opacity, boolean white) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .color(white ? 1.0F : 0.5F, 1.0F, 1.0F,
                (float) Math.max(0.0D, Math.min(1.0D, opacity)))
            .uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private Vec3 getCameraPosition() {
        Camera camera = this.entityRenderDispatcher.camera;
        return camera == null ? Vec3.ZERO : camera.getPosition();
    }

    @Override
    public ResourceLocation getTextureLocation(BeastBezierCurveEntity entity) {
        return BLANK_TEXTURE;
    }
}
