package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SixEyeDemonHeadModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.SixEyeDemonHeadItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
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
    public ResourceLocation getTextureLocation(SixEyeDemonHeadItem animatable) {
        ItemStack stack = getCurrentItemStack();
        if (isEyesOnly(stack)) {
            return stack.getTag().getBoolean("SixEyeDemonHeadKanji")
                ? SixEyeDemonHeadEyeLayer.EYES_KANJI_TEXTURE : SixEyeDemonHeadEyeLayer.EYES_TEXTURE;
        }
        return super.getTextureLocation(animatable);
    }

    @Override
    public RenderType getRenderType(SixEyeDemonHeadItem animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return isEyesOnly(getCurrentItemStack())
            ? CustomRenderTypes.geoEntityTranslucentEmissive(texture)
            : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        try {
            if (isEyesOnly(stack) && displayContext == ItemDisplayContext.HEAD) {
                // Undo item centering and the shared JSON HEAD transform before applying the eye placement.
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.translate(0.0D, -7.75D / 16.0D, 1.0D / 16.0D);
                poseStack.translate(0.0D, 6.25D / 16.0D, 0.25D / 16.0D);
                poseStack.scale(1.6F, 1.6F, 1.6F);
                poseStack.translate(-0.5D, -0.5D, -0.5D);
            }
            super.renderByItem(stack, displayContext, poseStack, bufferSource,
                isEyesOnly(stack) ? 0xF000F0 : packedLight, packedOverlay);
        } finally {
            poseStack.popPose();
        }
    }

    static boolean isEyesOnly(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean("SixEyeDemonHeadEyesOnly");
    }
}
