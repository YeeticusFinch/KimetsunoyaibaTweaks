package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidStrengthItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** GeckoLib chestplate renderer for the arachnid strength body and arm model. */
public class ArachnidStrengthRenderer extends GeoArmorRenderer<ArachnidStrengthItem> {
    public ArachnidStrengthRenderer() {
        super(new Model());
    }

    private static final class Model extends GeoModel<ArachnidStrengthItem> {
        @Override
        public ResourceLocation getModelResource(ArachnidStrengthItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/arachnid_strength.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ArachnidStrengthItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/entity/rui_father_molted.png");
        }

        @Override
        public ResourceLocation getAnimationResource(ArachnidStrengthItem item) {
            return null;
        }
    }
}
