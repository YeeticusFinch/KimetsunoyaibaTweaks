package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
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
 * Now includes per-sword positions and hotbar slot tracking
 */
public class SwordDisplaySyncPacket {

    private final UUID playerUUID;
    private final ItemStack leftHipSword;
    private final ItemStack rightHipSword;
    private final SwordDisplayConfig.SwordDisplayPosition leftPosition;
    private final SwordDisplayConfig.SwordDisplayPosition rightPosition;
    private final int leftSlot;
    private final int rightSlot;
    private final ItemStack leftSheath;
    private final ItemStack rightSheath;
    private final ItemStack leftSheathSword;
    private final ItemStack rightSheathSword;
    private final boolean leftSheathVisible;
    private final boolean rightSheathVisible;
    private final SwordDisplayConfig.SwordDisplayPosition leftSheathPosition;
    private final SwordDisplayConfig.SwordDisplayPosition rightSheathPosition;

    public SwordDisplaySyncPacket(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword,
                                  SwordDisplayConfig.SwordDisplayPosition leftPosition,
                                  SwordDisplayConfig.SwordDisplayPosition rightPosition,
                                  int leftSlot, int rightSlot) {
        this(playerUUID, leftHipSword, rightHipSword, leftPosition, rightPosition, leftSlot, rightSlot,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, false, false,
            leftPosition, rightPosition);
    }

    public SwordDisplaySyncPacket(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword,
                                  SwordDisplayConfig.SwordDisplayPosition leftPosition,
                                  SwordDisplayConfig.SwordDisplayPosition rightPosition,
                                  int leftSlot, int rightSlot,
                                  ItemStack leftSheath, ItemStack rightSheath,
                                  ItemStack leftSheathSword, ItemStack rightSheathSword,
                                  boolean leftSheathVisible, boolean rightSheathVisible,
                                  SwordDisplayConfig.SwordDisplayPosition leftSheathPosition,
                                  SwordDisplayConfig.SwordDisplayPosition rightSheathPosition) {
        this.playerUUID = playerUUID;
        this.leftHipSword = leftHipSword != null ? leftHipSword : ItemStack.EMPTY;
        this.rightHipSword = rightHipSword != null ? rightHipSword : ItemStack.EMPTY;
        this.leftPosition = leftPosition != null ? leftPosition : SwordDisplayConfig.SwordDisplayPosition.HIP;
        this.rightPosition = rightPosition != null ? rightPosition : SwordDisplayConfig.SwordDisplayPosition.HIP;
        this.leftSlot = leftSlot;
        this.rightSlot = rightSlot;
        this.leftSheath = leftSheath != null ? leftSheath : ItemStack.EMPTY;
        this.rightSheath = rightSheath != null ? rightSheath : ItemStack.EMPTY;
        this.leftSheathSword = leftSheathSword != null ? leftSheathSword : ItemStack.EMPTY;
        this.rightSheathSword = rightSheathSword != null ? rightSheathSword : ItemStack.EMPTY;
        this.leftSheathVisible = leftSheathVisible;
        this.rightSheathVisible = rightSheathVisible;
        this.leftSheathPosition = leftSheathPosition != null ? leftSheathPosition : this.leftPosition;
        this.rightSheathPosition = rightSheathPosition != null ? rightSheathPosition : this.rightPosition;
    }

    public SwordDisplaySyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.leftHipSword = buf.readItem();
        this.rightHipSword = buf.readItem();
        this.leftPosition = buf.readEnum(SwordDisplayConfig.SwordDisplayPosition.class);
        this.rightPosition = buf.readEnum(SwordDisplayConfig.SwordDisplayPosition.class);
        this.leftSlot = buf.readInt();
        this.rightSlot = buf.readInt();
        this.leftSheath = buf.readItem();
        this.rightSheath = buf.readItem();
        this.leftSheathSword = buf.readItem();
        this.rightSheathSword = buf.readItem();
        this.leftSheathVisible = buf.readBoolean();
        this.rightSheathVisible = buf.readBoolean();
        this.leftSheathPosition = buf.readEnum(SwordDisplayConfig.SwordDisplayPosition.class);
        this.rightSheathPosition = buf.readEnum(SwordDisplayConfig.SwordDisplayPosition.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeItem(leftHipSword);
        buf.writeItem(rightHipSword);
        buf.writeEnum(leftPosition);
        buf.writeEnum(rightPosition);
        buf.writeInt(leftSlot);
        buf.writeInt(rightSlot);
        buf.writeItem(leftSheath);
        buf.writeItem(rightSheath);
        buf.writeItem(leftSheathSword);
        buf.writeItem(rightSheathSword);
        buf.writeBoolean(leftSheathVisible);
        buf.writeBoolean(rightSheathVisible);
        buf.writeEnum(leftSheathPosition);
        buf.writeEnum(rightSheathPosition);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                // Server received update from client - relay to all other clients
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    if (Config.logDebug) {
                        Log.info("Server received sword display sync from player {}: left={}@{} ({}), right={}@{} ({})",
                            sender.getName().getString(),
                            leftHipSword.isEmpty() ? "empty" : leftHipSword.getItem().toString(),
                            leftSlot, leftPosition,
                            rightHipSword.isEmpty() ? "empty" : rightHipSword.getItem().toString(),
                            rightSlot, rightPosition);
                    }

                    // Relay to all other clients
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                        new SwordDisplaySyncPacket(playerUUID, leftHipSword, rightHipSword,
                            leftPosition, rightPosition, leftSlot, rightSlot,
                            leftSheath, rightSheath, leftSheathSword, rightSheathSword,
                            leftSheathVisible, rightSheathVisible, leftSheathPosition, rightSheathPosition),
                        sender
                    );

                    if (Config.logDebug) {
                        Log.info("Server relayed sword display sync to all other clients");
                    }
                }
            } else {
                // Client received update - use DistExecutor to safely call client-only code
                net.minecraftforge.api.distmarker.Dist clientDist = net.minecraftforge.api.distmarker.Dist.CLIENT;
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(clientDist, () -> () -> {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.updateRemotePlayerDisplay(
                        playerUUID, leftHipSword, rightHipSword,
                        leftPosition, rightPosition, leftSlot, rightSlot,
                        leftSheath, rightSheath, leftSheathSword, rightSheathSword,
                        leftSheathVisible, rightSheathVisible, leftSheathPosition, rightSheathPosition
                    );
                });
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
