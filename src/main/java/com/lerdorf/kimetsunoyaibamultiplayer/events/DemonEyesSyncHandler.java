package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonRankingConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRank;
import com.lerdorf.kimetsunoyaibamultiplayer.demonranking.DemonRankingSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonEyesSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class DemonEyesSyncHandler {
    private static final Map<UUID, SyncedDemonEyesState> LAST_SYNCED_STATE = new ConcurrentHashMap<>();

    private DemonEyesSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncAllTo(player);
        broadcastState(player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        ModNetworking.sendToPlayer(createPacket(target), watcher);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        DemonEyesHelper.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        boolean demon = Damager.isDemon(player);
        int eyesIndex = getAllowedEyesIndex(player);
        int hue = DemonEyesHelper.getHue(player);
        int rankTier = getRankTier(player);
        float offsetX = DemonEyesHelper.getOffsetX(player);
        float offsetY = DemonEyesHelper.getOffsetY(player);
        SyncedDemonEyesState current = new SyncedDemonEyesState(demon, eyesIndex, hue, rankTier, offsetX, offsetY);
        SyncedDemonEyesState previous = LAST_SYNCED_STATE.put(player.getUUID(), current);
        if (!current.equals(previous)) {
            broadcastState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNCED_STATE.remove(event.getEntity().getUUID());
    }

    public static void broadcastState(ServerPlayer player) {
        DemonEyesSyncPacket packet = createPacket(player);
        LAST_SYNCED_STATE.put(player.getUUID(), new SyncedDemonEyesState(
            packet.isDemon(), packet.getEyesIndex(), packet.getHue(), packet.getRankTier(),
            packet.getOffsetX(), packet.getOffsetY()
        ));
        ModNetworking.sendToAllClients(packet);
    }

    private static void syncAllTo(ServerPlayer player) {
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            ModNetworking.sendToPlayer(createPacket(other), player);
        }
    }

    private static DemonEyesSyncPacket createPacket(ServerPlayer player) {
        boolean demon = Damager.isDemon(player);
        int eyesIndex = getAllowedEyesIndex(player);
        int hue = DemonEyesHelper.getHue(player);
        int rankTier = getRankTier(player);
        float offsetX = DemonEyesHelper.getOffsetX(player);
        float offsetY = DemonEyesHelper.getOffsetY(player);
        return new DemonEyesSyncPacket(player.getUUID(), demon, eyesIndex, hue, rankTier, offsetX, offsetY);
    }

    private static int getAllowedEyesIndex(ServerPlayer player) {
        int eyesIndex = DemonEyesHelper.getOrCreateIndex(player);
        if (!CustomProgressionConfig.isEmptyDemonEyesAllowed()
            && eyesIndex == DemonEyesHelper.EMPTY_DEMON_EYES_INDEX) {
            eyesIndex = DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX;
            DemonEyesHelper.setIndex(player, eyesIndex);
        }
        return eyesIndex;
    }

    private static int getRankTier(ServerPlayer player) {
        if (!DemonRankingConfig.isEnabled()) {
            return -1;
        }
        DemonRank rank = DemonRankingSavedData.get(player.serverLevel()).getRankOf(player.getUUID());
        return rank == null ? -1 : rank.tier();
    }

    private record SyncedDemonEyesState(boolean demon, int eyesIndex, int hue, int rankTier,
                                        float offsetX, float offsetY) {
    }
}
