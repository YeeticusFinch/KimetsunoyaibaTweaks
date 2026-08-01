package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import net.minecraft.nbt.CompoundTag;

public record RotationParameters(boolean rotateVelocity, boolean rotateView, int rotationTimeMS) {
    private static RotationParameters defaultParam = new RotationParameters(true, true, 500);

    public static void updateDefault(int rotationTimeMs) {
        defaultParam = new RotationParameters(true, true, rotationTimeMs);
    }

    public static RotationParameters getDefault() {
        return defaultParam;
    }

    public RotationParameters withRotationTimeMs(int rotationTimeMS) {
        return new RotationParameters(this.rotateVelocity, this.rotateView, rotationTimeMS);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("RotateVelocity", rotateVelocity);
        tag.putBoolean("RotateView", rotateView);
        tag.putInt("RotationTimeMS", rotationTimeMS);
        return tag;
    }

    public static RotationParameters fromTag(CompoundTag tag) {
        return new RotationParameters(tag.getBoolean("RotateVelocity"), tag.getBoolean("RotateView"), tag.getInt("RotationTimeMS"));
    }
}
