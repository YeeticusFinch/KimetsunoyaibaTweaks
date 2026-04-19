package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonEyesSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetDemonEyesPacket {
    private final int eyesIndex;

    public SetDemonEyesPacket(int eyesIndex) {
        this.eyesIndex = Math.max(0, eyesIndex);
    }

    public SetDemonEyesPacket(FriendlyByteBuf buf) {
        this.eyesIndex = Math.max(0, buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(eyesIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !Damager.isDemon(player)) {
                return;
            }
            DemonEyesHelper.setIndex(player, eyesIndex);
            DemonEyesSyncHandler.broadcastState(player);
        });
        context.setPacketHandled(true);
        return true;
    }
}
