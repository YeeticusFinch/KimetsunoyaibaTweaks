package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.EyeFamiliarEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class EyeFamiliarModel extends GeoModel<EyeFamiliarEntity> {
    @Override
    public ResourceLocation getModelResource(EyeFamiliarEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/eye_familiar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EyeFamiliarEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/eye_familiar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EyeFamiliarEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/eye_familiar.animation.json");
    }

    @Override
    public void setCustomAnimations(EyeFamiliarEntity animatable, long instanceId, AnimationState<EyeFamiliarEntity> animationState) {
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
