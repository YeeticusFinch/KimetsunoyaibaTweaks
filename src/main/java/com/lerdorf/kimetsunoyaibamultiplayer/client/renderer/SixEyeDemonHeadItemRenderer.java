package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SixEyeDemonHeadModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.SixEyeDemonHeadItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SixEyeDemonHeadItemRenderer extends GeoItemRenderer<SixEyeDemonHeadItem> {
    public SixEyeDemonHeadItemRenderer() {
        super(new SixEyeDemonHeadModel<>());
        addRenderLayer(new SixEyeDemonHeadEyeLayer<>(this));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (displayContext == ItemDisplayContext.HEAD) {
            poseStack.translate(0.0D, -0.16D, 0.0D);
            poseStack.scale(0.9F, 0.9F, 0.9F);
        }
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
