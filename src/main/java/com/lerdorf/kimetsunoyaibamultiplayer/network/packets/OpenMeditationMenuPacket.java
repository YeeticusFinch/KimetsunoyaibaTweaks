package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMeditationMenuPacket {
    private final MeditationMenuData data;

    public OpenMeditationMenuPacket(MeditationMenuData data) {
        this.data = data;
    }

    public OpenMeditationMenuPacket(FriendlyByteBuf buf) {
        this.data = new MeditationMenuData(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        data.write(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientOnlyOpeners.openMeditationMenu(data)
            )
        );
        context.setPacketHandled(true);
        return true;
    }

    /**
     * Isolated client-only screen opener to keep outer packet class server-safe.
     */
    private static final class ClientOnlyOpeners {
        private ClientOnlyOpeners() {
        }

        private static void openMeditationMenu(MeditationMenuData data) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.screen instanceof com.lerdorf.kimetsunoyaibamultiplayer.client.MeditationMenuScreen current) {
                minecraft.setScreen(current.refreshed(data));
            } else {
                minecraft.setScreen(new com.lerdorf.kimetsunoyaibamultiplayer.client.MeditationMenuScreen(data));
            }
        }
    }
}
