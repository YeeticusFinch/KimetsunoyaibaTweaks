package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the generic Love Breathing animated sword.
 */
public class LoveSwordModel extends GeoModel<NichirinSwordLoveAnimated> {

    @Override
    public ResourceLocation getModelResource(NichirinSwordLoveAnimated animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "geo/nichirinsword_love.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NichirinSwordLoveAnimated animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "textures/block/nichirinsword_love_base.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NichirinSwordLoveAnimated animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "animations/kanroji_sword.animation.json");
    }
}
