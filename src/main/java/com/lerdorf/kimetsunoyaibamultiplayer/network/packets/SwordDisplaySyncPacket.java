package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes sword display state between clients
 */
public class SwordDisplaySyncPacket {

    private final UUID playerUUID;
    private final ItemStack leftHipSword;
    private final ItemStack rightHipSword;
    private final SwordDisplayConfig.SwordDisplayPosition displayPosition;

    public SwordDisplaySyncPacket(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword,
                                  SwordDisplayConfig.SwordDisplayPosition displayPosition) {
        this.playerUUID = playerUUID;
        this.leftHipSword = leftHipSword != null ? leftHipSword : ItemStack.EMPTY;
        this.rightHipSword = rightHipSword != null ? rightHipSword : ItemStack.EMPTY;
        this.displayPosition = displayPosition != null ? displayPosition : SwordDisplayConfig.SwordDisplayPosition.HIP;
    }

    public SwordDisplaySyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.leftHipSword = buf.readItem();
        this.rightHipSword = buf.readItem();
        this.displayPosition = buf.readEnum(SwordDisplayConfig.SwordDisplayPosition.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeItem(leftHipSword);
        buf.writeItem(rightHipSword);
        buf.writeEnum(displayPosition);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                // Server received update from client - relay to all other clients
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    if (Config.logDebug) {
                        Log.info("Server received sword display sync from player {}: left={}, right={}, position={}",
                            sender.getName().getString(),
                            leftHipSword.isEmpty() ? "empty" : leftHipSword.getItem().toString(),
                            rightHipSword.isEmpty() ? "empty" : rightHipSword.getItem().toString(),
                            displayPosition);
                    }

                    // Relay to all other clients
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                        new SwordDisplaySyncPacket(playerUUID, leftHipSword, rightHipSword, displayPosition),
                        sender
                    );

                    if (Config.logDebug) {
                        Log.info("Server relayed sword display sync to all other clients");
                    }
                }
            } else {
                // Client received update - use proxy to handle
                KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleSwordDisplaySync(
                    playerUUID, leftHipSword, rightHipSword
                );
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
