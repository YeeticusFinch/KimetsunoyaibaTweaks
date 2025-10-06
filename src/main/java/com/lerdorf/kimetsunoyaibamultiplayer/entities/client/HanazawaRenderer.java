package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.HanazawaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Yukire Hanazawa entity using hanazawa.png texture
 */
public class HanazawaRenderer extends GeoEntityRenderer<HanazawaEntity> {
    public HanazawaRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<HanazawaEntity>() {
            @Override
            public ResourceLocation getModelResource(HanazawaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(HanazawaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/hanazawa.png");
            }

            @Override
            public ResourceLocation getAnimationResource(HanazawaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });
    }
}
