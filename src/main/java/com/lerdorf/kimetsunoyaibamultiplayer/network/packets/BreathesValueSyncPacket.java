package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync the server-side "breathes" NBT value to the client.
 * This allows the client to display the correct breathing form without relying on chat message parsing.
 *
 * Direction: Server -> Client
 */
public class BreathesValueSyncPacket {
    private final UUID playerUUID;
    private final double breathesValue;

    public BreathesValueSyncPacket(UUID playerUUID, double breathesValue) {
        this.playerUUID = playerUUID;
        this.breathesValue = breathesValue;
    }

    public BreathesValueSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.breathesValue = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeDouble(breathesValue);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        Player player = ClientPacketHandler.getClientPlayer();

        if (player == null) {
            return;
        }

        // Only update if this packet is for the local player
        if (!player.getUUID().equals(playerUUID)) {
            return;
        }

        // Log current state before update
        if (Config.logDebug) {
            double clientNBT = player.getPersistentData().getDouble("breathes");
            double oldCached = !player.getMainHandItem().isEmpty()
                ? com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.getCachedForm(playerUUID, player.getMainHandItem())
                : 0.0;
            Log.debug("[BreathesSync] Client BEFORE: NBT=" + clientNBT + ", cached=" + oldCached + ", received=" + breathesValue);
        }

        // Update the cached breathes value in BreathingFormTracker
        // We'll store it in the player's current held sword
        if (!player.getMainHandItem().isEmpty()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.updateForm(
                playerUUID,
                player.getMainHandItem(),
                breathesValue
            );

            if (Config.logDebug) {
                double newCached = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingFormTracker.getCachedForm(playerUUID, player.getMainHandItem());
                Log.debug("[BreathesSync] Client AFTER: cached=" + newCached + " (should be " + breathesValue + ")");
            }
        }
    }
}
