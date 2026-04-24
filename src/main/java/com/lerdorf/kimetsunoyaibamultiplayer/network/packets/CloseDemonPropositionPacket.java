package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionClientController;
import com.lerdorf.kimetsunoyaibamultiplayer.client.DemonPropositionScreen;
import net.minecraft.client.Minecraft;
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
                Minecraft minecraft = Minecraft.getInstance();
                DemonPropositionClientController.deactivate();
                if (minecraft.screen instanceof DemonPropositionScreen) {
                    minecraft.setScreen(null);
                }
            })
        );
        context.setPacketHandled(true);
        return true;
    }
}
