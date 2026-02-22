package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.MuichiroUniformHaoriItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for uniform_muichiro_fp_chestplate.
 */
public class MuichiroUniformHaoriRenderer extends GeoArmorRenderer<MuichiroUniformHaoriItem> {
    public MuichiroUniformHaoriRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<MuichiroUniformHaoriItem> {
        @Override
        public ResourceLocation getModelResource(MuichiroUniformHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/haori.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MuichiroUniformHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/uniform_muichiro_fp.png");
        }

        @Override
        public ResourceLocation getAnimationResource(MuichiroUniformHaoriItem item) {
            return null;
        }
    }
}
