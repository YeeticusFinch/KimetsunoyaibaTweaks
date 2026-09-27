package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.config.ConfigEditorRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Applies one server config value after the server has verified the sender's permission. */
public final class ConfigEditPacket {
    private final String specId;
    private final String path;
    private final String value;

    public ConfigEditPacket(String specId, String path, String value) {
        this.specId = specId;
        this.path = path;
        this.value = value;
    }

    public ConfigEditPacket(FriendlyByteBuf buffer) {
        this.specId = buffer.readUtf(64);
        this.path = buffer.readUtf(256);
        this.value = buffer.readUtf(4096);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(specId, 64);
        buffer.writeUtf(path, 256);
        buffer.writeUtf(value, 4096);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                ConfigEditorRegistry.applySerialized(specId, path, value);
            }
        });
        context.setPacketHandled(true);
    }
}
