package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BeastSlashesEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BeastSlashesModel extends GeoModel<BeastSlashesEntity> {
    @Override
    public ResourceLocation getModelResource(BeastSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/beast_slashes.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BeastSlashesEntity animatable) {
        int variant = Math.max(0, Math.min(2, animatable.getTextureVariant()));
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/sword_slash_beast" + variant + ".png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(BeastSlashesEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/beast_slashes.animation.json");
    }
}
