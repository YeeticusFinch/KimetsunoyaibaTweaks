package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonEyesClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class DemonEyesSyncPacket {
    private final UUID playerUUID;
    private final boolean demon;
    private final int eyesIndex;

    public DemonEyesSyncPacket(UUID playerUUID, boolean demon, int eyesIndex) {
        this.playerUUID = playerUUID;
        this.demon = demon;
        this.eyesIndex = eyesIndex;
    }

    public DemonEyesSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.demon = buf.readBoolean();
        this.eyesIndex = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(demon);
        buf.writeVarInt(eyesIndex);
    }

    public boolean isDemon() {
        return demon;
    }

    public int getEyesIndex() {
        return eyesIndex;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DemonEyesClientState.setPlayerState(playerUUID, demon, eyesIndex)
            )
        );
        context.setPacketHandled(true);
        return true;
    }
}
