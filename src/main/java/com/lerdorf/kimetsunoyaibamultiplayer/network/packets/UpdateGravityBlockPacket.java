package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateGravityBlockPacket {
    private final BlockPos blockPos;
    private final BlockPos startOffset;
    private final BlockPos size;
    private final Direction gravityDirection;

    public UpdateGravityBlockPacket(BlockPos blockPos, BlockPos startOffset, BlockPos size, Direction gravityDirection) {
        this.blockPos = blockPos;
        this.startOffset = startOffset;
        this.size = size;
        this.gravityDirection = gravityDirection == null ? Direction.DOWN : gravityDirection;
    }

    public UpdateGravityBlockPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readEnum(Direction.class));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeBlockPos(startOffset);
        buffer.writeBlockPos(size);
        buffer.writeEnum(gravityDirection);
    }

    public static boolean handle(UpdateGravityBlockPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(message.blockPos.getX() + 0.5D, message.blockPos.getY() + 0.5D, message.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (!player.isCreative()) {
                return;
            }
            if (player.level().getBlockEntity(message.blockPos) instanceof GravityBlockEntity gravityBlock) {
                if (!GravityBlockEntity.isHoldingGravityBlock(player)) {
                    return;
                }
                gravityBlock.applySettings(message.startOffset, message.size, message.gravityDirection);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
