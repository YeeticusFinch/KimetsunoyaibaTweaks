package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ThunderDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib resource binding for the Flaming Thunder God dragon. */
public final class ThunderDragonModel extends GeoModel<ThunderDragonEntity> {
    @Override
    public ResourceLocation getModelResource(ThunderDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "geo/thunder_dragon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ThunderDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "textures/entity/thunder_dragon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ThunderDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID, "animations/thunder_dragon.animation.json");
    }
}
