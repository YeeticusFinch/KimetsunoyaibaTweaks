package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells the owning client to follow or release Rui's detached head. */
public class RuiHeadCameraPacket {
    private final int headEntityId;
    private final boolean attach;

    public RuiHeadCameraPacket(int headEntityId, boolean attach) {
        this.headEntityId = headEntityId;
        this.attach = attach;
    }

    public RuiHeadCameraPacket(FriendlyByteBuf buffer) {
        this.headEntityId = buffer.readVarInt();
        this.attach = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(headEntityId);
        buffer.writeBoolean(attach);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.lerdorf.kimetsunoyaibamultiplayer.client.RuiHeadCameraClientHandler
                .handle(headEntityId, attach)));
        context.setPacketHandled(true);
        return true;
    }
}
