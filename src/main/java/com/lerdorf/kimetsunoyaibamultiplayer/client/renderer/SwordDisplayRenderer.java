package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.SheathModelRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSheathRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders swords and their sheaths attached to player models (on hip or back)
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

        if (isInvisibilityAnimationActive(player)) {
            return;
        }

        SwordDisplayTracker.SwordDisplayState state = SwordDisplayTracker.getDisplayState(player.getUUID());

        // Check if any swords are in transition (being sheathed)
        // During transition, render them in the player's hand
        if (state.hasLeftSwordInTransition()) {
            renderSwordInHand(poseStack, buffer, packedLight, player, state.getLeftHipSword(),
                            limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        } else if (state.hasRightSwordInTransition()) {
            renderSwordInHand(poseStack, buffer, packedLight, player, state.getRightHipSword(),
                            limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        }

        // Keep sheath visible during sheathing transition so it doesn't flicker out.
        if (state.hasLeftSwordInTransition()) {
            ItemStack transitioningSword = state.getLeftHipSword();
            Item transitionSheath = SwordSheathRegistry.getSheathItem(transitioningSword);
            if (transitionSheath != null) {
                renderSheathOnly(poseStack, buffer, packedLight, player, transitionSheath,
                    transitioningSword.getItem(), true, state.getLeftPosition());
            }
        }

        if (state.hasRightSwordInTransition()) {
            ItemStack transitioningSword = state.getRightHipSword();
            Item transitionSheath = SwordSheathRegistry.getSheathItem(transitioningSword);
            if (transitionSheath != null) {
                renderSheathOnly(poseStack, buffer, packedLight, player, transitionSheath,
                    transitioningSword.getItem(), false, state.getRightPosition());
            }
        }

        // Render left hip/back sword (and sheath)
        if (state.hasLeftSword()) {
            renderSwordWithSheath(poseStack, buffer, packedLight, player, state.getLeftHipSword(),
                                 true, true, state.getLeftPosition());
        } else if (state.shouldShowLeftSheath()) {
            // Render just the sheath if sword is drawn but sheath persists
            renderSheathOnly(poseStack, buffer, packedLight, player, state.leftSheathItem,
                           state.leftSheathSwordItem, true, state.getLeftPosition());
        }

        // Render right hip/back sword (and sheath)
        if (state.hasRightSword()) {
            renderSwordWithSheath(poseStack, buffer, packedLight, player, state.getRightHipSword(),
                                 true, false, state.getRightPosition());
        } else if (state.shouldShowRightSheath()) {
            // Render just the sheath if sword is drawn but sheath persists
            renderSheathOnly(poseStack, buffer, packedLight, player, state.rightSheathItem,
                           state.rightSheathSwordItem, false, state.getRightPosition());
        }
    }

    private boolean isInvisibilityAnimationActive(AbstractClientPlayer player) {
        if (player == null) {
            return false;
        }

        return AnimationTracker.isAnimationActive(player.getUUID(), "invisibility")
            || AnimationSyncHandler.isAnimationActive(player.getUUID(), "invisibility");
    }

    /**
     * Renders a sword with its sheath
     */
    private void renderSwordWithSheath(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      AbstractClientPlayer player, ItemStack sword,
                                      boolean renderSheath, boolean isLeft,
                                      SwordDisplayConfig.SwordDisplayPosition position) {
        poseStack.pushPose();

        // Use the per-sword position (pass sword for custom offsets)
        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            renderSwordOnHip(poseStack, player, isLeft, sword);
        } else {
            renderSwordOnBack(poseStack, player, isLeft, sword);
        }

        // Apply scale from config
        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        // Render the sheath first (behind the sword) if enabled
        if (renderSheath && SwordDisplayConfig.renderSheaths) {
        	Item sheathItem = SwordSheathRegistry.getSheathItem(sword);
        	if (sheathItem != null) {
        	    SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, player.getId());
        	}
        }

        // Render the sword (apply display override if registered, e.g. kokushibo_2 -> kokushibo_1)
        ItemStack displaySword = SwordSheathRegistry.getSheathDisplayItem(sword);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
            displaySword,
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
     * Renders just a sheath (when sword is drawn but sheath persists)
     */
    private void renderSheathOnly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, Item sheathItem, Item swordItem, boolean isLeft,
            SwordDisplayConfig.SwordDisplayPosition position) {
        if (!SwordDisplayConfig.renderSheaths || sheathItem == null) {
            return;
        }

        poseStack.pushPose();

        // Create a dummy ItemStack for the sword to pass to rendering methods
        // (we need it to look up custom offsets)
        ItemStack swordStack = swordItem != null ? new ItemStack(swordItem) : ItemStack.EMPTY;

        // Use the per-sword position (pass sword for custom offsets)
        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            renderSwordOnHip(poseStack, player, isLeft, swordStack);
        } else {
            renderSwordOnBack(poseStack, player, isLeft, swordStack);
        }

        // Apply scale from config
        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        // Render the sheath
        SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, player.getId());

        poseStack.popPose();
    }

    /**
     * Renders a sword in the player's hand during transition (first 5 ticks of sheath animation).
     * This keeps the sword visible in-hand before it moves to hip/back.
     */
    private void renderSwordInHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   AbstractClientPlayer player, ItemStack sword,
                                   float limbSwing, float limbSwingAmount,
                                   float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (sword.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position the sword in the main hand
        // Use the player model's right arm bone position
        PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();

        // Translate to the right arm position
        playerModel.rightArm.translateAndRotate(poseStack);

        // Additional positioning to align with hand
        poseStack.translate(0.0625, 0.125, 0.0625); // Fine-tune position in hand

        // Apply rotations for proper sword grip
        poseStack.mulPose(Axis.XP.rotationDegrees(-90)); // Point sword forward
        poseStack.mulPose(Axis.YP.rotationDegrees(0));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0));

        // Render the sword as if held
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
    private void renderSwordOnHip(PoseStack poseStack, Player player, boolean isLeft, ItemStack sword) {
        // Get per-sword custom offsets if they exist
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword.getItem());
        SwordDisplayConfig.SwordOffsets customOffsets = itemId == null ? null : SwordDisplayConfig.getSwordOffsets(
            itemId.toString(),
            isLeft ? SwordDisplayConfig.SwordDisplaySlot.HIP_LEFT : SwordDisplayConfig.SwordDisplaySlot.HIP_RIGHT
        );

        if (isLeft) {
            // Left hip - use config values + custom offsets
            poseStack.translate(
                SwordDisplayConfig.hipLeftTranslateX + (customOffsets != null ? customOffsets.translateX : 0.0),
                SwordDisplayConfig.hipLeftTranslateY + (customOffsets != null ? customOffsets.translateY : 0.0),
                SwordDisplayConfig.hipLeftTranslateZ + (customOffsets != null ? customOffsets.translateZ : 0.0)
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (SwordDisplayConfig.hipLeftRotateZ + (customOffsets != null ? customOffsets.rotateZ : 0.0))));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (SwordDisplayConfig.hipLeftRotateY + (customOffsets != null ? customOffsets.rotateY : 0.0))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (SwordDisplayConfig.hipLeftRotateX + (customOffsets != null ? customOffsets.rotateX : 0.0))));
        } else {
            // Right hip - use config values + custom offsets
            poseStack.translate(
                SwordDisplayConfig.hipRightTranslateX + (customOffsets != null ? customOffsets.translateX : 0.0),
                SwordDisplayConfig.hipRightTranslateY + (customOffsets != null ? customOffsets.translateY : 0.0),
                SwordDisplayConfig.hipRightTranslateZ + (customOffsets != null ? customOffsets.translateZ : 0.0)
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (SwordDisplayConfig.hipRightRotateZ + (customOffsets != null ? customOffsets.rotateZ : 0.0))));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (SwordDisplayConfig.hipRightRotateY + (customOffsets != null ? customOffsets.rotateY : 0.0))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (SwordDisplayConfig.hipRightRotateX + (customOffsets != null ? customOffsets.rotateX : 0.0))));
        }
    }

    /**
     * Positions and rotates the sword for back placement
     */
    private void renderSwordOnBack(PoseStack poseStack, Player player, boolean isLeft, ItemStack sword) {
        // Get per-sword custom offsets if they exist
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword.getItem());
        SwordDisplayConfig.SwordOffsets customOffsets = itemId == null ? null : SwordDisplayConfig.getSwordOffsets(
            itemId.toString(),
            isLeft ? SwordDisplayConfig.SwordDisplaySlot.BACK_LEFT : SwordDisplayConfig.SwordDisplaySlot.BACK_RIGHT
        );

        if (isLeft) {
            // Left side of back - use config values + custom offsets
            poseStack.translate(
                SwordDisplayConfig.backLeftTranslateX + (customOffsets != null ? customOffsets.translateX : 0.0),
                SwordDisplayConfig.backLeftTranslateY + (customOffsets != null ? customOffsets.translateY : 0.0),
                SwordDisplayConfig.backLeftTranslateZ + (customOffsets != null ? customOffsets.translateZ : 0.0)
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (SwordDisplayConfig.backLeftRotateZ + (customOffsets != null ? customOffsets.rotateZ : 0.0))));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (SwordDisplayConfig.backLeftRotateY + (customOffsets != null ? customOffsets.rotateY : 0.0))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (SwordDisplayConfig.backLeftRotateX + (customOffsets != null ? customOffsets.rotateX : 0.0))));
        } else {
            // Right side of back - use config values + custom offsets
            poseStack.translate(
                SwordDisplayConfig.backRightTranslateX + (customOffsets != null ? customOffsets.translateX : 0.0),
                SwordDisplayConfig.backRightTranslateY + (customOffsets != null ? customOffsets.translateY : 0.0),
                SwordDisplayConfig.backRightTranslateZ + (customOffsets != null ? customOffsets.translateZ : 0.0)
            );
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (SwordDisplayConfig.backRightRotateZ + (customOffsets != null ? customOffsets.rotateZ : 0.0))));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (SwordDisplayConfig.backRightRotateY + (customOffsets != null ? customOffsets.rotateY : 0.0))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (SwordDisplayConfig.backRightRotateX + (customOffsets != null ? customOffsets.rotateX : 0.0))));
        }
    }

}
