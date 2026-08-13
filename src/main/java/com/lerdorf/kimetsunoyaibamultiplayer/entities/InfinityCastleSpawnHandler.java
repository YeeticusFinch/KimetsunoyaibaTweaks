package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.InfinityCastleCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityFieldManager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfinityCastleSpawnHandler {
    private static final double LOCAL_DEMON_CAP_RADIUS = 96.0D;
    private static final int LOCAL_DEMON_CAP = 18;
    private static final int SPAWN_INTERVAL_TICKS = 100;
    private static final int PLAYER_SPAWN_ATTEMPTS = 12;
    private static final int SURFACE_SCAN_DISTANCE = 10;
    private static final int MIN_PLAYER_DISTANCE = 24;
    private static final int MAX_PLAYER_DISTANCE = 64;
    private static final int VERTICAL_RANGE = 32;

    private InfinityCastleSpawnHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        ServerLevel serverLevel = event.getLevel().getLevel();
        if (serverLevel == null || !isCastle(serverLevel) || !isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        EntityType<?> entityType = event.getEntityType();
        if (!isDemonType(entityType)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (shouldDenyCastleDemonSpawn(serverLevel, entityType, pos)) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (shouldRandomlyDenyCastleSpawnRate(serverLevel)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        event.setResult(isSafeSpawnSurface(serverLevel, pos, getSpawnGravity(serverLevel, pos), entityType)
            ? Event.Result.ALLOW
            : Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        ServerLevel serverLevel = event.getLevel().getLevel();
        if (serverLevel == null
            || !isCastle(serverLevel)
            || !isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        Mob mob = event.getEntity();
        if (mob == null || !EntityTagHelper.isDemon(mob)) {
            return;
        }

        BlockPos pos = mob.blockPosition();
        EntityType<?> entityType = mob.getType();
        if (shouldDenyCastleDemonSpawn(serverLevel, entityType, pos)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        event.setResult(isSafeSpawnSurface(serverLevel, pos, getSpawnGravity(serverLevel, pos), entityType)
            ? Event.Result.ALLOW
            : Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
            || !isCastle(serverLevel)
            || !isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        Mob mob = event.getEntity();
        if (mob == null || !EntityTagHelper.isDemon(mob)) {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        EntityType<?> entityType = mob.getType();
        if (shouldDenyCastleDemonSpawn(serverLevel, entityType, pos)) {
            cancelSpawn(event);
            return;
        }

        Direction gravityDirection = getSpawnGravity(serverLevel, pos);
        if (!isSafeSpawnSurface(serverLevel, pos, gravityDirection, entityType)) {
            cancelSpawn(event);
            return;
        }

        applySpawnGravity(mob, gravityDirection);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        if (event.getServer().getTickCount() % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!isCastle(level) || level.getDifficulty() == Difficulty.PEACEFUL) {
                continue;
            }
            if (isCastleDemonSpawnRateDisabled() || isCastleLoadedDemonCapReached(level)) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                trySpawnNearPlayer(level, player);
            }
        }
    }

    private static void trySpawnNearPlayer(ServerLevel level, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        if (isCastleDemonSpawnRateDisabled() || isCastleLoadedDemonCapReached(level)) {
            return;
        }
        if (countNearbyDemons(level, playerPos, LOCAL_DEMON_CAP_RADIUS) >= LOCAL_DEMON_CAP) {
            return;
        }

        List<EntityType<?>> demonTypes = getDemonSpawnTypes();
        if (demonTypes.isEmpty()) {
            return;
        }

        int spawnAttempts = getScaledPlayerSpawnAttempts(level);
        for (int attempt = 0; attempt < spawnAttempts; attempt++) {
            EntityType<?> entityType = demonTypes.get(level.random.nextInt(demonTypes.size()));
            if (shouldDenyCastleDemonSpawn(level, entityType, playerPos)) {
                continue;
            }

            BlockPos randomPos = randomPosAround(level, playerPos);
            BlockPos spawnPos = findSafeSpawnPos(level, randomPos, entityType);
            if (spawnPos == null || player.distanceToSqr(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D) < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) {
                continue;
            }
            if (shouldDenyCastleDemonSpawn(level, entityType, spawnPos)) {
                continue;
            }

            Entity created = entityType.create(level);
            if (!(created instanceof Mob mob)) {
                continue;
            }

            Direction gravityDirection = getSpawnGravity(level, spawnPos);
            mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            applySpawnGravity(mob, gravityDirection);
            if (!level.noCollision(mob) || !isSafeSpawnSurface(level, spawnPos, gravityDirection, entityType)) {
                mob.discard();
                continue;
            }

            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null, null);
            if (level.addFreshEntity(mob)) {
                return;
            }
        }
    }

    private static BlockPos randomPosAround(ServerLevel level, BlockPos center) {
        int radius = MIN_PLAYER_DISTANCE + level.random.nextInt(MAX_PLAYER_DISTANCE - MIN_PLAYER_DISTANCE + 1);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
        int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
        int y = center.getY() + level.random.nextInt(VERTICAL_RANGE * 2 + 1) - VERTICAL_RANGE;
        return new BlockPos(x, y, z);
    }

    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos origin, EntityType<?> entityType) {
        Direction primaryGravity = getSpawnGravity(level, origin);
        for (int step = -SURFACE_SCAN_DISTANCE; step <= SURFACE_SCAN_DISTANCE; step++) {
            BlockPos candidate = origin.relative(primaryGravity, step);
            Direction candidateGravity = getSpawnGravity(level, candidate);
            if (isSafeSpawnSurface(level, candidate, candidateGravity, entityType)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSafeSpawnSurface(ServerLevel level, BlockPos pos, Direction gravityDirection, EntityType<?> entityType) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockPos supportPos = pos.relative(gravityDirection);
        BlockPos headPos = pos.relative(gravityDirection.getOpposite());
        if (!level.isLoaded(supportPos) || !level.isLoaded(headPos)) {
            return false;
        }

        BlockState supportState = level.getBlockState(supportPos);
        if (!supportState.isFaceSturdy(level, supportPos, gravityDirection.getOpposite())) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(headPos).isEmpty()) {
            return false;
        }

        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        return level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty();
    }

    private static Direction getSpawnGravity(ServerLevel level, BlockPos pos) {
        if (!KNYGravity.isEnabled()) {
            return Direction.DOWN;
        }
        return GravityFieldManager.resolveDirectionAt(level, pos).orElse(Direction.DOWN);
    }

    private static void applySpawnGravity(Mob mob, Direction gravityDirection) {
        if (gravityDirection != Direction.DOWN && KNYGravity.isEnabled() && KNYGravity.canChangeGravity(mob)) {
            KNYGravity.setBaseGravityDirection(mob, gravityDirection);
        }
    }

    private static int countNearbyDemons(ServerLevel level, BlockPos pos, double radius) {
        return level.getEntities((Entity) null, new AABB(pos).inflate(radius),
            entity -> entity.isAlive() && entity instanceof Mob mob && EntityTagHelper.isDemon(mob)).size();
    }

    private static boolean shouldDenyCastleDemonSpawn(ServerLevel level, EntityType<?> entityType, BlockPos pos) {
        if (isCastleDemonSpawnRateDisabled() || isCastleLoadedDemonCapReached(level)) {
            return true;
        }
        if (isMuzanType(entityType)) {
            return hasMuzanNearby(level, pos);
        }
        return isTwelveKizukiType(entityType) && hasSameKizukiNearby(level, entityType, pos);
    }

    private static boolean isCastleDemonSpawnRateDisabled() {
        return CustomProgressionConfig.getInfinityCastleDemonSpawnRate() <= 0.0D;
    }

    private static boolean shouldRandomlyDenyCastleSpawnRate(ServerLevel level) {
        double spawnRate = CustomProgressionConfig.getInfinityCastleDemonSpawnRate();
        return spawnRate < 1.0D && level.random.nextDouble() >= spawnRate;
    }

    private static int getScaledPlayerSpawnAttempts(ServerLevel level) {
        double spawnRate = CustomProgressionConfig.getInfinityCastleDemonSpawnRate();
        if (spawnRate <= 0.0D) {
            return 0;
        }
        double scaled = PLAYER_SPAWN_ATTEMPTS * spawnRate;
        int attempts = (int) Math.floor(scaled);
        if (level.random.nextDouble() < scaled - attempts) {
            attempts++;
        }
        return Math.max(1, attempts);
    }

    private static boolean isCastleLoadedDemonCapReached(ServerLevel level) {
        int capPerPlayer = CustomProgressionConfig.getInfinityCastleDemonSpawnCapPerPlayer();
        if (capPerPlayer <= 0) {
            return true;
        }
        int playerCount = Math.max(1, level.players().size());
        int maxLoadedDemons = capPerPlayer * playerCount;
        return countLoadedCastleDemons(level) >= maxLoadedDemons;
    }

    private static int countLoadedCastleDemons(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && entity instanceof Mob mob && EntityTagHelper.isDemon(mob)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasSameKizukiNearby(ServerLevel level, EntityType<?> entityType, BlockPos pos) {
        int radius = CustomProgressionConfig.getInfinityCastleUniqueDemonHorizontalRadius();
        if (radius <= 0) {
            return false;
        }
        return level.getEntities((Entity) null, horizontalSearchBox(level, pos, radius),
            entity -> entity.isAlive()
                && entity.getType() == entityType
                && horizontalDistanceSqr(entity.blockPosition(), pos) <= (double) radius * radius).size() > 0;
    }

    private static boolean hasMuzanNearby(ServerLevel level, BlockPos pos) {
        int radius = CustomProgressionConfig.getInfinityCastleUniqueDemonHorizontalRadius();
        if (radius <= 0) {
            return false;
        }
        return level.getEntities((Entity) null, horizontalSearchBox(level, pos, radius),
            entity -> entity.isAlive()
                && isMuzanType(entity.getType())
                && horizontalDistanceSqr(entity.blockPosition(), pos) <= (double) radius * radius).size() > 0;
    }

    private static AABB horizontalSearchBox(ServerLevel level, BlockPos pos, double radius) {
        return new AABB(
            pos.getX() - radius,
            level.getMinBuildHeight(),
            pos.getZ() - radius,
            pos.getX() + radius + 1.0D,
            level.getMaxBuildHeight(),
            pos.getZ() + radius + 1.0D
        );
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean isCastle(ServerLevel level) {
        return InfinityCastleCompat.isCastleDimension(level.dimension());
    }

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
            || spawnType == MobSpawnType.CHUNK_GENERATION
            || spawnType == MobSpawnType.STRUCTURE
            || spawnType == MobSpawnType.SPAWNER
            || spawnType == MobSpawnType.REINFORCEMENT
            || spawnType == MobSpawnType.PATROL;
    }

    private static boolean isDemonType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }
        if (entityType.is(EntityTagHelper.DEMON)) {
            return true;
        }
        return DemonRegistry.isRegistered(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    private static boolean isTwelveKizukiType(EntityType<?> entityType) {
        return entityType != null && entityType.is(EntityTagHelper.TWELVE_KIZUKI);
    }

    private static boolean isMuzanType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityId != null && entityId.getPath().contains("muzan");
    }

    private static List<EntityType<?>> getDemonSpawnTypes() {
        Set<EntityType<?>> types = new LinkedHashSet<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(EntityTagHelper.DEMON)
            .ifPresent(holders -> {
                for (Holder<EntityType<?>> holder : holders) {
                    EntityType<?> type = holder.value();
                    if (type != null) {
                        types.add(type);
                    }
                }
            });
        for (DemonRegistry.RegisteredDemon registeredDemon : DemonRegistry.getAll()) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(registeredDemon.getEntityId()).ifPresent(types::add);
        }
        return new ArrayList<>(types);
    }

    private static void cancelSpawn(MobSpawnEvent.FinalizeSpawn event) {
        event.setSpawnCancelled(true);
        event.setCanceled(true);
    }
}
