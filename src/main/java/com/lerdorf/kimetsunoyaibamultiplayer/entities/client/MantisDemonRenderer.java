package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MantisDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the Mantis Demon and its emissive eyes. */
public class MantisDemonRenderer extends GeoEntityRenderer<MantisDemonEntity> {
    private static final GeoModel<MantisDemonEntity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(MantisDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_mantis_demon.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MantisDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/entity/mantis_demon.png");
        }

        @Override
        public ResourceLocation getAnimationResource(MantisDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public MantisDemonRenderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_mantis_demon.geo.json",
            "textures/entity/mantis_demon_eyes.png", "animations/biped.animation.json"));
    }
}
