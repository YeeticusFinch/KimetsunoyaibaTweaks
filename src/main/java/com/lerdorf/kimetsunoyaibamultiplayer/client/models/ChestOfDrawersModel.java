package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ChestOfDrawersBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class ChestOfDrawersModel extends GeoModel<ChestOfDrawersBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ChestOfDrawersBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/chest_of_drawers.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChestOfDrawersBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/block/chest_of_drawers.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChestOfDrawersBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/chest_of_drawers.animation.json");
    }

    @Override
    public void setCustomAnimations(ChestOfDrawersBlockEntity animatable, long instanceId, AnimationState<ChestOfDrawersBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        setDrawerOffset("top", animatable.getDrawerOffset(0));
        setDrawerOffset("topmiddle", animatable.getDrawerOffset(1));
        setDrawerOffset("middlebottom", animatable.getDrawerOffset(2));
        setDrawerOffset("bottom", animatable.getDrawerOffset(3));
    }

    private void setDrawerOffset(String boneName, float offset) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setPosZ(offset);
        }
    }
}
