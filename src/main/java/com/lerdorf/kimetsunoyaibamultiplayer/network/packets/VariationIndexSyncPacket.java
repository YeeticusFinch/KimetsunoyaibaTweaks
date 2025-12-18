package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs the current variation index (0 = base) for a player.
 * Direction: BIDIRECTIONAL (Client <-> Server)
 */
public class VariationIndexSyncPacket {
    private final UUID playerUUID;
    private final int variationIndex;

    public VariationIndexSyncPacket(UUID playerUUID, int variationIndex) {
        this.playerUUID = playerUUID;
        this.variationIndex = variationIndex;
    }

    public VariationIndexSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.variationIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeInt(variationIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on BOTH client and server
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                // Server side: Update server-side PlayerBreathingData
                PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(playerUUID);
                data.setCurrentVariationIndex(variationIndex);
                System.out.println("[Server] Updated variation index for " + playerUUID + ": " + variationIndex);
            } else {
                // Client side: Update client-side PlayerBreathingData
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    var level = ClientPacketHandler.getClientLevel();
                    if (level == null) return;
                    var player = level.getPlayerByUUID(playerUUID);
                    if (player == null) return;
                    PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(playerUUID);
                    data.setCurrentVariationIndex(variationIndex);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
