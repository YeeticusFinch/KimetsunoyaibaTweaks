package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SixEyeDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the six-eye demon's oversized biped model. */
public class SixEyeDemonRenderer extends GeoEntityRenderer<SixEyeDemonEntity> {
    private static final GeoModel<SixEyeDemonEntity> MODEL = new GeoModel<>() {
        @Override
        public ResourceLocation getModelResource(SixEyeDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biped_six_eye_demon.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(SixEyeDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/entity/six_eye_demon.png");
        }

        @Override
        public ResourceLocation getAnimationResource(SixEyeDemonEntity entity) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "animations/biped.animation.json");
        }
    };

    public SixEyeDemonRenderer(EntityRendererProvider.Context context) {
        super(context, MODEL);
        this.withScale(1.4F);
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_six_eye_demon.geo.json",
            "textures/entity/six_eye_demon_eyes.png", "animations/biped.animation.json"));
    }
}
