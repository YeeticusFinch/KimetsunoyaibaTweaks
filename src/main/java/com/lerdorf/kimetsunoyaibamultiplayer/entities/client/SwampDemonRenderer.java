package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SwampDemonRenderer extends GeoEntityRenderer<SwampDemonEntity> {
    private static final GeoModel<SwampDemonEntity> BASE_MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(SwampDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_swamp_demon.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SwampDemonEntity entity) {
            return switch (entity.getTextureVariant()) {
                case 1 -> ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/swamp_demon1.png");
                case 2 -> ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/swamp_demon2.png");
                case 3 -> ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/swamp_demon3.png");
                default -> ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/swamp_demon.png");
            };
        }

        @Override
        public ResourceLocation getAnimationResource(SwampDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
        }
    };

    public SwampDemonRenderer(EntityRendererProvider.Context context) {
        super(context, BASE_MODEL);
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
    }
}
