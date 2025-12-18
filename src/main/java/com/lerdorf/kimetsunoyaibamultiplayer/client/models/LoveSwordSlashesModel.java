package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Love Sword Slashes entity.
 *
 * Model file: love_sword_slashes.geo.json
 * Animation file: love_sword_slashes.animation.json
 * Texture: sword_slash_love.png
 */
public class LoveSwordSlashesModel extends GeoModel<LoveSwordSlashesEntity> {
    @Override
    public ResourceLocation getModelResource(LoveSwordSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "geo/love_sword_slashes.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(LoveSwordSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/sword_slash_love.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(LoveSwordSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "animations/love_sword_slashes.animation.json"
        );
    }
}
