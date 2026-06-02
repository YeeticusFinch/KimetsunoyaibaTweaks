package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CustomDemonArtModel extends GeoModel<CustomDemonArtItem> {
    private int modelVariant = CustomDemonArtItem.minModelVariant();

    public void setModelVariant(int modelVariant) {
        this.modelVariant = Math.max(CustomDemonArtItem.minModelVariant(),
            Math.min(CustomDemonArtItem.maxModelVariant(), modelVariant));
    }

    @Override
    public ResourceLocation getModelResource(CustomDemonArtItem animatable) {
        return CustomDemonArtItem.getGeoModelForVariant(modelVariant);
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
