package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CustomBdaPassiveAttackPacket {
    private final UUID excludedTargetId;

    public CustomBdaPassiveAttackPacket() {
        this((UUID) null);
    }

    public CustomBdaPassiveAttackPacket(UUID excludedTargetId) {
        this.excludedTargetId = excludedTargetId;
    }

    public CustomBdaPassiveAttackPacket(FriendlyByteBuf buf) {
        this.excludedTargetId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.excludedTargetId != null);
        if (this.excludedTargetId != null) {
            buf.writeUUID(this.excludedTargetId);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !FearEffectHandler.isParalyzed(player)) {
                PassiveSkillManager.handleCustomBdaPassiveAttack(player, excludedTargetId);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
