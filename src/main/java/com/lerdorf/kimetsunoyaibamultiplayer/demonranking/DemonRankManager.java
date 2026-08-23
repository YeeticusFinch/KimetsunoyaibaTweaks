package com.lerdorf.kimetsunoyaibamultiplayer.demonranking;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Business logic for the Demon Ranking (Twelve Kizuki) ladder: promotion/demotion on kills,
 * rank buffs, and rank forfeiture. Keeps {@link DemonRankingSavedData} as the sole source of truth.
 */
public final class DemonRankManager {
    private static final int BUFF_DURATION_TICKS = 20 * 60 * 10; // 10 minutes; refreshed well before expiry by the tick handler

    private DemonRankManager() {
    }

    /**
     * A demon player killed something. Handles both "killed a ranked player" (promotion swap)
     * and "killed a rank's fallback entity" (claim a free/offline-takeable slot).
     */
    public static void handleKill(ServerPlayer attacker, LivingEntity victim) {
        if (!DemonRankingConfig.isEnabled() || attacker == null || victim == null) {
            return;
        }
        try {
            DemonRankingSavedData data = DemonRankingSavedData.get(attacker.serverLevel());
            if (victim instanceof ServerPlayer victimPlayer) {
                handlePlayerKillsPlayer(data, attacker, victimPlayer);
            } else {
                handlePlayerKillsEntity(data, attacker, victim);
            }
        } catch (Exception e) {
            System.err.println("[DemonRankManager] Error handling kill: " + e.getMessage());
        }
    }

    /**
     * A fallback boss entity killed a ranked player. Demotes the victim if the entity
     * represents the victim's own rank tier or the tier directly below it.
     */
    public static void handleEntityKillsPlayer(LivingEntity attacker, ServerPlayer victim) {
        if (!DemonRankingConfig.isEnabled() || attacker == null || victim == null) {
            return;
        }
        try {
            DemonRankingSavedData data = DemonRankingSavedData.get(victim.serverLevel());
            DemonRank entityRank = DemonRank.fromFallbackEntity(attacker.getType());
            if (entityRank == null) {
                return;
            }
            DemonRank victimRank = data.getRankOf(victim.getUUID());
            if (victimRank == null) {
                return;
            }
            if (entityRank.tier() == victimRank.tier() || entityRank.tier() == victimRank.tier() + 1) {
                demote(data, victim, victimRank);
            }
        } catch (Exception e) {
            System.err.println("[DemonRankManager] Error handling entity kill: " + e.getMessage());
        }
    }

    private static void handlePlayerKillsPlayer(DemonRankingSavedData data, ServerPlayer attacker, ServerPlayer victim) {
        if (!Damager.isDemon(attacker) || !Damager.isDemon(victim)) {
            return;
        }
        DemonRank victimRank = data.getRankOf(victim.getUUID());
        if (victimRank == null) {
            return;
        }
        DemonRank attackerRank = data.getRankOf(attacker.getUUID());
        int attackerTier = attackerRank == null ? DemonRank.UNRANKED_TIER : attackerRank.tier();
        if (attackerTier != victimRank.tier() + 1) {
            return;
        }

        // Attacker held exactly the rank directly below the victim's — swap: attacker takes
        // the victim's old rank, victim takes whatever the attacker held before (or unranked).
        data.assign(victimRank, attacker.getUUID());
        syncOnlinePlayer(attacker, victimRank);
        if (attackerRank != null) {
            data.assign(attackerRank, victim.getUUID());
            syncOnlinePlayer(victim, attackerRank);
        } else {
            data.clearPlayer(victim.getUUID());
            clearOnlinePlayerBuffs(victim);
        }
    }

    private static void handlePlayerKillsEntity(DemonRankingSavedData data, ServerPlayer attacker, LivingEntity victim) {
        if (!Damager.isDemon(attacker)) {
            return;
        }
        DemonRank entityRank = DemonRank.fromFallbackEntity(victim.getType());
        if (entityRank == null) {
            return;
        }

        UUID slotHolder = data.getHolder(entityRank);
        boolean offlineEligible = slotHolder != null
            && data.isOfflineTakeable(entityRank, System.currentTimeMillis(), DemonRankingConfig.getOfflineTakeoverThresholdMillis());
        if (slotHolder != null && !offlineEligible) {
            // A player occupies this rank; killing the fallback entity doesn't count — must kill the player.
            return;
        }

        DemonRank attackerRank = data.getRankOf(attacker.getUUID());
        int attackerTier = attackerRank == null ? DemonRank.UNRANKED_TIER : attackerRank.tier();
        if (attackerTier != entityRank.tier() + 1) {
            return;
        }

        data.assign(entityRank, attacker.getUUID());
        syncOnlinePlayer(attacker, entityRank);

        if (offlineEligible) {
            // The offline holder is dethroned and bumped down, same as a normal player-vs-player swap.
            if (attackerRank != null) {
                data.assign(attackerRank, slotHolder);
            } else {
                data.clearPlayer(slotHolder);
            }
        }
    }

    /**
     * Drops a player one tier down the ladder. If the tier below is already held by a
     * different player, the demoted player simply falls off the ladder instead of
     * displacing that unrelated holder.
     */
    private static void demote(DemonRankingSavedData data, ServerPlayer player, DemonRank currentRank) {
        DemonRank lower = currentRank.below();
        if (lower != null && data.getHolder(lower) == null) {
            data.assign(lower, player.getUUID());
            syncOnlinePlayer(player, lower);
        } else {
            data.clearPlayer(player.getUUID());
            clearOnlinePlayerBuffs(player);
        }
    }

    /** Forcibly removes a player's rank (used by /clearrank and by demon-status self-healing). */
    public static void clearRank(ServerPlayer player) {
        DemonRankingSavedData data = DemonRankingSavedData.get(player.serverLevel());
        if (data.getRankOf(player.getUUID()) != null) {
            data.clearPlayer(player.getUUID());
        }
        clearOnlinePlayerBuffs(player);
    }

    /** Refreshes buffs and self-heals rank loss for an online ranked player. Called periodically from the tick handler. */
    public static void tickRankedPlayer(ServerPlayer player, DemonRankingSavedData data) {
        DemonRank rank = data.getRankOf(player.getUUID());
        if (rank == null) {
            return;
        }
        if (!Damager.isDemon(player)) {
            clearRank(player);
            return;
        }
        applyBuffs(player, rank);
    }

    private static void syncOnlinePlayer(ServerPlayer player, DemonRank rank) {
        applyBuffs(player, rank);
        player.getPersistentData().putString("kizuki_rank", rank.displayName());
    }

    private static void clearOnlinePlayerBuffs(ServerPlayer player) {
        removeBuffs(player);
        player.getPersistentData().remove("kizuki_rank");
    }

    public static void applyBuffs(ServerPlayer player, DemonRank rank) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION_TICKS, rank.strengthLevel() - 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION_TICKS, rank.resistanceLevel() - 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION_TICKS, rank.regenLevel() - 1, true, false, true));
    }

    public static void removeBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
    }
}
