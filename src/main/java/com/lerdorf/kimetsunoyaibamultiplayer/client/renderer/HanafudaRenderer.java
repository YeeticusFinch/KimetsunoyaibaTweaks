package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.HanafudaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for hanafuda helmet.
 */
public class HanafudaRenderer extends GeoArmorRenderer<HanafudaItem> {
    public HanafudaRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<HanafudaItem> {
        @Override
        public ResourceLocation getModelResource(HanafudaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/hanafuda.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(HanafudaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/hanafuda.png");
        }

        @Override
        public ResourceLocation getAnimationResource(HanafudaItem item) {
            return null;
        }
    }
}
