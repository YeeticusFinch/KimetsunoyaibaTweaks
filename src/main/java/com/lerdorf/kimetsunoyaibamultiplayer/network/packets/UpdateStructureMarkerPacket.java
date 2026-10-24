package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.StructureMarkerMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateStructureMarkerPacket {
    private final UUID markerUuid;
    private final String markerId;

    public UpdateStructureMarkerPacket(UUID markerUuid, String markerId) {
        this.markerUuid = markerUuid;
        this.markerId = markerId == null ? "" : markerId;
    }

    public UpdateStructureMarkerPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readUtf(StructureMarkerEntity.MAX_ID_LENGTH));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUUID(markerUuid);
        buffer.writeUtf(markerId, StructureMarkerEntity.MAX_ID_LENGTH);
    }

    public static boolean handle(UpdateStructureMarkerPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
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
                marker.setMarkerId(message.markerId);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
