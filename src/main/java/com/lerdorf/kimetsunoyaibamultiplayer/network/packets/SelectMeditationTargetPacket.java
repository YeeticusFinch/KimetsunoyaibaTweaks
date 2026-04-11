package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectMeditationTargetPacket {
    private final String type;
    private final String id;

    public SelectMeditationTargetPacket(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public SelectMeditationTargetPacket(FriendlyByteBuf buf) {
        this.type = buf.readUtf();
        this.id = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(type);
        buf.writeUtf(id);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MeditationMenuService.saveSelection(player, type, id);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
