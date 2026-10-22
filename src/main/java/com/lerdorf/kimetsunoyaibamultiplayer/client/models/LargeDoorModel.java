package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LargeDoorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LargeDoorModel extends GeoModel<LargeDoorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(LargeDoorBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/large_door.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LargeDoorBlockEntity animatable) {
        String texture = animatable.getBlockState().is(ModBlocks.LARGE_DOOR_DARKER.get())
            ? "textures/block/large_door_darker.png"
            : "textures/block/large_door.png";
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, texture);
    }

    @Override
    public ResourceLocation getAnimationResource(LargeDoorBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "animations/large_door.animation.json");
    }
}
