package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenDemonPropositionPacket {
    private final UUID attackerUuid;
    private final Component attackerName;
    private final long endGameTime;

    public OpenDemonPropositionPacket(UUID attackerUuid, Component attackerName, long endGameTime) {
        this.attackerUuid = attackerUuid;
        this.attackerName = attackerName;
        this.endGameTime = endGameTime;
    }

    public OpenDemonPropositionPacket(FriendlyByteBuf buf) {
        this.attackerUuid = buf.readUUID();
        this.attackerName = buf.readComponent();
        this.endGameTime = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(attackerUuid);
        buf.writeComponent(attackerName);
        buf.writeLong(endGameTime);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientOnlyOpeners.openDemonProposition(attackerUuid, attackerName, endGameTime);
            })
        );
        context.setPacketHandled(true);
        return true;
    }

    private static final class ClientOnlyOpeners {
        private ClientOnlyOpeners() {
        }

        private static void openDemonProposition(UUID attackerUuid, Component attackerName, long endGameTime) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            minecraft.setScreen(new com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionScreen(attackerUuid, attackerName, endGameTime));
        }
    }
}
