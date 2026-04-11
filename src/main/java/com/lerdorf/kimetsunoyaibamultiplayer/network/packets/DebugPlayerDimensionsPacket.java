package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DebugPlayerDimensionsPacket {
    private final int entityId;
    private final boolean active;
    private final float height;
    private final float eyeHeight;

    public DebugPlayerDimensionsPacket(int entityId, boolean active, float height, float eyeHeight) {
        this.entityId = entityId;
        this.active = active;
        this.height = height;
        this.eyeHeight = eyeHeight;
    }

    public DebugPlayerDimensionsPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.active = buf.readBoolean();
        this.height = buf.readFloat();
        this.eyeHeight = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        buf.writeFloat(height);
        buf.writeFloat(eyeHeight);
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

            if (active) {
                living.getPersistentData().putBoolean(SwampDemonArt.DEBUG_DIMENSIONS_ACTIVE_TAG, true);
                living.getPersistentData().putFloat(SwampDemonArt.DEBUG_DIMENSIONS_HEIGHT_TAG, height);
                living.getPersistentData().putFloat(SwampDemonArt.DEBUG_DIMENSIONS_EYE_HEIGHT_TAG, eyeHeight);
            } else {
                living.getPersistentData().remove(SwampDemonArt.DEBUG_DIMENSIONS_ACTIVE_TAG);
                living.getPersistentData().remove(SwampDemonArt.DEBUG_DIMENSIONS_HEIGHT_TAG);
                living.getPersistentData().remove(SwampDemonArt.DEBUG_DIMENSIONS_EYE_HEIGHT_TAG);
            }

            SwampDemonArt.applyCurrentDimensions(living);

            String summary = SwampDemonArt.buildDimensionDebugSummary(living);
            Log.alwaysWarn("Client debug dimensions applied for entity {}: {}", entityId, summary);
            if (minecraft.player != null && minecraft.player.getId() == entityId) {
                minecraft.player.sendSystemMessage(Component.literal("[debugplayerdims client] " + summary));
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
