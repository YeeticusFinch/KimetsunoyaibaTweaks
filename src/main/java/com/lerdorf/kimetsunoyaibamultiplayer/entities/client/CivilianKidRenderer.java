package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CivilianKidEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer for civilian children using the shared civilian biped model. */
public class CivilianKidRenderer<T extends CivilianKidEntity> extends GeoEntityRenderer<T> {
    public CivilianKidRenderer(EntityRendererProvider.Context context, String textureName, float scale) {
        super(context, new GeoModel<T>() {
            @Override
            public ResourceLocation getModelResource(T entity) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "geo/biped_civilian.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(T entity) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "textures/entity/" + textureName + ".png");
            }

            @Override
            public ResourceLocation getAnimationResource(T entity) {
                return ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });
        this.addRenderLayer(new SkinLayersGeoLayer<>(this));
        this.scaleHeight = scale;
        this.scaleWidth = scale;
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0.0F;
    }
}
