package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.SlayerUniform2ArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for slayer_uniform_2 armor pieces.
 */
public class SlayerUniform2ArmorRenderer extends GeoArmorRenderer<SlayerUniform2ArmorItem> {
    public SlayerUniform2ArmorRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<SlayerUniform2ArmorItem> {
        @Override
        public ResourceLocation getModelResource(SlayerUniform2ArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "geo/slayer_uniform_2.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SlayerUniform2ArmorItem item) {
            return switch (item.getType()) {
                case CHESTPLATE -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getChestTexturePath());
                case LEGGINGS -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getLeggingsTexturePath());
                case BOOTS -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getBootsTexturePath());
                default -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getChestTexturePath());
            };
        }

        @Override
        public ResourceLocation getAnimationResource(SlayerUniform2ArmorItem item) {
            return null;
        }
    }
}
