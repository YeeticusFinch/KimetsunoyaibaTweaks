package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.BlindfoldItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class BlindfoldRenderer extends GeoArmorRenderer<BlindfoldItem> {
    public BlindfoldRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<BlindfoldItem> {
        @Override
        public ResourceLocation getModelResource(BlindfoldItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/blindfold.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(BlindfoldItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/blindfold.png");
        }

        @Override
        public ResourceLocation getAnimationResource(BlindfoldItem item) {
            return null;
        }
    }
}
