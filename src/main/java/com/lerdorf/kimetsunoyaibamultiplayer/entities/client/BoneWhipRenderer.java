package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BoneWhipEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FleshVineEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/** Renders Bone Whips with square tube geometry and bone-specific assets. */
public final class BoneWhipRenderer extends FleshVineRenderer<BoneWhipEntity> {
    private static final int WALLS = 4;
    private static final double TEXTURE_REPEAT_LENGTH = 0.85D;
    private static final ResourceLocation BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/item/bone_whip_flesh.png");

    public BoneWhipRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ResourceLocation getBodyTexture() {
        return BODY_TEXTURE;
    }

    @Override
    protected net.minecraft.world.item.Item getTipItem() {
        return ModItems.BONE_WHIP_TIP.get();
    }

    @Override
    protected void renderTube(List<Vec3> path, FleshVineEntity entity,
                              Matrix4f matrix, Matrix3f normal, VertexConsumer buffer) {
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
            float lump = 0.84F + 0.16F * net.minecraft.util.Mth.sin(seed * 0.13F + i * 1.71F);
            float taper = 1.0F - Math.max(0.0F, progress - 0.82F) * 0.9F;
            float radius = diameter * 0.5F * lump * taper;
            float nextRadius = diameter * 0.5F
                * (0.84F + 0.16F * net.minecraft.util.Mth.sin(seed * 0.13F + (i + 1) * 1.71F))
                * (1.0F - Math.max(0.0F, progress + 0.08F - 0.82F) * 0.9F);

            for (int sideIndex = 0; sideIndex < WALLS; sideIndex++) {
                int nextSide = (sideIndex + 1) % WALLS;
                Vec3 startA = squarePoint(start, side, binormal, radius, sideIndex);
                Vec3 startB = squarePoint(start, side, binormal, radius, nextSide);
                Vec3 endA = squarePoint(end, side, binormal, nextRadius, sideIndex);
                Vec3 endB = squarePoint(end, side, binormal, nextRadius, nextSide);
                float u0 = (float) (distanceAlongPath / TEXTURE_REPEAT_LENGTH);
                float u1 = (float) ((distanceAlongPath + start.distanceTo(end)) / TEXTURE_REPEAT_LENGTH);
                float v0 = sideIndex / (float) WALLS;
                float v1 = nextSide / (float) WALLS;
                Vec3 faceNormal = endA.subtract(startA).cross(startB.subtract(startA)).normalize();
                quad(startA, endA, endB, startB, u0, u1, v0, v1, faceNormal,
                    entity.level(), matrix, normal, buffer, entity.getFadeAlpha());
            }
            distanceAlongPath += start.distanceTo(end);
        }
    }

    private static Vec3 squarePoint(Vec3 center, Vec3 side, Vec3 binormal,
                                    float radius, int corner) {
        double sideSign = (corner == 0 || corner == 3) ? 1.0D : -1.0D;
        double binormalSign = (corner == 0 || corner == 1) ? 1.0D : -1.0D;
        return center.add(side.scale(sideSign * radius)).add(binormal.scale(binormalSign * radius));
    }
}
