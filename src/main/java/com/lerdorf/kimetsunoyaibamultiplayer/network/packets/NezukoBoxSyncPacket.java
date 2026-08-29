package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.NezukoBoxClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class NezukoBoxSyncPacket {
    private final UUID playerId;
    private final boolean hasBox;
    private final boolean open;

    public NezukoBoxSyncPacket(UUID playerId, boolean hasBox, boolean open) {
        this.playerId = playerId;
        this.hasBox = hasBox;
        this.open = open;
    }

    public NezukoBoxSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.hasBox = buf.readBoolean();
        this.open = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(hasBox);
        buf.writeBoolean(open);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            NezukoBoxClientState.set(playerId, hasBox, open)));
        context.setPacketHandled(true);
        return true;
    }
}
