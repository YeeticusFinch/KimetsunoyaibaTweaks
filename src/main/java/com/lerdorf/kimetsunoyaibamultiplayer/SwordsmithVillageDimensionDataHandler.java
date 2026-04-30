package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordsmithVillageConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class SwordsmithVillageDimensionDataHandler {

    private static final ResourceLocation SWORDSMITH_VILLAGE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swordsmith_village");

    private static final String DIMENSION_NAME = "Swordsmith Village";

    private static final double WORLD_BORDER_SIZE = 2048.0D;
    private static final double WORLD_BORDER_CENTER_X = 0.0D;
    private static final double WORLD_BORDER_CENTER_Z = 0.0D;

    private static final double ENTRY_X = 3.0D;
    private static final double ENTRY_Y = 70.0D;
    private static final double ENTRY_Z = 280.0D;
    private static final float ENTRY_YAW = 180.0F;
    private static final float ENTRY_PITCH = 0.0F;

    private static final String RESIDENT_TAG = "SwordsmithVillageResident";
    private static final String RESIDENT_TYPE_TAG = "SwordsmithVillageResidentType";
    private static final String ENTRY_KAKUSHI_TAG = "SwordsmithVillageEntryKakushi";
    private static final String POPULATION_DATA_NAME = "swordsmith_village_population";
    private static final int RESIDENT_CHECK_INTERVAL_TICKS = 200;
    private static final int WEATHER_ENFORCEMENT_INTERVAL_TICKS = 20;
    private static final double SPAWN_DENSITY_RADIUS = 15.0D;
    private static final int MAX_ENTITIES_PER_SPAWN_CLUSTER = 2;
    private static final long TICKS_PER_DAY = 24000L;
    private static final long NOON_TIME = 6000L;

    private static final ResourceLocation HYOTTOKO_MASK_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mask_hyottoko_helmet");
    private static final ResourceLocation KAKUSHI_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi");

    private static final BlockPos CHIEF_LOCATION = new BlockPos(15, 83, 42);
    private static final List<BlockPos> CHIEF_ASSISTANT_LOCATIONS = List.of(
        new BlockPos(16, 83, 44),
                new BlockPos(16, 83, 40)
    );

    private static final List<BlockPos> SPAWN_LOCATIONS = List.of(
        new BlockPos(7, 72, 154),
        new BlockPos(0, 72, 190),
        new BlockPos(16, 73, 214),
        new BlockPos(4, 72, 69),
        new BlockPos(-11, 72, 76),
        new BlockPos(-8, 78, 100),
        new BlockPos(-13, 72, 114),
        new BlockPos(-6, 72, 101),
        new BlockPos(29, 72, 107),
        new BlockPos(27, 72, 120),
        new BlockPos(28, 77, 119),
        new BlockPos(25, 77, 112),
        new BlockPos(28, 77, 105),
        new BlockPos(-7, 72, 115),
        new BlockPos(-7, 72, 100),
        new BlockPos(-14, 78, 100),
        new BlockPos(-6, 78, 115),
        new BlockPos(-17, 74, 138),
        new BlockPos(-16, 74, 145),
        new BlockPos(-18, 79, 161),
        new BlockPos(-19, 73, 161),
        new BlockPos(-19, 73, 176),
        new BlockPos(-11, 73, 176),
        new BlockPos(25, 74, 170),
        new BlockPos(33, 79, 169),
        new BlockPos(32, 79, 140),
        new BlockPos(31, 74, 163),
        new BlockPos(33, 75, 178),
        new BlockPos(24, 79, 176),
        new BlockPos(-13, 74, 199),
        new BlockPos(-13, 74, 206),
        new BlockPos(-19, 74, 193),
        new BlockPos(-12, 80, 199),
        new BlockPos(28, 75, 199),
        new BlockPos(29, 75, 206),
        new BlockPos(-7, 76, 230),
        new BlockPos(-22, 76, 236),
        new BlockPos(-15, 83, 231),
        new BlockPos(-7, 83, 228),
        new BlockPos(21, 76, 231),
        new BlockPos(8, 74, 43),
        new BlockPos(18, 74, 36),
        new BlockPos(20, 74, 50),
        new BlockPos(0, 74, 46),
        new BlockPos(0, 78, 47),
        new BlockPos(9, 78, 38),
        new BlockPos(19, 78, 45),
        new BlockPos(11, 78, 56),
        new BlockPos(17, 77, 81),
        new BlockPos(5, 77, 107),
        new BlockPos(17, 77, 115),
        new BlockPos(-3, 74, 25),
        new BlockPos(21, 74, 25),
        new BlockPos(17, 74, 36),
        new BlockPos(6, 83, 43),
        new BlockPos(111, 155, -241),
        new BlockPos(26, 170, -285),
        new BlockPos(-27, 85, -159),
        new BlockPos(-76, 97, -88),
        new BlockPos(-47, 86, -48),
        new BlockPos(-20, 80, -32),
        new BlockPos(124, 75, 53),
        new BlockPos(224, 176, 19),
        new BlockPos(23, 79, -192)
    );

    private static final List<ResidentSpec> RESIDENT_SPECS = List.of(
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "haganeduka"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kotetsu"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "doctor"), true),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "toyosan"), true),
        //new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yushiro"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kanawo_buyer"), true)
    );

    private static final AtomicBoolean residentMaintenanceInProgress = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Log.debug(prefix() + " World payload bootstrap is provided by kny_worlds.");
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }

        WorldBorder border = serverLevel.getWorldBorder();
        border.setCenter(WORLD_BORDER_CENTER_X, WORLD_BORDER_CENTER_Z);
        border.setSize(WORLD_BORDER_SIZE);
        border.setWarningBlocks(64);
        border.setWarningTime(15);
        border.setDamagePerBlock(0.2D);

        Log.debug(prefix() + " World border configured: "
            + (int) WORLD_BORDER_SIZE + "x" + (int) WORLD_BORDER_SIZE);

        clearVillageWeather(serverLevel, "level load");
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getTo().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }

        ServerLevel targetLevel = player.getServer().getLevel(ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            SWORDSMITH_VILLAGE_DIM_ID
        ));
        if (targetLevel == null) {
            return;
        }

        player.teleportTo(targetLevel, ENTRY_X, ENTRY_Y, ENTRY_Z, ENTRY_YAW, ENTRY_PITCH);
        clearVillageWeather(targetLevel, "player entered dimension");
    }

    @SubscribeEvent
    public static void onSwordsmithVillageSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }

        Mob mob = event.getEntity();
        BlockPos spawnPos = mob.blockPosition();
        if (!serverLevel.isLoaded(spawnPos)) {
            event.setSpawnCancelled(true);
            return;
        }

        if (isPopulationTrackedEntity(mob)
            && !isVillageEntryKakushi(mob)
            && isVillagePopulationAtOrAboveStored(serverLevel)) {
            event.setSpawnCancelled(true);
            return;
        }

        if (isNaturalLikeSpawn(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        ServerLevel swordsmithVillage = event.getServer().getLevel(ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            SWORDSMITH_VILLAGE_DIM_ID
        ));
        if (swordsmithVillage == null) {
            return;
        }

        if (event.getServer().getTickCount() % WEATHER_ENFORCEMENT_INTERVAL_TICKS == 0) {
            clearVillageWeatherIfNeeded(swordsmithVillage, "server tick");
            processNoonPopulationRecovery(swordsmithVillage);
        }

        if (event.getServer().getTickCount() % RESIDENT_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (!residentMaintenanceInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            ensureResidentsPresent(swordsmithVillage);
        } finally {
            residentMaintenanceInProgress.set(false);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        stripTorilGateMarkers(chunk);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!isPopulationTrackedEntity(entity)) {
            return;
        }

        VillagePopulationState state = getPopulationState(level);
        state.setCurrentPopulation(Math.max(0, state.getCurrentPopulation() - 1));
        state.setDirty();
    }

    private static void ensureResidentsPresent(ServerLevel level) {
        for (ResidentSpec spec : RESIDENT_SPECS) {
            if (findResident(level, spec) != null) {
                continue;
            }

            BlockPos spawnPos = pickSpawnLocation(level);
            if (spawnPos == null) {
                Log.debug(prefix() + " No free spawn location available for " + spec.entityId);
                continue;
            }

            Mob mob = spawnResident(level, spawnPos, spec);
            if (mob != null) {
                Log.debug(prefix() + " Spawned resident " + spec.entityId + " at " + spawnPos);
            }
        }
    }

    private static Mob findResident(ServerLevel level, ResidentSpec spec) {
        for (Entity entity : level.getEntities().getAll()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (!mob.getPersistentData().getBoolean(RESIDENT_TAG)) {
                continue;
            }
            if (!spec.entityId.toString().equals(mob.getPersistentData().getString(RESIDENT_TYPE_TAG))) {
                continue;
            }
            return mob;
        }
        return null;
    }

    private static BlockPos pickSpawnLocation(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>(SPAWN_LOCATIONS);
        java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
        for (BlockPos pos : candidates) {
            if (canSpawnAtLocation(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canSpawnAtLocation(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        if (isExactSpawnLocationOccupied(level, pos)) {
            return false;
        }
        if (isVillagePopulationAtOrAboveStored(level)) {
            return false;
        }
        return countNearbyLivingEntities(level, pos) < getAllowedEntitiesNearSpawnPoint(pos);
    }

    private static boolean isExactSpawnLocationOccupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(
                pos.getX() - 1.5D, pos.getY() - 2.0D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 3.0D, pos.getZ() + 2.5D
            ),
            living -> living.isAlive()
        ).isEmpty();
    }

    private static int countNearbyLivingEntities(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;
        double maxDistanceSqr = SPAWN_DENSITY_RADIUS * SPAWN_DENSITY_RADIUS;
        return (int) level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(pos).inflate(SPAWN_DENSITY_RADIUS),
            living -> living.isAlive()
        ).stream()
            .filter(living -> living.distanceToSqr(centerX, centerY, centerZ) <= maxDistanceSqr)
            .count();
    }

    private static int getAllowedEntitiesNearSpawnPoint(BlockPos pos) {
        int nearbySpawnPoints = 0;
        for (BlockPos otherPos : SPAWN_LOCATIONS) {
            if (otherPos.closerThan(pos, SPAWN_DENSITY_RADIUS + 0.001D)) {
                nearbySpawnPoints++;
            }
        }
        if (nearbySpawnPoints > 1) {
            return MAX_ENTITIES_PER_SPAWN_CLUSTER;
        }
        return 1;
    }

    private static Mob spawnResident(ServerLevel level, BlockPos pos, ResidentSpec spec) {
        if (!level.isLoaded(pos) || isVillagePopulationAtOrAboveStored(level)) {
            return null;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(spec.entityId).orElse(null);
        if (entityType == null) {
            System.err.println(prefix() + " Entity type not found: " + spec.entityId);
            return null;
        }

        Entity created = entityType.create(level);
        if (!(created instanceof Mob mob)) {
            System.err.println(prefix() + " Entity is not a mob: " + spec.entityId);
            return null;
        }

        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
            ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        mob.getPersistentData().putBoolean(RESIDENT_TAG, true);
        mob.getPersistentData().putString(RESIDENT_TYPE_TAG, spec.entityId.toString());

        if (spec.wearHyottokoMask) {
            Item maskItem = BuiltInRegistries.ITEM.getOptional(HYOTTOKO_MASK_ID).orElse(net.minecraft.world.item.Items.AIR);
            if (maskItem != net.minecraft.world.item.Items.AIR) {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(maskItem));
                mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
            }
        }

        if (level.addFreshEntity(mob)) {
            return mob;
        }
        return null;
    }

    private static boolean isVillagePopulationAtOrAboveStored(ServerLevel level) {
        return countTrackedVillageResidents(level) >= getPopulationState(level).getCurrentPopulation();
    }

    private static int countTrackedVillageResidents(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof LivingEntity living && living.isAlive() && isPopulationTrackedEntity(living)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPopulationTrackedEntity(Entity entity) {
        return entity instanceof LivingEntity
            && (EntityTagHelper.isCivilian(entity) || EntityTagHelper.isSwordSmith(entity));
    }

    private static boolean isVillageEntryKakushi(Entity entity) {
        if (!KAKUSHI_ID.equals(EntityType.getKey(entity.getType()))) {
            return false;
        }
        return entity.getPersistentData().getBoolean(ENTRY_KAKUSHI_TAG);
    }

    private static void processNoonPopulationRecovery(ServerLevel level) {
        VillagePopulationState state = getPopulationState(level);
        long dayTime = level.getDayTime();
        long dayIndex = dayTime / TICKS_PER_DAY;
        long timeOfDay = dayTime % TICKS_PER_DAY;
        if (timeOfDay < NOON_TIME || state.getLastNoonRollDay() == dayIndex) {
            return;
        }

        state.setLastNoonRollDay(dayIndex);
        int maxPopulation = Math.max(0, SwordsmithVillageConfig.maxPopulation);
        if (state.getCurrentPopulation() < maxPopulation
            && level.getRandom().nextDouble() < SwordsmithVillageConfig.noonRecoveryChance) {
            state.setCurrentPopulation(state.getCurrentPopulation() + 1);
        }
        state.clampToConfiguredMaximum();
        state.setDirty();
    }

    private static VillagePopulationState getPopulationState(ServerLevel level) {
        VillagePopulationState state = level.getDataStorage().computeIfAbsent(
            VillagePopulationState::load,
            VillagePopulationState::new,
            POPULATION_DATA_NAME
        );
        state.ensureInitialized();
        return state;
    }

    private static void clearVillageWeather(ServerLevel village, String reason) {
        boolean weatherActive = hasActiveVillageWeather(village);
        clearWritableWeatherFlags(village);
        village.setWeatherParameters(12000, 0, false, false);
        if (weatherActive) {
            Log.info(prefix() + " Cleared lingering weather on {}", reason);
        }
    }

    private static void clearVillageWeatherIfNeeded(ServerLevel village, String reason) {
        if (!hasActiveVillageWeather(village)) {
            return;
        }
        clearVillageWeather(village, reason);
    }

    private static boolean hasActiveVillageWeather(ServerLevel village) {
        return village.isRaining() || village.isThundering()
            || village.getRainLevel(1.0F) > 0.0F || village.getThunderLevel(1.0F) > 0.0F;
    }

    private static void clearWritableWeatherFlags(ServerLevel village) {
        if (village.getLevelData() instanceof WritableLevelData levelData) {
            levelData.setRaining(false);
        }

        Object levelData = village.getLevelData();
        invokeBooleanSetter(levelData, "setRaining", false);
        invokeBooleanSetter(levelData, "setThundering", false);
    }

    private static void invokeBooleanSetter(Object target, String methodName, boolean value) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void stripTorilGateMarkers(LevelChunk chunk) {
        long startNanos = System.nanoTime();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int removed = 0;
        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    cursor.set(x, y, z);
                    if (!chunk.getBlockState(cursor).is(ModBlocks.TORIL_GATE_MARKER.get())) {
                        continue;
                    }

                    chunk.setBlockState(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            Log.debug(prefix() + " Removed " + removed + " Toril Gate marker block(s) from swordsmith village chunk "
                + chunk.getPos().x + "," + chunk.getPos().z);
        }
        Log.debugVisibleIfSlow(
            "swordsmith-strip-toril-markers",
            startNanos,
            50L,
            prefix() + " stripTorilGateMarkers took {} ms in chunk {},{} (removed={})",
            (System.nanoTime() - startNanos) / 1_000_000L,
            chunk.getPos().x,
            chunk.getPos().z,
            removed
        );
    }

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    private static String prefix() {
        return "[" + DIMENSION_NAME + "]";
    }

    private record ResidentSpec(ResourceLocation entityId, boolean wearHyottokoMask) {
    }

    private static final class VillagePopulationState extends SavedData {
        private int currentPopulation = -1;
        private long lastNoonRollDay = -1L;

        private VillagePopulationState() {
        }

        private static VillagePopulationState load(CompoundTag tag) {
            VillagePopulationState state = new VillagePopulationState();
            if (tag.contains("CurrentPopulation", Tag.TAG_INT)) {
                state.currentPopulation = tag.getInt("CurrentPopulation");
            }
            if (tag.contains("LastNoonRollDay", Tag.TAG_LONG)) {
                state.lastNoonRollDay = tag.getLong("LastNoonRollDay");
            }
            state.ensureInitialized();
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("CurrentPopulation", currentPopulation);
            tag.putLong("LastNoonRollDay", lastNoonRollDay);
            return tag;
        }

        private void ensureInitialized() {
            if (currentPopulation < 0) {
                currentPopulation = Math.max(0, SwordsmithVillageConfig.maxPopulation);
                setDirty();
                return;
            }
            clampToConfiguredMaximum();
        }

        private void clampToConfiguredMaximum() {
            int maxPopulation = Math.max(0, SwordsmithVillageConfig.maxPopulation);
            if (currentPopulation > maxPopulation) {
                currentPopulation = maxPopulation;
                setDirty();
            } else if (currentPopulation < 0) {
                currentPopulation = 0;
                setDirty();
            }
        }

        private int getCurrentPopulation() {
            return currentPopulation;
        }

        private void setCurrentPopulation(int currentPopulation) {
            this.currentPopulation = Math.max(0, Math.min(currentPopulation, Math.max(0, SwordsmithVillageConfig.maxPopulation)));
        }

        private long getLastNoonRollDay() {
            return lastNoonRollDay;
        }

        private void setLastNoonRollDay(long lastNoonRollDay) {
            this.lastNoonRollDay = lastNoonRollDay;
        }
    }
}
