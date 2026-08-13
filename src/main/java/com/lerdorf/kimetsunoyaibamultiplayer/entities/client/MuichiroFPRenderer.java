package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroFullPotentialEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Muichiro Tokito (Full Potential).
 */
public class MuichiroFPRenderer extends GeoEntityRenderer<MuichiroFullPotentialEntity> {
    public MuichiroFPRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<MuichiroFullPotentialEntity>() {
            @Override
            public ResourceLocation getModelResource(MuichiroFullPotentialEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(MuichiroFullPotentialEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/tokito_fp.png");
            }

            @Override
            public ResourceLocation getAnimationResource(MuichiroFullPotentialEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        this.addRenderLayer(new GeoArmorLayer<>(this));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
        this.addRenderLayer(new DemonSlayerDemonEyesLayer<>(this, "geo/biped.geo.json", "animations/biped.animation.json"));

        // Slightly larger than base Muichiro (0.8)
        this.scaleHeight = 0.9F;
        this.scaleWidth = 0.9F;
    }

    @Override
    protected float getDeathMaxRotation(MuichiroFullPotentialEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
