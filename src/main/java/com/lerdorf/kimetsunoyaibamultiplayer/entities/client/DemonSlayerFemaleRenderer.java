package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for female demon slayer entities.
 * Uses biped_female.geo.json with slayer_female textures.
 */
public class DemonSlayerFemaleRenderer extends GeoEntityRenderer<DemonSlayerEntity> {

    private static final ResourceLocation[] TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_1.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_2.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_3.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_4.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_5.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_6.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_7.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_8.png"),
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/slayer_female_9.png"),
    };

    public DemonSlayerFemaleRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<DemonSlayerEntity>() {
            @Override
            public ResourceLocation getModelResource(DemonSlayerEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/biped_female.geo.json");
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
        this.addRenderLayer(new DemonSlayerDemonEyesLayer(this, "geo/biped_female.geo.json", "animations/biped.animation.json"));
    }

    @Override
    protected float getDeathMaxRotation(DemonSlayerEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
