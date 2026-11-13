package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Small camera-facing ribbon helpers for bright world-space line effects. */
public final class WorldLineRenderUtil {
    public static final float[] WHITE = {1.0F, 1.0F, 1.0F};
    public static final float[] FLAME = {1.0F, 0.25F, 0.08F};
    public static final float[] GLOW = {1.0F, 0.9F, 0.35F};
    public static final float[] HAPPY = {0.35F, 1.0F, 0.35F};
    public static final float[] GRAVITY_OUTLINE = {0.25F, 0.8F, 1.0F};
    public static final float[] GRAVITY_ARROW = {1.0F, 0.25F, 0.2F};

    private WorldLineRenderUtil() {
    }

    public static RenderType renderType() {
        return CustomRenderTypes.geoEntityTranslucentEmissive(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
                "textures/misc/blank.png"));
    }

    public static void drawSegment(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                   Vec3 camera, Vec3 start, Vec3 end,
                                   float[] color, float startAlpha, float endAlpha,
                                   float startWidth, float endWidth) {
        Vec3 tangent = end.subtract(start);
        if (tangent.lengthSqr() < 1.0E-8D) {
            return;
        }

        tangent = tangent.normalize();
        Vec3 side = tangent.cross(camera.subtract(start));
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();

        Vec3 leftStart = start.add(side.scale(startWidth)).subtract(camera);
        Vec3 rightStart = start.subtract(side.scale(startWidth)).subtract(camera);
        Vec3 leftEnd = end.add(side.scale(endWidth)).subtract(camera);
        Vec3 rightEnd = end.subtract(side.scale(endWidth)).subtract(camera);

        vertex(buffer, matrix, normal, leftStart, color, startAlpha, 0.0F, 0.0F);
        vertex(buffer, matrix, normal, leftEnd, color, endAlpha, 0.0F, 1.0F);
        vertex(buffer, matrix, normal, rightEnd, color, endAlpha, 1.0F, 1.0F);
        vertex(buffer, matrix, normal, rightStart, color, startAlpha, 1.0F, 0.0F);
    }

    public static void drawArrow(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                 Vec3 camera, Vec3 start, Vec3 direction, double length,
                                 float[] color, float alpha, float width,
                                 double headLength, double headWidth) {
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 directionUnit = direction.normalize();
        Vec3 tip = start.add(directionUnit.scale(length));
        drawSegment(buffer, matrix, normal, camera, start, tip, color, alpha, alpha, width, width * 0.9F);

        Vec3 basis = Math.abs(directionUnit.y) > 0.9D
            ? new Vec3(1.0D, 0.0D, 0.0D)
            : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = directionUnit.cross(basis).normalize();
        Vec3 secondSide = directionUnit.cross(side).normalize();
        Vec3 headBase = tip.subtract(directionUnit.scale(headLength));

        drawSegment(buffer, matrix, normal, camera, tip, headBase.add(side.scale(headWidth)),
            color, alpha, alpha * 0.85F, width, width * 0.75F);
        drawSegment(buffer, matrix, normal, camera, tip, headBase.subtract(side.scale(headWidth)),
            color, alpha, alpha * 0.85F, width, width * 0.75F);
        drawSegment(buffer, matrix, normal, camera, tip, headBase.add(secondSide.scale(headWidth)),
            color, alpha, alpha * 0.85F, width, width * 0.75F);
        drawSegment(buffer, matrix, normal, camera, tip, headBase.subtract(secondSide.scale(headWidth)),
            color, alpha, alpha * 0.85F, width, width * 0.75F);
    }

    public static void drawBox(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 camera, AABB box, float[] color, float alpha, float width) {
        Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);

        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, min.y, min.z), new Vec3(max.x, min.y, min.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, min.y, max.z), new Vec3(max.x, min.y, max.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, max.y, min.z), new Vec3(max.x, max.y, min.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, max.y, max.z), new Vec3(max.x, max.y, max.z), color, alpha, alpha, width, width);

        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, min.y, min.z), new Vec3(min.x, max.y, min.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(max.x, min.y, min.z), new Vec3(max.x, max.y, min.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, min.y, max.z), new Vec3(min.x, max.y, max.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(max.x, min.y, max.z), new Vec3(max.x, max.y, max.z), color, alpha, alpha, width, width);

        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, min.y, min.z), new Vec3(min.x, min.y, max.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(max.x, min.y, min.z), new Vec3(max.x, min.y, max.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(min.x, max.y, min.z), new Vec3(min.x, max.y, max.z), color, alpha, alpha, width, width);
        drawSegment(buffer, matrix, normal, camera, new Vec3(max.x, max.y, min.z), new Vec3(max.x, max.y, max.z), color, alpha, alpha, width, width);
    }

    public static void drawCircle(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                  Vec3 camera, Vec3 center, double radius, int segments,
                                  float[] color, float alpha, float width) {
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            if (previous != null) {
                drawSegment(buffer, matrix, normal, camera, previous, point, color, alpha, alpha, width, width);
            }
            previous = point;
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               Vec3 position, float[] color, float alpha, float u, float v) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
            .color(color[0], color[1], color[2], Math.max(0.0F, alpha))
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }
}
