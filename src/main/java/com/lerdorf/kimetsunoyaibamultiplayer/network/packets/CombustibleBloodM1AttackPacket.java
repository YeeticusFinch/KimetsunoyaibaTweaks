package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.CombustibleBlood;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CombustibleBloodM1AttackPacket {
    private final UUID excludedTargetId;

    public CombustibleBloodM1AttackPacket() {
        this((UUID) null);
    }

    public CombustibleBloodM1AttackPacket(UUID excludedTargetId) {
        this.excludedTargetId = excludedTargetId;
    }

    public CombustibleBloodM1AttackPacket(FriendlyByteBuf buf) {
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
            if (player != null
                && !FearEffectHandler.isParalyzed(player)
                && CombustibleBlood.isCombustibleBloodMeleeItem(player.getMainHandItem())) {
                BloodDemonArtM1AttackHandler.performNezukoAttack(player, excludedTargetId);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
