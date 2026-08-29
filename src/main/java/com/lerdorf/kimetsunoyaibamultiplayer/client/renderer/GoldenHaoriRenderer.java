package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.GoldenHaoriItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for the golden haori.
 */
public class GoldenHaoriRenderer extends GeoArmorRenderer<GoldenHaoriItem> {
    public GoldenHaoriRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<GoldenHaoriItem> {
        @Override
        public ResourceLocation getModelResource(GoldenHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/haori.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(GoldenHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/haori_gold.png");
        }

        @Override
        public ResourceLocation getAnimationResource(GoldenHaoriItem item) {
            return null;
        }
    }
}
