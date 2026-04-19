package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesClientState;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesResourceHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class DemonEyesPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
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
            DemonEyesResourceHelper.getTexture(state.index())
        );
        VertexConsumer consumer = buffer.getBuffer(renderType);
        getParentModel().renderToBuffer(
            poseStack,
            consumer,
            0xF000F0,
            LivingEntityRenderer.getOverlayCoords(player, 0.0F),
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );
    }
}
