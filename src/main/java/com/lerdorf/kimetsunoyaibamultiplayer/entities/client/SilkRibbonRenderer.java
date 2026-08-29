package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SilkRibbonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Vector3f;

/**
 * Renders the silk ribbon as a smooth camera-facing ribbon mesh through the
 * entity's synced trail points. Pure white for SILK; dark green interleaved
 * segments for the ACID variant.
 *
 * The path is smoothed with a Catmull-Rom spline so the recorded points read
 * as a continuous silky curve rather than a polyline.
 */
public class SilkRibbonRenderer extends EntityRenderer<SilkRibbonEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/silk_ribbon.png");

    private static final float RIBBON_WIDTH = 0.09F;
    private static final float ACID_RIBBON_WIDTH = 0.075F;
    private static final int SPLINE_SUBDIVISIONS = 6;

    private static final float WHITE_R = 1.00F, WHITE_G = 1.00F, WHITE_B = 1.00F;
    private static final float ACID_R = 0.18F, ACID_G = 0.42F, ACID_B = 0.12F;

    public SilkRibbonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SilkRibbonEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float[] trail = entity.getTrail();
        if (trail.length < 6) {
            return;
        }

        // The trail points are stored in absolute WORLD coordinates. The
        // entity renderer's PoseStack is translated to the entity's
        // interpolated position, so translate by the negated interpolated
        // position to draw in true world space. Using the tick-aligned
        // interpolated position (not the raw trail) keeps the whole ribbon
        // perfectly stable between ticks - no jitter.
        Vec3 entityPos = entity.getPosition(partialTicks);
        poseStack.pushPose();
        poseStack.translate(-entityPos.x, -entityPos.y, -entityPos.z);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Build spline control points (head first).
        int pointCount = trail.length / 3;
        Vector3f[] points = new Vector3f[pointCount + 1];
        // Head: current interpolated position.
        points[0] = new Vector3f((float) entityPos.x, (float) entityPos.y, (float) entityPos.z);
        // Trail is stored oldest-first; reverse so index order runs head -> tail.
        for (int i = 0; i < pointCount; i++) {
            int src = (pointCount - 1 - i) * 3;
            points[i + 1] = new Vector3f(trail[src], trail[src + 1], trail[src + 2]);
        }

        RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        boolean acid = entity.getKind() == SilkRibbonEntity.RibbonKind.ACID;
        float width = acid ? ACID_RIBBON_WIDTH : RIBBON_WIDTH;

        float alpha = 1.0F;

        Vec3 camPos = this.entityRenderDispatcher.camera != null
            ? this.entityRenderDispatcher.camera.getPosition() : Vec3.ZERO;

        int segments = points.length - 1;
        Vector3f prevLeft = null;
        Vector3f prevRight = null;

        for (int seg = 0; seg < segments; seg++) {
            Vector3f p0 = points[Math.max(0, seg - 1)];
            Vector3f p1 = points[seg];
            Vector3f p2 = points[seg + 1];
            Vector3f p3 = points[Math.min(points.length - 1, seg + 2)];

            for (int s = 0; s < SPLINE_SUBDIVISIONS; s++) {
                float t0 = (float) s / SPLINE_SUBDIVISIONS;
                float t1 = (float) (s + 1) / SPLINE_SUBDIVISIONS;

                Vector3f a = catmullRom(p0, p1, p2, p3, t0);
                Vector3f b = catmullRom(p0, p1, p2, p3, t1);

                // Camera-facing ribbon orientation per endpoint.
                float taperHead = seg == 0 && s == 0 ? 0.35F : 1.0F;
                boolean tailTapering = seg == segments - 1;
                float taperTail = tailTapering ? Math.max(0.15F, 1.0F - t1) : 1.0F;

                Vector3f leftA = billboardOffset(a, b, camPos, width * taperHead * fade(seg, s, segments));
                Vector3f rightA = new Vector3f(a).mul(2.0F).sub(leftA);

                Vector3f leftB = billboardOffset(b, b, camPos, width * taperTail);
                Vector3f rightB = new Vector3f(b).mul(2.0F).sub(leftB);

                // Color: white for silk; alternating white/dark-green bands for acid.
                float r, g, bl;
                if (acid && (s / 2) % 2 == 1) {
                    r = ACID_R; g = ACID_G; bl = ACID_B;
                } else {
                    r = WHITE_R; g = WHITE_G; bl = WHITE_B;
                }

                float u0 = t0 * 0.5F;
                float u1 = t1 * 0.5F;
                float v0 = ((seg * SPLINE_SUBDIVISIONS + s) % 8) / 8.0F;
                float v1 = v0 + 1.0F / 8.0F;

                if (prevLeft != null && prevRight != null) {
                    quad(buffer, matrix, normal, prevLeft, leftA, rightA, prevRight, u0, u1, v0, v1, r, g, bl, alpha);
                    quad(buffer, matrix, normal, leftA, leftB, rightB, rightA, u0, u1, v0, v1, r, g, bl, alpha);
                }

                prevLeft = leftB;
                prevRight = rightB;
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static float fade(int seg, int s, int segments) {
        // Fade out toward the tail.
        float progress = (seg * SPLINE_SUBDIVISIONS + s) / (float) (segments * SPLINE_SUBDIVISIONS);
        return 0.35F + 0.65F * (1.0F - progress);
    }

    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vector3f(
            0.5F * ((2.0F * p1.x()) + (-p0.x() + p2.x()) * t + (2.0F * p0.x() - 5.0F * p1.x() + 4.0F * p2.x() - p3.x()) * t2 + (-p0.x() + 3.0F * p1.x() - 3.0F * p2.x() + p3.x()) * t3),
            0.5F * ((2.0F * p1.y()) + (-p0.y() + p2.y()) * t + (2.0F * p0.y() - 5.0F * p1.y() + 4.0F * p2.y() - p3.y()) * t2 + (-p0.y() + 3.0F * p1.y() - 3.0F * p2.y() + p3.y()) * t3),
            0.5F * ((2.0F * p1.z()) + (-p0.z() + p2.z()) * t + (2.0F * p0.z() - 5.0F * p1.z() + 4.0F * p2.z() - p3.z()) * t2 + (-p0.z() + 3.0F * p1.z() - 3.0F * p2.z() + p3.z()) * t3)
        );
    }

    /** Offset perpendicular to the segment direction and the view vector (billboarded half-width). */
    private static Vector3f billboardOffset(Vector3f point, Vector3f next, Vec3 camera, float halfWidth) {
        Vector3f dir = new Vector3f(next).sub(point);
        if (dir.lengthSquared() < 1.0E-8F) {
            dir.set(0.0F, 0.0F, 1.0F);
        }
        Vector3f toCam = new Vector3f(
            (float) (camera.x - point.x()),
            (float) (camera.y - point.y()),
            (float) (camera.z - point.z()));
        Vector3f side = dir.cross(toCam);
        if (side.lengthSquared() < 1.0E-8F) {
            side.set(0.0F, 1.0F, 0.0F);
        }
        return side.normalize().mul(halfWidth).add(point);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                             Vector3f leftA, Vector3f leftB, Vector3f rightB, Vector3f rightA,
                             float u0, float u1, float v0, float v1,
                             float r, float g, float b, float alpha) {
        buffer.vertex(matrix, leftA.x(), leftA.y(), leftA.z()).color(r, g, b, alpha).uv(u0, v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, leftB.x(), leftB.y(), leftB.z()).color(r, g, b, alpha).uv(u0, v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightB.x(), rightB.y(), rightB.z()).color(r, g, b, alpha).uv(u1, v1)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightA.x(), rightA.y(), rightA.z()).color(r, g, b, alpha).uv(u1, v0)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SilkRibbonEntity entity) {
        return TEXTURE;
    }
}
