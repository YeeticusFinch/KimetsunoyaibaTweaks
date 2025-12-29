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
                System.out.println("[Client] VariationIndexSyncPacket received for player " + playerUUID + ", variation index: " + variationIndex);
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    var level = ClientPacketHandler.getClientLevel();
                    if (level == null) {
                        System.out.println("[Client] ERROR: Client level is null, cannot update variation index!");
                        return;
                    }
                    var player = level.getPlayerByUUID(playerUUID);
                    if (player == null) {
                        System.out.println("[Client] ERROR: Player " + playerUUID + " not found in client level!");
                        return;
                    }
                    PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(playerUUID);
                    data.setCurrentVariationIndex(variationIndex);
                    System.out.println("[Client] Successfully updated variation index for " + playerUUID + " to " + variationIndex);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
