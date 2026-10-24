package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the marker's block model only while the local player is holding its item. */
public class StructureMarkerRenderer extends EntityRenderer<StructureMarkerEntity> {
    private static final ItemStack MARKER_STACK = new ItemStack(ModItems.STRUCTURE_MARKER.get());
    private final ItemRenderer itemRenderer;

    public StructureMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(StructureMarkerEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
            && StructureMarkerEntity.isHoldingMarker(minecraft.player)
            && super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public void render(StructureMarkerEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        itemRenderer.renderStatic(MARKER_STACK, ItemDisplayContext.NONE,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StructureMarkerEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
