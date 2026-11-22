package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.Biokineses;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.BiokinesesArmorRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Renders Biokineses armor while model 2 is active on the held item. */
public final class BiokinesesArmorPlayerLayer
    extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final BiokinesesArmorRenderer armorRenderer = new BiokinesesArmorRenderer();

    public BiokinesesArmorPlayerLayer(
        RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack heldItem = player.getMainHandItem();
        if (!isBiokinesesModelTwo(heldItem)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();
        renderSlot(poseStack, packedLight, player, playerModel, heldItem, EquipmentSlot.HEAD);
        renderSlot(poseStack, packedLight, player, playerModel, heldItem, EquipmentSlot.CHEST);
        renderSlot(poseStack, packedLight, player, playerModel, heldItem, EquipmentSlot.LEGS);
        renderSlot(poseStack, packedLight, player, playerModel, heldItem, EquipmentSlot.FEET);
    }

    private void renderSlot(PoseStack poseStack, int packedLight, AbstractClientPlayer player,
                            PlayerModel<AbstractClientPlayer> playerModel, ItemStack heldItem,
                            EquipmentSlot slot) {
        armorRenderer.prepForHeldItem(player, heldItem, slot, playerModel);
        armorRenderer.renderToBuffer(poseStack, null, packedLight, 0,
            1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean isBiokinesesModelTwo(ItemStack stack) {
        return stack.getItem() == ModItems.BIOKINESES.get()
            && stack.hasTag()
            && stack.getTag().getInt(Biokineses.MODEL_VARIANT_TAG) == 2;
    }
}
