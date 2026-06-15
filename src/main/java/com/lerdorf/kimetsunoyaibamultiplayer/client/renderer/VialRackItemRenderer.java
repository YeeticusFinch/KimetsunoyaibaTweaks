package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class VialRackItemRenderer extends BlockEntityWithoutLevelRenderer {
    public VialRackItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VialRackRenderer.renderRackModel(poseStack, bufferSource, packedLight, packedOverlay);
        VialRackRenderer.renderVials(Arrays.asList(VialRackBlockItem.getStoredItems(stack)),
            poseStack, bufferSource, packedLight);
    }
}
