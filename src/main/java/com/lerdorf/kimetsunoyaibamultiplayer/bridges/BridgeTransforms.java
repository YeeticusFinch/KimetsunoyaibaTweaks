package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

public final class BridgeTransforms {
    private BridgeTransforms() {
    }

    public static BlockPos transformHorizontalLocal(BlockPos local, Direction facing) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();

        return switch (facing) {
            case EAST -> new BlockPos(x, y, z);
            case WEST -> new BlockPos(-x, y, -z);
            case SOUTH -> new BlockPos(-z, y, x);
            case NORTH -> new BlockPos(z, y, -x);
            default -> throw new IllegalArgumentException("Horizontal bridge must face horizontally");
        };
    }

    public static BlockPos transformVerticalLocal(BlockPos local, Direction facing) {
        if (facing == Direction.DOWN) {
            return new BlockPos(local.getX(), -local.getY(), local.getZ());
        }
        return local;
    }

    public static Rotation rotationFromEast(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.NONE;
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
