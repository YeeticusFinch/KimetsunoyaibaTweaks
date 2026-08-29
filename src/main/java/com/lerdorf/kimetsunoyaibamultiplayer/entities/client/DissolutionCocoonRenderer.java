package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DissolutionCocoonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the Dissolution Cocoon.
 */
public class DissolutionCocoonRenderer extends GeoEntityRenderer<DissolutionCocoonEntity> {
    private static final ResourceLocation TETHER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/silk_ribbon.png");
    private static final float TETHER_WIDTH = 0.045F;

    private static final class CocoonModel extends GeoModel<DissolutionCocoonEntity> {
        private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "geo/cocoon.geo.json");
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/entity/cocoon.png");
        private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "animations/cocoon.animation.json");

        @Override
        public ResourceLocation getModelResource(DissolutionCocoonEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(DissolutionCocoonEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(DissolutionCocoonEntity animatable) {
            return ANIMATION;
        }
    }

    public DissolutionCocoonRenderer(EntityRendererProvider.Context context) {
        super(context, new CocoonModel());
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(DissolutionCocoonEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (entity.hasTether()) {
            renderTether(entity, partialTick, poseStack, bufferSource);
        }
    }

    @Override
    public RenderType getRenderType(DissolutionCocoonEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    private void renderTether(DissolutionCocoonEntity entity, float partialTick,
                              PoseStack poseStack, MultiBufferSource bufferSource) {
        Vec3 entityPos = entity.getPosition(partialTick);
        Vector3f start = new Vector3f(0.0F, entity.getBbHeight(), 0.0F);
        Vector3f end = new Vector3f(
            (float) (entity.getTetherX() - entityPos.x),
            (float) (entity.getTetherY() - entityPos.y),
            (float) (entity.getTetherZ() - entityPos.z)
        );
        if (end.y() <= start.y()) {
            return;
        }

        Vec3 camera = this.entityRenderDispatcher.camera != null
            ? this.entityRenderDispatcher.camera.getPosition() : Vec3.ZERO;

        Vector3f leftStart = billboardOffset(start, end, entityPos, camera, TETHER_WIDTH);
        Vector3f rightStart = new Vector3f(start).mul(2.0F).sub(leftStart);
        Vector3f leftEnd = billboardOffset(end, start, entityPos, camera, TETHER_WIDTH * 0.7F);
        Vector3f rightEnd = new Vector3f(end).mul(2.0F).sub(leftEnd);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE));
        quad(buffer, matrix, normal, leftStart, leftEnd, rightEnd, rightStart);
    }

    private static Vector3f billboardOffset(Vector3f point, Vector3f next, Vec3 entityPos, Vec3 camera, float halfWidth) {
        Vector3f dir = new Vector3f(next).sub(point);
        if (dir.lengthSquared() < 1.0E-8F) {
            dir.set(0.0F, 1.0F, 0.0F);
        }
        Vector3f worldPoint = new Vector3f(point).add((float) entityPos.x, (float) entityPos.y, (float) entityPos.z);
        Vector3f toCam = new Vector3f(
            (float) (camera.x - worldPoint.x()),
            (float) (camera.y - worldPoint.y()),
            (float) (camera.z - worldPoint.z())
        );
        Vector3f side = dir.cross(toCam);
        if (side.lengthSquared() < 1.0E-8F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        return side.normalize().mul(halfWidth).add(point);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                             Vector3f leftStart, Vector3f leftEnd, Vector3f rightEnd, Vector3f rightStart) {
        buffer.vertex(matrix, leftStart.x(), leftStart.y(), leftStart.z()).color(1.0F, 1.0F, 1.0F, 0.95F).uv(0.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, leftEnd.x(), leftEnd.y(), leftEnd.z()).color(1.0F, 1.0F, 1.0F, 0.95F).uv(0.0F, 1.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightEnd.x(), rightEnd.y(), rightEnd.z()).color(1.0F, 1.0F, 1.0F, 0.95F).uv(1.0F, 1.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, rightStart.x(), rightStart.y(), rightStart.z()).color(1.0F, 1.0F, 1.0F, 0.95F).uv(1.0F, 0.0F)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
