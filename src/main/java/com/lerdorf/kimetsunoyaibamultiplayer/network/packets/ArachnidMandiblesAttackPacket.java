package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.events.ArachnidMandiblesHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests the server to process an arachnid mandibles air click. */
public final class ArachnidMandiblesAttackPacket {
    public ArachnidMandiblesAttackPacket() {
    }

    public ArachnidMandiblesAttackPacket(FriendlyByteBuf ignored) {
    }

    public void toBytes(FriendlyByteBuf ignored) {
    }

    public static boolean handle(ArachnidMandiblesAttackPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ArachnidMandiblesHandler.performAttack(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
