package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyeKanjiHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesClientState;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesResourceHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DemonEyesPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final double HEAD_FRONT_Z = -4.08D / 16.0D;
    private static final double SKIN_PIXEL = 1.0D / 16.0D;

    public DemonEyesPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        DemonEyesClientState.PlayerDemonEyesState state = DemonEyesClientState.getPlayerState(player.getUUID());
        if (state == null || !state.demon()) {
            return;
        }

        RenderType renderType = CustomRenderTypes.geoEntityTranslucentEmissive(
            DemonEyeKanjiHelper.getEyeOverlayTexture(state.index(), state.rankTier())
        );
        float[] tint = DemonEyesResourceHelper.getHueTint(state.hue());
        VertexConsumer consumer = buffer.getBuffer(renderType);
        getParentModel().renderToBuffer(
            poseStack,
            consumer,
            0xF000F0,
            LivingEntityRenderer.getOverlayCoords(player, 0.0F),
            tint[0],
            tint[1],
            tint[2],
            1.0F
        );

        renderKanji(poseStack, buffer, state);
    }

    private void renderKanji(PoseStack poseStack, MultiBufferSource buffer,
                             DemonEyesClientState.PlayerDemonEyesState state) {
        DemonEyeKanjiHelper.EyeKanjiPlacement placement = DemonEyeKanjiHelper.getPlacement(state.index());
        double width = Math.max(0.0D, placement.width()) * SKIN_PIXEL;
        double height = Math.max(0.0D, placement.height()) * SKIN_PIXEL;
        if (width <= 0.0D || height <= 0.0D) {
            return;
        }

        if (DemonEyeKanjiHelper.shouldRenderMirroredUpperKanji(state.rankTier(), placement)) {
            ResourceLocation rightTexture = DemonEyeKanjiHelper.getRightTexture(state.rankTier());
            ResourceLocation leftTexture = DemonEyeKanjiHelper.getLeftTexture(state.rankTier());
            if (rightTexture != null && leftTexture != null) {
                renderKanjiQuad(poseStack, buffer, rightTexture, placement.xOffset(), placement.yOffset(), width, height, placement.rotation());
                renderKanjiQuad(poseStack, buffer, leftTexture, -placement.xOffset(), placement.yOffset(), width, height, -placement.rotation());
                return;
            }
        }

        ResourceLocation texture = DemonEyeKanjiHelper.getTexture(state.rankTier());
        if (texture == null) {
            return;
        }
        renderKanjiQuad(poseStack, buffer, texture, placement.xOffset(), placement.yOffset(), width, height, placement.rotation());
    }

    private void renderKanjiQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
                                 double xOffset, double yOffset, double width, double height, double rotation) {
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(xOffset * SKIN_PIXEL, (-4.0D + yOffset) * SKIN_PIXEL, HEAD_FRONT_Z);
        if (rotation != 0.0D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation));
        }

        //RenderType renderType = CustomRenderTypes.geoEntityTranslucentEmissive(texture);
        RenderType renderType = CustomRenderTypes.geoEntityTranslucentFullbright(texture);
        //RenderType renderType = RenderType.entityTranslucentEmissive(texture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float halfWidth = (float) (width * 0.5D);
        float halfHeight = (float) (height * 0.5D);

        vertex(consumer, matrix, normal, -halfWidth, halfHeight, 0.0F, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, halfWidth, halfHeight, 0.0F, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, halfWidth, -halfHeight, 0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, -halfWidth, -halfHeight, 0.0F, 0.0F, 0.0F);
        poseStack.popPose();
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
