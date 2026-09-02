package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MotherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for Mother using the shared female biped model and animation set. */
public class MotherRenderer extends GeoEntityRenderer<MotherEntity> {
    public MotherRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<MotherEntity>() {
            @Override
            public ResourceLocation getModelResource(MotherEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "geo/biped_female_2.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(MotherEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "textures/entity/mother.png");
            }

            @Override
            public ResourceLocation getAnimationResource(MotherEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "animations/biped.animation.json");
            }
        });
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_female_2.geo.json",
            "textures/entity/demon_eyes_mother.png", "animations/biped.animation.json"));
    }
}
