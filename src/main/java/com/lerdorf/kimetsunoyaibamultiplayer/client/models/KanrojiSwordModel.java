package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Kanroji animated sword.
 */
public class KanrojiSwordModel extends GeoModel<NichirinSwordKanrojiAnimated> {
    private static boolean loggedResources = false;

    @Override
    public ResourceLocation getModelResource(NichirinSwordKanrojiAnimated animatable) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "geo/kanroji_sword.geo.json");
        if (!loggedResources) {
            Log.info("[KanrojiSwordModel] Model resource: {}", loc);
        }
        return loc;
    }

    @Override
    public ResourceLocation getTextureResource(NichirinSwordKanrojiAnimated animatable) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "textures/block/nitirintou_kanroji.png");
        if (!loggedResources) {
            Log.info("[KanrojiSwordModel] Texture resource: {}", loc);
        }
        return loc;
    }

    @Override
    public ResourceLocation getAnimationResource(NichirinSwordKanrojiAnimated animatable) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "animations/kanroji_sword.animation.json");
        if (!loggedResources) {
            Log.info("[KanrojiSwordModel] Animation resource: {}", loc);
            loggedResources = true;
        }
        return loc;
    }
}
