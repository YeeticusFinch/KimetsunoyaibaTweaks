package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.world.entity.player.Player;

public final class DemonEyesHelper {
    public static final String DEMON_EYES_INDEX_KEY = "DemonEyesIndex";
    public static final int DEFAULT_DEMON_EYES_INDEX = 2;

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

    public static void copy(Player source, Player target) {
        if (source == null || target == null) {
            return;
        }
        if (source.getPersistentData().contains(DEMON_EYES_INDEX_KEY)) {
            target.getPersistentData().putInt(DEMON_EYES_INDEX_KEY, getStoredIndex(source));
        }
    }
}
