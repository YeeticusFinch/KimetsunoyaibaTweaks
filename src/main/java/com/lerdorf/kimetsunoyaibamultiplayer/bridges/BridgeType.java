package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public record BridgeType(
    ResourceLocation id,
    ResourceLocation segmentStructure,
    ResourceLocation endStructure,
    ResourceLocation shortEndStructure,
    BridgeMovement defaultMovement,
    BridgeAuthoringAxis authoringAxis,
    int defaultMaxLength,
    int minLength,
    BlockPos endOffset,
    BlockPos shortEndOffset,
    BlockPos downEndOffset,
    BlockPos downShortEndOffset,
    BridgeMovement[] allowedMovements,
    boolean mirrorVerticalDown
) {
    public BridgeType {
        allowedMovements = allowedMovements == null || allowedMovements.length == 0
            ? defaultAllowedMovements(defaultMovement)
            : allowedMovements.clone();
    }

    public BridgeType(
        ResourceLocation id,
        ResourceLocation segmentStructure,
        ResourceLocation endStructure,
        ResourceLocation shortEndStructure,
        BridgeMovement defaultMovement,
        BridgeAuthoringAxis authoringAxis,
        int defaultMaxLength,
        int minLength,
        BlockPos endOffset,
        BlockPos shortEndOffset
    ) {
        this(id, segmentStructure, endStructure, shortEndStructure, defaultMovement, authoringAxis,
            defaultMaxLength, minLength, endOffset, shortEndOffset, endOffset, shortEndOffset,
            defaultAllowedMovements(defaultMovement), true);
    }

    public BridgeType(
        ResourceLocation id,
        ResourceLocation segmentStructure,
        ResourceLocation endStructure,
        ResourceLocation shortEndStructure,
        BridgeMovement defaultMovement,
        BridgeAuthoringAxis authoringAxis,
        int defaultMaxLength,
        int minLength,
        BlockPos endOffset,
        BlockPos shortEndOffset,
        BridgeMovement... allowedMovements
    ) {
        this(id, segmentStructure, endStructure, shortEndStructure, defaultMovement, authoringAxis,
            defaultMaxLength, minLength, endOffset, shortEndOffset, endOffset, shortEndOffset,
            allowedMovements, true);
    }

    public BridgeType(
        ResourceLocation id,
        ResourceLocation segmentStructure,
        ResourceLocation endStructure,
        ResourceLocation shortEndStructure,
        BridgeMovement defaultMovement,
        BridgeAuthoringAxis authoringAxis,
        int defaultMaxLength,
        int minLength,
        BlockPos endOffset,
        BlockPos shortEndOffset,
        boolean mirrorVerticalDown,
        BridgeMovement... allowedMovements
    ) {
        this(id, segmentStructure, endStructure, shortEndStructure, defaultMovement, authoringAxis,
            defaultMaxLength, minLength, endOffset, shortEndOffset, endOffset, shortEndOffset,
            allowedMovements, mirrorVerticalDown);
    }

    public BridgeType(
        ResourceLocation id,
        ResourceLocation segmentStructure,
        ResourceLocation endStructure,
        ResourceLocation shortEndStructure,
        BridgeMovement defaultMovement,
        BridgeAuthoringAxis authoringAxis,
        int defaultMaxLength,
        int minLength,
        BlockPos endOffset,
        BlockPos shortEndOffset,
        BlockPos downEndOffset,
        BlockPos downShortEndOffset
    ) {
        this(id, segmentStructure, endStructure, shortEndStructure, defaultMovement, authoringAxis,
            defaultMaxLength, minLength, endOffset, shortEndOffset, downEndOffset, downShortEndOffset,
            defaultAllowedMovements(defaultMovement), true);
    }

    private static BridgeMovement[] defaultAllowedMovements(BridgeMovement defaultMovement) {
        return switch (defaultMovement) {
            case STAIR_UP, STAIR_DOWN -> new BridgeMovement[] { BridgeMovement.STAIR_UP, BridgeMovement.STAIR_DOWN };
            case VERTICAL_UP, VERTICAL_DOWN -> new BridgeMovement[] { BridgeMovement.VERTICAL_UP, BridgeMovement.VERTICAL_DOWN };
            case VERTICAL_STAIR_UP, VERTICAL_STAIR_DOWN -> new BridgeMovement[] { BridgeMovement.VERTICAL_STAIR_UP, BridgeMovement.VERTICAL_STAIR_DOWN };
            case SIDE_STAIR_UP, SIDE_STAIR_DOWN -> new BridgeMovement[] { BridgeMovement.SIDE_STAIR_UP, BridgeMovement.SIDE_STAIR_DOWN };
            default -> new BridgeMovement[] { BridgeMovement.HORIZONTAL };
        };
    }
}
