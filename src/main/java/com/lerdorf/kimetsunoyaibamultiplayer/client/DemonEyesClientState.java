package com.lerdorf.kimetsunoyaibamultiplayer.client;

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

    public static void setPlayerState(UUID playerId, boolean demon, int index) {
        if (playerId == null) {
            return;
        }
        STATES.put(playerId, new PlayerDemonEyesState(demon, Math.max(0, index)));
    }

    public static PlayerDemonEyesState getPlayerState(UUID playerId) {
        return playerId == null ? null : STATES.get(playerId);
    }

    public static void clear() {
        STATES.clear();
    }

    public record PlayerDemonEyesState(boolean demon, int index) {
    }
}
