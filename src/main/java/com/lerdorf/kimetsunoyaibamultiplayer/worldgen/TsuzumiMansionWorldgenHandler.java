package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Blends generated quest structures into the surface and decorates Tsuzumi Mansion sites. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class TsuzumiMansionWorldgenHandler {
    private static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "tsuzumi_mansion");
    private static final ResourceLocation HEADQUARTERS_ID = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "headquarters");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SPRUCE_TREE = ResourceKey.create(
        Registries.CONFIGURED_FEATURE,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "tsuzumi_spruce_tree"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK_TREE = ResourceKey.create(
        Registries.CONFIGURED_FEATURE,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "tsuzumi_dark_oak_tree"));
    private static final int BLEND_RADIUS = 14;
    private static final int TREE_ATTEMPTS = 32;
    private static final Set<Long> PENDING_SITES = new HashSet<>();

    private TsuzumiMansionWorldgenHandler() {
    }

    /** Replays the large HQ piece for the chunk currently being loaded. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onHeadquartersChunkLoad(ChunkEvent.Load event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
                return;
            }

            Structure headquarters = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(HEADQUARTERS_ID);
            if (headquarters == null) {
                return;
            }

            ChunkPos chunkPos = event.getChunk().getPos();
            BoundingBox chunkBox = new BoundingBox(
                chunkPos.getMinBlockX(), level.getMinBuildHeight() + 1, chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunkPos.getMaxBlockZ());
            for (StructureStart start : level.structureManager().startsForStructure(
                SectionPos.of(chunkPos, level.getMinSection()), headquarters)) {
                if (start == null || !start.isValid() || !start.getBoundingBox().intersects(chunkBox)) {
                    continue;
                }

                List<StructurePiece> pieces = start.getPieces();
                if (pieces.isEmpty()) {
                    continue;
                }
                BoundingBox firstBox = pieces.get(0).getBoundingBox();
                BlockPos placementAnchor = new BlockPos(
                    firstBox.getCenter().getX(), firstBox.minY(), firstBox.getCenter().getZ());
                for (StructurePiece piece : pieces) {
                    if (piece instanceof PoolElementStructurePiece poolPiece
                        && piece.getBoundingBox().intersects(chunkBox)) {
                        poolPiece.place(level, level.structureManager(),
                            level.getChunkSource().getGenerator(), level.random, chunkBox,
                            placementAnchor, false);
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("[Headquarters] Error placing structure piece: " + exception);
        }
    }

    // Structure lookup from ChunkEvent.Load can synchronously request neighboring chunks
    // while those chunks are being generated, causing a server-thread deadlock.
    public static void onChunkLoad(ChunkEvent.Load event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
                return;
            }

            net.minecraft.world.level.chunk.ChunkAccess chunk = event.getChunk();
            ChunkPos chunkPos = chunk.getPos();
            int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();
            for (int dx = 0; dx < 16; dx += 4) {
                for (int dz = 0; dz < 16; dz += 4) {
                    int x = minX + dx;
                    int z = minZ + dz;
                    int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
                    findMansionStart(level, new BlockPos(x, y, z));
                }
            }
        } catch (Exception exception) {
            System.err.println("[Tsuzumi Mansion] Error checking loaded chunk: " + exception);
        }
    }

    private static void findMansionStart(ServerLevel level, net.minecraft.core.BlockPos probe) {
        findStructureStart(level, probe, STRUCTURE_ID, "Tsuzumi Mansion");
    }

    private static void findStructureStart(ServerLevel level, net.minecraft.core.BlockPos probe,
                                           ResourceLocation structureId, String structureName) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Holder<Structure> holder : registry.holders().toList()) {
            ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
            if (!structureId.equals(id)) {
                continue;
            }
            try {
                StructureStart start = level.structureManager().getStructureAt(probe, holder.value());
                if (start != null && start.isValid()) {
                    BoundingBox box = start.getBoundingBox();
                    long siteKey = siteKey(box);
                    if (PENDING_SITES.add(siteKey)) {
                        level.getServer().execute(() -> {
                            try {
                                decorate(level, box);
                            } finally {
                                PENDING_SITES.remove(siteKey);
                            }
                        });
                    }
                }
            } catch (Exception exception) {
                System.err.println("[" + structureName + "] Error locating structure: " + exception);
            }
            return;
        }
    }

    private static void decorate(ServerLevel level, BoundingBox box) {
        TsuzumiMansionSavedData data = TsuzumiMansionSavedData.get(level);
        long key = siteKey(box);
        if (data.sites.contains(key)) {
            return;
        }

        try {
            removeBiomeVegetation(level, box);
            prepareBlend(level, box, BLEND_RADIUS);
            growTrees(level, box);
            data.sites.add(key);
            data.setDirty();
        } catch (Exception exception) {
            System.err.println("[Tsuzumi Mansion] Error decorating " + box + ": " + exception);
        }
    }

    private static void removeBiomeVegetation(ServerLevel level, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                // Vegetation is surface-level; never scan the full structure height.
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int minY = Math.max(box.minY(), surfaceY - 2);
                int maxY = Math.min(box.maxY(), surfaceY + 1);
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isBiomeVegetation(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean isBiomeVegetation(BlockState state) {
        return state.is(Blocks.GRASS)
            || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.FERN)
            || state.is(Blocks.LARGE_FERN)
            || state.is(Blocks.DEAD_BUSH);
    }

    private static void prepareBlend(ServerLevel level, BoundingBox box, int blendRadius) {
        int minX = box.minX() - blendRadius;
        int maxX = box.maxX() + blendRadius;
        int minZ = box.minZ() - blendRadius;
        int maxZ = box.maxZ() + blendRadius;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                    continue;
                }
                int distance = horizontalDistanceToBox(box, x, z);
                if (distance > blendRadius) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (y <= level.getMinBuildHeight() + 2 || !level.getFluidState(new net.minecraft.core.BlockPos(x, y, z)).isEmpty()) {
                    continue;
                }
                net.minecraft.core.BlockPos top = new net.minecraft.core.BlockPos(x, y, z);
                level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(top.below(), Blocks.DIRT.defaultBlockState(), 3);
                if (distance < blendRadius / 2) {
                    level.setBlock(top.below(2), Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void growTrees(ServerLevel level, BoundingBox box) {
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        Optional<Holder.Reference<ConfiguredFeature<?, ?>>> spruce = registry.getHolder(SPRUCE_TREE);
        Optional<Holder.Reference<ConfiguredFeature<?, ?>>> darkOak = registry.getHolder(DARK_OAK_TREE);
        if (spruce.isEmpty() || darkOak.isEmpty()) {
            return;
        }

        RandomSource random = RandomSource.create(siteKey(box) ^ 0x5A17C0DE4B1DL);
        int width = box.getXSpan() + BLEND_RADIUS * 2 + 1;
        int depth = box.getZSpan() + BLEND_RADIUS * 2 + 1;
        for (int attempt = 0; attempt < TREE_ATTEMPTS; attempt++) {
            int x = box.minX() - BLEND_RADIUS + random.nextInt(Math.max(1, width));
            int z = box.minZ() - BLEND_RADIUS + random.nextInt(Math.max(1, depth));
            if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(x, y, z);
            if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                || !level.getBlockState(ground.above()).isAir()) {
                continue;
            }
            Holder<ConfiguredFeature<?, ?>> feature = attempt % 2 == 0 ? spruce.get() : darkOak.get();
            feature.value().place(level, level.getChunkSource().getGenerator(), random, ground.above());
        }
    }

    private static int horizontalDistanceToBox(BoundingBox box, int x, int z) {
        int dx = Math.max(Math.max(box.minX() - x, 0), x - box.maxX());
        int dz = Math.max(Math.max(box.minZ() - z, 0), z - box.maxZ());
        return Math.max(dx, dz);
    }

    private static long siteKey(BoundingBox box) {
        return net.minecraft.core.BlockPos.asLong(box.minX(), box.minY(), box.minZ());
    }

    private static final class TsuzumiMansionSavedData extends SavedData {
        private static final String DATA_NAME = "kimetsunoyaibamultiplayer_tsuzumi_mansions";
        private static final int DATA_VERSION = 2;
        private final Set<Long> sites = new HashSet<>();

        private static TsuzumiMansionSavedData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                TsuzumiMansionSavedData::load,
                TsuzumiMansionSavedData::new,
                DATA_NAME);
        }

        private static TsuzumiMansionSavedData load(CompoundTag tag) {
            TsuzumiMansionSavedData data = new TsuzumiMansionSavedData();
            if (tag.getInt("version") != DATA_VERSION) {
                return data;
            }
            ListTag sites = tag.getList("sites", Tag.TAG_LONG_ARRAY);
            for (int i = 0; i < sites.size(); i++) {
                long[] values = sites.getLongArray(i);
                if (values.length > 0) {
                    data.sites.add(values[0]);
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("version", DATA_VERSION);
            ListTag sites = new ListTag();
            for (Long site : this.sites) {
                sites.add(new net.minecraft.nbt.LongArrayTag(new long[] {site}));
            }
            tag.put("sites", sites);
            return tag;
        }
    }
}
