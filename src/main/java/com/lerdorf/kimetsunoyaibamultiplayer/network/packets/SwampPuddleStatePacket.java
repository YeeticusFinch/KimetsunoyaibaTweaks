package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwampPuddleStatePacket {
    private final int entityId;
    private final boolean active;
    private final boolean hidden;

    public SwampPuddleStatePacket(int entityId, boolean active, boolean hidden) {
        this.entityId = entityId;
        this.active = active;
        this.hidden = hidden;
    }

    public SwampPuddleStatePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.active = buf.readBoolean();
        this.hidden = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        buf.writeBoolean(hidden);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            Entity entity = minecraft.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity living)) {
                return;
            }

            living.getPersistentData().putBoolean("SwampPuddleClientActive", active);
            living.getPersistentData().putBoolean("SwampPuddleClientHidden", hidden);
        });
        context.setPacketHandled(true);
        return true;
    }
}
