package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class SheathCosmeticsClientState {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private SheathCosmeticsClientState() {
    }

    public static void set(UUID playerId, SwordDisplayConfig.SwordDisplayPosition position, int textureIndex,
                           double translateX, double translateY, double translateZ,
                           double rotateX, double rotateY, double rotateZ) {
        if (playerId != null) {
            STATES.put(playerId, new State(position, Math.max(0, textureIndex), translateX, translateY, translateZ,
                rotateX, rotateY, rotateZ));
        }
    }

    public static State get(UUID playerId) {
        return playerId == null ? null : STATES.get(playerId);
    }

    public static void clear() {
        STATES.clear();
    }

    public record State(SwordDisplayConfig.SwordDisplayPosition position, int textureIndex,
                        double translateX, double translateY, double translateZ,
                        double rotateX, double rotateY, double rotateZ) {
    }
}
