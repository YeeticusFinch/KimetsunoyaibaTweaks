package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MugenDemon1Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the Mugen demon using the demon 1 biped model. */
public class MugenDemon1Renderer extends GeoEntityRenderer<MugenDemon1Entity> {
    private static final GeoModel<MugenDemon1Entity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(MugenDemon1Entity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_demon_1.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(MugenDemon1Entity entity) {
            return entity.getSkinTexture();
        }

        @Override
        public ResourceLocation getAnimationResource(MugenDemon1Entity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public MugenDemon1Renderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_demon_1.geo.json",
            "animations/biped.animation.json", MugenDemon1Entity::getEyesTexture));
    }
}
