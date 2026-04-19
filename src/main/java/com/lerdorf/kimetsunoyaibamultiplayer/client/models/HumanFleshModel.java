package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.items.HumanFleshItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HumanFleshModel extends GeoModel<HumanFleshItem> {
    @Override
    public ResourceLocation getModelResource(HumanFleshItem animatable) {
        return ResourceLocation.parse(animatable.getModelPath());
    }

    @Override
    public ResourceLocation getTextureResource(HumanFleshItem animatable) {
        return animatable.getDefaultTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(HumanFleshItem animatable) {
        return null;
    }
}
