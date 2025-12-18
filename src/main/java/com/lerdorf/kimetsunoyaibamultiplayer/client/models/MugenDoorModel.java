package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDoorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Mugen Door entity.
 */
public class MugenDoorModel extends GeoModel<MugenDoorEntity> {
    @Override
    public ResourceLocation getModelResource(MugenDoorEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "geo/mugen_door.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(MugenDoorEntity animatable) {
        // Texture is typically defined in the geo.json file
        // If a separate texture file is needed, it would be:
        // return ResourceLocation.fromNamespaceAndPath(
        //     KimetsunoyaibaMultiplayer.MODID,
        //     "textures/entity/mugen_door.png"
        // );
        // For now, return a placeholder since the geo.json likely has the texture
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/mugen_door.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(MugenDoorEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "animations/mugen_door.animation.json"
        );
    }
}
