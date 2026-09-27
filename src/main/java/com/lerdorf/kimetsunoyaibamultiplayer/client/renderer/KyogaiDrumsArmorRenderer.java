package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.KyogaiDrumsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** GeckoLib renderer for the drums attached to Kyogai's armor bones. */
public class KyogaiDrumsArmorRenderer extends GeoArmorRenderer<KyogaiDrumsItem> {
    public KyogaiDrumsArmorRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<KyogaiDrumsItem> {
        @Override
        public ResourceLocation getModelResource(KyogaiDrumsItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/kyogai_drums.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(KyogaiDrumsItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/armor/kyogai_drums.png");
        }

        @Override
        public ResourceLocation getAnimationResource(KyogaiDrumsItem item) {
            return null;
        }
    }
}
