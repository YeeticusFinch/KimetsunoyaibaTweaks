package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
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
        int eyesIndex = DemonEyesHelper.getOrCreateIndex(player);
        SyncedDemonEyesState current = new SyncedDemonEyesState(demon, eyesIndex);
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
        LAST_SYNCED_STATE.put(player.getUUID(), new SyncedDemonEyesState(packet.isDemon(), packet.getEyesIndex()));
        ModNetworking.sendToAllClients(packet);
    }

    private static void syncAllTo(ServerPlayer player) {
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            ModNetworking.sendToPlayer(createPacket(other), player);
        }
    }

    private static DemonEyesSyncPacket createPacket(ServerPlayer player) {
        boolean demon = Damager.isDemon(player);
        int eyesIndex = DemonEyesHelper.getOrCreateIndex(player);
        return new DemonEyesSyncPacket(player.getUUID(), demon, eyesIndex);
    }

    private record SyncedDemonEyesState(boolean demon, int eyesIndex) {
    }
}
