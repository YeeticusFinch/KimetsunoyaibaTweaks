package com.lerdorf.kimetsunoyaibamultiplayer;

import java.util.ArrayList;
import java.util.List;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.FinalSelectionProcedure;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.MtFujikasaneDaylightController;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.config.FinalSelectionRaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gameplay rules for Mt Fujikasane.
 * The bundled world payload and dimension data are provided by the kny_worlds dependency.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class MtFujikasaneDimensionDataHandler {
    private static final String MT_FUJIKASANE_SUN_BURN_TICKS_TAG = "KnYMtFujikasaneSunBurnTicks";

    private static final ResourceLocation MT_FUJIKASANE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mt_fujikasane");
    private static final ResourceLocation BASE_SWAMP_DEMON_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "swamp_demon");
    private static final ResourceLocation CUSTOM_SWAMP_DEMON_ID =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "swamp_demon");

    // World border settings - 1000x1000 blocks centered at 0,0
    private static final double WORLD_BORDER_SIZE = 1000.0;
    private static final double WORLD_BORDER_CENTER_X = 0.0;
    private static final double WORLD_BORDER_CENTER_Z = 0.0;
    private static final java.util.List<ResourceLocation> FINAL_SELECTION_EASY_DEMON_REPLACEMENTS = java.util.List.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_2"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_3"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "spider_demon")
    );

    /**
     * Reset runtime state for the dimension gameplay systems.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MtFujikasaneDaylightController.resetRuntimeState(event.getServer());
        FinalSelectionProcedure.resetRuntimeState();
        Log.debug("[Mt Fujikasane] World payload bootstrap is provided by kny_worlds.");
    }

    /**
     * Called when a dimension/level loads - sets up vanilla world border
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        // Only run on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Check if this is the Mt Fujikasane dimension
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ResourceKey<Level> dimensionKey = serverLevel.dimension();
        if (!dimensionKey.location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }

        // Set up vanilla world border
        WorldBorder border = serverLevel.getWorldBorder();

        // Set center and size
        border.setCenter(WORLD_BORDER_CENTER_X, WORLD_BORDER_CENTER_Z);
        border.setSize(WORLD_BORDER_SIZE);

        // Configure border behavior
        border.setWarningBlocks(50); // Warning when within 50 blocks
        border.setWarningTime(15); // Warning time in seconds
        border.setDamagePerBlock(0.2); // Damage when outside border

        Log.debug("[Mt Fujikasane] World border configured: " +
            (int)WORLD_BORDER_SIZE + "x" + (int)WORLD_BORDER_SIZE +
            " blocks centered at (" + (int)WORLD_BORDER_CENTER_X + ", " + (int)WORLD_BORDER_CENTER_Z + ")");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MtFujikasaneDaylightController.resetRuntimeState(event.getServer());
        FinalSelectionProcedure.resetRuntimeState();
    }

    // ===== Mt Fujikasane dimension-specific event handlers =====

    /**
     * Hard blacklist for swamp demons in Mt Fujikasane.
     * This catches programmatic spawns that bypass MobSpawnEvent.
     */
    @SubscribeEvent
    public static void onMtFujikasaneEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }
        if (!isSwampDemon(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.getEntity().discard();
    }

    /**
     * Deny all natural mob spawn placement checks in Mt Fujikasane.
     * Fires before the entity is even created — most efficient prevention.
     * Programmatic spawns via level.addFreshEntity() bypass this event entirely.
     */
    @SubscribeEvent
    public static void onMtFujikasaneSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (!serverLevel.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
                return;
            }

            if (!isNaturalLikeSpawn(event.getSpawnType())) {
                return;
            }

            if (event.getPos() != null
                && shouldAllowFinalSelectionNaturalHostileSpawn(serverLevel, event.getEntityType(), event.getPos().getX(), event.getPos().getZ(), true)) {
                return;
            }

            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    /**
     * Prevent all natural mob spawning in the Mt Fujikasane dimension (late-stage safety net).
     * Only entities spawned programmatically (e.g., by raids or commands) are allowed.
     */
    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (!serverLevel.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
                return;
            }

            if (!isNaturalLikeSpawn(event.getSpawnType())) {
                return;
            }

            if (tryReplaceVanillaHostileWithEasyDemon(serverLevel, event)) {
                // Cancel original vanilla hostile spawn after successful replacement.
                event.setSpawnCancelled(true);
                return;
            }

            if (shouldAllowFinalSelectionNaturalHostileSpawn(
                serverLevel,
                event.getEntity() != null ? event.getEntity().getType() : null,
                event.getEntity() != null ? event.getEntity().getX() : 0.0D,
                event.getEntity() != null ? event.getEntity().getZ() : 0.0D,
                true
            )) {
                return;
            }

            // Deny all other natural spawns in Mt Fujikasane.
            event.setSpawnCancelled(true);
        }
    }

    private static boolean tryReplaceVanillaHostileWithEasyDemon(ServerLevel level, MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null) {
            return false;
        }

        EntityType<?> originalType = event.getEntity().getType();
        if (!isHostileEntityType(originalType) || isDemonEntityType(originalType)) {
            return false;
        }

        ResourceLocation originalId = BuiltInRegistries.ENTITY_TYPE.getKey(originalType);
        if (originalId == null || !"minecraft".equals(originalId.getNamespace())) {
            return false; // Only replace vanilla hostile mobs.
        }

        double x = event.getEntity().getX();
        double z = event.getEntity().getZ();
        if (!FinalSelectionProcedure.isInsideActiveRaidArea(level, x, z)) {
            return false;
        }

        int night = FinalSelectionProcedure.getActiveRaidNight(level);
        double replaceChance = getVanillaHostileReplacementChanceForNight(night);
        if (replaceChance <= 0.0D || level.random.nextDouble() >= replaceChance) {
            return false;
        }

        // Cap non-boss demon population during Final Selection.
        // Returning true here still cancels the original hostile spawn.
        if (!FinalSelectionProcedure.canSpawnAdditionalNonBossDemon(level)) {
            return true;
        }

        ResourceLocation demonId = FINAL_SELECTION_EASY_DEMON_REPLACEMENTS.get(level.random.nextInt(FINAL_SELECTION_EASY_DEMON_REPLACEMENTS.size()));
        EntityType<?> demonType = BuiltInRegistries.ENTITY_TYPE.get(demonId);
        if (demonType == null) {
            return false;
        }

        net.minecraft.world.entity.Entity created = demonType.create(level);
        if (!(created instanceof Mob demonMob)) {
            return false;
        }

        demonMob.moveTo(
            event.getEntity().getX(),
            event.getEntity().getY(),
            event.getEntity().getZ(),
            level.random.nextFloat() * 360.0F,
            0.0F
        );
        demonMob.setPersistenceRequired();
        level.addFreshEntity(demonMob);

        return true;
    }

    private static boolean shouldAllowFinalSelectionNaturalHostileSpawn(ServerLevel level, EntityType<?> entityType, double x, double z, boolean applyNightReduction) {
        if (!isHostileEntityType(entityType)) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (isSwampDemonId(entityId)) {
            return false;
        }
        if (!FinalSelectionProcedure.isInsideActiveRaidArea(level, x, z)) {
            return false;
        }

        // During Final Selection, reduce natural non-demon hostile spawning by night.
        if (!applyNightReduction) {
            return true;
        }
        if (isDemonEntityType(entityType)) {
            return FinalSelectionProcedure.canSpawnAdditionalNonBossDemon(level);
        }
        if (isVanillaHostileEntityType(entityType) && !FinalSelectionRaidConfig.allowVanillaMonstersInFinalSelection.get()) {
            return false;
        }

        int night = FinalSelectionProcedure.getActiveRaidNight(level);
        double allowChance = getNonDemonHostileAllowChanceForNight(night);
        return level.random.nextDouble() < allowChance;
    }

    private static boolean isSwampDemon(Entity entity) {
        return entity != null && isSwampDemonId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private static boolean isSwampDemonId(ResourceLocation entityId) {
        return BASE_SWAMP_DEMON_ID.equals(entityId) || CUSTOM_SWAMP_DEMON_ID.equals(entityId);
    }

    private static void purgeSwampDemons(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (isSwampDemon(entity)) {
                entity.discard();
            }
        }
    }

    private static boolean isHostileEntityType(EntityType<?> entityType) {
        return entityType != null && entityType.getCategory() == MobCategory.MONSTER;
    }

    private static boolean isDemonEntityType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }
        ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityId != null && com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization.isDemon(entityId);
    }

    private static boolean isVanillaHostileEntityType(EntityType<?> entityType) {
        if (!isHostileEntityType(entityType)) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityId != null && "minecraft".equals(entityId.getNamespace());
    }

    private static double getNonDemonHostileAllowChanceForNight(int night) {
        return switch (night) {
            case 1 -> 0.10D; // 90% reduction
            case 2 -> 0.40D; // 60% reduction
            case 3 -> 0.60D; // 40% reduction
            case 4 -> 0.70D; // 30% reduction
            case 5 -> 0.80D; // 20% reduction
            case 6 -> 0.90D; // 10% reduction
            default -> 1.00D; // Night 7+ no reduction
        };
    }

    private static double getVanillaHostileReplacementChanceForNight(int night) {
        return switch (night) {
            case 3 -> 0.20D;
            case 4 -> 0.30D;
            case 5 -> 0.40D;
            case 6, 7 -> 0.50D;
            default -> 0.0D;
        };
    }

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    /**
     * If a chunk unloads during an active Final Selection raid, preserve trainee movement by
     * relocating some DemonSlayerEntity mobs to a nearby loaded surface chunk.
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }
        if (!FinalSelectionProcedure.shouldRelocateDemonSlayersOnChunkUnload(level)) {
            return;
        }
        if (level.players().isEmpty()) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        ChunkPos unloadingChunkPos = chunk.getPos();
        AABB bounds = new AABB(
            unloadingChunkPos.getMinBlockX(),
            level.getMinBuildHeight(),
            unloadingChunkPos.getMinBlockZ(),
            unloadingChunkPos.getMaxBlockX() + 1,
            level.getMaxBuildHeight(),
            unloadingChunkPos.getMaxBlockZ() + 1
        );

        for (DemonSlayerEntity slayer : level.getEntitiesOfClass(DemonSlayerEntity.class, bounds, EntitySelector.ENTITY_STILL_ALIVE)) {
            // 50% chance to relocate.
            if (ThreadLocalRandom.current().nextBoolean()) {
                continue;
            }

            BlockPos safePos = findNearestLoadedSafeSurface(level, unloadingChunkPos, slayer.blockPosition());
            if (safePos == null) {
                // Safeguard: if there are no suitable loaded chunks, skip safely.
                continue;
            }

            slayer.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
            slayer.setDeltaMovement(0.0D, 0.0D, 0.0D);
            PathNavigation navigation = slayer.getNavigation();
            if (navigation != null) {
                navigation.stop();
            }
        }
    }

    private static BlockPos findNearestLoadedSafeSurface(ServerLevel level, ChunkPos sourceChunk, BlockPos origin) {
        final int maxSearchRadiusChunks = 16;
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int radius = 1; radius <= maxSearchRadiusChunks; radius++) {
            boolean foundAnyLoadedAtRadius = false;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int chunkX = sourceChunk.x + dx;
                    int chunkZ = sourceChunk.z + dz;

                    if (chunkX == sourceChunk.x && chunkZ == sourceChunk.z) {
                        continue;
                    }
                    if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    foundAnyLoadedAtRadius = true;
                    BlockPos candidate = findSafeSurfaceInChunk(level, chunkX, chunkZ, origin.getY());
                    if (candidate == null) {
                        continue;
                    }

                    double distSq = candidate.distSqr(origin);
                    if (distSq < nearestDistSq) {
                        nearest = candidate;
                        nearestDistSq = distSq;
                    }
                }
            }

            // Since this is a ring search, first ring with a valid position is the nearest loaded area.
            if (nearest != null) {
                return nearest;
            }

            // Continue searching outer rings even if no loaded chunks in this ring.
            if (!foundAnyLoadedAtRadius) {
                continue;
            }
        }

        return nearest;
    }

    private static BlockPos findSafeSurfaceInChunk(ServerLevel level, int chunkX, int chunkZ, int anchorY) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        // Probe center + random points inside chunk for a valid standing location.
        BlockPos centerCandidate = level.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos(minX + 8, anchorY, minZ + 8)
        );
        if (isSafeSurface(level, centerCandidate)) {
            return centerCandidate;
        }

        for (int i = 0; i < 6; i++) {
            int x = minX + ThreadLocalRandom.current().nextInt(16);
            int z = minZ + ThreadLocalRandom.current().nextInt(16);
            BlockPos candidate = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, anchorY, z)
            );
            if (isSafeSurface(level, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isSafeSurface(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        return !level.getFluidState(pos).isSource();
    }

    private static void tickMtFujikasaneSunlightBurn(ServerLevel level) {
        if (level == null || !level.isDay()) {
            return;
        }

        List<Mob> targets = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (mob instanceof AbstractDemonEntity) {
                continue;
            }
            if (!isSunlightVulnerableDemon(mob)) {
                continue;
            }

            targets.add(mob);
        }

        for (Mob mob : targets) {
            if (!mob.isAlive() || mob.isRemoved()) {
                continue;
            }

            if (isInBurningSunlight(level, mob)) {
                int burnTicks = mob.getPersistentData().getInt(MT_FUJIKASANE_SUN_BURN_TICKS_TAG) + 1;
                mob.getPersistentData().putInt(MT_FUJIKASANE_SUN_BURN_TICKS_TAG, burnTicks);

                mob.setSecondsOnFire(2);

                level.sendParticles(
                    ParticleTypes.FLAME,
                    mob.getX(),
                    mob.getY(0.5D),
                    mob.getZ(),
                    4,
                    0.3D,
                    0.4D,
                    0.3D,
                    0.01D
                );

                level.sendParticles(
                    ParticleTypes.LAVA,
                    mob.getX(),
                    mob.getY(0.2D),
                    mob.getZ(),
                    2,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.0D
                );

                if (burnTicks % 10 == 0 && burnTicks <= 40 && !mob.isRemoved()) {
                    mob.hurt(mob.damageSources().onFire(), 10.0F);
                }

                if (burnTicks >= 40 && mob.isAlive() && !mob.isRemoved()) {
                    level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        mob.getX(),
                        mob.getY(0.6D),
                        mob.getZ(),
                        12,
                        0.3D,
                        0.4D,
                        0.3D,
                        0.02D
                    );

                    mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.1F);
                    mob.discard();
                }
            } else if (mob.getPersistentData().contains(MT_FUJIKASANE_SUN_BURN_TICKS_TAG)) {
                mob.getPersistentData().remove(MT_FUJIKASANE_SUN_BURN_TICKS_TAG);
            }
        }
    }

    private static boolean isSunlightVulnerableDemon(Mob mob) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (entityId != null && com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry.isSunlightImmune(entityId)) {
            return false;
        }
        return (entityId != null && EntityCategorization.isDemon(entityId)) || mob.getPersistentData().getBoolean("oni");
    }

    private static boolean isInBurningSunlight(ServerLevel level, Mob mob) {
        if (mob.isInWaterRainOrBubble() || mob.isUnderWater()) {
            return false;
        }

        BlockPos pos = mob.blockPosition();
        return level.canSeeSky(pos) && !level.isRainingAt(pos);
    }

    /**
     * Tick the Mt Fujikasane daylight controller and final selection procedure.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // Get the Mt Fujikasane dimension if loaded
        ResourceKey<Level> mtFujikasaneKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            MT_FUJIKASANE_DIM_ID
        );
        ServerLevel mtFujikasane = event.getServer().getLevel(mtFujikasaneKey);
        if (mtFujikasane == null) return;

        // Tick daylight controller
        MtFujikasaneDaylightController.tick(mtFujikasane);

        // Hard purge: no swamp demons are allowed in Mt Fujikasane.
        purgeSwampDemons(mtFujikasane);

        // Apply sunlight death to all demons in Mt Fujikasane, including base-mod demons.
        tickMtFujikasaneSunlightBurn(mtFujikasane);

        // Tick final selection procedure
        FinalSelectionProcedure.tickActive(mtFujikasane);
    }
}
