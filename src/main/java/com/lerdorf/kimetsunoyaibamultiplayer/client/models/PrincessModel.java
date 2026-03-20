package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.PrincessEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PrincessModel extends GeoModel<PrincessEntity> {
    @Override
    public ResourceLocation getModelResource(PrincessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/princess.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PrincessEntity animatable) {
        String texture = animatable.shouldUseClosedEyesTexture()
            ? "textures/entity/princess_poodle_eyes_closed.png"
            : "textures/entity/princess_poodle.png";
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texture);
    }

    @Override
    public ResourceLocation getAnimationResource(PrincessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/princess.animation.json");
    }
}
