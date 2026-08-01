package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public enum BridgeMovement {
    HORIZONTAL,
    STAIR_UP,
    STAIR_DOWN,
    VERTICAL_UP,
    VERTICAL_DOWN,
    VERTICAL_STAIR_UP,
    VERTICAL_STAIR_DOWN,
    SIDE_STAIR_UP,
    SIDE_STAIR_DOWN;

    public boolean isVertical() {
        return this == VERTICAL_UP || this == VERTICAL_DOWN || this == VERTICAL_STAIR_UP || this == VERTICAL_STAIR_DOWN;
    }

    public boolean isHorizontalFacing() {
        return this == HORIZONTAL || this == STAIR_UP || this == STAIR_DOWN || this == SIDE_STAIR_UP || this == SIDE_STAIR_DOWN;
    }

    public BlockPos step(Direction facing) {
        return switch (this) {
            case HORIZONTAL -> new BlockPos(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            case STAIR_UP -> new BlockPos(facing.getStepX(), facing.getStepY() + 1, facing.getStepZ());
            case STAIR_DOWN -> new BlockPos(facing.getStepX(), facing.getStepY() - 1, facing.getStepZ());
            case VERTICAL_UP -> new BlockPos(0, 1, 0);
            case VERTICAL_DOWN -> new BlockPos(0, -1, 0);
            case VERTICAL_STAIR_UP -> new BlockPos(0, 1, 1);
            case VERTICAL_STAIR_DOWN -> new BlockPos(0, 1, -1);
            case SIDE_STAIR_UP -> rotateSideStep(facing, 1, 1);
            case SIDE_STAIR_DOWN -> rotateSideStep(facing, 1, -1);
        };
    }

    private static BlockPos rotateSideStep(Direction facing, int forward, int sideways) {
        return switch (facing) {
            case EAST -> new BlockPos(forward, 0, sideways);
            case WEST -> new BlockPos(-forward, 0, -sideways);
            case SOUTH -> new BlockPos(-sideways, 0, forward);
            case NORTH -> new BlockPos(sideways, 0, -forward);
            default -> new BlockPos(forward, 0, sideways);
        };
    }
}
