package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class DemonEyesClientState {
    private static final Map<UUID, PlayerDemonEyesState> STATES = new ConcurrentHashMap<>();

    private DemonEyesClientState() {
    }

    public static void setPlayerState(UUID playerId, boolean demon, int index, int hue) {
        PlayerDemonEyesState current = getPlayerState(playerId);
        int rankTier = current == null ? -1 : current.rankTier();
        float offsetX = current == null ? DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET : current.offsetX();
        float offsetY = current == null ? DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET : current.offsetY();
        setPlayerState(playerId, demon, index, hue, rankTier, offsetX, offsetY);
    }

    public static void setPlayerState(UUID playerId, boolean demon, int index, int hue, int rankTier) {
        PlayerDemonEyesState current = getPlayerState(playerId);
        float offsetX = current == null ? DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET : current.offsetX();
        float offsetY = current == null ? DemonEyesHelper.DEFAULT_DEMON_EYES_OFFSET : current.offsetY();
        setPlayerState(playerId, demon, index, hue, rankTier, offsetX, offsetY);
    }

    public static void setPlayerState(UUID playerId, boolean demon, int index, int hue, int rankTier,
                                      float offsetX, float offsetY) {
        if (playerId == null) {
            return;
        }
        STATES.put(playerId, new PlayerDemonEyesState(
            demon,
            Math.max(0, index),
            Math.floorMod(hue, 360),
            rankTier,
            DemonEyesHelper.normalizeOffset(offsetX),
            DemonEyesHelper.normalizeOffset(offsetY)
        ));
    }

    public static PlayerDemonEyesState getPlayerState(UUID playerId) {
        return playerId == null ? null : STATES.get(playerId);
    }

    public static void clear() {
        STATES.clear();
    }

    public record PlayerDemonEyesState(boolean demon, int index, int hue, int rankTier,
                                       float offsetX, float offsetY) {
    }
}
