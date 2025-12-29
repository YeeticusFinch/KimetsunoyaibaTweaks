package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet for synchronizing whip animation state across clients.
 *
 * Sent from server to clients when a player's whip animation changes.
 * This ensures all players see the same whip motion.
 */
public class WhipAnimationPacket {

    private final UUID playerUUID;
    private final String animationName;
    private final double extensionLevel;
    private final int animationTick;
    private final long timestamp;

    /**
     * Create a new whip animation sync packet.
     *
     * @param playerUUID Player whose whip animation changed
     * @param animationName Name of the animation (e.g., "idle", "sword_to_left", "love_form_1")
     * @param extensionLevel Whip extension (0.0 = idle length, 1.0 = fully extended)
     * @param animationTick Current animation tick for interpolation
     */
    public WhipAnimationPacket(UUID playerUUID, String animationName, double extensionLevel, int animationTick) {
        this.playerUUID = playerUUID;
        this.animationName = animationName;
        this.extensionLevel = extensionLevel;
        this.animationTick = animationTick;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Decode packet from network buffer.
     */
    public WhipAnimationPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.animationName = buf.readUtf();
        this.extensionLevel = buf.readDouble();
        this.animationTick = buf.readVarInt();
        this.timestamp = buf.readLong();
    }

    /**
     * Encode packet to network buffer.
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeUtf(animationName);
        buf.writeDouble(extensionLevel);
        buf.writeVarInt(animationTick);
        buf.writeLong(timestamp);
    }

    /**
     * Handle packet reception.
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                // Server received from client - relay to all other clients
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    WhipAnimationPacket relayPacket = new WhipAnimationPacket(
                        playerUUID, animationName, extensionLevel, animationTick
                    );
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                        relayPacket, sender
                    );
                }
            } else {
                // Client received update - apply to whip physics
                handleClientSide();
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    /**
     * Client-side packet handling.
     * Uses DistExecutor for safe client-only code execution.
     */
    private void handleClientSide() {
        net.minecraftforge.api.distmarker.Dist clientDist = net.minecraftforge.api.distmarker.Dist.CLIENT;
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(clientDist, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                // Search for entity by UUID (could be player or any other LivingEntity)
                net.minecraft.world.entity.LivingEntity entity = null;
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.LivingEntity living && e.getUUID().equals(playerUUID)) {
                        entity = living;
                        break;
                    }
                }

                if (entity != null) {
                    // Apply animation state to whip physics
                    com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics.playAnimation(
                        entity,
                        animationName,
                        extensionLevel,
                        10 // Default transition ticks
                    );

                    // Note: We don't set animation tick directly to avoid jittery interpolation
                    // The client will smoothly interpolate from current state
                }
            }
        });
    }

    // Getters for packet data
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getAnimationName() {
        return animationName;
    }

    public double getExtensionLevel() {
        return extensionLevel;
    }

    public int getAnimationTick() {
        return animationTick;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
