package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.PetriDishBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PetriDishBlockRenderer implements BlockEntityRenderer<PetriDishBlockEntity> {
    private static final double BLOCK_Y_OFFSET = 0.05D;
    private static final double MODEL_CENTER = 8.0D;
    private static final double MODEL_DISH_X = 5.0D;
    private static final double MODEL_DISH_Y = 0.0D;
    private static final double MODEL_DISH_Z = 5.0D;
    private static final DishPose[][] LAYOUTS = {
        {},
        {new DishPose(5.0D, 0.0D, 5.0D, 0.0F)},
        {
            new DishPose(8.0D, 0.0D, 7.0D, -22.5F),
            new DishPose(2.0D, 0.0D, 3.0D, -22.5F)
        },
        {
            new DishPose(8.5D, 0.0D, 8.5D, -22.5F),
            new DishPose(1.0D, 0.0D, 5.75D, 22.5F),
            new DishPose(7.75D, 0.0D, 0.5D, 0.0F)
        }
    };

    public PetriDishBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PetriDishBlockEntity petriDish, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int dishCount = Math.min(petriDish.getDishCount(), PetriDishBlockEntity.MAX_DISHES);
        DishPose[] layout = LAYOUTS[dishCount];
        for (int slot = 0; slot < layout.length; slot++) {
            renderDish(petriDish.getRenderItem(slot), layout[slot], slot, poseStack, bufferSource, packedLight);
        }
    }

    private static void renderDish(ItemStack stack, DishPose dishPose, int slot, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(dishPose.x() / 16.0D, dishPose.y() / 16.0D + BLOCK_Y_OFFSET, dishPose.z() / 16.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(dishPose.yRotation()));
        poseStack.translate(
            (MODEL_CENTER - MODEL_DISH_X) / 16.0D,
            (MODEL_CENTER - MODEL_DISH_Y) / 16.0D,
            (MODEL_CENTER - MODEL_DISH_Z) / 16.0D
        );
        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.NONE,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            Minecraft.getInstance().level,
            stack.hashCode() + slot
        );
        poseStack.popPose();
    }

    private record DishPose(double x, double y, double z, float yRotation) {
    }
}
