package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseDemonPropositionPacket {
    public CloseDemonPropositionPacket() {
    }

    public CloseDemonPropositionPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientOnlyOpeners.closeDemonProposition();
            })
        );
        context.setPacketHandled(true);
        return true;
    }

    private static final class ClientOnlyOpeners {
        private ClientOnlyOpeners() {
        }

        private static void closeDemonProposition() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionClientController.deactivate();
            if (minecraft.screen instanceof com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionScreen) {
                minecraft.setScreen(null);
            }
        }
    }
}
