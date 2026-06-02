package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenBloodDemonArtBuilderPacket {
    private final BloodDemonArtBuilderData data;
    private final String view;
    private final int editorSlot;

    public OpenBloodDemonArtBuilderPacket(BloodDemonArtBuilderData data) {
        this(data, "main", -1);
    }

    public OpenBloodDemonArtBuilderPacket(BloodDemonArtBuilderData data, String view, int editorSlot) {
        this.data = data;
        this.view = view == null ? "main" : view;
        this.editorSlot = editorSlot;
    }

    public OpenBloodDemonArtBuilderPacket(FriendlyByteBuf buf) {
        this.data = new BloodDemonArtBuilderData(buf);
        this.view = buf.readUtf();
        this.editorSlot = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        data.write(buf);
        buf.writeUtf(view);
        buf.writeVarInt(editorSlot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientOnlyOpeners.openBloodDemonArtBuilder(data, view, editorSlot)
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

        private static void openBloodDemonArtBuilder(BloodDemonArtBuilderData data, String view, int editorSlot) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.BloodDemonArtBuilderScreen.openFromNetwork(data, view, editorSlot);
        }
    }
}
