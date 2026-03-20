package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class TorilGateGuaranteeHandler {
    private static final String CONSOLE_PREFIX = "[KnY-MP TorilGateGuarantee] ";
    private static final String SAVE_DATA_NAME = "kny_toril_gate_guarantee";

    private static final int GUARANTEE_HARD_MIN_RADIUS_BLOCKS = 300;
    private static final int GUARANTEE_MIN_RADIUS_BLOCKS = 800;
    private static final int GUARANTEE_MAX_RADIUS_BLOCKS = 1600;
    private static final int GUARANTEE_HARD_MAX_RADIUS_BLOCKS = 6000;
    private static final int LOCATE_SEARCH_RADIUS_CHUNKS = 256;

    private static final int MAX_SITE_ATTEMPTS = 180;
    private static final int EXPANSION_ATTEMPTS_PER_ROUND = 120;
    private static final int EXPANSION_ROUNDS = 6;
    private static final int EXPAND_MIN_STEP = 150;
    private static final int EXPAND_MAX_STEP = 250;
    private static final int SEARCH_ATTEMPTS_PER_TICK = 8;
    private static final int WISTERIA_PATCH_RADIUS = 48;
    private static final int TERRAIN_PATCH_RADIUS = 42;
    private static final int WISTERIA_TREE_COUNT = 8;
    private static final int MARKER_SCAN_XZ_RADIUS = 24;
    private static final int MARKER_SCAN_Y_RADIUS = 12;

    private static final TagKey<Structure> TORIL_GATE_TAG = TagKey.create(
        Registries.STRUCTURE,
        new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "toril_gate")
    );

    private static final ResourceLocation TORIL_GATE_STRUCTURE_ID = new ResourceLocation(
        KimetsunoyaibaMultiplayer.MODID, "toril_gate"
    );
    private static final ResourceLocation WISTERIA_BIOME_ID = new ResourceLocation(
        KimetsunoyaibaMultiplayer.MODID, "wisteria_forest"
    );
    private static final ResourceKey<Biome> WISTERIA_BIOME_KEY = ResourceKey.create(Registries.BIOME, WISTERIA_BIOME_ID);
    private static SearchJob activeJob;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        System.out.println(CONSOLE_PREFIX + "ServerStartedEvent fired for overworld guarantee check.");

        if (!CustomProgressionConfig.guaranteeTorilGateNearOrigin.get()) {
            System.out.println(CONSOLE_PREFIX + "Disabled by config.");
            return;
        }

        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        GuaranteeState state = GuaranteeState.get(overworld);
        boolean guaranteedStateValid = hasValidGuaranteedGateInBand(overworld);
        boolean naturalGateValid = hasValidGateWithinRadius(overworld);
        boolean satisfied = guaranteedStateValid || naturalGateValid;

        if (satisfied) {
            if (!state.completed) {
                state.completed = true;
                state.setDirty();
            }
            System.out.println(CONSOLE_PREFIX + "Already satisfied (guaranteed=" + guaranteedStateValid +
                ", natural=" + naturalGateValid + ").");
            return;
        }

        if (state.completed) {
            System.out.println(CONSOLE_PREFIX + "State was marked completed but no valid gate found in 300-6000 range. Re-generating.");
            state.completed = false;
            state.setDirty();
        }

        activeJob = new SearchJob(overworld, state);
        System.out.println(CONSOLE_PREFIX + "Scheduled non-blocking guarantee search.");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeJob == null) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (server == null || activeJob.level.getServer() != server) {
            activeJob = null;
            return;
        }

        try {
            activeJob.tick();
            if (activeJob.finished) {
                activeJob = null;
            }
        } catch (Exception e) {
            System.out.println(CONSOLE_PREFIX + "Error during tick search: " + e.getMessage());
            Log.error("Error during toril gate guarantee tick search: {}", e.getMessage());
            activeJob = null;
        }
    }

    private static void applyGuaranteePlacement(ServerLevel level, GuaranteeState state, BlockPos placementPos) {
        paintWisteriaBiomePatch(level, placementPos, WISTERIA_PATCH_RADIUS);
        paintGroundPatch(level, placementPos, TERRAIN_PATCH_RADIUS);
        growWisteriaTrees(level, placementPos, TERRAIN_PATCH_RADIUS, WISTERIA_TREE_COUNT);

        boolean placed = placeTorilGate(level, placementPos);
        if (!placed) {
            Log.error("Failed to place guaranteed toril gate at {}", placementPos);
            System.out.println(CONSOLE_PREFIX + "Structure placement failed at " + placementPos.getX() + " ~ " + placementPos.getZ());
            return;
        }

        BlockPos markerPos = findGateMarkerNearby(level, placementPos);
        state.completed = true;
        if (markerPos != null) {
            state.setGuaranteedGatePos(markerPos);
        } else {
            state.setGuaranteedGatePos(placementPos);
        }
        state.setDirty();
        Log.info("Placed guaranteed toril gate near origin at {} ~ {}", placementPos.getX(), placementPos.getZ());
        System.out.println(CONSOLE_PREFIX + "Placed guaranteed toril gate at " + placementPos.getX() + " ~ " + placementPos.getZ());
    }

    public static BlockPos getGuaranteedGateMarker(ServerLevel level) {
        GuaranteeState state = GuaranteeState.get(level);
        BlockPos stored = state.getGuaranteedGatePos();
        if (stored == null) {
            return null;
        }

        if (level.getBlockState(stored).is(ModBlocks.TORIL_GATE_MARKER.get())) {
            return stored;
        }

        BlockPos nearby = findGateMarkerNearby(level, stored);
        if (nearby != null) {
            state.setGuaranteedGatePos(nearby);
            state.setDirty();
            return nearby;
        }

        return null;
    }

    private static boolean hasValidGateWithinRadius(ServerLevel level) {
        BlockPos origin = new BlockPos(0, 64, 0);
        BlockPos nearest = level.findNearestMapStructure(TORIL_GATE_TAG, origin, LOCATE_SEARCH_RADIUS_CHUNKS, false);
        if (nearest == null) {
            return false;
        }

        BlockPos marker = findGateMarkerNearby(level, nearest);
        if (marker == null) {
            return false;
        }

        double distance = Math.sqrt(marker.getX() * marker.getX() + marker.getZ() * marker.getZ());
        return isWithinAllowedGuaranteeRange(distance);
    }

    private static boolean hasValidGuaranteedGateInBand(ServerLevel level) {
        BlockPos marker = getGuaranteedGateMarker(level);
        if (marker == null) {
            return false;
        }

        double distance = Math.sqrt(marker.getX() * marker.getX() + marker.getZ() * marker.getZ());
        return isWithinAllowedGuaranteeRange(distance);
    }

    private static boolean isWithinAllowedGuaranteeRange(double distance) {
        return distance >= GUARANTEE_HARD_MIN_RADIUS_BLOCKS && distance <= GUARANTEE_HARD_MAX_RADIUS_BLOCKS;
    }

    private static BlockPos tryFindNonOceanSite(ServerLevel level, int minRadiusBlocks, int maxRadiusBlocks, int attempts) {
        RandomSource random = level.random;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        double minSq = (double) minRadiusBlocks * minRadiusBlocks;
        double maxSq = (double) maxRadiusBlocks * maxRadiusBlocks;

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(minSq + random.nextDouble() * (maxSq - minSq));
            int x = (int) Math.round(Math.cos(angle) * r);
            int z = (int) Math.round(Math.sin(angle) * r);

            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            if (topY < minY || topY > maxY) {
                continue;
            }

            BlockPos groundPos = new BlockPos(x, topY, z);
            Holder<Biome> biome = level.getBiome(groundPos);
            if (biome.is(BiomeTags.IS_OCEAN)) {
                continue;
            }

            if (!level.getFluidState(groundPos).isEmpty()) {
                continue;
            }

            return groundPos.above();
        }

        return null;
    }

    private static void paintWisteriaBiomePatch(ServerLevel level, BlockPos center, int radius) {
        CommandSourceStack source = level.getServer()
            .createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        int x1 = center.getX() - radius;
        int z1 = center.getZ() - radius;
        int x2 = center.getX() + radius;
        int z2 = center.getZ() + radius;

        String cmd = "fillbiome " + x1 + " " + minY + " " + z1 + " " + x2 + " " + maxY + " " + z2 + " " + WISTERIA_BIOME_ID;
        int result = level.getServer().getCommands().performPrefixedCommand(source, cmd);
        if (result <= 0 || !isWisteriaBiomeAtCenter(level, center)) {
            Log.warn("fillbiome returned {} for toril gate guarantee patch at {} ~ {}", result, center.getX(), center.getZ());
        } else {
            Log.info("Applied wisteria biome patch for guaranteed toril gate at {} ~ {}", center.getX(), center.getZ());
        }
    }

    private static boolean isWisteriaBiomeAtCenter(ServerLevel level, BlockPos center) {
        Holder<Biome> biome = level.getBiome(center);
        return biome.is(WISTERIA_BIOME_KEY);
    }

    private static boolean placeTorilGate(ServerLevel level, BlockPos pos) {
        CommandSourceStack source = level.getServer()
            .createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();

        String cmd = "place structure " + TORIL_GATE_STRUCTURE_ID + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        int result = level.getServer().getCommands().performPrefixedCommand(source, cmd);
        return result > 0;
    }

    private static void paintGroundPatch(ServerLevel level, BlockPos center, int radius) {
        int changed = 0;
        int minY = level.getMinBuildHeight();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (groundY <= minY + 2) {
                    continue;
                }

                BlockPos top = new BlockPos(x, groundY, z);
                if (!level.getFluidState(top).isEmpty()) {
                    continue;
                }

                // Make a stable natural base before tree placement.
                level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(top.below(), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(top.below(2), Blocks.DIRT.defaultBlockState(), 3);
                changed++;
            }
        }

        Log.info("Prepared guaranteed toril gate ground patch at {} ~ {} ({} columns)", center.getX(), center.getZ(), changed);
    }

    private static void growWisteriaTrees(ServerLevel level, BlockPos center, int radius, int count) {
        RandomSource random = level.random;
        int planted = 0;

        for (int i = 0; i < count * 4 && planted < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * r);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * r);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                continue;
            }

            // Rotate through variants for visual variety.
            ResourceLocation featureId;
            switch (planted % 4) {
                case 1 -> featureId = new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_cyan_placed");
                case 2 -> featureId = new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_cream_placed");
                case 3 -> featureId = new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_lavender_placed");
                default -> featureId = new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_pink_placed");
            }

            CommandSourceStack source = level.getServer()
                .createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();

            String cmd = "place feature " + featureId + " " + x + " " + y + " " + z;
            int result = level.getServer().getCommands().performPrefixedCommand(source, cmd);
            if (result > 0) {
                planted++;
            }
        }

        Log.info("Placed {} wisteria trees for guaranteed toril gate area at {} ~ {}", planted, center.getX(), center.getZ());
    }

    private static BlockPos findGateMarkerNearby(ServerLevel level, BlockPos center) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        for (int dx = -MARKER_SCAN_XZ_RADIUS; dx <= MARKER_SCAN_XZ_RADIUS; dx++) {
            for (int dz = -MARKER_SCAN_XZ_RADIUS; dz <= MARKER_SCAN_XZ_RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                int startY = Math.max(minY, surfaceY - MARKER_SCAN_Y_RADIUS);
                int endY = Math.min(maxY, surfaceY + MARKER_SCAN_Y_RADIUS);
                for (int y = startY; y <= endY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.TORIL_GATE_MARKER.get())) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private static class GuaranteeState extends SavedData {
        private static final String COMPLETED_KEY = "completed";
        private static final String GUARANTEED_PLACED_KEY = "guaranteed_placed";
        private static final String GUARANTEED_X_KEY = "guaranteed_x";
        private static final String GUARANTEED_Y_KEY = "guaranteed_y";
        private static final String GUARANTEED_Z_KEY = "guaranteed_z";

        private boolean completed;
        private boolean guaranteedPlaced;
        private int guaranteedX;
        private int guaranteedY;
        private int guaranteedZ;

        static GuaranteeState get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                GuaranteeState::load,
                GuaranteeState::new,
                SAVE_DATA_NAME
            );
        }

        static GuaranteeState load(CompoundTag tag) {
            GuaranteeState state = new GuaranteeState();
            state.completed = tag.getBoolean(COMPLETED_KEY);
            state.guaranteedPlaced = tag.getBoolean(GUARANTEED_PLACED_KEY);
            if (state.guaranteedPlaced) {
                state.guaranteedX = tag.getInt(GUARANTEED_X_KEY);
                state.guaranteedY = tag.getInt(GUARANTEED_Y_KEY);
                state.guaranteedZ = tag.getInt(GUARANTEED_Z_KEY);
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean(COMPLETED_KEY, completed);
            tag.putBoolean(GUARANTEED_PLACED_KEY, guaranteedPlaced);
            if (guaranteedPlaced) {
                tag.putInt(GUARANTEED_X_KEY, guaranteedX);
                tag.putInt(GUARANTEED_Y_KEY, guaranteedY);
                tag.putInt(GUARANTEED_Z_KEY, guaranteedZ);
            }
            return tag;
        }

        BlockPos getGuaranteedGatePos() {
            if (!guaranteedPlaced) {
                return null;
            }
            return new BlockPos(guaranteedX, guaranteedY, guaranteedZ);
        }

        void setGuaranteedGatePos(BlockPos pos) {
            this.guaranteedPlaced = true;
            this.guaranteedX = pos.getX();
            this.guaranteedY = pos.getY();
            this.guaranteedZ = pos.getZ();
        }
    }

    private static class SearchJob {
        private final ServerLevel level;
        private final GuaranteeState state;
        private final int maxRounds;
        private int round;
        private int attemptsRemainingInRound;
        private int currentMinRadius;
        private int currentMaxRadius;
        private boolean finished;

        private SearchJob(ServerLevel level, GuaranteeState state) {
            this.level = level;
            this.state = state;
            this.maxRounds = computeMaxRounds();
            this.round = 0;
            applyRoundSettings();
        }

        private void tick() {
            if (finished) {
                return;
            }

            for (int i = 0; i < SEARCH_ATTEMPTS_PER_TICK; i++) {
                BlockPos placementPos = tryFindNonOceanSite(level, currentMinRadius, currentMaxRadius, 1);
                if (placementPos != null) {
                    System.out.println(CONSOLE_PREFIX + "Found placement site in band " +
                        currentMinRadius + "-" + currentMaxRadius + " blocks.");
                    applyGuaranteePlacement(level, state, placementPos);
                    finished = true;
                    return;
                }

                attemptsRemainingInRound--;
                if (attemptsRemainingInRound > 0) {
                    continue;
                }

                round++;
                if (round > maxRounds) {
                    System.out.println(CONSOLE_PREFIX + "Failed to find non-ocean placement site after expansion up to " +
                        GUARANTEE_HARD_MAX_RADIUS_BLOCKS + " blocks.");
                    Log.warn("Failed to find non-ocean placement site after expansion up to {} blocks",
                        GUARANTEE_HARD_MAX_RADIUS_BLOCKS);
                    finished = true;
                    return;
                }

                applyRoundSettings();
                System.out.println(CONSOLE_PREFIX + "Expansion round " + round + ": trying band " +
                    currentMinRadius + "-" + currentMaxRadius + " blocks.");
            }
        }

        private void applyRoundSettings() {
            if (round == 0) {
                currentMinRadius = GUARANTEE_MIN_RADIUS_BLOCKS;
                currentMaxRadius = GUARANTEE_MAX_RADIUS_BLOCKS;
                attemptsRemainingInRound = MAX_SITE_ATTEMPTS;
                return;
            }

            currentMinRadius = Math.max(GUARANTEE_HARD_MIN_RADIUS_BLOCKS,
                GUARANTEE_MIN_RADIUS_BLOCKS - (round * EXPAND_MIN_STEP));
            currentMaxRadius = Math.min(GUARANTEE_HARD_MAX_RADIUS_BLOCKS,
                GUARANTEE_MAX_RADIUS_BLOCKS + (round * EXPAND_MAX_STEP));
            attemptsRemainingInRound = EXPANSION_ATTEMPTS_PER_ROUND;
        }

        private int computeMaxRounds() {
            int roundsToMin = (int) Math.ceil((GUARANTEE_MIN_RADIUS_BLOCKS - GUARANTEE_HARD_MIN_RADIUS_BLOCKS) / (double) EXPAND_MIN_STEP);
            int roundsToMax = (int) Math.ceil((GUARANTEE_HARD_MAX_RADIUS_BLOCKS - GUARANTEE_MAX_RADIUS_BLOCKS) / (double) EXPAND_MAX_STEP);
            return Math.max(EXPANSION_ROUNDS, Math.max(roundsToMin, roundsToMax));
        }
    }
}
