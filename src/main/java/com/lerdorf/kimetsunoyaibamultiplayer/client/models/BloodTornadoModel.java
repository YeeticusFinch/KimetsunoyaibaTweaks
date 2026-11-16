package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib model for the Blood Tornado entity. */
public final class BloodTornadoModel extends GeoModel<BloodTornadoEntity> {
    @Override
    public ResourceLocation getModelResource(BloodTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "geo/blood_tornado.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloodTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/entity/blood_tornado.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloodTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "animations/blood_tornado.animation.json");
    }
}
