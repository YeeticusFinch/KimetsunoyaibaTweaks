package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public class DemonSlayerSkillPacket {
    public static final int GUARD_START = 0;
    public static final int GUARD_STOP = 1;
    public static final int GUARD_HEARTBEAT = 2;
    public static final int DASH = 3;
    public static final int GUARD_STATE = 4;

    private final int action;
    private final int guardRemaining;

    public DemonSlayerSkillPacket(int action) {
        this(action, 0);
    }

    public DemonSlayerSkillPacket(int action, int guardRemaining) {
        this.action = action;
        this.guardRemaining = guardRemaining;
    }

    public DemonSlayerSkillPacket(FriendlyByteBuf buf) {
        this.action = buf.readByte();
        this.guardRemaining = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(action);
        buf.writeVarInt(guardRemaining);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (context.getDirection().getReceptionSide().isServer()) {
                if (player == null) {
                    return;
                }
                switch (action) {
                    case GUARD_START -> PassiveSkillManager.startGuard(player);
                    case GUARD_STOP -> PassiveSkillManager.cancelGuard(player);
                    case GUARD_HEARTBEAT -> PassiveSkillManager.refreshGuardInput(player);
                    case DASH -> PassiveSkillManager.useDash(player);
                    default -> {
                    }
                }
            } else if (action == GUARD_STATE) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.lerdorf.kimetsunoyaibamultiplayer.client.DemonSlayerSkillClient.updateGuardState(guardRemaining)
                );
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
