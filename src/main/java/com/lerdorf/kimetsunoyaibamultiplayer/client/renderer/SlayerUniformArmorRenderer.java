package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.SlayerUniformArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for slayer_uniform armor pieces.
 */
public class SlayerUniformArmorRenderer extends GeoArmorRenderer<SlayerUniformArmorItem> {
    public SlayerUniformArmorRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<SlayerUniformArmorItem> {
        @Override
        public ResourceLocation getModelResource(SlayerUniformArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getModelPath());
        }

        @Override
        public ResourceLocation getTextureResource(SlayerUniformArmorItem item) {
            return switch (item.getType()) {
                case CHESTPLATE -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getChestTexturePath());
                case LEGGINGS -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getLeggingsTexturePath());
                case BOOTS -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getBootsTexturePath());
                default -> ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", item.getChestTexturePath());
            };
        }

        @Override
        public ResourceLocation getAnimationResource(SlayerUniformArmorItem item) {
            return null;
        }
    }
}
