package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes player rotation (yaw/pitch/head rotation) from server to client
 */
public class PlayerRotationSyncPacket {
    private final UUID playerUUID;
    private final float yaw;
    private final float pitch;
    private final float headYaw;

    public PlayerRotationSyncPacket(UUID playerUUID, float yaw, float pitch, float headYaw) {
        this.playerUUID = playerUUID;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
    }

    public PlayerRotationSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.headYaw = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(headYaw);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // This packet only goes from server -> client
            if (ctx.getDirection().getReceptionSide().isClient()) {
                // Use proxy to handle client-side rotation
                KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleRotationSync(
                    playerUUID, yaw, pitch, headYaw
                );
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
