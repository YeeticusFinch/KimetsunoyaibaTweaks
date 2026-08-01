package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BridgerBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BridgeStructureMaterializer {
    private static final Map<ResourceLocation, BridgePlacementResolver.SourceStructure> STRUCTURE_CACHE = new HashMap<>();
    private static final double PILLAR_MAX_LENGTH_VARIATION = 0.5D;
    private static final double PILLAR_1_MIDDLE_ENDCAP_CHANCE = 0.1D;

    private BridgeStructureMaterializer() {
    }

    public static List<StructureTemplate.StructureBlockInfo> expandProcessedBridgers(ServerLevelAccessor level,
                                                                                     StructurePlaceSettings settings,
                                                                                     List<StructureTemplate.StructureBlockInfo> processedInfos) {
        List<BridgeMarker> markers = new ArrayList<>();
        List<StructureTemplate.StructureBlockInfo> expanded = new ArrayList<>(processedInfos.size());

        for (StructureTemplate.StructureBlockInfo info : processedInfos) {
            if (info.state().is(ModBlocks.BRIDGER_BLOCK.get())) {
                markers.add(readMarker(settings, info));
            } else {
                expanded.add(info);
            }
        }

        if (markers.isEmpty()) {
            return processedInfos;
        }

        RandomSource random = level.getRandom();
        Set<BlockPos> consumedMarkers = new HashSet<>();
        for (BridgeMarker marker : markers) {
            if (consumedMarkers.contains(marker.pos())) {
                continue;
            }
            Expansion expansion = materialize(settings, marker, markers, consumedMarkers, random);
            expanded.addAll(expansion.blocks());
            logExpansion(marker, expansion);
        }

        return expanded;
    }

    private static BridgeMarker readMarker(StructurePlaceSettings settings, StructureTemplate.StructureBlockInfo info) {
        CompoundTag tag = info.nbt() == null ? new CompoundTag() : info.nbt();
        BridgeType type = BridgeTypes.getOrDefault(tag.contains("BridgeType") ? ResourceLocation.tryParse(tag.getString("BridgeType")) : BridgeTypes.BRIDGE_1_ID);
        BridgeMovement movement = BridgeTypes.sanitizeMovement(type, readMovement(tag.getString("Movement"), type.defaultMovement()));
        int maxLength = Mth.clamp(tag.contains("MaxLength") ? tag.getInt("MaxLength") : type.defaultMaxLength(), 1, 512);
        int minLength = Mth.clamp(tag.contains("MinLength") ? tag.getInt("MinLength") : type.minLength(), 1, maxLength);
        boolean allowEndcap = !tag.contains("AllowEndcap") || tag.getBoolean("AllowEndcap");
        boolean allowShortEndcap = !tag.contains("AllowShortEndcap") || tag.getBoolean("AllowShortEndcap");
        boolean allowConnectToOpposite = !tag.contains("AllowConnectToOpposite") || tag.getBoolean("AllowConnectToOpposite");

        BlockState finalMarkerState = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
        Direction facing = finalMarkerState.hasProperty(BridgerBlock.FACING)
            ? finalMarkerState.getValue(BridgerBlock.FACING)
            : Direction.EAST;

        movement = restrictPillarMovement(type, movement);
        facing = sanitizeFacing(type, movement, facing);
        return new BridgeMarker(info.pos(), type, movement, facing, minLength, maxLength, allowEndcap, allowShortEndcap, allowConnectToOpposite);
    }

    private static Expansion materialize(StructurePlaceSettings settings, BridgeMarker marker, List<BridgeMarker> markers,
                                         Set<BlockPos> consumedMarkers, RandomSource random) {
        BridgePlacementResolver.SourceStructure structure = load(BridgeTypes.segmentStructure(marker.type(), marker.movement()));
        if (structure == null || structure.blocks().isEmpty()) {
            return new Expansion(List.of(), 0, EndcapMode.NONE, false);
        }

        int maxLength = adjustedMaxLength(marker.type(), marker.minLength(), marker.maxLength(), random);
        ConnectedLength connectedLength = resolveLength(marker, markers, consumedMarkers, maxLength);
        if (connectedLength.connectedMarker() != null) {
            consumedMarkers.add(connectedLength.connectedMarker());
        }

        int length = connectedLength.length();
        List<StructureTemplate.StructureBlockInfo> generated = new ArrayList<>();

        for (BridgePlacementResolver.PlacedBlock block : BridgePlacementResolver.resolveRepeats(
            marker.pos(), marker.facing(), marker.movement(), marker.type(), structure, length)) {
            addBlock(generated, settings, block);
        }

        boolean middlePillar1Endcap = addRandomMiddlePillar1Endcap(generated, settings, marker, structure, length, random);
        EndcapMode endcapMode = addEndcap(generated, settings, marker, structure, length,
            marker.allowEndcap(), marker.allowShortEndcap());

        return new Expansion(generated, length, endcapMode, middlePillar1Endcap);
    }

    private static int adjustedMaxLength(BridgeType type, int minLength, int maxLength, RandomSource random) {
        if (!isRandomizedPillar(type)) {
            return maxLength;
        }
        double variation = (random.nextDouble() * 2.0D - 1.0D) * PILLAR_MAX_LENGTH_VARIATION;
        int adjusted = (int) Math.round(maxLength + variation * maxLength);
        return Mth.clamp(adjusted, minLength, 512);
    }

    private static boolean isRandomizedPillar(BridgeType type) {
        return type.id().equals(BridgeTypes.PILLAR_1_ID) || type.id().equals(BridgeTypes.PILLAR_2_ID);
    }

    private static ConnectedLength resolveLength(BridgeMarker marker, List<BridgeMarker> markers,
                                                 Set<BlockPos> consumedMarkers, int maxLength) {
        if (!marker.allowConnectToOpposite()) {
            return new ConnectedLength(maxLength, null);
        }

        for (int length = Math.max(1, marker.minLength()); length <= maxLength; length++) {
            BlockPos endpoint = marker.pos().offset(repeatOffset(marker.facing(), marker.movement(), length, 1));
            for (BridgeMarker other : markers) {
                if (other == marker || consumedMarkers.contains(other.pos()) || !endpoint.equals(other.pos())) {
                    continue;
                }
                if (marker.type().id().equals(other.type().id())
                    && marker.movement() == other.movement()
                    && other.facing() == marker.facing().getOpposite()) {
                    return new ConnectedLength(length, other.pos());
                }
            }
        }
        return new ConnectedLength(maxLength, null);
    }

    private static EndcapMode addEndcap(List<StructureTemplate.StructureBlockInfo> generated, StructurePlaceSettings settings,
                                        BridgeMarker marker, BridgePlacementResolver.SourceStructure segmentStructure,
                                        int length, boolean allowEndcap, boolean allowShortEndcap) {
        if (!allowEndcap && !allowShortEndcap) {
            return EndcapMode.NONE;
        }

        boolean shortEndcap = !allowEndcap && allowShortEndcap;
        BridgePlacementResolver.SourceStructure endcap = shortEndcap
            ? loadShortEndcap(marker.type(), marker.movement())
            : load(BridgeTypes.endStructure(marker.type(), marker.movement()));
        if (endcap == null || endcap.blocks().isEmpty()) {
            return EndcapMode.NONE;
        }

        for (BridgePlacementResolver.PlacedBlock block : BridgePlacementResolver.resolveEndcap(
            marker.pos(), marker.facing(), marker.movement(), marker.type(), segmentStructure, endcap, length, shortEndcap)) {
            addBlock(generated, settings, block);
        }
        return shortEndcap ? EndcapMode.SHORT : EndcapMode.FULL;
    }

    private static boolean addRandomMiddlePillar1Endcap(List<StructureTemplate.StructureBlockInfo> generated,
                                                        StructurePlaceSettings settings, BridgeMarker marker,
                                                        BridgePlacementResolver.SourceStructure segmentStructure,
                                                        int length, RandomSource random) {
        if (!marker.type().id().equals(BridgeTypes.PILLAR_1_ID) || marker.movement() != BridgeMovement.VERTICAL_DOWN) {
            return false;
        }
        if (random.nextDouble() >= PILLAR_1_MIDDLE_ENDCAP_CHANCE) {
            return false;
        }

        BridgePlacementResolver.SourceStructure endcap = load(BridgeTypes.PILLAR_1.endStructure());
        if (endcap == null || endcap.blocks().isEmpty()) {
            return false;
        }

        int middleLength = Math.max(1, length / 2);
        removeGeneratedBlocks(generated, BridgePlacementResolver.resolveEndcapVolume(
            marker.pos(), marker.facing(), marker.movement(), marker.type(), segmentStructure, endcap,
            middleLength, false));

        for (BridgePlacementResolver.PlacedBlock block : BridgePlacementResolver.resolveEndcap(
            marker.pos(), marker.facing(), marker.movement(), marker.type(), segmentStructure, endcap,
            middleLength, false)) {
            addBlock(generated, settings, block);
        }
        return true;
    }

    private static void removeGeneratedBlocks(List<StructureTemplate.StructureBlockInfo> generated, List<BlockPos> positions) {
        Set<BlockPos> positionSet = new HashSet<>(positions);
        generated.removeIf(info -> positionSet.contains(info.pos()));
    }

    private static void addBlock(List<StructureTemplate.StructureBlockInfo> generated, StructurePlaceSettings settings,
                                 BridgePlacementResolver.PlacedBlock block) {
        CompoundTag nbt = block.nbt() == null ? null : block.nbt().copy();
        if (nbt != null) {
            nbt.putInt("x", block.pos().getX());
            nbt.putInt("y", block.pos().getY());
            nbt.putInt("z", block.pos().getZ());
        }

        generated.add(new StructureTemplate.StructureBlockInfo(
            block.pos(),
            stateBeforeVanillaTransform(block.state(), settings),
            nbt
        ));
    }

    private static BlockState stateBeforeVanillaTransform(BlockState finalState, StructurePlaceSettings settings) {
        for (BlockState possible : finalState.getBlock().getStateDefinition().getPossibleStates()) {
            if (possible.mirror(settings.getMirror()).rotate(settings.getRotation()).equals(finalState)) {
                return possible;
            }
        }
        return finalState;
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

    private static BridgePlacementResolver.SourceStructure loadShortEndcap(BridgeType type, BridgeMovement movement) {
        BridgePlacementResolver.SourceStructure structure = load(BridgeTypes.shortEndStructure(type, movement));
        if (structure != null) {
            return structure;
        }
        ResourceLocation alternate = BridgeTypes.alternateShortEndStructure(type, movement);
        if (!alternate.equals(BridgeTypes.shortEndStructure(type, movement))) {
            return load(alternate);
        }
        return null;
    }

    private static BridgePlacementResolver.SourceStructure load(ResourceLocation structureId) {
        if (STRUCTURE_CACHE.containsKey(structureId)) {
            return STRUCTURE_CACHE.get(structureId);
        }

        String path = structureResourcePath("assets", structureId);
        try (InputStream stream = BridgeStructureMaterializer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                STRUCTURE_CACHE.put(structureId, null);
                return null;
            }
            BridgePlacementResolver.SourceStructure structure = parse(NbtIo.readCompressed(stream));
            STRUCTURE_CACHE.put(structureId, structure);
            return structure;
        } catch (Exception ignored) {
            STRUCTURE_CACHE.put(structureId, null);
            return null;
        }
    }

    private static String structureResourcePath(String root, ResourceLocation structureId) {
        return root + "/" + structureId.getNamespace() + "/structures/" + structureId.getPath() + ".nbt";
    }

    private static BridgePlacementResolver.SourceStructure parse(CompoundTag tag) {
        ListTag sizeTag = tag.getList("size", 3);
        BlockPos size = new BlockPos(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
        List<BlockState> palette = parsePalette(tag.getList("palette", 10));
        List<BridgePlacementResolver.SourceBlock> blocks = new ArrayList<>();
        ListTag blockList = tag.getList("blocks", 10);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag blockTag = blockList.getCompound(i);
            ListTag posTag = blockTag.getList("pos", 3);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) {
                continue;
            }

            BlockState state = palette.get(stateIndex);
            if (!state.isAir()) {
                CompoundTag nbt = blockTag.contains("nbt", 10) ? blockTag.getCompound("nbt").copy() : null;
                blocks.add(new BridgePlacementResolver.SourceBlock(
                    new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2)),
                    state,
                    nbt
                ));
            }
        }
        return new BridgePlacementResolver.SourceStructure(size, blocks);
    }

    private static List<BlockState> parsePalette(ListTag paletteTag) {
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            ResourceLocation blockId = ResourceLocation.tryParse(stateTag.getString("Name"));
            Block block = blockId == null ? Blocks.AIR : ForgeRegistries.BLOCKS.getValue(blockId);
            BlockState state = block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
            if (stateTag.contains("Properties", 10)) {
                state = applyProperties(state, stateTag.getCompound("Properties"));
            }
            palette.add(state);
        }
        return palette;
    }

    private static BlockState applyProperties(BlockState state, CompoundTag propertiesTag) {
        for (String key : propertiesTag.getAllKeys()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property != null) {
                state = setProperty(state, property, propertiesTag.getString(key));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(t -> state.setValue(property, t)).orElse(state);
    }

    private static BridgeMovement readMovement(String name, BridgeMovement fallback) {
        try {
            return BridgeMovement.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static BridgeMovement restrictPillarMovement(BridgeType type, BridgeMovement movement) {
        if (type.id().equals(BridgeTypes.PILLAR_1_ID) || type.id().equals(BridgeTypes.PILLAR_2_ID)) {
            return BridgeMovement.VERTICAL_DOWN;
        }
        return movement;
    }

    private static Direction sanitizeFacing(BridgeType type, BridgeMovement movement, Direction facing) {
        BridgeMovement sanitizedMovement = BridgeTypes.sanitizeMovement(type, movement);
        if (sanitizedMovement == BridgeMovement.VERTICAL_UP || sanitizedMovement == BridgeMovement.VERTICAL_STAIR_UP) {
            return Direction.UP;
        }
        if (sanitizedMovement == BridgeMovement.VERTICAL_DOWN || sanitizedMovement == BridgeMovement.VERTICAL_STAIR_DOWN) {
            return Direction.DOWN;
        }
        return facing.getAxis().isHorizontal() ? facing : Direction.EAST;
    }

    private static void logExpansion(BridgeMarker marker, Expansion expansion) {
        Log.debug("[Bridger] Expanded type={} marker={} movement={} length={} endcap={} middlePillar1Endcap={}",
            marker.type().id(), marker.pos(), marker.movement(), expansion.length(), expansion.endcapMode().name().toLowerCase(),
            expansion.middlePillar1Endcap());
    }

    private enum EndcapMode {
        NONE,
        FULL,
        SHORT
    }

    private record BridgeMarker(BlockPos pos, BridgeType type, BridgeMovement movement, Direction facing,
                                int minLength, int maxLength, boolean allowEndcap, boolean allowShortEndcap,
                                boolean allowConnectToOpposite) {
    }

    private record ConnectedLength(int length, BlockPos connectedMarker) {
    }

    private record Expansion(List<StructureTemplate.StructureBlockInfo> blocks, int length, EndcapMode endcapMode,
                             boolean middlePillar1Endcap) {
    }
}
