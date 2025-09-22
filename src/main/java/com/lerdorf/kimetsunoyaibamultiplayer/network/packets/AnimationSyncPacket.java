package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class AnimationSyncPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final UUID playerUUID;
    private final ResourceLocation animationId;
    private final int currentTick;
    private final int animationLength;
    private final boolean isLooping;
    private final boolean stopAnimation;
    private final KeyframeAnimation animationData; // The actual animation

    public AnimationSyncPacket(UUID playerUUID, ResourceLocation animationId, int currentTick, int animationLength, boolean isLooping, boolean stopAnimation, KeyframeAnimation animationData) {
        this.playerUUID = playerUUID;
        this.animationId = animationId;
        this.currentTick = currentTick;
        this.animationLength = animationLength;
        this.isLooping = isLooping;
        this.stopAnimation = stopAnimation;
        this.animationData = animationData;
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
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        if (Config.logDebug) {
            LOGGER.debug("Writing packet to buffer: player={}, animation={}, tick={}, stop={}",
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
                        LOGGER.info("Server received animation sync from player {}: animation={}, tick={}, stop={}",
                            sender.getName().getString(), animationId, currentTick, stopAnimation);
                    }

                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                        new AnimationSyncPacket(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation),
                        sender
                    );

                    if (Config.logDebug) {
                        LOGGER.info("Server relayed animation sync to all other clients");
                    }
                }
            } else {
                if (Config.logDebug) {
                    LOGGER.info("Client received animation sync for player {}: animation={}, tick={}, stop={}",
                        playerUUID, animationId, currentTick, stopAnimation);
                }
                AnimationSyncHandler.handleAnimationSync(playerUUID, animationId, currentTick, animationLength, isLooping, stopAnimation, animationData);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    public static AnimationSyncPacket createStopPacket(UUID playerUUID) {
        return new AnimationSyncPacket(playerUUID, null, 0, 0, false, true);
    }
}