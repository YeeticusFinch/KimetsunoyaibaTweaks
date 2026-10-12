package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyeKanjiHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesResourceHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RuiDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

/** Renders Rui's eye and Lower Five overlays after the base model traversal. */
public class RuiFaceOverlaysLayer extends GeoRenderLayer<RuiEntity> {
    private static final ResourceLocation EYES_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_eyes_rui_kanji.png");
    private static final ResourceLocation KANJI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/lower5.png");
    private static final double SKIN_PIXEL = 1.0D / 16.0D;

    private Matrix4f headPose;
    private Matrix3f headNormal;
    private GeoBone headBone;
    private Matrix4f armorHeadPose;
    private Matrix3f armorHeadNormal;
    private GeoBone armorHeadBone;

    public RuiFaceOverlaysLayer(GeoRenderer<RuiEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, RuiEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isInvisible() || animatable.isHeadless()) {
            return;
        }

        // Capture only the animated transforms here. Requesting another buffer during
        // recursive model rendering can corrupt GeckoLib's active model buffer.
        PoseStack.Pose pose = poseStack.last();
        if ("head".equals(bone.getName())) {
            headBone = bone;
            headPose = new Matrix4f(pose.pose());
            headNormal = new Matrix3f(pose.normal());
        } else if ("armorHead".equals(bone.getName())) {
            armorHeadBone = bone;
            armorHeadPose = new Matrix4f(pose.pose());
            armorHeadNormal = new Matrix3f(pose.normal());
        }
    }

    @Override
    public void render(PoseStack poseStack, RuiEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        try {
            if (animatable.isInvisible() || animatable.isHeadless()) {
                return;
            }

            renderEyes(poseStack, bufferSource);
            renderKanji(poseStack, bufferSource);
        } finally {
            headPose = null;
            headNormal = null;
            headBone = null;
            armorHeadPose = null;
            armorHeadNormal = null;
            armorHeadBone = null;
        }
    }

    private void renderEyes(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (armorHeadBone == null || armorHeadPose == null || armorHeadNormal == null) {
            return;
        }

        RenderType eyesRenderType = CustomRenderTypes.geoEntityTranslucentEmissive(EYES_TEXTURE);
        poseStack.pushPose();
        try {
            restorePose(poseStack, armorHeadPose, armorHeadNormal);
            getRenderer().renderCubesOfBone(
                poseStack,
                armorHeadBone,
                bufferSource.getBuffer(eyesRenderType),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
        } finally {
            poseStack.popPose();
        }
    }

    private void renderKanji(PoseStack poseStack, MultiBufferSource bufferSource) {
        if (headBone == null || headPose == null || headNormal == null) {
            return;
        }

        DemonEyeKanjiHelper.EyeKanjiPlacement placement = DemonEyeKanjiHelper.getPlacement(
            DemonEyesResourceHelper.RUI_EYES_INDEX);
        RuiDisplayConfig.TransformConfig config = RuiDisplayConfig.KANJI;
        double width = placement.width() * SKIN_PIXEL * config.scaleX();
        double height = placement.height() * SKIN_PIXEL * config.scaleY();
        if (width <= 0.0D || height <= 0.0D) {
            return;
        }

        RenderType kanjiRenderType = CustomRenderTypes.geoEntityTranslucentFullbright(KANJI_TEXTURE);
        poseStack.pushPose();
        try {
            restorePose(poseStack, headPose, headNormal);
            RenderUtils.translateToPivotPoint(poseStack, headBone);
            poseStack.translate(config.translateX(), config.translateY(), config.translateZ());
            poseStack.mulPose(Axis.XP.rotationDegrees(config.rotateX()));
            poseStack.mulPose(Axis.YP.rotationDegrees(config.rotateY()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(config.rotateZ()));

            VertexConsumer overlayBuffer = bufferSource.getBuffer(kanjiRenderType);
            PoseStack.Pose pose = poseStack.last();
            Matrix4f matrix = pose.pose();
            Matrix3f normal = pose.normal();
            float halfWidth = (float) (width * 0.5D);
            float halfHeight = (float) (height * 0.5D);

            vertex(overlayBuffer, matrix, normal, -halfWidth, halfHeight, 0.0F, 0.0F, 1.0F);
            vertex(overlayBuffer, matrix, normal, halfWidth, halfHeight, 0.0F, 1.0F, 1.0F);
            vertex(overlayBuffer, matrix, normal, halfWidth, -halfHeight, 0.0F, 1.0F, 0.0F);
            vertex(overlayBuffer, matrix, normal, -halfWidth, -halfHeight, 0.0F, 0.0F, 0.0F);
        } finally {
            poseStack.popPose();
        }
    }

    private static void restorePose(PoseStack poseStack, Matrix4f pose, Matrix3f normal) {
        poseStack.last().pose().set(pose);
        poseStack.last().normal().set(normal);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
            .color(1.0F, 1.0F, 1.0F, 1.0F)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0)
            .normal(normal, 0.0F, 0.0F, -1.0F)
            .endVertex();
    }
}
