package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.HairMuichiroFpItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for hair_muichiro_fp helmet.
 */
public class HairMuichiroFpRenderer extends GeoArmorRenderer<HairMuichiroFpItem> {
    public HairMuichiroFpRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<HairMuichiroFpItem> {
        @Override
        public ResourceLocation getModelResource(HairMuichiroFpItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/hair_muichiro_fp.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(HairMuichiroFpItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/hair_muichiro_fp.png");
        }

        @Override
        public ResourceLocation getAnimationResource(HairMuichiroFpItem item) {
            return null;
        }
    }
}
