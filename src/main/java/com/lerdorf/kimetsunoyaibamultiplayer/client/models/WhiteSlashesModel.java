package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.WhiteSlashesEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WhiteSlashesModel extends GeoModel<WhiteSlashesEntity> {
    @Override
    public ResourceLocation getModelResource(WhiteSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/white_slashes.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WhiteSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/love_tornado.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WhiteSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/white_slashes.animation.json");
    }
}
