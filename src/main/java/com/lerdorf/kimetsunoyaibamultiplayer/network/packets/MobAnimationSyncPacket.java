package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper;
import net.minecraft.client.Minecraft;
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
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            // Get the entity by ID
            Entity entity = mc.level.getEntity(entityId);
            if (entity == null || !(entity instanceof LivingEntity)) {
                return;
            }

            LivingEntity livingEntity = (LivingEntity) entity;

            // Play animation using MobAnimationHelper
            MobAnimationHelper.playAnimationOnMob(livingEntity, animationName);

        } catch (Exception e) {
            System.err.println("[MobAnimationSyncPacket] Failed to play mob animation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
