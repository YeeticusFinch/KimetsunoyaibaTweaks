package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.PurpleDemon1Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the purple demon variant using the demon 1 biped model. */
public class PurpleDemon1Renderer extends GeoEntityRenderer<PurpleDemon1Entity> {
    private static final GeoModel<PurpleDemon1Entity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(PurpleDemon1Entity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_demon_1.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(PurpleDemon1Entity entity) {
            return entity.getSkinTexture();
        }

        @Override
        public ResourceLocation getAnimationResource(PurpleDemon1Entity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public PurpleDemon1Renderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_demon_1.geo.json",
            "animations/biped.animation.json", PurpleDemon1Entity::getEyesTexture));
    }
}
