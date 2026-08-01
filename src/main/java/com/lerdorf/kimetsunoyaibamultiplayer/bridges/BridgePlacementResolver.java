package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public final class BridgePlacementResolver {
    private BridgePlacementResolver() {
    }

    public static List<PlacedBlock> resolveRepeats(BlockPos origin, Direction facing, BridgeMovement movement,
                                                   BridgeType type, SourceStructure structure, int length) {
        int segmentLength = segmentLength(type, structure);
        BlockPos localOriginOffset = localOriginOffset(movement, structure);
        int repeats = Math.max(1, (int) Math.ceil((double) length / (double) segmentLength));
        List<PlacedBlock> placed = new ArrayList<>();

        for (int repeat = 0; repeat < repeats; repeat++) {
            addRepeat(placed, origin, facing, movement, type, structure, localOriginOffset, segmentLength, repeat, length);
        }

        return placed;
    }

    public static List<PlacedBlock> resolveEndcap(BlockPos origin, Direction facing, BridgeMovement movement,
                                                  BridgeType type, SourceStructure segmentStructure,
                                                  SourceStructure endcap, int length, boolean shortEndcap) {
        List<PlacedBlock> placed = new ArrayList<>();
        addEndcap(placed, origin, facing, movement, type, localOriginOffset(movement, segmentStructure), length, endcap, shortEndcap);
        return placed;
    }

    public static List<BlockPos> resolveEndcapVolume(BlockPos origin, Direction facing, BridgeMovement movement,
                                                     BridgeType type, SourceStructure segmentStructure,
                                                     SourceStructure endcap, int length, boolean shortEndcap) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos localOriginOffset = localOriginOffset(movement, segmentStructure);
        for (int x = 0; x < endcap.size().getX(); x++) {
            for (int y = 0; y < endcap.size().getY(); y++) {
                for (int z = 0; z < endcap.size().getZ(); z++) {
                    positions.add(resolveEndcapPos(origin, facing, movement, type, localOriginOffset, length, endcap,
                        shortEndcap, new BlockPos(x, y, z)));
                }
            }
        }
        return positions;
    }

    public static int segmentLength(BridgeType type, SourceStructure structure) {
        int raw = switch (type.authoringAxis()) {
            case POSITIVE_Y, POSITIVE_Y_POSITIVE_Z -> structure.size().getY();
            default -> structure.size().getX();
        };
        return Math.max(1, raw);
    }

    private static void addRepeat(List<PlacedBlock> placed, BlockPos origin, Direction facing, BridgeMovement movement,
                                  BridgeType type, SourceStructure structure, BlockPos localOriginOffset,
                                  int segmentLength, int repeat, int length) {
        Rotation rotation = facing.getAxis().isHorizontal() ? BridgeTransforms.rotationFromEast(facing) : Rotation.NONE;
        BlockPos originOffset = transformLocal(type, localOriginOffset, facing);
        BlockPos repeatOffset = repeatOffset(facing, movement, segmentLength, repeat);
        UnaryOperator<BlockPos> localTransform = local -> transformLocal(type, local, facing);

        for (SourceBlock block : structure.blocks()) {
            int forward = forwardCoordinate(type.authoringAxis(), block.localPos());
            int generatedForward = forward + repeat * segmentLength;
            if (generatedForward >= length) {
                continue;
            }

            BlockPos worldPos = origin
                .offset(originOffset)
                .offset(localTransform.apply(block.localPos()))
                .offset(repeatOffset);
            addBlock(placed, worldPos, block, rotation);
        }
    }

    private static void addEndcap(List<PlacedBlock> placed, BlockPos origin, Direction facing, BridgeMovement movement,
                                  BridgeType type, BlockPos localOriginOffset, int length, SourceStructure endcap,
                                  boolean shortEndcap) {
        Rotation rotation = facing.getAxis().isHorizontal()
            ? BridgeTransforms.rotationFromEast(facing)
            : Rotation.NONE;
        BlockPos originOffset = transformLocal(type, localOriginOffset, facing);
        BlockPos endOffset = repeatOffset(facing, movement, length, 1);
        BlockPos localEndcapOffset = shortEndcap
            ? BridgeTypes.shortEndOffset(type, movement)
            : BridgeTypes.endOffset(type, movement);
        BlockPos localEndcapOriginOffset = localEndcapOriginOffset(type, movement, endcap);
        UnaryOperator<BlockPos> localTransform = local -> transformEndcapLocal(type, movement, local, facing);

        for (SourceBlock block : endcap.blocks()) {
            BlockPos worldPos = resolveEndcapPos(origin, originOffset, endOffset, localEndcapOriginOffset,
                localEndcapOffset, localTransform, block.localPos());
            addBlock(placed, worldPos, block, rotation);
        }
    }

    private static BlockPos resolveEndcapPos(BlockPos origin, Direction facing, BridgeMovement movement,
                                             BridgeType type, BlockPos localOriginOffset, int length,
                                             SourceStructure endcap, boolean shortEndcap, BlockPos localPos) {
        BlockPos originOffset = transformLocal(type, localOriginOffset, facing);
        BlockPos endOffset = repeatOffset(facing, movement, length, 1);
        BlockPos localEndcapOffset = shortEndcap
            ? BridgeTypes.shortEndOffset(type, movement)
            : BridgeTypes.endOffset(type, movement);
        BlockPos localEndcapOriginOffset = localEndcapOriginOffset(type, movement, endcap);
        UnaryOperator<BlockPos> localTransform = local -> transformEndcapLocal(type, movement, local, facing);
        return resolveEndcapPos(origin, originOffset, endOffset, localEndcapOriginOffset, localEndcapOffset,
            localTransform, localPos);
    }

    private static BlockPos resolveEndcapPos(BlockPos origin, BlockPos originOffset, BlockPos endOffset,
                                             BlockPos localEndcapOriginOffset, BlockPos localEndcapOffset,
                                             UnaryOperator<BlockPos> localTransform, BlockPos localPos) {
        return origin
            .offset(originOffset)
            .offset(endOffset)
            .offset(localEndcapOriginOffset)
            .offset(localTransform.apply(localEndcapOffset))
            .offset(localTransform.apply(localPos));
    }

    private static void addBlock(List<PlacedBlock> placed, BlockPos pos, SourceBlock block, Rotation rotation) {
        BlockState state = block.state().rotate(rotation);
        if (!state.isAir()) {
            placed.add(new PlacedBlock(pos, state, block.nbt() == null ? null : block.nbt().copy()));
        }
    }

    private static BlockPos repeatOffset(Direction facing, BridgeMovement movement, int segmentLength, int repeat) {
        int amount = segmentLength * repeat;
        return switch (movement) {
            case HORIZONTAL -> new BlockPos(facing.getStepX() * amount, 0, facing.getStepZ() * amount);
            case STAIR_UP -> new BlockPos(facing.getStepX() * amount, amount, facing.getStepZ() * amount);
            case STAIR_DOWN -> new BlockPos(facing.getStepX() * amount, -amount, facing.getStepZ() * amount);
            case VERTICAL_UP -> new BlockPos(0, amount, 0);
            case VERTICAL_DOWN -> new BlockPos(0, -amount, 0);
            case VERTICAL_STAIR_UP -> new BlockPos(0, amount, amount);
            case VERTICAL_STAIR_DOWN -> new BlockPos(0, amount, -amount);
            case SIDE_STAIR_UP -> sideStairOffset(facing, amount, amount);
            case SIDE_STAIR_DOWN -> sideStairOffset(facing, amount, -amount);
        };
    }

    private static BlockPos sideStairOffset(Direction facing, int forward, int sideways) {
        return switch (facing) {
            case EAST -> new BlockPos(forward, 0, sideways);
            case WEST -> new BlockPos(-forward, 0, -sideways);
            case SOUTH -> new BlockPos(-sideways, 0, forward);
            case NORTH -> new BlockPos(sideways, 0, -forward);
            default -> new BlockPos(forward, 0, sideways);
        };
    }

    private static int forwardCoordinate(BridgeAuthoringAxis axis, BlockPos pos) {
        return switch (axis) {
            case POSITIVE_Y, POSITIVE_Y_POSITIVE_Z -> pos.getY();
            default -> pos.getX();
        };
    }

    private static BlockPos transformLocal(BridgeType type, BlockPos local, Direction facing) {
        return switch (type.authoringAxis()) {
            case POSITIVE_X, POSITIVE_X_POSITIVE_Y, POSITIVE_X_POSITIVE_Z -> BridgeTransforms.transformHorizontalLocal(local, facing);
            case POSITIVE_Y -> BridgeTransforms.transformVerticalLocal(local, facing);
            case POSITIVE_Y_POSITIVE_Z -> local;
        };
    }

    private static BlockPos transformEndcapLocal(BridgeType type, BridgeMovement movement, BlockPos local, Direction facing) {
        if (keepsVerticalDownEndcapUpright(type, movement)) {
            return local;
        }
        return transformLocal(type, local, facing);
    }

    private static BlockPos localEndcapOriginOffset(BridgeType type, BridgeMovement movement, SourceStructure endcap) {
        if (keepsVerticalDownEndcapUpright(type, movement)) {
            return new BlockPos(0, 1 - endcap.size().getY(), 0);
        }
        return BlockPos.ZERO;
    }

    private static boolean keepsVerticalDownEndcapUpright(BridgeType type, BridgeMovement movement) {
        return movement == BridgeMovement.VERTICAL_DOWN
            && type.authoringAxis() == BridgeAuthoringAxis.POSITIVE_Y
            && !type.mirrorVerticalDown();
    }

    private static BlockPos localOriginOffset(BridgeMovement movement, SourceStructure structure) {
        return switch (movement) {
            case STAIR_DOWN -> new BlockPos(0, -structure.size().getY(), 0);
            case VERTICAL_STAIR_DOWN, SIDE_STAIR_DOWN -> new BlockPos(0, 0, -structure.size().getZ());
            default -> BlockPos.ZERO;
        };
    }

    public record SourceStructure(BlockPos size, List<SourceBlock> blocks) {
    }

    public record SourceBlock(BlockPos localPos, BlockState state, CompoundTag nbt) {
    }

    public record PlacedBlock(BlockPos pos, BlockState state, CompoundTag nbt) {
    }
}
