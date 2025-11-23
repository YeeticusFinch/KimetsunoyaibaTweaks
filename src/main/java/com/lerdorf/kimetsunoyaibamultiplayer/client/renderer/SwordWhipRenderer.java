package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Dual-layer emissive renderer for Mitsuri's whip sword.
 *
 * Renders two layers:
 * 1. Base layer: Dark grey (back half) to bright pink (front half) - Normal rendering
 * 2. Emissive layer: Same colors but with additive blending - Glows in darkness
 */
public class SwordWhipRenderer {
    private static final ResourceLocation BLANK_TEXTURE =
        new ResourceLocation("kimetsunoyaibamultiplayer", "textures/misc/blank.png");

    // Base layer: Normal translucent
    private static final RenderType BASE_RENDER_TYPE =
        RenderType.entityTranslucentEmissive(BLANK_TEXTURE);

    /**
     * Main render method called from ClientRenderEvents.
     */
    public static void render(PoseStack stack, MultiBufferSource buffer,
                              Player player, Vec3 camPos, float partialTicks) {
        List<Vec3> pts = WhipPhysics.getPoints(player);

        if (pts.size() < 2) {
            return;
        }

        int res = EnhancedBreathingConfig.whipCurveResolution;
        float width = (float) EnhancedBreathingConfig.whipWidth;

        // Compute interpolated curve positions
        List<Vec3> curve = sampleCurve(pts, res);

        stack.pushPose();
        stack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = stack.last().pose();

        // Render GREY ribbon (full width)
        renderSingleColorLayer(mat, buffer.getBuffer(BASE_RENDER_TYPE), curve, width,
            0.24f, 0.24f, 0.24f, 1.0f); // Dark grey

        // Render PINK ribbon (slightly narrower, offset)
        renderSingleColorLayer(mat, buffer.getBuffer(BASE_RENDER_TYPE), curve, width * 0.6f,
            1.0f, 0.41f, 0.71f, 1.0f); // Hot pink

        // Render emissive layers (glowing)
        if (EnhancedBreathingConfig.whipEmissive) {
            RenderType emissiveType = CustomRenderTypes.swordSlashAdditive(BLANK_TEXTURE);
            float brightness = (float) EnhancedBreathingConfig.whipEmissiveBrightness;

            // Emissive grey
            renderSingleColorLayer(mat, buffer.getBuffer(emissiveType), curve, width * 0.8f,
                0.24f * brightness, 0.24f * brightness, 0.24f * brightness, 0.6f);

            // Emissive pink
            renderSingleColorLayer(mat, buffer.getBuffer(emissiveType), curve, width * 0.5f,
                1.0f * brightness, 0.41f * brightness, 0.71f * brightness, 0.8f);
        }

        stack.popPose();
    }

    /**
     * Render a single-color ribbon layer.
     *
     * @param mat Transform matrix
     * @param vb Vertex consumer
     * @param curve Interpolated curve points
     * @param width Ribbon width
     * @param r Red color component (0-1)
     * @param g Green color component (0-1)
     * @param b Blue color component (0-1)
     * @param alpha Alpha transparency (0-1)
     */
    private static void renderSingleColorLayer(Matrix4f mat, VertexConsumer vb, List<Vec3> curve,
                                                float width, float r, float g, float b, float alpha) {
        int totalSegments = curve.size() - 1;

        for (int i = 0; i < totalSegments; i++) {
            Vec3 a = curve.get(i);
            Vec3 b_pt = curve.get(i + 1);

            // Calculate perpendicular vector for ribbon width
            Vec3 forward = b_pt.subtract(a).normalize();
            Vec3 side = forward.cross(new Vec3(0, 1, 0));

            // Handle vertical segments (when cross product is near zero)
            if (side.lengthSqr() < 0.001) {
                side = forward.cross(new Vec3(1, 0, 0));
            }
            side = side.normalize().scale(width);

            // Create quad (ribbon segment)
            // Top-right vertex
            vb.vertex(mat, (float)(b_pt.x + side.x), (float)(b_pt.y + side.y), (float)(b_pt.z + side.z))
              .color(r, g, b, alpha)
              .uv(1, 0)
              .overlayCoords(0, 10)
              .uv2(240, 240) // Full bright
              .normal(0, 1, 0)
              .endVertex();

            // Top-left vertex
            vb.vertex(mat, (float)(a.x + side.x), (float)(a.y + side.y), (float)(a.z + side.z))
              .color(r, g, b, alpha)
              .uv(0, 0)
              .overlayCoords(0, 10)
              .uv2(240, 240)
              .normal(0, 1, 0)
              .endVertex();

            // Bottom-left vertex
            vb.vertex(mat, (float)(a.x - side.x), (float)(a.y - side.y), (float)(a.z - side.z))
              .color(r, g, b, alpha)
              .uv(0, 1)
              .overlayCoords(0, 10)
              .uv2(240, 240)
              .normal(0, 1, 0)
              .endVertex();

            // Bottom-right vertex
            vb.vertex(mat, (float)(b_pt.x - side.x), (float)(b_pt.y - side.y), (float)(b_pt.z - side.z))
              .color(r, g, b, alpha)
              .uv(1, 1)
              .overlayCoords(0, 10)
              .uv2(240, 240)
              .normal(0, 1, 0)
              .endVertex();
        }
    }

    /**
     * Sample curve using Catmull-Rom interpolation for smooth rendering.
     */
    private static List<Vec3> sampleCurve(List<Vec3> pts, int res) {
        List<Vec3> result = new ArrayList<>();

        if (pts.size() < 2) {
            return pts;
        }

        for (int i = 0; i < res; i++) {
            float t = i / (float)(res - 1);
            int seg = (int)(t * (pts.size() - 1));

            // Clamp indices
            seg = Math.min(seg, pts.size() - 2);

            int i0 = Math.max(0, seg - 1);
            int i1 = seg;
            int i2 = Math.min(pts.size() - 1, seg + 1);
            int i3 = Math.min(pts.size() - 1, seg + 2);

            float lt = (t * (pts.size() - 1)) - seg;
            result.add(catmullRom(pts.get(i0), pts.get(i1), pts.get(i2), pts.get(i3), lt));
        }

        return result;
    }

    /**
     * Catmull-Rom spline interpolation for smooth curves.
     */
    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p1.scale(2)
            .add(p2.subtract(p0).scale(t))
            .add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(t2))
            .add(p3.subtract(p0).add(p1.scale(3)).subtract(p2.scale(3)).scale(t3))
            .scale(0.5);
    }
}
