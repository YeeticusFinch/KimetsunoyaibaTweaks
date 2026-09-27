package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KyogaiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** GeckoLib renderer for the custom Kyogai entity. */
public class KyogaiRenderer extends GeoEntityRenderer<KyogaiEntity> {
    public KyogaiRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<KyogaiEntity>() {
            @Override
            public ResourceLocation getModelResource(KyogaiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "geo/biped_kyogai.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(KyogaiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "textures/entity/kyogai.png");
            }

            @Override
            public ResourceLocation getAnimationResource(KyogaiEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                    "animations/biped.animation.json");
            }
        });

        this.addRenderLayer(new GeoArmorLayer<>(this));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/biped_kyogai.geo.json",
            "textures/entity/kyogai_eyes.png", "animations/biped.animation.json"));
        this.scaleHeight = 1.4F;
        this.scaleWidth = 1.4F;
    }

    @Override
    protected float getDeathMaxRotation(KyogaiEntity entity) {
        return 90.0F;
    }
}
