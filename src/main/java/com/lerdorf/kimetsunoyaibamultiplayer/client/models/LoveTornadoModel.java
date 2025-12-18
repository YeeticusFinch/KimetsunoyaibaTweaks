package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Love Tornado entity.
 *
 * Model file: love_tornado.geo.json
 * Animation file: love_tornado.animation.json
 * Texture: love_tornado.png
 */
public class LoveTornadoModel extends GeoModel<LoveTornadoEntity> {
    @Override
    public ResourceLocation getModelResource(LoveTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "geo/love_tornado.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(LoveTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/love_tornado.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(LoveTornadoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "animations/love_tornado.animation.json"
        );
    }
}
