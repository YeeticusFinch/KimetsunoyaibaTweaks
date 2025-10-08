package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Render layer that displays equipment (sword) on GeckoLib entities
 * Uses BlockAndItemGeoLayer as a base to properly hook into GeckoLib's rendering
 */
public class GeoEquipmentLayer<T extends GeoAnimatable> extends BlockAndItemGeoLayer<T> {

    public GeoEquipmentLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        // Only render equipment on LivingEntity instances
        if (!(animatable instanceof LivingEntity entity)) {
            return null;
        }

        String boneName = bone.getName();

        // Main hand item - ONLY render on itemMainHand bone
        if (boneName.equals("itemMainHand")) {
            return entity.getItemBySlot(EquipmentSlot.MAINHAND);
        }

        // Off hand item
        if (boneName.equals("itemOffHand")) {
            return entity.getItemBySlot(EquipmentSlot.OFFHAND);
        }

        // NOTE: Armor rendering is disabled here because rendering armor as items looks wrong.
        // Armor still provides protection, it just won't be visually rendered on GeckoLib entities.
        // To properly render armor on GeckoLib entities would require custom armor models
        // that match the GeckoLib skeleton structure, which is beyond the scope of this layer.

        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();

        // Use third person right hand for main hand items
        if (boneName.equals("itemMainHand")) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        // Use third person left hand for off hand items
        if (boneName.equals("itemOffHand")) {
            return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        return ItemDisplayContext.NONE;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable,
                                     MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        String boneName = bone.getName();

        // Main hand item - apply rotations for proper sword holding
        if (boneName.equals("itemMainHand")) {
            poseStack.pushPose();

            // Rotate the sword to be held properly (pointing forward)
            poseStack.mulPose(Axis.XP.rotationDegrees(0));   // X rotation
            poseStack.mulPose(Axis.YP.rotationDegrees(0));   // Y rotation
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));  // Z rotation - rotate 90 degrees

            poseStack.popPose();
        }

        // Off hand item - rotate and move upward
        if (boneName.equals("itemOffHand")) {
            poseStack.pushPose();

            // Move the item upward
            poseStack.translate(0, -0.3, 0);  // Move up by 0.3 blocks

            // Rotate the item for off-hand
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90));

            poseStack.popPose();
        }

        // Call super to do the actual rendering
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
    }
}
