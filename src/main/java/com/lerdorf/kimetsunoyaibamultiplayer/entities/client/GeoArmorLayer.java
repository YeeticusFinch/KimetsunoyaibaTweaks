package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Armor rendering layer for GeckoLib entities using biped.geo.json
 * Uses GeckoLib's built-in ItemArmorGeoLayer which handles all the rendering
 * We just map bone names to armor slots
 */
public class GeoArmorLayer<T extends LivingEntity & GeoAnimatable> extends ItemArmorGeoLayer<T> {

    public GeoArmorLayer(GeoRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    /**
     * Returns which armor piece should be rendered on this bone
     */
    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        String boneName = bone.getName();

        switch (boneName) {
            // Boots
            case "armorLeftBoot":
            case "armorRightBoot":
                return this.bootsStack;

            // Leggings
            case "armorLeftLeg":
            case "armorRightLeg":
                return this.leggingsStack;

            // Chestplate (body + arms)
            case "armorBody":
            case "armorLeftArm":
            case "armorRightArm":
                return this.chestplateStack;

            // Helmet
            case "armorHead":
                return this.helmetStack;

            default:
                return null;
        }
    }

    /**
     * Returns which equipment slot this bone corresponds to
     */
    @Nonnull
    @Override
    protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();

        switch (boneName) {
            case "armorLeftBoot":
            case "armorRightBoot":
                return EquipmentSlot.FEET;

            case "armorLeftLeg":
            case "armorRightLeg":
                return EquipmentSlot.LEGS;

            case "armorBody":
            case "armorLeftArm":
            case "armorRightArm":
                return EquipmentSlot.CHEST;

            case "armorHead":
                return EquipmentSlot.HEAD;

            default:
                return super.getEquipmentSlotForBone(bone, stack, animatable);
        }
    }

    /**
     * Returns which part of the HumanoidModel should be rendered on this bone
     */
    @Nonnull
    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, HumanoidModel<?> baseModel) {
        String boneName = bone.getName();

        switch (boneName) {
            case "armorLeftLeg":
            case "armorLeftBoot":
                return baseModel.leftLeg;

            case "armorRightLeg":
            case "armorRightBoot":
                return baseModel.rightLeg;

            case "armorLeftArm":
                return baseModel.leftArm;

            case "armorRightArm":
                return baseModel.rightArm;

            case "armorBody":
                return baseModel.body;

            case "armorHead":
                return baseModel.head;

            default:
                return super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
        }
    }
}
