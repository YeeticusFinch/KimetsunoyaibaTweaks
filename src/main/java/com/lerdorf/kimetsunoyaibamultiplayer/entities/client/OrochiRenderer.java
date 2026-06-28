package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.OrochiModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OrochiRenderer extends GeoEntityRenderer<OrochiEntity> {
    public OrochiRenderer(EntityRendererProvider.Context context) {
        super(context, new OrochiModel());
        this.shadowRadius = 0.25F;
        this.addRenderLayer(new EyesGlowLayer<>(this, "geo/orochi.geo.json",
            "textures/entity/orochi_eyes.png", "animations/orochi.animation.json"));
    }

    @Override
    protected float getDeathMaxRotation(OrochiEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
