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
    private final int hue;

    public DemonEyesSyncPacket(UUID playerUUID, boolean demon, int eyesIndex) {
        this(playerUUID, demon, eyesIndex, 0);
    }

    public DemonEyesSyncPacket(UUID playerUUID, boolean demon, int eyesIndex, int hue) {
        this.playerUUID = playerUUID;
        this.demon = demon;
        this.eyesIndex = eyesIndex;
        this.hue = Math.floorMod(hue, 360);
    }

    public DemonEyesSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.demon = buf.readBoolean();
        this.eyesIndex = buf.readVarInt();
        this.hue = Math.floorMod(buf.readVarInt(), 360);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(demon);
        buf.writeVarInt(eyesIndex);
        buf.writeVarInt(hue);
    }

    public boolean isDemon() {
        return demon;
    }

    public int getEyesIndex() {
        return eyesIndex;
    }

    public int getHue() {
        return hue;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                DemonEyesClientState.setPlayerState(playerUUID, demon, eyesIndex, hue)
            )
        );
        context.setPacketHandled(true);
        return true;
    }
}
