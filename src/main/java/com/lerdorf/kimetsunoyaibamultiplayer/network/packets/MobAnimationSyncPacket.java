package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to sync mob animations from server to clients.
 * Used for Custom NPCs and other mobs that support MobPlayerAnimator.
 */
public class MobAnimationSyncPacket {

    private final int entityId;
    private final String animationName;

    public MobAnimationSyncPacket(int entityId, String animationName) {
        this.entityId = entityId;
        this.animationName = animationName;
    }

    public MobAnimationSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.animationName = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(animationName, 256);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Only process on client side
            if (ctx.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    handleClientSide(entityId, animationName);
                });
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleClientSide(int entityId, String animationName) {
        try {
            var level = ClientPacketHandler.getClientLevel();
            if (level == null) {
                return;
            }

            // Get the entity by ID
            Entity entity = level.getEntity(entityId);
            if (entity == null || !(entity instanceof LivingEntity)) {
                System.err.println("[MobAnimationSyncPacket] Client received packet but entity not found or not LivingEntity: id=" + entityId);
                return;
            }

            LivingEntity livingEntity = (LivingEntity) entity;

            // Play animation using MobAnimationHelper
            Log.debug("[MobAnimationSyncPacket] Client applying mob animation '" + animationName + "' to entity:");
            Log.debug("  - Entity ID: " + entityId);
            Log.debug("  - Entity Class: " + livingEntity.getClass().getName());
            Log.debug("  - Entity Name: " + livingEntity.getName().getString());

            // Try to get additional CustomNPC info
            try {
                Object displayObj = livingEntity.getClass().getMethod("getDisplay").invoke(livingEntity);
                if (displayObj != null) {
                    Object modelDataObj = displayObj.getClass().getMethod("getModel").invoke(displayObj);
                    if (modelDataObj != null) {
                        Log.debug("  - Model Data: " + modelDataObj.getClass().getSimpleName());
                    }
                }
            } catch (Throwable ignored) {
                // Not a CustomNPC or method doesn't exist
            }

            MobAnimationHelper.playAnimationOnMob(livingEntity, animationName);

        } catch (Exception e) {
            System.err.println("[MobAnimationSyncPacket] Failed to play mob animation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
