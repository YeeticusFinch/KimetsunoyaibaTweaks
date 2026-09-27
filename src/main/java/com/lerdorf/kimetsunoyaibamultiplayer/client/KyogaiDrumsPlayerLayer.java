package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.KyogaiDrumsArmorRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.KyogaiDrumsItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Renders Kyogai's drums for players carrying them in the hotbar. */
public class KyogaiDrumsPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final KyogaiDrumsArmorRenderer armorRenderer = new KyogaiDrumsArmorRenderer();

    public KyogaiDrumsPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack drums = findHotbarDrums(player);
        if (drums.isEmpty()) {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();
        renderSlot(poseStack, bufferSource, packedLight, partialTick, player, playerModel, drums, EquipmentSlot.CHEST);
        renderSlot(poseStack, bufferSource, packedLight, partialTick, player, playerModel, drums, EquipmentSlot.LEGS);
        renderSlot(poseStack, bufferSource, packedLight, partialTick, player, playerModel, drums, EquipmentSlot.FEET);
    }

    private void renderSlot(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick,
                            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
                            ItemStack drums, EquipmentSlot slot) {
        armorRenderer.prepForRender(player, drums, slot, playerModel);
        armorRenderer.renderToBuffer(poseStack, null, packedLight, 0, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private ItemStack findHotbarDrums(AbstractClientPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == ModItems.KYOGAI_DRUMS.get()
                && stack.getItem() instanceof KyogaiDrumsItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
