package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OrochiDismountPacket {
    private final int orochiEntityId;

    public OrochiDismountPacket() {
        this(-1);
    }

    public OrochiDismountPacket(int orochiEntityId) {
        this.orochiEntityId = orochiEntityId;
    }

    public OrochiDismountPacket(FriendlyByteBuf buffer) {
        this.orochiEntityId = buffer.readInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(this.orochiEntityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            OrochiEntity orochi = OrochiEntity.resolveOwnedOrochi(player, this.orochiEntityId);
            if (orochi != null) {
                Log.alwaysWarn("[Orochi] Server received dismount packet from {} (entityId={}, cooldownRemaining={} ticks, mounted={})",
                    player.getName().getString(), this.orochiEntityId, orochi.getMountToggleCooldownRemainingTicks(),
                    orochi.getVehicle() == player || player.getPassengers().contains(orochi));
                boolean dismounted = orochi.forceDismountToSafeLocation(player);
                if (!dismounted) {
                    Log.alwaysWarn("[Orochi] Server denied dismount packet from {} (cooldownRemaining={} ticks)",
                        player.getName().getString(), orochi.getMountToggleCooldownRemainingTicks());
                }
            } else {
                Log.alwaysWarn("[Orochi] Server received dismount packet from {} but no owned Orochi could be resolved",
                    player.getName().getString());
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
