package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.TrainingGourdMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TrainingGourdActionPacket {
    public static final int BREATH = 0;

    private final int action;

    public TrainingGourdActionPacket(int action) {
        this.action = action;
    }

    public TrainingGourdActionPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof TrainingGourdMenu menu)) {
                return;
            }
            if (action == BREATH) {
                menu.addBreath(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
