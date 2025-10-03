package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class AnimationSyncPacket {

    private final UUID playerUUID;
    private final ResourceLocation animationId;
    private final int currentTick;
    private final int animationLength;
    private final boolean isLooping;
    private final boolean stopAnimation;
    private final KeyframeAnimation animationData; // The actual animation
    private final ItemStack swordItem; // The sword being used for particle effects
    private final ResourceLocation particleType; // The particle type to spawn

    public AnimationSyncPacket(UUID playerUUID, ResourceLocation animationId, int currentTick, int animationLength, boolean isLooping, boolean stopAnimation, KeyframeAnimation animationData) {
        this.playerUUID = playerUUID;
        this.animationId = animationId;
        this.currentTick = currentTick;
        this.animationLength = animationLength;
        this.isLooping = isLooping;
        this.stopAnimation = stopAnimation;
        this.animationData = animationData;
        this.swordItem = ItemStack.EMPTY; // Default for backward compatibility
        this.particleType = null; // Default for backward compatibility
    }

    public AnimationSyncPacket(UUID playerUUID, ResourceLocation animationId, int currentTick, int animationLength, boolean isLooping, boolean stopAnimation, KeyframeAnimation animationData, ItemStack swordItem, ResourceLocation particleType) {
        this.playerUUID = playerUUID;
        this.animationId = animationId;
        this.currentTick = currentTick;
        this.animationLength = animationLength;
        this.isLooping = isLooping;
        this.stopAnimation = stopAnimation;
        this.animationData = animationData;
        this.swordItem = swordItem != null ? swordItem : ItemStack.EMPTY;
        this.particleType = particleType;
    }

    // Constructor for stop packets
    public AnimationSyncPacket(UUID playerUUID, ResourceLocation animationId, int currentTick, int animationLength, boolean isLooping, boolean stopAnimation) {
        this(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation, null);
    }

    public AnimationSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        boolean hasAnimation = buf.readBoolean();

        if (hasAnimation) {
            this.animationId = buf.readResourceLocation();
            this.currentTick = buf.readVarInt();
            this.animationLength = buf.readVarInt();
            this.isLooping = buf.readBoolean();
            this.stopAnimation = buf.readBoolean();

            // Read sword item and particle data
            boolean hasSwordData = buf.readBoolean();
            if (hasSwordData) {
                this.swordItem = buf.readItem();
                this.particleType = buf.readResourceLocation();
            } else {
                this.swordItem = ItemStack.EMPTY;
                this.particleType = null;
            }

            // For now, we can't easily serialize KeyframeAnimation
            // So we'll just pass null and work with the IDs
            this.animationData = null;
        } else {
            this.animationId = null;
            this.currentTick = 0;
            this.animationLength = 0;
            this.isLooping = false;
            this.stopAnimation = true;
            this.animationData = null;
            this.swordItem = ItemStack.EMPTY;
            this.particleType = null;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (Config.logDebug) {
            Log.debug("Writing packet to buffer: player={}, animation={}, tick={}, stop={}",
                playerUUID, animationId, currentTick, stopAnimation);
        }
        buf.writeUUID(playerUUID);

        if (animationId != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(animationId);
            buf.writeVarInt(currentTick);
            buf.writeVarInt(animationLength);
            buf.writeBoolean(isLooping);
            buf.writeBoolean(stopAnimation);

            // Write sword item and particle data
            boolean hasSwordData = !swordItem.isEmpty() && particleType != null;
            buf.writeBoolean(hasSwordData);
            if (hasSwordData) {
                buf.writeItem(swordItem);
                buf.writeResourceLocation(particleType);
            }
        } else {
            buf.writeBoolean(false);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    if (Config.logDebug) {
                        Log.info("Server received animation sync from player {}: animation={}, tick={}, stop={}",
                            sender.getName().getString(), animationId, currentTick, stopAnimation);
                    }

                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                        new AnimationSyncPacket(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation, animationData, swordItem, particleType),
                        sender
                    );

                    if (Config.logDebug) {
                        Log.info("Server relayed animation sync to all other clients");
                    }
                }
            } else {
                if (Config.logDebug) {
                    Log.info("Client received animation sync for player {}: animation={}, tick={}, stop={}",
                        playerUUID, animationId, currentTick, stopAnimation);
                }
                AnimationSyncHandler.handleAnimationSync(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation, animationData, swordItem, particleType);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    public static AnimationSyncPacket createStopPacket(UUID playerUUID) {
        return new AnimationSyncPacket(playerUUID, null, 0, 0, false, true);
    }
}