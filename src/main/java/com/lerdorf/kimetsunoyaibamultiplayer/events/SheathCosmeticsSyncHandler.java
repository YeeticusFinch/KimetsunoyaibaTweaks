package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SheathCosmeticsSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SheathCosmeticsHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class SheathCosmeticsSyncHandler {
    private static final Map<UUID, State> LAST_SYNCED = new ConcurrentHashMap<>();

    private SheathCosmeticsSyncHandler() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAllTo(player);
            broadcastState(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
            ModNetworking.sendToPlayer(createPacket(target), watcher);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        SheathCosmeticsHelper.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
            || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State current = stateOf(player);
        if (!current.equals(LAST_SYNCED.put(player.getUUID(), current))) {
            broadcastState(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNCED.remove(event.getEntity().getUUID());
    }

    public static void broadcastState(ServerPlayer player) {
        ModNetworking.sendToAllClients(createPacket(player));
        LAST_SYNCED.put(player.getUUID(), stateOf(player));
    }

    private static void syncAllTo(ServerPlayer player) {
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            ModNetworking.sendToPlayer(createPacket(other), player);
        }
    }

    private static SheathCosmeticsSyncPacket createPacket(ServerPlayer player) {
        return new SheathCosmeticsSyncPacket(player.getUUID(), SheathCosmeticsHelper.getPosition(player),
            SheathCosmeticsHelper.getTextureIndex(player), SheathCosmeticsHelper.getTranslateX(player),
            SheathCosmeticsHelper.getTranslateY(player), SheathCosmeticsHelper.getTranslateZ(player),
            SheathCosmeticsHelper.getRotateX(player), SheathCosmeticsHelper.getRotateY(player),
            SheathCosmeticsHelper.getRotateZ(player));
    }

    private static State stateOf(ServerPlayer player) {
        return new State(SheathCosmeticsHelper.getPosition(player), SheathCosmeticsHelper.getTextureIndex(player),
            SheathCosmeticsHelper.getTranslateX(player), SheathCosmeticsHelper.getTranslateY(player),
            SheathCosmeticsHelper.getTranslateZ(player), SheathCosmeticsHelper.getRotateX(player),
            SheathCosmeticsHelper.getRotateY(player), SheathCosmeticsHelper.getRotateZ(player));
    }

    private record State(com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.SwordDisplayPosition position,
                         int textureIndex, double translateX, double translateY, double translateZ,
                         double rotateX, double rotateY, double rotateZ) {
    }
}
