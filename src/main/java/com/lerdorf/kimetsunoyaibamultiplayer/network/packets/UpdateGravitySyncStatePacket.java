package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateGravitySyncStatePacket {
    private final UUID entityUUID;

    public UpdateGravitySyncStatePacket(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }

    public UpdateGravitySyncStatePacket(FriendlyByteBuf buf) {
        this(buf.readUUID());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
    }

    public static boolean handle(UpdateGravitySyncStatePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        return true;
    }
}
