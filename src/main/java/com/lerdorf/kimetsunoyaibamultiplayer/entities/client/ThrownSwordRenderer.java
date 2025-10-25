package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ThrownSwordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer for thrown sword projectiles (Frost Breathing Sixth Form)
 * Uses GeckoLib to render a spinning sword model
 */
@OnlyIn(Dist.CLIENT)
public class ThrownSwordRenderer extends GeoEntityRenderer<ThrownSwordEntity> {

    public ThrownSwordRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<ThrownSwordEntity>() {
            @Override
            public ResourceLocation getModelResource(ThrownSwordEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/thrown_sword.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(ThrownSwordEntity entity) {
                // Use golden texture if golden flag is set, otherwise frost texture
                String textureName = entity.isGolden() ? "thrown_sword_golden.png" : "thrown_sword_frost.png";
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/" + textureName);
            }

            @Override
            public ResourceLocation getAnimationResource(ThrownSwordEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/thrown_sword.animation.json");
            }
        });
    }
}
