package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CustomBdaPassiveAttackPacket {
    public CustomBdaPassiveAttackPacket() {
    }

    public CustomBdaPassiveAttackPacket(FriendlyByteBuf ignored) {
    }

    public void toBytes(FriendlyByteBuf ignored) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PassiveSkillManager.handleCustomBdaPassiveAttack(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
