package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.client.NezukoBoxClientState;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NezukoBoxBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public NezukoBoxBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!SwordDisplayConfig.renderNezukoBox || player.isInvisible()) {
            return;
        }

        ItemStack box = findBoxInHotbar(player);
        if (box.isEmpty()) {
            box = NezukoBoxClientState.createDisplayStack(player.getUUID());
        }
        if (box.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(
            SwordDisplayConfig.nezukoBoxTranslateX,
            SwordDisplayConfig.nezukoBoxTranslateY,
            SwordDisplayConfig.nezukoBoxTranslateZ
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.nezukoBoxRotateZ));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.nezukoBoxRotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.nezukoBoxRotateX));
        float scale = (float) SwordDisplayConfig.nezukoBoxScale;
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
            box,
            ItemDisplayContext.HEAD,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            player.level(),
            player.getId()
        );
        poseStack.popPose();
    }

    private static ItemStack findBoxInHotbar(AbstractClientPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.NEZUKO_BOX.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
