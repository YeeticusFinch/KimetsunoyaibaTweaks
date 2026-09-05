package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.NamedDemonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for the named demons using the shared biped model and animation set. */
public class NamedDemonRenderer extends GeoEntityRenderer<NamedDemonEntity> {
    public NamedDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<NamedDemonEntity>() {
            @Override
            public ResourceLocation getModelResource(NamedDemonEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(NamedDemonEntity entity) {
                return entity.getSkinTexture();
            }

            @Override
            public ResourceLocation getAnimationResource(NamedDemonEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "animations/biped.animation.json");
            }
        });
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped.geo.json",
            "animations/biped.animation.json", NamedDemonEntity::getEyesTexture));
    }
}
