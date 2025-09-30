package com.lerdorf.kimetsunoyaibamultiplayer.client.model;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CrowAnimatableWrapper;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for kasugai_crow entities
 * Provides the model, texture, and animation files for rendering
 */
public class CrowGeoModel extends GeoModel<CrowAnimatableWrapper> {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public ResourceLocation getModelResource(CrowAnimatableWrapper wrapper) {
        ResourceLocation loc = ResourceLocation.tryBuild(KimetsunoyaibaMultiplayer.MODID, "geo/crow.geo.json");
        LOGGER.info("getModelResource() returning: {}", loc);
        return loc;
    }

    @Override
    public ResourceLocation getTextureResource(CrowAnimatableWrapper wrapper) {
        // Use our custom crow texture
        ResourceLocation loc = ResourceLocation.tryBuild(KimetsunoyaibaMultiplayer.MODID, "textures/entity/crow.png");
        LOGGER.info("getTextureResource() returning: {}", loc);
        return loc;
    }

    @Override
    public ResourceLocation getAnimationResource(CrowAnimatableWrapper wrapper) {
        ResourceLocation loc = ResourceLocation.tryBuild(KimetsunoyaibaMultiplayer.MODID, "animations/crow.animation.json");
        LOGGER.info("getAnimationResource() returning: {}", loc);
        return loc;
    }
}