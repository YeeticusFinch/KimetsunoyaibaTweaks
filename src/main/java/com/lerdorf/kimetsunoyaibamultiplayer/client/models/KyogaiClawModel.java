package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiClawEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KyogaiClawModel extends GeoModel<KyogaiClawEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/kyogai_claw.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/kyogai_claw.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/kyogai_claw.animation.json");

    @Override
    public ResourceLocation getModelResource(KyogaiClawEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KyogaiClawEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KyogaiClawEntity animatable) {
        return ANIMATION;
    }
}
