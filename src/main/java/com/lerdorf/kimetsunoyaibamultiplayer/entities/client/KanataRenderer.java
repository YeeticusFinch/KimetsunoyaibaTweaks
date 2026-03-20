package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanataEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KanataRenderer extends GeoEntityRenderer<KanataEntity> {
    public KanataRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KanataEntity>() {
            @Override
            public ResourceLocation getModelResource(KanataEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_ubuyashiki_kid.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KanataEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/kanata.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KanataEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        this.scaleHeight = 0.8F;
        this.scaleWidth = 0.8F;
    }

    @Override
    protected float getDeathMaxRotation(KanataEntity entityLivingBaseIn) {
        return 0.0F;
    }
}
