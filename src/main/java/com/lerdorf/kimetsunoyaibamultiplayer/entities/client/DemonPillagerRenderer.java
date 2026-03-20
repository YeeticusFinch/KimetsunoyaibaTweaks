package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonPillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DemonPillagerRenderer extends GeoEntityRenderer<DemonPillagerEntity> {
    private static final GeoModel<DemonPillagerEntity> BASE_MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(DemonPillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_pillager.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(DemonPillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/demon_pillager.png");
        }

        @Override
        public ResourceLocation getAnimationResource(DemonPillagerEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
        }
    };

    public DemonPillagerRenderer(EntityRendererProvider.Context context) {
        super(context, BASE_MODEL);
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_pillager.geo.json",
            "textures/entity/demon_pillager_eyes.png", "animations/biped.animation.json"));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
    }
}
