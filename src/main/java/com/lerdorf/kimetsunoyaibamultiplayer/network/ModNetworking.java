package com.lerdorf.kimetsunoyaibamultiplayer.network;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Register the packet for BOTH directions with the same ID
        // Use PLAY_TO_SERVER as primary registration
        int packetId = id();
        net.messageBuilder(AnimationSyncPacket.class, packetId)
                .decoder(AnimationSyncPacket::new)
                .encoder(AnimationSyncPacket::toBytes)
                .consumerMainThread(AnimationSyncPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToAllClientsExcept(MSG message, ServerPlayer excludePlayer) {
        for (ServerPlayer player : excludePlayer.server.getPlayerList().getPlayers()) {
            if (!player.equals(excludePlayer)) {
                sendToPlayer(message, player);
            }
        }
    }
}