package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonPropositionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DemonPropositionResponsePacket {
    private final boolean accept;

    public DemonPropositionResponsePacket(boolean accept) {
        this.accept = accept;
    }

    public DemonPropositionResponsePacket(FriendlyByteBuf buf) {
        this.accept = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(accept);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DemonPropositionHandler.handleResponse(player, accept);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
