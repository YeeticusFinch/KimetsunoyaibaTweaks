package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.CustomRenderTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidEyesItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** GeckoLib armor renderer with fullbright emissive eye rendering. */
public class ArachnidEyesRenderer extends GeoArmorRenderer<ArachnidEyesItem> {
    public ArachnidEyesRenderer() {
        super(new Model());
    }

    @Override
    public RenderType getRenderType(ArachnidEyesItem item, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return CustomRenderTypes.geoEntityTranslucentEmissive(texture);
    }

    private static final class Model extends GeoModel<ArachnidEyesItem> {
        @Override
        public ResourceLocation getModelResource(ArachnidEyesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/arachnid_eyes.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ArachnidEyesItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/armor/arachnid_eyes.png");
        }

        @Override
        public ResourceLocation getAnimationResource(ArachnidEyesItem item) {
            return null;
        }
    }
}
