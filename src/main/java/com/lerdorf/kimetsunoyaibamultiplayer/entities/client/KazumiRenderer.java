package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KazumiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KazumiRenderer extends GeoEntityRenderer<KazumiEntity> {
    public KazumiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KazumiEntity>() {
            @Override
            public ResourceLocation getModelResource(KazumiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_civilian.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KazumiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/kazumi.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KazumiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });
    }

    @Override
    protected float getDeathMaxRotation(KazumiEntity entity) {
        return 0.0F;
    }
}
