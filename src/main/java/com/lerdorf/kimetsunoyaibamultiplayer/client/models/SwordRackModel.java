package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SwordRackBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.SwordRackBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SwordRackModel extends GeoModel<SwordRackBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SwordRackBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            animatable != null && animatable.getBlockState().getValue(SwordRackBlock.WALL)
                ? "models/item/sword_rack_wall.json"
                : "models/item/sword_rack_floor.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(SwordRackBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/item/sword_rack.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwordRackBlockEntity animatable) {
        return null;
    }

    @Override
    public void setCustomAnimations(SwordRackBlockEntity animatable, long instanceId, AnimationState<SwordRackBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone root = getAnimationProcessor().getBone("sword_rack");
        if (root == null || animatable == null) {
            return;
        }

        if (animatable.getBlockState().getValue(SwordRackBlock.WALL)) {
            root.setRotY(0.0F);
            return;
        }

        root.setRotY(animatable.getBlockState().getValue(SwordRackBlock.ROTATION) * 22.5F * Mth.DEG_TO_RAD);
    }
}
