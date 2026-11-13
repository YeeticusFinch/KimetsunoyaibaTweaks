package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Blends generated Tsuzumi Mansion sites into the surface and adds nearby trees. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class TsuzumiMansionWorldgenHandler {
    private static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(
        KimetsunoyaibaMultiplayer.MODID, "tsuzumi_mansion");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SPRUCE_TREE = ResourceKey.create(
        Registries.CONFIGURED_FEATURE,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "tsuzumi_spruce_tree"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> DARK_OAK_TREE = ResourceKey.create(
        Registries.CONFIGURED_FEATURE,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "tsuzumi_dark_oak_tree"));
    private static final int BLEND_RADIUS = 14;
    private static final int TREE_ATTEMPTS = 32;

    private TsuzumiMansionWorldgenHandler() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
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
                findMansionStart(level, new net.minecraft.core.BlockPos(x, y, z));
            }
        }
    }

    private static void findMansionStart(ServerLevel level, net.minecraft.core.BlockPos probe) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Holder<Structure> holder : registry.holders().toList()) {
            ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
            if (!STRUCTURE_ID.equals(id)) {
                continue;
            }
            StructureStart start = level.structureManager().getStructureAt(probe, holder.value());
            if (start != null && start.isValid()) {
                BoundingBox box = start.getBoundingBox();
                level.getServer().execute(() -> decorate(level, box));
            }
            return;
        }
    }

    private static void decorate(ServerLevel level, BoundingBox box) {
        TsuzumiMansionSavedData data = TsuzumiMansionSavedData.get(level);
        long key = siteKey(box);
        if (!data.sites.add(key)) {
            return;
        }

        prepareBlend(level, box);
        growTrees(level, box);
        data.setDirty();
    }

    private static void prepareBlend(ServerLevel level, BoundingBox box) {
        int minX = box.minX() - BLEND_RADIUS;
        int maxX = box.maxX() + BLEND_RADIUS;
        int minZ = box.minZ() - BLEND_RADIUS;
        int maxZ = box.maxZ() + BLEND_RADIUS;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) {
                    continue;
                }
                int distance = horizontalDistanceToBox(box, x, z);
                if (distance > BLEND_RADIUS) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (y <= level.getMinBuildHeight() + 2 || !level.getFluidState(new net.minecraft.core.BlockPos(x, y, z)).isEmpty()) {
                    continue;
                }
                net.minecraft.core.BlockPos top = new net.minecraft.core.BlockPos(x, y, z);
                level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(top.below(), Blocks.DIRT.defaultBlockState(), 3);
                if (distance < BLEND_RADIUS / 2) {
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

        for (int attempt = 0; attempt < TREE_ATTEMPTS; attempt++) {
            int x = box.minX() - BLEND_RADIUS + level.random.nextInt(Math.max(1, box.getXSpan() + BLEND_RADIUS * 2));
            int z = box.minZ() - BLEND_RADIUS + level.random.nextInt(Math.max(1, box.getZSpan() + BLEND_RADIUS * 2));
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
            feature.value().place(level, level.getChunkSource().getGenerator(), level.random, ground.above());
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
        private final Set<Long> sites = new HashSet<>();

        private static TsuzumiMansionSavedData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                TsuzumiMansionSavedData::load,
                TsuzumiMansionSavedData::new,
                DATA_NAME);
        }

        private static TsuzumiMansionSavedData load(CompoundTag tag) {
            TsuzumiMansionSavedData data = new TsuzumiMansionSavedData();
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
            ListTag sites = new ListTag();
            for (Long site : this.sites) {
                sites.add(new net.minecraft.nbt.LongArrayTag(new long[] {site}));
            }
            tag.put("sites", sites);
            return tag;
        }
    }
}
