package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.AndonBakamaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for the Andon Bakama leggings.
 * Works on both players and GeckoLib entities via the standard armor rendering pipeline.
 * Bone names in the geo model (armorHead, armorBody, armorRightLeg, etc.) match
 * GeoArmorRenderer defaults, so no overrides are needed.
 */
public class AndonBakamaRenderer extends GeoArmorRenderer<AndonBakamaItem> {
    public AndonBakamaRenderer() {
        super(new AndonBakamaModel());
    }

    private static class AndonBakamaModel extends GeoModel<AndonBakamaItem> {
        @Override
        public ResourceLocation getModelResource(AndonBakamaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/andon_bakama.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(AndonBakamaItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/andon_bakama.png");
        }

        @Override
        public ResourceLocation getAnimationResource(AndonBakamaItem item) {
            return null;
        }
    }
}
