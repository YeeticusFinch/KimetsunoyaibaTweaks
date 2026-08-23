package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class DemonRankClientState {
    private static final Map<UUID, Integer> RANK_TIER_BY_PLAYER = new ConcurrentHashMap<>();

    private DemonRankClientState() {
    }

    /** @param rankTier -1 for unranked, otherwise {@code DemonRank.tier()} */
    public static void setPlayerRank(UUID playerId, int rankTier) {
        if (playerId == null) {
            return;
        }
        if (rankTier < 0) {
            RANK_TIER_BY_PLAYER.remove(playerId);
        } else {
            RANK_TIER_BY_PLAYER.put(playerId, rankTier);
        }
    }

    /** @return the player's rank tier, or -1 if unranked/unknown */
    public static int getPlayerRankTier(UUID playerId) {
        if (playerId == null) {
            return -1;
        }
        return RANK_TIER_BY_PLAYER.getOrDefault(playerId, -1);
    }

    public static void clear() {
        RANK_TIER_BY_PLAYER.clear();
    }
}
