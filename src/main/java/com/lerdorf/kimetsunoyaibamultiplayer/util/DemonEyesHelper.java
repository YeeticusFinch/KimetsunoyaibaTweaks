package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.world.entity.player.Player;

public final class DemonEyesHelper {
    public static final String DEMON_EYES_INDEX_KEY = "DemonEyesIndex";
    public static final String DEMON_EYES_HUE_KEY = "DemonEyesHue";
    public static final String DEMON_EYES_OFFSET_X_KEY = "DemonEyesOffsetX";
    public static final String DEMON_EYES_OFFSET_Y_KEY = "DemonEyesOffsetY";
    public static final int DEFAULT_DEMON_EYES_INDEX = 2;
    public static final int EMPTY_DEMON_EYES_INDEX = 1011;
    public static final int DEFAULT_DEMON_EYES_HUE = 0;
    public static final float DEFAULT_DEMON_EYES_OFFSET = 0.0F;

    private DemonEyesHelper() {
    }

    public static int getStoredIndex(Player player) {
        if (player == null) {
            return DEFAULT_DEMON_EYES_INDEX;
        }
        if (!player.getPersistentData().contains(DEMON_EYES_INDEX_KEY)) {
            return DEFAULT_DEMON_EYES_INDEX;
        }
        return Math.max(0, player.getPersistentData().getInt(DEMON_EYES_INDEX_KEY));
    }

    public static int getOrCreateIndex(Player player) {
        int index = getStoredIndex(player);
        setIndex(player, index);
        return index;
    }

    public static void setIndex(Player player, int index) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putInt(DEMON_EYES_INDEX_KEY, Math.max(0, index));
    }

    public static int getHue(Player player) {
        if (player == null || !player.getPersistentData().contains(DEMON_EYES_HUE_KEY)) {
            return DEFAULT_DEMON_EYES_HUE;
        }
        return normalizeHue(player.getPersistentData().getInt(DEMON_EYES_HUE_KEY));
    }

    public static void setHue(Player player, int hue) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putInt(DEMON_EYES_HUE_KEY, normalizeHue(hue));
    }

    public static float getOffsetX(Player player) {
        return getOffset(player, DEMON_EYES_OFFSET_X_KEY);
    }

    public static float getOffsetY(Player player) {
        return getOffset(player, DEMON_EYES_OFFSET_Y_KEY);
    }

    public static void setOffsetX(Player player, float offsetX) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putFloat(DEMON_EYES_OFFSET_X_KEY, normalizeOffset(offsetX));
    }

    public static void setOffsetY(Player player, float offsetY) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putFloat(DEMON_EYES_OFFSET_Y_KEY, normalizeOffset(offsetY));
    }

    public static void setOffsets(Player player, float offsetX, float offsetY) {
        setOffsetX(player, offsetX);
        setOffsetY(player, offsetY);
    }

    public static void setStyle(Player player, int index, int hue) {
        setIndex(player, index);
        setHue(player, hue);
    }

    public static void setStyle(Player player, int index, int hue, float offsetX, float offsetY) {
        setIndex(player, index);
        setHue(player, hue);
        setOffsets(player, offsetX, offsetY);
    }

    public static int normalizeHue(int hue) {
        return Math.floorMod(hue, 360);
    }

    public static void copy(Player source, Player target) {
        if (source == null || target == null) {
            return;
        }
        if (source.getPersistentData().contains(DEMON_EYES_INDEX_KEY)) {
            target.getPersistentData().putInt(DEMON_EYES_INDEX_KEY, getStoredIndex(source));
        }
        if (source.getPersistentData().contains(DEMON_EYES_HUE_KEY)) {
            target.getPersistentData().putInt(DEMON_EYES_HUE_KEY, getHue(source));
        }
        if (source.getPersistentData().contains(DEMON_EYES_OFFSET_X_KEY)) {
            target.getPersistentData().putFloat(DEMON_EYES_OFFSET_X_KEY, getOffsetX(source));
        }
        if (source.getPersistentData().contains(DEMON_EYES_OFFSET_Y_KEY)) {
            target.getPersistentData().putFloat(DEMON_EYES_OFFSET_Y_KEY, getOffsetY(source));
        }
    }

    public static float normalizeOffset(float offset) {
        if (!Float.isFinite(offset)) {
            return DEFAULT_DEMON_EYES_OFFSET;
        }
        return offset;
    }

    private static float getOffset(Player player, String key) {
        if (player == null || !player.getPersistentData().contains(key)) {
            return DEFAULT_DEMON_EYES_OFFSET;
        }
        return normalizeOffset(player.getPersistentData().getFloat(key));
    }
}
