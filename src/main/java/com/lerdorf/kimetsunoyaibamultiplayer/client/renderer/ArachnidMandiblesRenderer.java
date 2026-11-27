package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidMandiblesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** GeckoLib renderer for the animated arachnid mandibles helmet. */
public class ArachnidMandiblesRenderer extends GeoArmorRenderer<ArachnidMandiblesItem> {
    public ArachnidMandiblesRenderer() {
        super(new Model());
    }

    private static final class Model extends GeoModel<ArachnidMandiblesItem> {
        @Override
        public ResourceLocation getModelResource(ArachnidMandiblesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/arachnid_mandibles.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ArachnidMandiblesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/armor/arachnid_mandibles.png");
        }

        @Override
        public ResourceLocation getAnimationResource(ArachnidMandiblesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/arachnid_mandibles.animation.json");
        }
    }
}
