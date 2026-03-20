package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.PrincessModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.PrincessEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PrincessRenderer extends GeoEntityRenderer<PrincessEntity> {
    public PrincessRenderer(EntityRendererProvider.Context context) {
        super(context, new PrincessModel());
        this.shadowRadius = 0.45F;
    }

    @Override
    protected float getDeathMaxRotation(PrincessEntity entityLivingBaseIn) {
        return 90.0F;
    }
}
