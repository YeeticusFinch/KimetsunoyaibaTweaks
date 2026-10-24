package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.StructureMarkerMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class DeleteStructureMarkerPacket {
    private final UUID markerUuid;

    public DeleteStructureMarkerPacket(UUID markerUuid) {
        this.markerUuid = markerUuid;
    }

    public DeleteStructureMarkerPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(markerUuid);
    }

    public static boolean handle(DeleteStructureMarkerPacket message,
                                 Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isCreative()
                || !StructureMarkerEntity.isHoldingMarker(player)
                || !(player.containerMenu instanceof StructureMarkerMenu menu)
                || !menu.getMarkerUuid().equals(message.markerUuid)
                || !menu.stillValid(player)) {
                return;
            }
            Entity entity = ((net.minecraft.server.level.ServerLevel) player.level()).getEntity(message.markerUuid);
            if (entity instanceof StructureMarkerEntity marker) {
                marker.discard();
                player.closeContainer();
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
