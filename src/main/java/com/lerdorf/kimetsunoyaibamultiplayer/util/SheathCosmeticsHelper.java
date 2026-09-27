package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Persistent, server-authoritative cosmetic values for a player's sheath display. */
public final class SheathCosmeticsHelper {
    public static final String POSITION_TAG = "KnYSheathPosition";
    public static final String TEXTURE_INDEX_TAG = "KnYSheathTextureIndex";
    public static final String TRANSLATE_X_TAG = "KnYSheathTranslateX";
    public static final String TRANSLATE_Y_TAG = "KnYSheathTranslateY";
    public static final String TRANSLATE_Z_TAG = "KnYSheathTranslateZ";
    public static final String ROTATE_X_TAG = "KnYSheathRotateX";
    public static final String ROTATE_Y_TAG = "KnYSheathRotateY";
    public static final String ROTATE_Z_TAG = "KnYSheathRotateZ";

    private SheathCosmeticsHelper() {
    }

    public static SwordDisplayConfig.SwordDisplayPosition getPosition(Player player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(POSITION_TAG)) {
            tag.putString(POSITION_TAG, SwordDisplayConfig.position.name());
        }
        try {
            return SwordDisplayConfig.SwordDisplayPosition.valueOf(tag.getString(POSITION_TAG));
        } catch (IllegalArgumentException exception) {
            tag.putString(POSITION_TAG, SwordDisplayConfig.position.name());
            return SwordDisplayConfig.position;
        }
    }

    public static int getTextureIndex(Player player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(TEXTURE_INDEX_TAG)) {
            tag.putInt(TEXTURE_INDEX_TAG, 0);
        }
        return Math.max(0, tag.getInt(TEXTURE_INDEX_TAG));
    }

    public static double getTranslateX(Player player) {
        return getDouble(player, TRANSLATE_X_TAG);
    }

    public static double getTranslateY(Player player) {
        return getDouble(player, TRANSLATE_Y_TAG);
    }

    public static double getTranslateZ(Player player) {
        return getDouble(player, TRANSLATE_Z_TAG);
    }

    public static double getRotateX(Player player) {
        return getDouble(player, ROTATE_X_TAG, -360.0, 360.0);
    }

    public static double getRotateY(Player player) {
        return getDouble(player, ROTATE_Y_TAG, -360.0, 360.0);
    }

    public static double getRotateZ(Player player) {
        return getDouble(player, ROTATE_Z_TAG, -360.0, 360.0);
    }

    public static void setPosition(Player player, SwordDisplayConfig.SwordDisplayPosition position) {
        player.getPersistentData().putString(POSITION_TAG,
            (position == null ? SwordDisplayConfig.position : position).name());
    }

    public static void setTextureIndex(Player player, int index) {
        player.getPersistentData().putInt(TEXTURE_INDEX_TAG, Math.max(0, index));
    }

    public static void setOffsets(Player player, double translateX, double translateY, double translateZ,
                                  double rotateX, double rotateY, double rotateZ) {
        CompoundTag tag = player.getPersistentData();
        tag.putDouble(TRANSLATE_X_TAG, clamp(translateX, -5.0, 5.0));
        tag.putDouble(TRANSLATE_Y_TAG, clamp(translateY, -5.0, 5.0));
        tag.putDouble(TRANSLATE_Z_TAG, clamp(translateZ, -5.0, 5.0));
        tag.putDouble(ROTATE_X_TAG, clamp(rotateX, -360.0, 360.0));
        tag.putDouble(ROTATE_Y_TAG, clamp(rotateY, -360.0, 360.0));
        tag.putDouble(ROTATE_Z_TAG, clamp(rotateZ, -360.0, 360.0));
    }

    public static void setAll(Player player, SwordDisplayConfig.SwordDisplayPosition position, int textureIndex,
                               double translateX, double translateY, double translateZ,
                               double rotateX, double rotateY, double rotateZ) {
        setPosition(player, position);
        setTextureIndex(player, textureIndex);
        setOffsets(player, translateX, translateY, translateZ, rotateX, rotateY, rotateZ);
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag source = original.getPersistentData();
        CompoundTag target = replacement.getPersistentData();
        for (String key : new String[] {POSITION_TAG, TEXTURE_INDEX_TAG, TRANSLATE_X_TAG, TRANSLATE_Y_TAG,
            TRANSLATE_Z_TAG, ROTATE_X_TAG, ROTATE_Y_TAG, ROTATE_Z_TAG}) {
            if (source.contains(key)) {
                target.put(key, source.get(key).copy());
            }
        }
    }

    private static double getDouble(Player player, String key) {
        return getDouble(player, key, -5.0, 5.0);
    }

    private static double getDouble(Player player, String key, double min, double max) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(key)) {
            tag.putDouble(key, 0.0D);
        }
        return clamp(tag.getDouble(key), min, max);
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : 0.0;
    }
}
