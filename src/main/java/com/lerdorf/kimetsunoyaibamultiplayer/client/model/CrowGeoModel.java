package com.lerdorf.kimetsunoyaibamultiplayer.client.model;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for GeckolibCrowEntity
 * Provides the model, texture, and animation files for rendering
 */
public class CrowGeoModel extends GeoModel<GeckolibCrowEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean hasLoggedResources = false;

    private static final ResourceLocation MODEL = ResourceLocation.tryBuild(
        KimetsunoyaibaMultiplayer.MODID,
        "geo/crow.geo.json"
    );

    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(
        KimetsunoyaibaMultiplayer.MODID,
        "textures/entity/crow.png"
    );

    private static final ResourceLocation ANIMATION = ResourceLocation.tryBuild(
        KimetsunoyaibaMultiplayer.MODID,
        "animations/crow.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(GeckolibCrowEntity animatable) {
        if (!hasLoggedResources) {
        	if (Config.logDebug) {
	            LOGGER.info("=== CROW MODEL RESOURCES ===");
	            LOGGER.info("Model: {}", MODEL);
	            LOGGER.info("Texture: {}", TEXTURE);
	            LOGGER.info("Animation: {}", ANIMATION);
        	}
            hasLoggedResources = true;
        }
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GeckolibCrowEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GeckolibCrowEntity animatable) {
        return ANIMATION;
    }
}