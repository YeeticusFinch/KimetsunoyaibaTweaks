package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.TrainWheelBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TrainWheelModel extends GeoModel<TrainWheelBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TrainWheelBlockEntity animatable) {
        Block block = animatable.getBlockState().getBlock();
        String model = block == ModBlocks.LARGE_TRAIN_WHEEL.get()
            ? "large_train_wheel"
            : block == ModBlocks.LARGE_TRAIN_WHEEL_BAR.get()
                ? "large_train_wheel_bar"
            : block == ModBlocks.TRAIN_WHEEL_HOODED_RIGHT.get()
                ? "train_wheel_hooded_right"
                : block == ModBlocks.TRAIN_WHEEL_HOODED_LEFT.get()
                    ? "train_wheel_hooded_left"
                    : "train_wheel";
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "geo/" + model + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TrainWheelBlockEntity animatable) {
        String texture = animatable.getBlockState().is(ModBlocks.LARGE_TRAIN_WHEEL.get())
            || animatable.getBlockState().is(ModBlocks.LARGE_TRAIN_WHEEL_BAR.get())
            ? "large_train_wheel"
            : "train_wheel";
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "textures/block/" + texture + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TrainWheelBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
            "animations/wheel.animation.json");
    }

    @Override
    public void setCustomAnimations(TrainWheelBlockEntity animatable, long instanceId,
                                    AnimationState<TrainWheelBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (animatable.getLevel() == null) {
            return;
        }

        double tick = Math.floorMod(animatable.getLevel().getGameTime(), 20L)
            + animationState.getPartialTick();
        float phase = (float) (tick / 20.0D * Math.PI * 2.0D);
        float rotation = (float) (animatable.isLeftSpinning() ? -phase : phase);

        setBoneRotation("base", rotation);
        setBonePosition("red_bar", (float) (-16.0D + 16.0D * Math.cos(phase)));
    }

    private void setBoneRotation(String name, float rotation) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotZ(rotation);
        }
    }

    private void setBonePosition(String name, float y) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setPosY(y);
        }
    }
}
