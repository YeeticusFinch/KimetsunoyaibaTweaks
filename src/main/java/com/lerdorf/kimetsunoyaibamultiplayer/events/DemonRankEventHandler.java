package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRank;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankManager;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankingSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonRankSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wires kill events, login/logout, and periodic buff refresh into {@link DemonRankManager}.
 * Never re-triggers {@code hurt()}, so no recursion guard is needed here (unlike SwordClashingHandler).
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class DemonRankEventHandler {
    private static final int BUFF_REFRESH_INTERVAL_TICKS = 100;
    private static final Map<UUID, Integer> LAST_SYNCED_TIER = new ConcurrentHashMap<>();

    private DemonRankEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!DemonRankingConfig.isEnabled()) {
            return;
        }
        try {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) {
                return;
            }
            if (!(event.getSource().getEntity() instanceof LivingEntity source)) {
                return;
            }

            if (source instanceof ServerPlayer attacker) {
                DemonRankManager.handleKill(attacker, victim);
            } else if (victim instanceof ServerPlayer victimPlayer) {
                DemonRankManager.handleEntityKillsPlayer(source, victimPlayer);
            }
        } catch (Exception e) {
            System.err.println("[DemonRankEventHandler] Error handling living death: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        try {
            DemonRankingSavedData data = DemonRankingSavedData.get(player.serverLevel());
            data.markOnline(player.getUUID());
            syncAllTo(player, data);
        } catch (Exception e) {
            System.err.println("[DemonRankEventHandler] Error handling player login: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        try {
            DemonRankingSavedData data = DemonRankingSavedData.get(player.serverLevel());
            data.markOffline(player.getUUID(), System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("[DemonRankEventHandler] Error handling player logout: " + e.getMessage());
        } finally {
            LAST_SYNCED_TIER.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || !DemonRankingConfig.isEnabled()) {
            return;
        }
        if (player.tickCount % BUFF_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        try {
            DemonRankingSavedData data = DemonRankingSavedData.get(player.serverLevel());
            DemonRankManager.tickRankedPlayer(player, data);
            broadcastIfChanged(player, data);
        } catch (Exception e) {
            System.err.println("[DemonRankEventHandler] Error on player tick: " + e.getMessage());
        }
    }

    private static void syncAllTo(ServerPlayer player, DemonRankingSavedData data) {
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            DemonRank rank = data.getRankOf(other.getUUID());
            ModNetworking.sendToPlayer(new DemonRankSyncPacket(other.getUUID(), rank == null ? -1 : rank.tier()), player);
        }
    }

    private static void broadcastIfChanged(ServerPlayer player, DemonRankingSavedData data) {
        DemonRank rank = data.getRankOf(player.getUUID());
        int tier = rank == null ? -1 : rank.tier();
        Integer previous = LAST_SYNCED_TIER.put(player.getUUID(), tier);
        if (previous == null || previous != tier) {
            ModNetworking.sendToAllClients(new DemonRankSyncPacket(player.getUUID(), tier));
        }
    }
}
