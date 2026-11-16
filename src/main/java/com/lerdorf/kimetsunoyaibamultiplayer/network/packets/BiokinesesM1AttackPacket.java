package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.Biokineses;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class BiokinesesM1AttackPacket {
    private final UUID excludedTargetId;

    public BiokinesesM1AttackPacket() {
        this((UUID) null);
    }

    public BiokinesesM1AttackPacket(UUID excludedTargetId) {
        this.excludedTargetId = excludedTargetId;
    }

    public BiokinesesM1AttackPacket(FriendlyByteBuf buffer) {
        excludedTargetId = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(excludedTargetId != null);
        if (excludedTargetId != null) {
            buffer.writeUUID(excludedTargetId);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !FearEffectHandler.isParalyzed(player)) {
                Biokineses.handleClientLeftClick(player, excludedTargetId);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
