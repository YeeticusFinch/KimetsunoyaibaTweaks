package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib render layer that attaches demon slayer marks to entity bones.
 * This layer renders the mark model at a specific bone position with custom offsets.
 */
public class DemonSlayerMarkLayer<T extends LivingEntity & GeoAnimatable> extends GeoRenderLayer<T> {
    private final DemonSlayerMark mark;

    public DemonSlayerMarkLayer(GeoRenderer<T> renderer, DemonSlayerMark mark) {
        super(renderer);
        this.mark = mark;
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        // Check if mark should be rendered for this entity
        if (!mark.shouldRender(animatable)) {
            return;
        }

        // Find the bone to attach to
        GeoBone bone = bakedModel.getBone(mark.getBoneName()).orElse(null);
        if (bone == null) {
            return;
        }

        // Get the item stack representing the mark model
        ItemStack markStack = getMarkItemStack();
        if (markStack.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position the pose stack at the bone's world position
        positionAtBone(poseStack, bone, animatable, partialTick);

        // Apply custom offsets
        applyMarkTransforms(poseStack);

        // Render the mark model
        Minecraft.getInstance().getItemRenderer().renderStatic(
            markStack,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            bufferSource,
            animatable.level(),
            animatable.getId()
        );

        poseStack.popPose();
    }

    /**
     * Positions the pose stack at the specified bone's world position.
     */
    private void positionAtBone(PoseStack poseStack, GeoBone bone, T animatable, float partialTick) {
        // Get bone world position from GeckoLib
        poseStack.translate(
            bone.getPivotX() / 16.0,
            bone.getPivotY() / 16.0,
            bone.getPivotZ() / 16.0
        );

        // Apply bone rotations
        if (bone.getRotX() != 0) {
            poseStack.mulPose(Axis.XP.rotation(bone.getRotX()));
        }
        if (bone.getRotY() != 0) {
            poseStack.mulPose(Axis.YP.rotation(bone.getRotY()));
        }
        if (bone.getRotZ() != 0) {
            poseStack.mulPose(Axis.ZP.rotation(bone.getRotZ()));
        }
    }

    /**
     * Applies the mark's custom position, rotation, and scale offsets.
     */
    private void applyMarkTransforms(PoseStack poseStack) {
        // Apply position offset
        poseStack.translate(
            mark.getPositionOffset().x,
            mark.getPositionOffset().y,
            mark.getPositionOffset().z
        );

        // Apply rotation offset (convert degrees to radians)
        if (mark.getRotationOffset().x != 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees((float) mark.getRotationOffset().x));
        }
        if (mark.getRotationOffset().y != 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees((float) mark.getRotationOffset().y));
        }
        if (mark.getRotationOffset().z != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) mark.getRotationOffset().z));
        }

        // Apply scale offset
        poseStack.scale(
            (float) mark.getScaleOffset().x,
            (float) mark.getScaleOffset().y,
            (float) mark.getScaleOffset().z
        );
    }

    /**
     * Creates an ItemStack that represents the mark model.
     * This uses the model's resource location to find the corresponding item.
     */
    @SuppressWarnings("deprecation")
    private ItemStack getMarkItemStack() {
        // Try to find a registered item with this name
        var item = BuiltInRegistries.ITEM.get(mark.getModelLocation());
        if (item != null && item != Items.AIR) {
            return new ItemStack(item);
        }

        // Fallback: return empty (mark won't render)
        return ItemStack.EMPTY;
    }
}
