package com.lerdorf.kimetsunoyaibamultiplayer.gravity.field;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GravityFieldManager {
    private static final int SWITCH_COOLDOWN_TICKS = 20;
    private static final double GRAVITY_BLOCK_PRIORITY = 100.0D;
    private static final Map<ResourceKey<Level>, LinkedHashMap<UUID, GravityEffectSource>> SOURCES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, GravityBlockEntity>> GRAVITY_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, TrackedGravity> TRACKED_ENTITIES = new ConcurrentHashMap<>();

    private GravityFieldManager() {
    }

    public static void registerBlock(GravityBlockEntity field) {
        if (!(field.getLevel() instanceof ServerLevel level) || !field.isGravityFieldActive()) {
            unregisterBlock(field);
            return;
        }
        GRAVITY_BLOCKS.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>())
            .put(field.getBlockPos().immutable(), field);
    }

    public static void unregisterBlock(GravityBlockEntity field) {
        if (field.getLevel() == null) {
            return;
        }
        Map<BlockPos, GravityBlockEntity> fields = GRAVITY_BLOCKS.get(field.getLevel().dimension());
        if (fields != null) {
            fields.remove(field.getBlockPos());
            if (fields.isEmpty()) {
                GRAVITY_BLOCKS.remove(field.getLevel().dimension());
            }
        }
    }

    public static void register(GravityField field) {
        if (!field.enabled()) {
            unregister(field.dimension(), field.id());
            return;
        }
        SOURCES.computeIfAbsent(field.dimension(), key -> new LinkedHashMap<>()).put(field.id(), field);
    }

    public static void unregister(ResourceKey<Level> dimension, UUID id) {
        LinkedHashMap<UUID, GravityEffectSource> sources = SOURCES.get(dimension);
        if (sources != null) {
            sources.remove(id);
            if (sources.isEmpty()) {
                SOURCES.remove(dimension);
            }
        }
    }

    public static Optional<GravityEffectSource> resolve(Entity entity) {
        LinkedHashMap<UUID, GravityEffectSource> sources = SOURCES.get(entity.level().dimension());
        if (sources == null || sources.isEmpty()) {
            return Optional.empty();
        }
        return sources.entrySet().stream()
            .filter(entry -> entry.getValue().affects(entity))
            .max(Comparator
                .<Map.Entry<UUID, GravityEffectSource>>comparingDouble(entry -> entry.getValue().getPriority(entity))
                .thenComparing(entry -> entry.getKey().toString(), Comparator.reverseOrder()))
            .map(Map.Entry::getValue);
    }

    public static Optional<Direction> resolveDirectionAt(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Direction bestDirection = null;
        double bestPriority = Double.NEGATIVE_INFINITY;
        long bestTieBreaker = Long.MAX_VALUE;

        Map<BlockPos, GravityBlockEntity> fields = GRAVITY_BLOCKS.get(level.dimension());
        if (fields != null && !fields.isEmpty()) {
            fields.entrySet().removeIf(entry -> entry.getValue().isRemoved()
                || entry.getValue().getLevel() != level
                || !entry.getValue().isGravityFieldActive());

            for (Map.Entry<BlockPos, GravityBlockEntity> entry : fields.entrySet()) {
                GravityBlockEntity field = entry.getValue();
                if (field.getFieldBox().contains(center)) {
                    long tieBreaker = entry.getKey().asLong();
                    if (GRAVITY_BLOCK_PRIORITY > bestPriority
                        || (Double.compare(GRAVITY_BLOCK_PRIORITY, bestPriority) == 0 && tieBreaker < bestTieBreaker)) {
                        bestPriority = GRAVITY_BLOCK_PRIORITY;
                        bestTieBreaker = tieBreaker;
                        bestDirection = field.getWorldGravityDirection();
                    }
                }
            }
        }

        LinkedHashMap<UUID, GravityEffectSource> sources = SOURCES.get(level.dimension());
        if (sources != null && !sources.isEmpty()) {
            for (Map.Entry<UUID, GravityEffectSource> entry : sources.entrySet()) {
                if (!(entry.getValue() instanceof GravityField field) || !field.enabled() || !field.box().contains(center)) {
                    continue;
                }

                double priority = field.priority();
                long tieBreaker = field.sourcePos().asLong();
                if (priority > bestPriority || (Double.compare(priority, bestPriority) == 0 && tieBreaker < bestTieBreaker)) {
                    bestPriority = priority;
                    bestTieBreaker = tieBreaker;
                    bestDirection = field.gravityDirection();
                }
            }
        }

        return Optional.ofNullable(bestDirection);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        if (!KNYGravity.isEnabled()) {
            TRACKED_ENTITIES.clear();
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            resolveLevel(level);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level) || !KNYGravity.isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!KNYGravity.canChangeGravity(entity)) {
            return;
        }
        Map<UUID, DesiredGravity> desired = new HashMap<>();
        addGravityBlockFields(level, entity, desired);
        addLegacyFields(level, entity, desired);
        if (desired.isEmpty() && entity instanceof Player) {
            addDesired(desired, entity, Direction.DOWN, Double.NEGATIVE_INFINITY, Long.MAX_VALUE, false);
        }
        applyDesiredGravity(level, desired);
    }

    private static void resolveLevel(ServerLevel level) {
        Map<UUID, DesiredGravity> desired = new HashMap<>();
        addGravityBlockFields(level, desired);
        addLegacyFields(level, desired);
        addTrackedEntitiesWithoutFields(level, desired);
        applyDesiredGravity(level, desired);
    }

    private static void addGravityBlockFields(ServerLevel level, Map<UUID, DesiredGravity> desired) {
        Map<BlockPos, GravityBlockEntity> fields = GRAVITY_BLOCKS.get(level.dimension());
        if (fields == null || fields.isEmpty()) {
            return;
        }

        fields.entrySet().removeIf(entry -> entry.getValue().isRemoved()
            || entry.getValue().getLevel() != level
            || !entry.getValue().isGravityFieldActive());

        List<Map.Entry<BlockPos, GravityBlockEntity>> sortedFields = new ArrayList<>(fields.entrySet());
        sortedFields.sort(Comparator.comparingLong(entry -> entry.getKey().asLong()));
        for (Map.Entry<BlockPos, GravityBlockEntity> entry : sortedFields) {
            GravityBlockEntity field = entry.getValue();
            Direction direction = field.getWorldGravityDirection();
            long tieBreaker = entry.getKey().asLong();
            for (Entity entity : level.getEntitiesOfClass(Entity.class, field.getFieldBox(), KNYGravity::canChangeGravity)) {
                addDesired(desired, entity, direction, GRAVITY_BLOCK_PRIORITY, tieBreaker, true);
            }
        }
    }

    private static void addGravityBlockFields(ServerLevel level, Entity entity, Map<UUID, DesiredGravity> desired) {
        Map<BlockPos, GravityBlockEntity> fields = GRAVITY_BLOCKS.get(level.dimension());
        if (fields == null || fields.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, GravityBlockEntity> entry : fields.entrySet()) {
            GravityBlockEntity field = entry.getValue();
            if (field.isRemoved() || field.getLevel() != level || !field.isGravityFieldActive()) {
                continue;
            }
            if (field.getFieldBox().intersects(entity.getBoundingBox())) {
                addDesired(desired, entity, field.getWorldGravityDirection(), GRAVITY_BLOCK_PRIORITY, entry.getKey().asLong(), true);
            }
        }
    }

    private static void addLegacyFields(ServerLevel level, Map<UUID, DesiredGravity> desired) {
        LinkedHashMap<UUID, GravityEffectSource> sources = SOURCES.get(level.dimension());
        if (sources == null || sources.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, GravityEffectSource> entry : sources.entrySet()) {
            if (!(entry.getValue() instanceof GravityField field) || !field.enabled()) {
                continue;
            }
            for (Entity entity : level.getEntitiesOfClass(Entity.class, field.box(), KNYGravity::canChangeGravity)) {
                if (field.affects(entity)) {
                    addDesired(desired, entity, field.getDirection(entity), field.getPriority(entity), field.sourcePos().asLong(), true);
                }
            }
        }
    }

    private static void addLegacyFields(ServerLevel level, Entity entity, Map<UUID, DesiredGravity> desired) {
        LinkedHashMap<UUID, GravityEffectSource> sources = SOURCES.get(level.dimension());
        if (sources == null || sources.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, GravityEffectSource> entry : sources.entrySet()) {
            GravityEffectSource source = entry.getValue();
            if (source.affects(entity)) {
                long tieBreaker = source instanceof GravityField field ? field.sourcePos().asLong() : entry.getKey().getLeastSignificantBits();
                addDesired(desired, entity, source.getDirection(entity), source.getPriority(entity), tieBreaker, true);
            }
        }
    }

    private static void addTrackedEntitiesWithoutFields(ServerLevel level, Map<UUID, DesiredGravity> desired) {
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if (!desired.containsKey(id) && KNYGravity.canChangeGravity(player)) {
                addDesired(desired, player, Direction.DOWN, Double.NEGATIVE_INFINITY, Long.MAX_VALUE, false);
            }
        }

        for (Map.Entry<UUID, TrackedGravity> entry : new ArrayList<>(TRACKED_ENTITIES.entrySet())) {
            if (!entry.getValue().dimension().equals(level.dimension()) || desired.containsKey(entry.getKey())) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !KNYGravity.canChangeGravity(entity)) {
                TRACKED_ENTITIES.remove(entry.getKey());
                continue;
            }
            addDesired(desired, entity, Direction.DOWN, Double.NEGATIVE_INFINITY, Long.MAX_VALUE, false);
        }
    }

    private static void addDesired(Map<UUID, DesiredGravity> desired, Entity entity, Direction direction, double priority, long tieBreaker, boolean fromField) {
        UUID id = entity.getUUID();
        DesiredGravity existing = desired.get(id);
        if (existing == null
            || priority > existing.priority()
            || (Double.compare(priority, existing.priority()) == 0 && tieBreaker < existing.tieBreaker())) {
            desired.put(id, new DesiredGravity(entity, direction, priority, tieBreaker, fromField));
        }
    }

    private static void applyDesiredGravity(ServerLevel level, Map<UUID, DesiredGravity> desired) {
        long gameTime = level.getGameTime();
        for (Map.Entry<UUID, DesiredGravity> entry : desired.entrySet()) {
            UUID id = entry.getKey();
            DesiredGravity target = entry.getValue();
            TrackedGravity tracked = TRACKED_ENTITIES.get(id);
            Direction currentDirection = tracked == null ? KNYGravity.getBaseGravityDirection(target.entity()) : tracked.direction();

            if (currentDirection == target.direction()) {
                if (target.fromField()) {
                    long lastSwitchTick = tracked == null ? Long.MIN_VALUE : tracked.lastSwitchTick();
                    TRACKED_ENTITIES.put(id, new TrackedGravity(level.dimension(), target.direction(), lastSwitchTick));
                } else if (target.direction() == Direction.DOWN) {
                    TRACKED_ENTITIES.remove(id);
                }
                continue;
            }

            long lastSwitchTick = tracked == null ? Long.MIN_VALUE : tracked.lastSwitchTick();
            if (lastSwitchTick != Long.MIN_VALUE && gameTime - lastSwitchTick < SWITCH_COOLDOWN_TICKS) {
                if (tracked != null) {
                    TRACKED_ENTITIES.put(id, new TrackedGravity(level.dimension(), tracked.direction(), tracked.lastSwitchTick()));
                }
                continue;
            }

            KNYGravity.setBaseGravityDirection(target.entity(), target.direction());
            if (target.fromField() || target.direction() != Direction.DOWN) {
                TRACKED_ENTITIES.put(id, new TrackedGravity(level.dimension(), target.direction(), gameTime));
            } else {
                TRACKED_ENTITIES.remove(id);
            }
        }
    }

    private record DesiredGravity(Entity entity, Direction direction, double priority, long tieBreaker, boolean fromField) {
    }

    private record TrackedGravity(ResourceKey<Level> dimension, Direction direction, long lastSwitchTick) {
    }
}
