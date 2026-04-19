package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.SatokosBowItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SatokosBowRenderer extends GeoArmorRenderer<SatokosBowItem> {
    public SatokosBowRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<SatokosBowItem> {
        @Override
        public ResourceLocation getModelResource(SatokosBowItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/satokos_bow.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SatokosBowItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/item/satokos_bow.png");
        }

        @Override
        public ResourceLocation getAnimationResource(SatokosBowItem item) {
            return null;
        }
    }
}
