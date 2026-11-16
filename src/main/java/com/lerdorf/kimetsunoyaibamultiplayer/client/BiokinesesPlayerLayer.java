package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the virtual Biokineses offhand while the player's arm pose is active. */
public final class BiokinesesPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BiokinesesPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack virtualStack = BiokinesesRenderHandler.getVirtualOffhandStack(player);
        if (virtualStack.isEmpty()) {
            return;
        }

        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        boolean leftHand = offhandArm == HumanoidArm.LEFT;
        poseStack.pushPose();
        getParentModel().translateToHand(offhandArm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (leftHand) {
            BiokinesesRenderHandler.applyThirdPersonLeftHandOffset(poseStack);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(
            virtualStack,
            leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            player.level(),
            player.getId()
        );
        poseStack.popPose();
    }
}
