package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.BridgerBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeMovement;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.EndcapPreviewMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateBridgerBlockPacket {
    private final BlockPos blockPos;
    private final ResourceLocation bridgeType;
    private final BridgeMovement movement;
    private final Direction facing;
    private final int maxLength;
    private final int minLength;
    private final boolean allowEndcap;
    private final boolean allowShortEndcap;
    private final boolean allowConnectToOpposite;
    private final boolean allowMerge;
    private final int priority;
    private final boolean previewEnabled;
    private final int previewLength;
    private final EndcapPreviewMode endcapPreviewMode;

    public UpdateBridgerBlockPacket(BlockPos blockPos, ResourceLocation bridgeType, BridgeMovement movement,
                                    Direction facing, int maxLength, int minLength, boolean allowEndcap,
                                    boolean allowShortEndcap, boolean allowConnectToOpposite, boolean allowMerge,
                                    int priority, boolean previewEnabled, int previewLength,
                                    EndcapPreviewMode endcapPreviewMode) {
        this.blockPos = blockPos;
        this.bridgeType = bridgeType;
        this.movement = movement;
        this.facing = facing;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.allowEndcap = allowEndcap;
        this.allowShortEndcap = allowShortEndcap;
        this.allowConnectToOpposite = allowConnectToOpposite;
        this.allowMerge = allowMerge;
        this.priority = priority;
        this.previewEnabled = previewEnabled;
        this.previewLength = previewLength;
        this.endcapPreviewMode = endcapPreviewMode;
    }

    public UpdateBridgerBlockPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(),
            buffer.readResourceLocation(),
            buffer.readEnum(BridgeMovement.class),
            buffer.readEnum(Direction.class),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readEnum(EndcapPreviewMode.class));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeResourceLocation(bridgeType);
        buffer.writeEnum(movement);
        buffer.writeEnum(facing);
        buffer.writeVarInt(maxLength);
        buffer.writeVarInt(minLength);
        buffer.writeBoolean(allowEndcap);
        buffer.writeBoolean(allowShortEndcap);
        buffer.writeBoolean(allowConnectToOpposite);
        buffer.writeBoolean(allowMerge);
        buffer.writeVarInt(priority);
        buffer.writeBoolean(previewEnabled);
        buffer.writeVarInt(previewLength);
        buffer.writeEnum(endcapPreviewMode);
    }

    public static boolean handle(UpdateBridgerBlockPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(message.blockPos.getX() + 0.5D, message.blockPos.getY() + 0.5D, message.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(message.blockPos) instanceof BridgerBlockEntity bridger) {
                bridger.applySettings(message.bridgeType, message.movement, message.facing, message.maxLength, message.minLength,
                    message.allowEndcap, message.allowShortEndcap, message.allowConnectToOpposite, message.allowMerge,
                    message.priority, message.previewEnabled, message.previewLength, message.endcapPreviewMode);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
