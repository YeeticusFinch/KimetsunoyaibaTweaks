package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DarkStarVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DarkStarRenderer extends EntityRenderer<DarkStarVisualEntity> {
    public DarkStarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DarkStarVisualEntity entity, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float age = entity.tickCount + partialTick;
        float spinDegrees = age * 30.0F;
        float scale = entity.getRenderScale();

        // Paper loc.setRotation(20, 20)
        poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F)); // yaw
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F)); // pitch

        // Paper display transformation rotation: rotateY(...)
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));

        poseStack.scale(scale, scale, scale);

        //ItemStack stack = new ItemStack(ModAlchemyItems.DARK_STAR.get());
        //stack.getOrCreateTag().putInt("TintColor", entity.getTintColor());

        Minecraft mc = Minecraft.getInstance();

        ItemStack stack = new ItemStack(ModAlchemyItems.DARK_STAR.get());
        stack.getOrCreateTag().putInt("TintColor", brightenColor(entity.getTintColor(), 1.5F));

        var itemRenderer = mc.getItemRenderer();
        var model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());

        itemRenderer.render(
                stack,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                buffer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model);

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
    
    private static int brightenColor(int color, float multiplier) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;

        r = Math.min(255, (int) (r * multiplier));
        g = Math.min(255, (int) (g * multiplier));
        b = Math.min(255, (int) (b * multiplier));

        return (r << 16) | (g << 8) | b;
    }

    @Override
    public ResourceLocation getTextureLocation(DarkStarVisualEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
