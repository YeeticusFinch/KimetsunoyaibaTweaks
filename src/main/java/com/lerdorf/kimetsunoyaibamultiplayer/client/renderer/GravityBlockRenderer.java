package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.model.data.ModelData;

public class GravityBlockRenderer implements BlockEntityRenderer<GravityBlockEntity> {
    private static final double MAX_RENDER_DISTANCE_SQR = 20.0D * 20.0D;

    public GravityBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GravityBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !GravityBlockEntity.isHoldingGravityBlock(player)) {
            return;
        }
        if (player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5D,
            blockEntity.getBlockPos().getY() + 0.5D,
            blockEntity.getBlockPos().getZ() + 0.5D) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockEntity.getBlockState(), poseStack, buffer,
            packedLight, packedOverlay, ModelData.EMPTY, null);
    }
}
