package com.lerdorf.kimetsunoyaibamultiplayer.biome;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DissolutionCocoonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Naturally spawns persistent decorative cocoons near players in the Sister region. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class NatagumoSisterCocoonHandler {
    private static final int COCOONS_PER_REGION = 10;
    private static final int MAX_SPAWNS_PER_INTERVAL = 2;
    private static final int SPAWN_INTERVAL_TICKS = 20;
    private static final int SEARCH_RADIUS = 50;
    private static final int SEARCH_RADIUS_SQUARED = SEARCH_RADIUS * SEARCH_RADIUS;
    private static final int TREE_SCAN_DEPTH = 48;
    private static final int POSITION_SALT = 0x53494C4B;
    private static long tickCounter;

    private NatagumoSisterCocoonHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tickCounter % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        try {
            ServerLevel level = event.getServer().overworld();
            if (level == null || !EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
                return;
            }

            int spawned = 0;
            for (ServerPlayer player : level.players()) {
                if (spawned >= MAX_SPAWNS_PER_INTERVAL
                        || !isSisterRegion(level.getSeed(), player.blockPosition().getX(),
                        player.blockPosition().getZ())) {
                    continue;
                }

                List<DissolutionCocoonEntity> nearby = nearbyCocoons(level, player);
                int needed = COCOONS_PER_REGION - nearby.size();
                if (needed <= 0) {
                    continue;
                }

                List<BlockPos> positions = findSpawnPositions(level, player, nearby,
                        Math.min(needed, MAX_SPAWNS_PER_INTERVAL - spawned));
                for (BlockPos position : positions) {
                    DissolutionCocoonEntity cocoon = ModEntities.DISSOLUTION_COCOON.get().create(level);
                    if (cocoon == null) {
                        continue;
                    }
                    cocoon.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                            0.0F, 0.0F);
                    cocoon.setDecorational(true);
                    cocoon.setPersistenceRequired();
                    if (level.addFreshEntity(cocoon)) {
                        nearby.add(cocoon);
                        spawned++;
                    }
                    if (spawned >= MAX_SPAWNS_PER_INTERVAL) {
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            System.err.println("Failed to naturally spawn Natagumo Sister cocoons: " + exception.getMessage());
        }
    }

    private static List<DissolutionCocoonEntity> nearbyCocoons(ServerLevel level, ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        return level.getEntitiesOfClass(DissolutionCocoonEntity.class, searchBox,
                cocoon -> isWithinRadius(cocoon, player));
    }

    private static List<BlockPos> findSpawnPositions(ServerLevel level, ServerPlayer player,
                                                      List<DissolutionCocoonEntity> nearby,
                                                      int maximum) {
        long seed = level.getSeed();
        int minChunkX = Math.floorDiv((int) Math.floor(player.getX() - SEARCH_RADIUS), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(player.getX() + SEARCH_RADIUS), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(player.getZ() - SEARCH_RADIUS), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(player.getZ() + SEARCH_RADIUS), 16);
        List<BlockPos> treePositions = new ArrayList<>();
        List<BlockPos> surfacePositions = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                ChunkPos chunkPos = chunk.getPos();
                int minBlockX = chunkPos.getMinBlockX();
                int minBlockZ = chunkPos.getMinBlockZ();
                for (int localX = 0; localX < 16; localX += 2) {
                    for (int localZ = 0; localZ < 16; localZ += 2) {
                        int blockX = minBlockX + localX;
                        int blockZ = minBlockZ + localZ;
                        if (!isWithinRadius(blockX, blockZ, player)
                                || !isSisterRegion(seed, blockX, blockZ)) {
                            continue;
                        }

                        BlockPos ground = findGrassGround(chunk, blockX, blockZ, localX, localZ);
                        if (ground == null) {
                            continue;
                        }
                        BlockPos spawnPosition = ground.above();
                        if (!chunk.getBlockState(spawnPosition).isAir()
                                || tooCloseToCocoon(spawnPosition, nearby)
                                || !seen.add(spawnPosition.asLong())) {
                            continue;
                        }
                        if (hasTreeCover(chunk, blockX, blockZ, ground.getY())) {
                            treePositions.add(spawnPosition);
                        } else {
                            surfacePositions.add(spawnPosition);
                        }
                    }
                }
            }
        }

        Comparator<BlockPos> order = Comparator.comparingLong(position ->
                position.asLong() ^ (long) POSITION_SALT * player.blockPosition().asLong());
        treePositions.sort(order);
        surfacePositions.sort(order);
        List<BlockPos> ordered = new ArrayList<>(treePositions);
        ordered.addAll(surfacePositions);
        return ordered.size() > maximum
                ? new ArrayList<>(ordered.subList(0, maximum))
                : ordered;
    }

    private static BlockPos findGrassGround(ChunkAccess chunk, int blockX, int blockZ,
                                            int localX, int localZ) {
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
        int minY = Math.max(chunk.getMinBuildHeight(), topY - TREE_SCAN_DEPTH);
        for (int y = topY; y >= minY; y--) {
            BlockState state = chunk.getBlockState(new BlockPos(blockX, y, blockZ));
            if (state.is(Blocks.GRASS_BLOCK)) {
                return new BlockPos(blockX, y, blockZ);
            }
            if (state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                return null;
            }
        }
        return null;
    }

    private static boolean hasTreeCover(ChunkAccess chunk, int blockX, int blockZ, int groundY) {
        int maxY = Math.min(chunk.getMinBuildHeight() + chunk.getHeight() - 1,
                groundY + TREE_SCAN_DEPTH);
        for (int y = groundY + 1; y <= maxY; y++) {
            BlockState state = chunk.getBlockState(new BlockPos(blockX, y, blockZ));
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                    || state.is(ModBlocks.DARK_OAK_WALL.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean tooCloseToCocoon(BlockPos position,
                                            List<DissolutionCocoonEntity> nearby) {
        for (DissolutionCocoonEntity cocoon : nearby) {
            if (cocoon.distanceToSqr(position.getX() + 0.5D, position.getY(),
                    position.getZ() + 0.5D) < 9.0D) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWithinRadius(Entity entity, ServerPlayer player) {
        return entity.distanceToSqr(player) <= SEARCH_RADIUS_SQUARED;
    }

    private static boolean isWithinRadius(int blockX, int blockZ, ServerPlayer player) {
        double dx = blockX + 0.5D - player.getX();
        double dz = blockZ + 0.5D - player.getZ();
        return dx * dx + dz * dz <= SEARCH_RADIUS_SQUARED;
    }

    private static boolean isSisterRegion(long seed, int blockX, int blockZ) {
        EnhancedMountBiomeSource.NatagumoRegion region = EnhancedMountBiomeSource.getNatagumoRegion(
                seed, blockX, blockZ);
        return "Boss Minions Ring - Sister Region".equals(
                EnhancedMountBiomeSource.natagumoRegionName(region, blockX, blockZ));
    }
}
