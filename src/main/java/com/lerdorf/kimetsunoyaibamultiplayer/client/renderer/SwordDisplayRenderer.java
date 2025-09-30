package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders swords attached to player models (on hip or back)
 */
public class SwordDisplayRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public SwordDisplayRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                      AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!SwordDisplayConfig.enabled) {
            return;
        }

        SwordDisplayTracker.SwordDisplayState state = SwordDisplayTracker.getDisplayState(player.getUUID());

        // Render left hip/back sword
        if (state.hasLeftSword()) {
            renderSword(poseStack, buffer, packedLight, player, state.leftHipSword, true);
        }

        // Render right hip/back sword
        if (state.hasRightSword()) {
            renderSword(poseStack, buffer, packedLight, player, state.rightHipSword, false);
        }
    }

    /**
     * Renders a single sword on the player model
     * @param isLeft true for left hip/back, false for right
     */
    private void renderSword(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                            AbstractClientPlayer player, ItemStack sword, boolean isLeft) {
        poseStack.pushPose();

        // Get the display position from config
        SwordDisplayConfig.SwordDisplayPosition position = SwordDisplayConfig.position;

        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            renderSwordOnHip(poseStack, player, isLeft);
        } else {
            renderSwordOnBack(poseStack, player, isLeft);
        }

        // Apply scale from config
        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        // Render the item
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
            sword,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            player.level(),
            player.getId()
        );

        poseStack.popPose();
    }

    /**
     * Positions and rotates the sword for hip placement
     */
    private void renderSwordOnHip(PoseStack poseStack, Player player, boolean isLeft) {
        if (isLeft) {
            // Left hip - use config values
            poseStack.translate(
                SwordDisplayConfig.hipLeftTranslateX,
                SwordDisplayConfig.hipLeftTranslateY,
                SwordDisplayConfig.hipLeftTranslateZ
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.hipLeftRotateZ));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.hipLeftRotateY));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.hipLeftRotateX));
        } else {
            // Right hip - use config values
            poseStack.translate(
                SwordDisplayConfig.hipRightTranslateX,
                SwordDisplayConfig.hipRightTranslateY,
                SwordDisplayConfig.hipRightTranslateZ
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateZ));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateY));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateX));
        }
    }

    /**
     * Positions and rotates the sword for back placement
     */
    private void renderSwordOnBack(PoseStack poseStack, Player player, boolean isLeft) {
        if (isLeft) {
            // Left side of back - use config values
            poseStack.translate(
                SwordDisplayConfig.backLeftTranslateX,
                SwordDisplayConfig.backLeftTranslateY,
                SwordDisplayConfig.backLeftTranslateZ
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.backLeftRotateZ));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.backLeftRotateY));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.backLeftRotateX));
        } else {
            // Right side of back - use config values
            poseStack.translate(
                SwordDisplayConfig.backRightTranslateX,
                SwordDisplayConfig.backRightTranslateY,
                SwordDisplayConfig.backRightTranslateZ
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.backRightRotateZ));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.backRightRotateY));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.backRightRotateX));
        }
    }
}
