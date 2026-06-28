package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.EyeFamiliarModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.EyeFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EyeFamiliarRenderer extends GeoEntityRenderer<EyeFamiliarEntity> {
    public EyeFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new EyeFamiliarModel());
        this.shadowRadius = 0.25F;
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/eye_familiar.geo.json",
            "textures/entity/eye_familiar_eye.png", "animations/eye_familiar.animation.json", 2.5F));
    }

    @Override
    protected float getDeathMaxRotation(EyeFamiliarEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
