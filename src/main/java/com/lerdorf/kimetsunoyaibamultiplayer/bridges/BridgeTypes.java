package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;

public final class BridgeTypes {
    public static final ResourceLocation BRIDGE_1_ID = id("bridge1");
    public static final ResourceLocation BRIDGE_2_ID = id("bridge2");
    public static final ResourceLocation BRIDGE_3_ID = id("bridge3");
    public static final ResourceLocation STAIR_1_ID = id("stair1");
    public static final ResourceLocation STAIR_2_ID = id("stair2");
    public static final ResourceLocation VERT_BRIDGE_1_ID = id("vert_bridge1");
    public static final ResourceLocation VERT_STAIR_2_ID = id("vert_stair2");
    public static final ResourceLocation PILLAR_1_ID = id("pillar1");
    public static final ResourceLocation PILLAR_2_ID = id("pillar2");
    public static final ResourceLocation FLIPPED_BRIDGE_1_ID = id("flipped_bridge1");
    public static final ResourceLocation SIDE_BRIDGE_1_ID = id("side_bridge1");
    public static final ResourceLocation SIDE_BRIDGE_2_ID = id("side_bridge2");
    public static final ResourceLocation SIDE_BRIDGE_3_ID = id("side_bridge3");
    public static final ResourceLocation SIDE_STAIR_1_ID = id("side_stair1");
    public static final ResourceLocation SIDE_STAIR_2_ID = id("side_stair2");

    public static final BridgeType BRIDGE_1 = new BridgeType(
        BRIDGE_1_ID,
        id("bridge1"),
        id("bridge1_end"),
        id("bridge1_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, 0, -2), // full endcap offset: 2 left in original +X orientation
        BlockPos.ZERO        // short endcap offset
    );

    public static final BridgeType BRIDGE_2 = new BridgeType(
        BRIDGE_2_ID,
        id("bridge2"),
        id("bridge2_end"),
        id("bridge2_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, 0, -1), // full endcap offset: 2 left in original +X orientation
        BlockPos.ZERO        // short endcap offset
    );

    public static final BridgeType BRIDGE_3 = new BridgeType(
        BRIDGE_3_ID,
        id("bridge3"),
        id("bridge3_end"),
        id("bridge3_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, 0, -2), // full endcap offset: 2 left in original +X orientation
        BlockPos.ZERO        // short endcap offset
    );

    public static final BridgeType STAIR_1 = new BridgeType(
        STAIR_1_ID,
        id("stair1"),
        id("stair1_end"),
        id("stair1_endshort"),
        BridgeMovement.STAIR_UP,
        BridgeAuthoringAxis.POSITIVE_X_POSITIVE_Y,
        96,
        4,
        new BlockPos(0, 0, -2), // full endcap offset: 2 left in original +X orientation
        new BlockPos(0, 0, 0), // short endcap offset
        new BlockPos(0, 0, -2), // full endcap offset for down stair
        new BlockPos(0, -1, 0) // short endcap offset for down stair
    );

    public static final BridgeType STAIR_2 = new BridgeType(
        STAIR_2_ID,
        id("stair2"),
        id("stair2_end"),
        id("stair2_endshort"),
        BridgeMovement.STAIR_UP,
        BridgeAuthoringAxis.POSITIVE_X_POSITIVE_Y,
        96,
        4,
        new BlockPos(0, 0, -1), // full endcap offset: 2 left in original +X orientation
        new BlockPos(0, 0, 0),        // short endcap offset
        new BlockPos(0, 2, -1), // full endcap offset for down stair
        new BlockPos(0, 3, 0) // short endcap offset for down stair
    );

    public static final BridgeType VERT_BRIDGE_1 = new BridgeType(
        VERT_BRIDGE_1_ID,
        id("vert_bridge1"),
        id("vert_bridge1_end"),
        id("vert_bridge1_endshort"),
        BridgeMovement.VERTICAL_UP,
        BridgeAuthoringAxis.POSITIVE_Y,
        128,
        4,
         new BlockPos(-2, 0, 0), // full endcap offset: 2 left in original +X orientation
        new BlockPos(0, 0, 0)        // short endcap offset
    );

    public static final BridgeType VERT_STAIR_2 = new BridgeType(
        VERT_STAIR_2_ID,
        id("vert_stair2"),
        id("vert_stair2_end"),
        id("vert_stair2_endshort"),
        BridgeMovement.VERTICAL_STAIR_UP,
        BridgeAuthoringAxis.POSITIVE_Y_POSITIVE_Z,
        128,
        4,
         new BlockPos(-1, 0, 0), // full endcap offset: 2 left in original +X orientation
        new BlockPos(0, 0, 0),        // short endcap offset
        new BlockPos(-1, 0, 2), // full endcap offset for down stair
        new BlockPos(0, 0, 3) // short endcap offset for down stair
    );

    public static final BridgeType PILLAR_1 = new BridgeType(
        PILLAR_1_ID,
        id("pillar1"),
        id("pillar1_end"),
        id("pillar1_endshort"),
        BridgeMovement.VERTICAL_DOWN,
        BridgeAuthoringAxis.POSITIVE_Y,
        128,
        4,
        new BlockPos(0, 0, 0), // full endcap offset
        BlockPos.ZERO, // short endcap offset
        false,
        BridgeMovement.VERTICAL_DOWN
    );

    public static final BridgeType PILLAR_2 = new BridgeType(
        PILLAR_2_ID,
        id("pillar2"),
        id("pillar2_end"),
        id("pillar2_endshort"),
        BridgeMovement.VERTICAL_DOWN,
        BridgeAuthoringAxis.POSITIVE_Y,
        128,
        4,
        new BlockPos(-1, 0, -1),
        BlockPos.ZERO,
        false,
        BridgeMovement.VERTICAL_DOWN
    );

    public static final BridgeType FLIPPED_BRIDGE_1 = new BridgeType(
        FLIPPED_BRIDGE_1_ID,
        id("flipped_bridge1"),
        id("flipped_bridge1_end"),
        id("flipped_bridge1_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, -2, -2),
        new BlockPos(0, 1, 0)
    );

    public static final BridgeType SIDE_BRIDGE_1 = new BridgeType(
        SIDE_BRIDGE_1_ID,
        id("side_bridge1"),
        id("side_bridge1_end"),
        id("side_bridge1_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, -2, 0),
        new BlockPos(0, 0, 0)
    );

    public static final BridgeType SIDE_BRIDGE_2 = new BridgeType(
        SIDE_BRIDGE_2_ID,
        id("side_bridge2"),
        id("side_bridge2_end"),
        id("side_bridge2_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, -1, 0),
        BlockPos.ZERO
    );

    public static final BridgeType SIDE_BRIDGE_3 = new BridgeType(
        SIDE_BRIDGE_3_ID,
        id("side_bridge3"),
        id("side_bridge3_end"),
        id("side_bridge3_endshort"),
        BridgeMovement.HORIZONTAL,
        BridgeAuthoringAxis.POSITIVE_X,
        128,
        4,
        new BlockPos(0, -2, 0),
        BlockPos.ZERO
    );

    public static final BridgeType SIDE_STAIR_1 = new BridgeType(
        SIDE_STAIR_1_ID,
        id("side_stair1"),
        id("side_stair1_end"),
        id("side_stair1_endshort"),
        BridgeMovement.SIDE_STAIR_UP,
        BridgeAuthoringAxis.POSITIVE_X_POSITIVE_Z,
        96,
        4,
        new BlockPos(0, -2, 0),
        new BlockPos(0, 0, 0),
        new BlockPos(0, -2, 0),
        new BlockPos(0, 0, -1)
    );

    public static final BridgeType SIDE_STAIR_2 = new BridgeType(
        SIDE_STAIR_2_ID,
        id("side_stair2"),
        id("side_stair2_end"),
        id("side_stair2_endshort"),
        BridgeMovement.SIDE_STAIR_UP,
        BridgeAuthoringAxis.POSITIVE_X_POSITIVE_Z,
        96,
        4,
        new BlockPos(0, -1, 0),
        BlockPos.ZERO,
        new BlockPos(0, -1, 2),
        new BlockPos(0, 0, 3)
    );

    private static final Map<ResourceLocation, BridgeType> TYPES = new LinkedHashMap<>();

    static {
        register(BRIDGE_1);
        register(BRIDGE_2);
        register(BRIDGE_3);
        register(STAIR_1);
        register(STAIR_2);
        register(VERT_BRIDGE_1);
        register(VERT_STAIR_2);
        register(PILLAR_1);
        register(PILLAR_2);
        register(FLIPPED_BRIDGE_1);
        register(SIDE_BRIDGE_1);
        register(SIDE_BRIDGE_2);
        register(SIDE_BRIDGE_3);
        register(SIDE_STAIR_1);
        register(SIDE_STAIR_2);
    }

    private BridgeTypes() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, path);
    }

    private static void register(BridgeType type) {
        TYPES.put(type.id(), type);
    }

    public static BridgeType getOrDefault(ResourceLocation id) {
        return TYPES.getOrDefault(id, BRIDGE_1);
    }

    public static Optional<BridgeType> get(ResourceLocation id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    public static Collection<BridgeType> all() {
        return TYPES.values();
    }

    public static ResourceLocation segmentStructure(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement)
            ? id(type.segmentStructure().getPath() + "_down")
            : type.segmentStructure();
    }

    public static ResourceLocation endStructure(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement)
            ? id(type.segmentStructure().getPath() + "_down_end")
            : type.endStructure();
    }

    public static ResourceLocation shortEndStructure(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement)
            ? id(type.segmentStructure().getPath() + "_down_end_short")
            : type.shortEndStructure();
    }

    public static ResourceLocation alternateShortEndStructure(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement)
            ? id(type.segmentStructure().getPath() + "_down_endshort")
            : type.shortEndStructure();
    }

    public static BlockPos endOffset(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement) ? type.downEndOffset() : type.endOffset();
    }

    public static BlockPos shortEndOffset(BridgeType type, BridgeMovement movement) {
        return usesDownStructure(movement) ? type.downShortEndOffset() : type.shortEndOffset();
    }

    private static boolean usesDownStructure(BridgeMovement movement) {
        return movement == BridgeMovement.STAIR_DOWN
            || movement == BridgeMovement.VERTICAL_STAIR_DOWN
            || movement == BridgeMovement.SIDE_STAIR_DOWN;
    }

    public static BridgeMovement sanitizeMovement(BridgeType type, BridgeMovement movement) {
        for (BridgeMovement allowed : allowedMovements(type)) {
            if (allowed == movement) {
                return movement;
            }
        }
        return allowedMovements(type)[0];
    }

    public static BridgeMovement[] allowedMovements(BridgeType type) {
        return type.allowedMovements().clone();
    }
}
