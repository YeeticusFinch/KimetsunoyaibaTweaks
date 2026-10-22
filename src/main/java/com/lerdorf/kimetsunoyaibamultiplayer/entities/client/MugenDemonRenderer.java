package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the Mugen demon using the demon 0 biped model. */
public class MugenDemonRenderer extends GeoEntityRenderer<MugenDemonEntity> {
    private static final GeoModel<MugenDemonEntity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(MugenDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_demon_0.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MugenDemonEntity entity) {
            return entity.getSkinTexture();
        }

        @Override
        public ResourceLocation getAnimationResource(MugenDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public MugenDemonRenderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_demon_0.geo.json",
            "animations/biped.animation.json", MugenDemonEntity::getEyesTexture));
    }
}
