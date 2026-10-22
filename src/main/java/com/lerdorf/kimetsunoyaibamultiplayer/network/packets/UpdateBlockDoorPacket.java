package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.BlockDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateBlockDoorPacket {
    private final BlockPos blockPos;
    private final BlockPos cornerA;
    private final BlockPos cornerB;
    private final Direction facing;

    public UpdateBlockDoorPacket(BlockPos blockPos, BlockPos cornerA, BlockPos cornerB, Direction facing) {
        this.blockPos = blockPos;
        this.cornerA = cornerA;
        this.cornerB = cornerB;
        this.facing = facing == null ? Direction.NORTH : facing;
    }

    public UpdateBlockDoorPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readEnum(Direction.class));
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(blockPos);
        buffer.writeBlockPos(cornerA);
        buffer.writeBlockPos(cornerB);
        buffer.writeEnum(facing);
    }

    public static boolean handle(UpdateBlockDoorPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isCreative()
                || player.distanceToSqr(message.blockPos.getX() + 0.5D, message.blockPos.getY() + 0.5D,
                    message.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(message.blockPos) instanceof BlockDoorBlockEntity blockDoor) {
                blockDoor.applySettings(message.cornerA, message.cornerB, message.facing);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
