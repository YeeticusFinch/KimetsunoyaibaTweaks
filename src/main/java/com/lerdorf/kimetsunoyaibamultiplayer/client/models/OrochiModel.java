package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class OrochiModel extends GeoModel<OrochiEntity> {
    @Override
    public ResourceLocation getModelResource(OrochiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/orochi.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OrochiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/orochi.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OrochiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/orochi.animation.json");
    }

    @Override
    public void setCustomAnimations(OrochiEntity animatable, long instanceId, AnimationState<OrochiEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) {
            return;
        }

        float partialTick = animationState.getPartialTick();
        float headPitch = animatable.getViewXRot(partialTick);
        float headYaw = animatable.getViewYRot(partialTick);

        head.setRotX(headPitch * Mth.DEG_TO_RAD);
        head.setRotY(headYaw * Mth.DEG_TO_RAD);
    }
}
