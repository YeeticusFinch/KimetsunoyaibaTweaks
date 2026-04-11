package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampHandEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Swamp Hand entity.
 */
public class SwampHandModel extends GeoModel<SwampHandEntity> {
    @Override
    public ResourceLocation getModelResource(SwampHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "geo/swamp_hand.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(SwampHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/swamp_demon_art.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(SwampHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "animations/swamp_hand.animation.json"
        );
    }
}
