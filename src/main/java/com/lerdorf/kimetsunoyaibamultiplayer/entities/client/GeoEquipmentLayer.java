package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Custom GeckoLib item layer for rendering held items (swords, shields, etc.)
 * Works like GeoArmorLayer but for held items.
 */
public class GeoEquipmentLayer<T extends LivingEntity & GeoAnimatable> extends BlockAndItemGeoLayer<T> {

    public GeoEquipmentLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    /**
     * Maps GeckoLib bone names to held item stacks
     */
    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        String boneName = bone.getName();

        // MAIN HAND
        if (boneName.equals("itemMainHand") || boneName.equals("itemMainHand2") || boneName.equals("itemMainHand3")) {
            return animatable.getItemBySlot(EquipmentSlot.MAINHAND);
        }

        // OFF HAND
        if (boneName.equals("itemOffHand") || boneName.equals("itemOffHand2") || boneName.equals("itemOffHand3")) {
            return animatable.getItemBySlot(EquipmentSlot.OFFHAND);
        }

        // HEAD (only render *non-armor* head items, like pumpkins)
        if (boneName.equals("Head") || boneName.equals("armorHead")) {
            ItemStack headStack = animatable.getItemBySlot(EquipmentSlot.HEAD);
            if (headStack.getItem() instanceof ArmorItem armorItem) {
                // Skip helmets — armor renderer already handles them
                if (armorItem.getEquipmentSlot() == EquipmentSlot.HEAD) {
                    return ItemStack.EMPTY;
                }
            }
            return headStack;
        }

        return null;
    }

    /**
     * Tells GeckoLib how to render each item (perspective transform)
     */
    @NotNull
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();
        
        boolean isNichirinSword = false;
        
       

        switch (boneName) {
            case "itemMainHand":
            case "itemMainHand2":
            case "itemMainHand3":
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

            case "itemOffHand":
            case "itemOffHand2":
            case "itemOffHand3":
                return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

            case "Head":
            case "armorHead":
                return ItemDisplayContext.HEAD;

            default:
                return ItemDisplayContext.NONE;
        }
    }

    /**
     * Applies custom rotation/scale offsets for items before rendering
     */
    @Override
    protected void renderStackForBone(
            PoseStack poseStack,
            GeoBone bone,
            ItemStack stack,
            T animatable,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        String boneName = bone.getName();

        // ✅ MAIN HAND – sword/tool alignment
        if (boneName.startsWith("itemMainHand")) {
            poseStack.pushPose();
            // Position sword in front of hand
            poseStack.translate(0.0, 0.05, 0.01);
            // Rotate sword downward slightly
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
            poseStack.mulPose(Axis.YP.rotationDegrees(0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(0f));
            poseStack.scale(1.0f, 1.0f, 1.0f);
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }

        // ✅ OFF HAND – shield or second weapon
        if (boneName.startsWith("itemOffHand")) {
        	boolean isNichirinSword = false;
        	 ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
             if (key != null && key.toString().toLowerCase().contains("nichirin")) {
                 // This is a Nichirin item
             	isNichirinSword = true;
             }
             
            poseStack.pushPose();
            if (isNichirinSword) {
            	poseStack.translate(0.0, 0.2, -0.03);
	            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
	            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
	            poseStack.mulPose(Axis.ZP.rotationDegrees(0f));
            } else {
	            poseStack.translate(0.0, 0.41, -0.2);
	            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
	            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
	            poseStack.mulPose(Axis.ZP.rotationDegrees(0f));
            }

            // Slight adjustment if it's a shield
            if (stack.getItem() instanceof ShieldItem) {
                poseStack.translate(0.0f, -0.2f, -0.05f);
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            }

            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }

        // ✅ HEAD – non-armor (pumpkins, skulls)
        if (boneName.equals("Head") || boneName.equals("armorHead")) {
            poseStack.pushPose();
            poseStack.translate(0.0, 0.25, 0.0);
            poseStack.scale(0.625f, 0.625f, 0.625f);
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }

        // Default
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
    }
}
