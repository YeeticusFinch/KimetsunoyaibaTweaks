package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonRankClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server-to-client sync of a player's current Demon Rank tier ({@code -1} for unranked). */
public class DemonRankSyncPacket {
    private final UUID playerUUID;
    private final int rankTier;

    public DemonRankSyncPacket(UUID playerUUID, int rankTier) {
        this.playerUUID = playerUUID;
        this.rankTier = rankTier;
    }

    public DemonRankSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.rankTier = buf.readVarInt() - 1;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeVarInt(rankTier + 1);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getRankTier() {
        return rankTier;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            DemonRankClientState.setPlayerRank(playerUUID, rankTier)));
        context.setPacketHandled(true);
        return true;
    }
}
