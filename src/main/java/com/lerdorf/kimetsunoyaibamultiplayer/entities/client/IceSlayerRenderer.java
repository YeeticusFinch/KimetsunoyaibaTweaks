package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.IceSlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Ice Slayer entity using GeckoLib
 */
public class IceSlayerRenderer extends GeoEntityRenderer<IceSlayerEntity> {
    public IceSlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<IceSlayerEntity>() {
            @Override
            public ResourceLocation getModelResource(IceSlayerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(IceSlayerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer.png");
            }

            @Override
            public ResourceLocation getAnimationResource(IceSlayerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        // Add equipment rendering layer
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Add armor rendering layer - renders armor textures on armor bones
        this.addRenderLayer(new GeoArmorLayer<>(this));
    }
}
