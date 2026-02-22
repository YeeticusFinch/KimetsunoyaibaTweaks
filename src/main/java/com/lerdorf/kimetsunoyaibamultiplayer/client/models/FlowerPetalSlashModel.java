package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlowerPetalSlashEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Flower Petal Slash entity.
 *
 * Model file: flower_petal_slash.geo.json
 * Textures: sword_loop_flower0.png to sword_loop_flower17.png (18 frames)
 *
 * Texture changes each tick based on entity's current frame.
 */
public class FlowerPetalSlashModel extends GeoModel<FlowerPetalSlashEntity> {
    @Override
    public ResourceLocation getModelResource(FlowerPetalSlashEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "geo/flower_petal_slash.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(FlowerPetalSlashEntity animatable) {
        // Get current frame from entity (0-17)
        int frame = animatable.getCurrentFrame();
        // Clamp to valid range
        frame = Math.max(0, Math.min(FlowerPetalSlashEntity.TOTAL_FRAMES - 1, frame));

        // Return texture for current frame: sword_loop_flower0.png to sword_loop_flower17.png
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/sword_loop_flower" + frame + ".png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(FlowerPetalSlashEntity animatable) {
        // No animation file needed - we're doing texture animation instead
        // Return a dummy path that won't be used
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "animations/flower_petal_slash.animation.json"
        );
    }
}
