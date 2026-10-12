package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.McaDemonStateClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes MCA demon marker and eye data to clients tracking the entity. */
public class McaDemonStatePacket {
    private final int entityId;
    private final int eyesIndex;
    private final int hue;

    public McaDemonStatePacket(int entityId, int eyesIndex, int hue) {
        this.entityId = entityId;
        this.eyesIndex = eyesIndex;
        this.hue = hue;
    }

    public McaDemonStatePacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.eyesIndex = buffer.readVarInt();
        this.hue = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(eyesIndex);
        buffer.writeVarInt(hue);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> McaDemonStateClientHandler.apply(entityId, eyesIndex, hue)
        ));
        context.setPacketHandled(true);
        return true;
    }
}
