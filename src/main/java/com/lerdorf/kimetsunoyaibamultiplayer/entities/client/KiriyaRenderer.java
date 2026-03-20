package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KiriyaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KiriyaRenderer extends GeoEntityRenderer<KiriyaEntity> {
    public KiriyaRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KiriyaEntity>() {
            @Override
            public ResourceLocation getModelResource(KiriyaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_ubuyashiki_kid.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KiriyaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/kiriya.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KiriyaEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        this.scaleHeight = 0.8F;
        this.scaleWidth = 0.8F;
    }

    @Override
    protected float getDeathMaxRotation(KiriyaEntity entityLivingBaseIn) {
        return 0.0F;
    }
}
