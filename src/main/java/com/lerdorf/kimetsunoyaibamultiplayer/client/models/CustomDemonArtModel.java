package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CustomDemonArtModel extends GeoModel<CustomDemonArtItem> {
    @Override
    public ResourceLocation getModelResource(CustomDemonArtItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "geo/custom_demon_art.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CustomDemonArtItem animatable) {
        return CustomDemonArtItem.getDefaultTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(CustomDemonArtItem animatable) {
        return null;
    }
}
