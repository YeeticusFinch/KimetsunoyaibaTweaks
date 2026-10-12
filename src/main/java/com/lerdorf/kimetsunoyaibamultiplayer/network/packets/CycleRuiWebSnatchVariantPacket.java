package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.RuiBloodDemonArt;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CycleRuiWebSnatchVariantPacket {
    public CycleRuiWebSnatchVariantPacket() {
    }

    public CycleRuiWebSnatchVariantPacket(FriendlyByteBuf buf) {
    }

    public static void encode(CycleRuiWebSnatchVariantPacket packet, FriendlyByteBuf buf) {
    }

    public static void handle(CycleRuiWebSnatchVariantPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !FearEffectHandler.isParalyzed(player)) {
                RuiBloodDemonArt.cycleWebSnatchVariant(player);
            }
        });
        context.setPacketHandled(true);
    }
}
