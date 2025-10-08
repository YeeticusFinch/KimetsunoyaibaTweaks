package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ShimizuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Akira Shimizu entity using GeckoLib
 */
public class ShimizuRenderer extends GeoEntityRenderer<ShimizuEntity> {
    public ShimizuRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<ShimizuEntity>() {
            @Override
            public ResourceLocation getModelResource(ShimizuEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(ShimizuEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/shimizu.png");
            }

            @Override
            public ResourceLocation getAnimationResource(ShimizuEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });
        

        // Add armor rendering layer - renders armor textures on armor bones
        this.addRenderLayer(new GeoArmorLayer<>(this));

        // Add equipment rendering layer
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

    }
}
