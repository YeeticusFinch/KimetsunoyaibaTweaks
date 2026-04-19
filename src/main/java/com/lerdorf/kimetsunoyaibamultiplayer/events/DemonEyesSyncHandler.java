package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonEyesSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class DemonEyesSyncHandler {
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

    public static void broadcastState(ServerPlayer player) {
        ModNetworking.sendToAllClients(createPacket(player));
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
}
