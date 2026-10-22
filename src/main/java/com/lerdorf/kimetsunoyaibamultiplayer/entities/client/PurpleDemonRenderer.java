package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.PurpleDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the purple demon using the demon 0 biped model. */
public class PurpleDemonRenderer extends GeoEntityRenderer<PurpleDemonEntity> {
    private static final GeoModel<PurpleDemonEntity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(PurpleDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_demon_0.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(PurpleDemonEntity entity) {
            return entity.getSkinTexture();
        }

        @Override
        public ResourceLocation getAnimationResource(PurpleDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public PurpleDemonRenderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_demon_0.geo.json",
            "animations/biped.animation.json", PurpleDemonEntity::getEyesTexture));
    }
}
