package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for Muichiro Tokito entity using GeckoLib
 * Uses biped model and tokito texture from the base mod
 */
public class MuichiroRenderer extends GeoEntityRenderer<MuichiroEntity> {
    public MuichiroRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<MuichiroEntity>() {
            @Override
            public ResourceLocation getModelResource(MuichiroEntity entity) {
                // Use biped model (same as other breathing slayers)
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(MuichiroEntity entity) {
                if (entity != null && entity.isDemonized() && entity.getDemonizedTextureOverride() != null) {
                    return entity.getDemonizedTextureOverride();
                }
                // Use tokito texture from our mod's resources
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/tokito.png");
            }

            @Override
            public ResourceLocation getAnimationResource(MuichiroEntity entity) {
                // Use biped animations
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/biped.animation.json");
            }
        });

        // Add armor rendering layer - renders armor textures on armor bones
        // TODO: Custom helmet offset can be added by overriding actuallyRender() if needed
        this.addRenderLayer(new GeoArmorLayer<>(this));

        // Add equipment rendering layer - renders held items
        this.addRenderLayer(new GeoEquipmentLayer<>(this));

        // Sword display on back/hip (GeoRenderLayer, matches player positioning)
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
        this.addRenderLayer(new DemonSlayerDemonEyesLayer<>(this, "geo/biped.geo.json", "animations/biped.animation.json"));

        // Scale to 80% to match Muichiro's younger appearance
        this.scaleHeight = 0.8F;
        this.scaleWidth = 0.8F;
    }

    @Override
    protected float getDeathMaxRotation(MuichiroEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
