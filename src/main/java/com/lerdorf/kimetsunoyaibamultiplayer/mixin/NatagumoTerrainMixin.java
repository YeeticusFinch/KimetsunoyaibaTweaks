package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoTerrainContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import org.apache.commons.lang3.mutable.MutableObject;
import java.util.OptionalInt;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;

/** Makes the active Natagumo source available while NoiseChunk builds vanilla density functions. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NatagumoTerrainMixin {
    private static final int SURFACE_REPAIR_MAX_SLOPE = 2;
    private static final int SURFACE_REPAIR_DEPTH = 3;
    private static final int RIVER_HEADROOM = 5;

    @WrapMethod(method = "createNoiseChunk")
    private NoiseChunk kimetsu$withNatagumoDensity(ChunkAccess chunk, StructureManager structureManager,
                                                  Blender blender, RandomState state,
                                                  Operation<NoiseChunk> original) {
        NatagumoTerrainContext.Context previous = NatagumoTerrainContext.get();
        NatagumoTerrainContext.clear();
        try {
            NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
            if (generator.getBiomeSource() instanceof EnhancedMountBiomeSource source
                    && source.mayAffectChunk(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), 16)) {
                NatagumoTerrainContext.set(source, state,
                        generator.generatorSettings().value().noiseSettings().clampToHeightAccessor(chunk), blender);
            }
            return original.call(chunk, structureManager, blender, state);
        } finally {
            NatagumoTerrainContext.restore(previous);
        }
    }

    @WrapMethod(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/levelgen/RandomState;"
            + "Lnet/minecraft/world/level/chunk/ChunkAccess;)V")
    private void kimetsu$surface(WorldGenRegion region, StructureManager structureManager,
                                 RandomState state, ChunkAccess chunk, Operation<Void> original) {
        if (!(((NoiseBasedChunkGenerator) (Object) this).getBiomeSource() instanceof EnhancedMountBiomeSource source)
                || !source.mayAffectChunk(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), 16)) {
            original.call(region, structureManager, state, chunk);
            return;
        }
        EnhancedMountBiomeSource.SurfaceColumn[] columns = new EnhancedMountBiomeSource.SurfaceColumn[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                columns[x * 16 + z] = source.surfaceColumn(chunk.getPos().getMinBlockX() + x,
                        chunk.getPos().getMinBlockZ() + z, state.sampler());
            }
        }
        protectNatagumoSurface(chunk, columns);
        original.call(region, structureManager, state, chunk);
        applySurfaceRepair(region, chunk, columns);
        applyRiverColumns(region, chunk, columns);
    }

    /** Structure height queries construct NoiseChunk directly, bypassing createNoiseChunk. */
    @WrapMethod(method = "iterateNoiseColumn")
    private OptionalInt kimetsu$heightQuery(LevelHeightAccessor height, RandomState state, int x, int z,
            MutableObject<NoiseColumn> column, Predicate<BlockState> predicate, Operation<OptionalInt> original) {
        NatagumoTerrainContext.Context previous = NatagumoTerrainContext.get();
        NatagumoTerrainContext.clear();
        try {
            NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
            if (generator.getBiomeSource() instanceof EnhancedMountBiomeSource source && source.mayAffectChunk(x, z, 1)) {
                NatagumoTerrainContext.set(source, state,
                        generator.generatorSettings().value().noiseSettings().clampToHeightAccessor(height), Blender.empty());
            }
            return original.call(height, state, x, z, column, predicate);
        } finally {
            NatagumoTerrainContext.restore(previous);
        }
    }

    private static void protectNatagumoSurface(ChunkAccess chunk, EnhancedMountBiomeSource.SurfaceColumn[] columns) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int maxBuildY = chunk.getMinBuildHeight() + chunk.getHeight() - 1;
        int maxSurfaceY = EnhancedMountBiomeSource.natagumoMaxSurfaceY();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                double profile = columns[localX * 16 + localZ].profile();
                if (profile <= 0.0) {
                    continue;
                }

                // Keep an inaccurate vanilla-height estimate from producing Y=320 plateaus.
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) - 1;
                if (surfaceY <= maxSurfaceY) {
                    continue;
                }
                for (int y = Math.min(maxBuildY, surfaceY); y > maxSurfaceY; y--) {
                    position.set(blockX, y, blockZ);
                    if (!chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void applySurfaceRepair(WorldGenRegion region, ChunkAccess chunk,
                                           EnhancedMountBiomeSource.SurfaceColumn[] columns) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                double profile = columns[localX * 16 + localZ].profile();
                if (profile <= 0.0) {
                    continue;
                }

                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) - 1;
                if (surfaceY < region.getMinBuildHeight()
                        || isSteepColumn(chunk, localX, localZ)) {
                    continue;
                }

                position.set(blockX, surfaceY, blockZ);
                BlockState top = chunk.getBlockState(position);
                if (!top.is(Blocks.STONE) && !top.is(Blocks.DIRT) && !top.is(Blocks.GRASS_BLOCK)) {
                    continue;
                }
                chunk.setBlockState(position, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                for (int depth = 1; depth <= SURFACE_REPAIR_DEPTH; depth++) {
                    position.set(blockX, surfaceY - depth, blockZ);
                    if (position.getY() < region.getMinBuildHeight()) {
                        break;
                    }
                    BlockState below = chunk.getBlockState(position);
                    if (!below.is(Blocks.STONE) && !below.is(Blocks.DIRT) && !below.is(Blocks.GRASS_BLOCK)) {
                        break;
                    }
                    chunk.setBlockState(position, Blocks.DIRT.defaultBlockState(), false);
                }
            }
        }
    }

    private static void applyRiverColumns(WorldGenRegion region, ChunkAccess chunk,
                                           EnhancedMountBiomeSource.SurfaceColumn[] columns) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int maxBuildY = chunk.getMinBuildHeight() + chunk.getHeight() - 1;
        int depth = EnhancedMountBiomeSource.riverDepth();
        if (depth == 0) return;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                if (!columns[localX * 16 + localZ].river()) {
                    continue;
                }

                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) - 1;
                int waterY = surfaceY - depth + 1;
                int bedY = surfaceY - depth;
                if (bedY < region.getMinBuildHeight()) {
                    continue;
                }

                for (int y = Math.max(region.getMinBuildHeight(), waterY + 1);
                     y <= Math.min(maxBuildY, waterY + RIVER_HEADROOM); y++) {
                    position.set(blockX, y, blockZ);
                    chunk.setBlockState(position, Blocks.AIR.defaultBlockState(), false);
                }
                position.set(blockX, bedY, blockZ);
                chunk.setBlockState(position, Blocks.GRAVEL.defaultBlockState(), false);
                position.set(blockX, waterY, blockZ);
                chunk.setBlockState(position, Blocks.WATER.defaultBlockState(), false);
                region.scheduleTick(position, Fluids.WATER, 1);
            }
        }
    }

    private static boolean isSteepColumn(ChunkAccess chunk, int localX, int localZ) {
        int westX = Math.max(0, localX - 1), eastX = Math.min(15, localX + 1);
        int northZ = Math.max(0, localZ - 1), southZ = Math.min(15, localZ + 1);
        int west = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, westX, localZ) - 1;
        int east = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, eastX, localZ) - 1;
        int north = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, northZ) - 1;
        int south = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, southZ) - 1;
        int slopeX = Math.abs(east - west) / (eastX - westX);
        int slopeZ = Math.abs(south - north) / (southZ - northZ);
        return Math.max(slopeX, slopeZ) > SURFACE_REPAIR_MAX_SLOPE;
    }

}
