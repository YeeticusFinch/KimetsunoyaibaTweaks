package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.AlchemyTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AlchemyTableRenderer implements BlockEntityRenderer<AlchemyTableBlockEntity> {
    private static final ResourceLocation FLAME_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/item/alchemy_burner_flame.png");

    public AlchemyTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AlchemyTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderBottomSlotContainer(table, poseStack, bufferSource, packedLight);
        if (table.isLit()) {
            renderFlame(poseStack, bufferSource);
        }
    }

    private void renderBottomSlotContainer(AlchemyTableBlockEntity table, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = table.getBottomDisplayStack();
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        if (BloodDemonArtAlchemyCatalog.isPetriDishDisplayItem(stack)) {
            poseStack.translate(0.5D, 0.4328125D, 0.3125D);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else {
            poseStack.translate(0.5D, 0.625D, 0.3125D);
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.NONE,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            Minecraft.getInstance().level,
            stack.hashCode()
        );
        poseStack.popPose();
    }

    private void renderFlame(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(FLAME_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        int light = LightTexture.FULL_BRIGHT;

        addQuad(buffer, matrix, normal,
            0.5F, 0.0625F, 0.25F,
            0.5F, 0.1875F, 0.25F,
            0.5F, 0.1875F, 0.375F,
            0.5F, 0.0625F, 0.375F,
            light);
        addQuad(buffer, matrix, normal,
            0.4375F, 0.0625F, 0.3125F,
            0.4375F, 0.1875F, 0.3125F,
            0.5625F, 0.1875F, 0.3125F,
            0.5625F, 0.0625F, 0.3125F,
            light);
    }

    private void addQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         int light) {
        vertex(buffer, matrix, normal, x1, y1, z1, 0.0F, 1.0F, light);
        vertex(buffer, matrix, normal, x2, y2, z2, 0.0F, 0.0F, light);
        vertex(buffer, matrix, normal, x3, y3, z3, 1.0F, 0.0F, light);
        vertex(buffer, matrix, normal, x4, y4, z4, 1.0F, 1.0F, light);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                        float x, float y, float z, float u, float v, int light) {
        buffer.vertex(matrix, x, y, z)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }
}
