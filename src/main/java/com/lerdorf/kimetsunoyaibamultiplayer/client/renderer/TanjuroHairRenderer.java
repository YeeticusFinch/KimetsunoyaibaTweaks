package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.TanjuroHairItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for hair_tanjuro helmet.
 * Uses the same model as the Muichiro FP hair with Tanjuro's texture.
 */
public class TanjuroHairRenderer extends GeoArmorRenderer<TanjuroHairItem> {
    public TanjuroHairRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<TanjuroHairItem> {
        @Override
        public ResourceLocation getModelResource(TanjuroHairItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/hair_muichiro_fp.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(TanjuroHairItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/hair_tanjuro.png");
        }

        @Override
        public ResourceLocation getAnimationResource(TanjuroHairItem item) {
            return null;
        }
    }
}
