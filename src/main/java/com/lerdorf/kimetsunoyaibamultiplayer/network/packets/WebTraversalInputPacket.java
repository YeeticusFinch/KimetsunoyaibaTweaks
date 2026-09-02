package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.DemonwebPuppetry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the local player's forward/backward input while Web Traversal is active. */
public class WebTraversalInputPacket {
    private final float forwardInput;
    private final float strafeInput;
    private final boolean jumping;
    private final boolean descending;

    public WebTraversalInputPacket(float forwardInput, float strafeInput, boolean jumping, boolean descending) {
        this.forwardInput = forwardInput;
        this.strafeInput = strafeInput;
        this.jumping = jumping;
        this.descending = descending;
    }

    public WebTraversalInputPacket(FriendlyByteBuf buffer) {
        this.forwardInput = buffer.readFloat();
        this.strafeInput = buffer.readFloat();
        this.jumping = buffer.readBoolean();
        this.descending = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeFloat(forwardInput);
        buffer.writeFloat(strafeInput);
        buffer.writeBoolean(jumping);
        buffer.writeBoolean(descending);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DemonwebPuppetry.setWebTraversalInput(player, forwardInput, strafeInput, jumping, descending);
            }
        });
        context.setPacketHandled(true);
    }
}
