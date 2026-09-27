package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoTerrainContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the active Natagumo source available while NoiseChunk builds vanilla density functions. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NatagumoTerrainMixin {
    private static final int SURFACE_REPAIR_MAX_SLOPE = 2;
    private static final int SURFACE_REPAIR_DEPTH = 3;
    private static final int NATAGUMO_MAX_SURFACE_Y = 280;
    private static final int SURFACE_PROTECTION_DEPTH = 10;
    private static final int RIVER_SURFACE_PROTECTION_DEPTH = 18;
    private static final double RIVER_PROTECTION_RADIUS = 12.0D;
    private static final int RIVER_HEADROOM = 5;

    @Inject(method = "createNoiseChunk", at = @At("HEAD"))
    private void kimetsu$startNatagumoDensity(
            net.minecraft.world.level.chunk.ChunkAccess chunk,
            net.minecraft.world.level.StructureManager structureManager,
            net.minecraft.world.level.levelgen.blending.Blender blender,
            RandomState randomState,
            CallbackInfoReturnable<?> cir) {
        if (!(((NoiseBasedChunkGenerator) (Object) this).getBiomeSource()
                instanceof EnhancedMountBiomeSource enhancedSource)) {
            return;
        }
        NatagumoTerrainContext.set(enhancedSource, randomState.sampler());
    }

    @Inject(method = "createNoiseChunk", at = @At("RETURN"))
    private void kimetsu$finishNatagumoDensity(CallbackInfoReturnable<?> cir) {
        NatagumoTerrainContext.clear();
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/levelgen/RandomState;"
            + "Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("HEAD"))
    private void kimetsu$protectNatagumoSurface(WorldGenRegion region, StructureManager structureManager,
                                                 RandomState randomState, ChunkAccess chunk,
                                                 CallbackInfo callbackInfo) {
        BiomeSource biomeSource = ((NoiseBasedChunkGenerator) (Object) this).getBiomeSource();
        if (!(biomeSource instanceof EnhancedMountBiomeSource enhancedSource)) {
            return;
        }
        protectNatagumoSurface(region, randomState, enhancedSource, chunk);
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;"
            + "Lnet/minecraft/world/level/StructureManager;"
            + "Lnet/minecraft/world/level/levelgen/RandomState;"
            + "Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("RETURN"))
    private void kimetsu$applyNatagumoSurfaceAndRiver(WorldGenRegion region,
                                                       StructureManager structureManager,
                                                       RandomState randomState, ChunkAccess chunk,
                                                       CallbackInfo callbackInfo) {
        BiomeSource biomeSource = ((NoiseBasedChunkGenerator) (Object) this).getBiomeSource();
        if (!(biomeSource instanceof EnhancedMountBiomeSource enhancedSource)) {
            return;
        }

        applySurfaceRepair(region, randomState, enhancedSource, chunk);
        applyRiverColumns(region, randomState, enhancedSource, chunk);
    }

    private static void protectNatagumoSurface(WorldGenRegion region, RandomState randomState,
                                                EnhancedMountBiomeSource source, ChunkAccess chunk) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int maxBuildY = chunk.getMinBuildHeight() + chunk.getHeight() - 1;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                double profile = source.natagumoTerrainProfile(blockX, blockZ, randomState.sampler());
                if (profile <= 0.0) {
                    continue;
                }

                // The density translation targets this height, but this block-level guard keeps
                // an inaccurate vanilla-height estimate from producing Y=320 plateaus.
                for (int y = maxBuildY; y > NATAGUMO_MAX_SURFACE_Y; y--) {
                    position.set(blockX, y, blockZ);
                    if (!chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, Blocks.AIR.defaultBlockState(), false);
                    }
                }

                int finalSurfaceY = (int) Math.floor(source.natagumoTargetSurfaceHeight(
                        blockX, blockZ, profile));
                int protectionDepth = source.riverDistance(blockX, blockZ)
                        < RIVER_PROTECTION_RADIUS
                        ? RIVER_SURFACE_PROTECTION_DEPTH
                        : SURFACE_PROTECTION_DEPTH;
                int bottomY = Math.max(region.getMinBuildHeight(), finalSurfaceY - protectionDepth);
                int topY = Math.min(maxBuildY, finalSurfaceY - 1);
                for (int y = bottomY; y <= topY; y++) {
                    position.set(blockX, y, blockZ);
                    if (chunk.getBlockState(position).isAir()) {
                        chunk.setBlockState(position, Blocks.STONE.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void applySurfaceRepair(WorldGenRegion region, RandomState randomState,
                                           EnhancedMountBiomeSource source, ChunkAccess chunk) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                double profile = source.natagumoTerrainProfile(blockX, blockZ, randomState.sampler());
                if (profile <= 0.0 || localX == 0 || localX == 15 || localZ == 0 || localZ == 15) {
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
                    chunk.setBlockState(position, Blocks.DIRT.defaultBlockState(), false);
                }
            }
        }
    }

    private static void applyRiverColumns(WorldGenRegion region, RandomState randomState,
                                           EnhancedMountBiomeSource source, ChunkAccess chunk) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(0, 0, 0);
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int maxBuildY = chunk.getMinBuildHeight() + chunk.getHeight() - 1;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = minBlockX + localX;
                int blockZ = minBlockZ + localZ;
                if (!source.isRiverColumn(blockX, blockZ, randomState.sampler())) {
                    continue;
                }

                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ) - 1;
                int waterY = surfaceY - 1;
                int bedY = surfaceY - 2;
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
        int west = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX - 1, localZ) - 1;
        int east = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX + 1, localZ) - 1;
        int north = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ - 1) - 1;
        int south = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ + 1) - 1;
        int slopeX = Math.abs(east - west) / 2;
        int slopeZ = Math.abs(south - north) / 2;
        return Math.max(slopeX, slopeZ) > SURFACE_REPAIR_MAX_SLOPE;
    }
}
