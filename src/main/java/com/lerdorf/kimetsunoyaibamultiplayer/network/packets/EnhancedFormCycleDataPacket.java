package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.EnhancedFormCycleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EnhancedFormCycleDataPacket {
    private final List<Integer> indices;
    private final List<String> names;
    private final int selected;
    private final int color;

    public EnhancedFormCycleDataPacket(List<Integer> indices, List<String> names, int selected, int color) {
        this.indices = List.copyOf(indices);
        this.names = List.copyOf(names);
        this.selected = selected;
        this.color = color;
    }

    public EnhancedFormCycleDataPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.indices = new ArrayList<>(size);
        this.names = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.indices.add(buf.readVarInt());
            this.names.add(buf.readUtf());
        }
        this.selected = buf.readVarInt();
        this.color = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(indices.size());
        for (int i = 0; i < indices.size(); i++) {
            buf.writeVarInt(indices.get(i));
            buf.writeUtf(names.get(i));
        }
        buf.writeVarInt(selected);
        buf.writeInt(color);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            EnhancedFormCycleHandler.acceptCustomData(indices, names, selected, color)));
        context.setPacketHandled(true);
    }
}
