package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.MuichiroHaoriItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for clothes_muichiro_fp_chestplate.
 */
public class MuichiroHaoriRenderer extends GeoArmorRenderer<MuichiroHaoriItem> {
    public MuichiroHaoriRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<MuichiroHaoriItem> {
        @Override
        public ResourceLocation getModelResource(MuichiroHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/haori.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MuichiroHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/clothes_muichiro_fp.png");
        }

        @Override
        public ResourceLocation getAnimationResource(MuichiroHaoriItem item) {
            return null;
        }
    }
}
