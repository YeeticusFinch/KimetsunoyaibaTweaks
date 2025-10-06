package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for all breathing slayer entities
 * Uses the biped.geo.json model and biped.animation.json animations
 */
public class SlayerGeoModel extends GeoModel<BreathingSlayerEntity> {

    @Override
    public ResourceLocation getModelResource(BreathingSlayerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BreathingSlayerEntity entity) {
        // Will be overridden in individual renderers
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BreathingSlayerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
    }
}
