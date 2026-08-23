package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.WisteriaIncenseBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.WisteriaIncenseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaIncenseBlockRenderer implements BlockEntityRenderer<WisteriaIncenseBlockEntity> {
    public WisteriaIncenseBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WisteriaIncenseBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof WisteriaIncenseBlock incense) || incense.isPotted()) {
            return;
        }

        for (WisteriaIncenseBlock.IncensePose pose : WisteriaIncenseBlock.getNormalIncensePoses(state.getValue(WisteriaIncenseBlock.COUNT))) {
            renderIncense(state, pose, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderIncense(BlockState state, WisteriaIncenseBlock.IncensePose pose, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState renderState = state.setValue(WisteriaIncenseBlock.COUNT, 1);

        poseStack.pushPose();
        poseStack.translate(pose.x() / 16.0D, 0.0D, pose.z() / 16.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yRotation()));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
            renderState,
            minecraft.getBlockRenderer().getBlockModel(renderState),
            1.0F,
            1.0F,
            1.0F,
            packedLight,
            packedOverlay
        );
        poseStack.popPose();
    }
}
