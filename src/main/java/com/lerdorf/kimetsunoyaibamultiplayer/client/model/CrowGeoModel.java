package com.lerdorf.kimetsunoyaibamultiplayer.client.model;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
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
    public ResourceLocation getModelResource(CrowAnimatableWrapper animatable) {
        if (!hasLoggedResources) {
        	if (Config.logDebug) {
	            LOGGER.info("=== CROW MODEL RESOURCES ===");
	            LOGGER.info("Model: {}", MODEL);
	            LOGGER.info("Texture: {}", TEXTURE);
	            LOGGER.info("Animation: {}", ANIMATION);
	            LOGGER.info("Expected file locations:");
	            LOGGER.info("  src/main/resources/assets/{}/geo/crow.geo.json", KimetsunoyaibaMultiplayer.MODID);
	            LOGGER.info("  src/main/resources/assets/{}/textures/entity/crow.png", KimetsunoyaibaMultiplayer.MODID);
	            LOGGER.info("  src/main/resources/assets/{}/animations/crow.animation.json", KimetsunoyaibaMultiplayer.MODID);
        	}
            hasLoggedResources = true;
        }
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrowAnimatableWrapper animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CrowAnimatableWrapper animatable) {
        return ANIMATION;
    }
}