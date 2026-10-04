package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DirectionArrowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DirectionArrowModel extends GeoModel<DirectionArrowEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "geo/arrow.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "textures/entity/arrow.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "animations/arrow.animation.json");

    @Override
    public ResourceLocation getModelResource(DirectionArrowEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DirectionArrowEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DirectionArrowEntity animatable) {
        return ANIMATION;
    }
}
