package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KomorebiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Setsu Komorebi entity using GeckoLib
 */
public class KomorebiRenderer extends GeoEntityRenderer<KomorebiEntity> {
    public KomorebiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KomorebiEntity>() {
            @Override
            public ResourceLocation getModelResource(KomorebiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KomorebiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/komorebi.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KomorebiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        // Add equipment rendering layer
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Add armor rendering layer - renders armor textures on armor bones
        this.addRenderLayer(new GeoArmorLayer<>(this));
    }
}
