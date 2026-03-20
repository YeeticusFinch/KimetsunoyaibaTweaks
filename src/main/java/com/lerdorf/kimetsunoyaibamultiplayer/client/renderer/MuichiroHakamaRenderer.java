package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.MuichiroHakamaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for Muichiro hakama (leggings).
 */
public class MuichiroHakamaRenderer extends GeoArmorRenderer<MuichiroHakamaItem> {
    public MuichiroHakamaRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<MuichiroHakamaItem> {
        @Override
        public ResourceLocation getModelResource(MuichiroHakamaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/hakama.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MuichiroHakamaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/muichiro_hakama.png");
        }

        @Override
        public ResourceLocation getAnimationResource(MuichiroHakamaItem item) {
            return null;
        }
    }
}
