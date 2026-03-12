package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for male demon slayer entities.
 * Uses biped.geo.json with mob_slayer textures.
 */
public class DemonSlayerRenderer extends GeoEntityRenderer<DemonSlayerEntity> {

    private static final ResourceLocation[] TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_1.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_2.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_3.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_4.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_5.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/mob_slayer_6.png"),
    };

    public DemonSlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<DemonSlayerEntity>() {
            @Override
            public ResourceLocation getModelResource(DemonSlayerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(DemonSlayerEntity entity) {
                int index = entity.getTextureIndex();
                if (index < 0 || index >= TEXTURES.length) index = 0;
                return TEXTURES[index];
            }

            @Override
            public ResourceLocation getAnimationResource(DemonSlayerEntity entity) {
                String namespace = entity.getCurrentAnimationNamespace();
                if (namespace == null || namespace.isEmpty()) {
                    namespace = KimetsunoyaibaMultiplayer.MODID;
                }
                return ResourceLocation.fromNamespaceAndPath(namespace, "animations/biped.animation.json");
            }
        });

        this.addRenderLayer(new GeoArmorLayer<>(this));
        this.addRenderLayer(new GeoEquipmentLayer<>(this));
        this.addRenderLayer(new GeoSwordDisplayLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(DemonSlayerEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
