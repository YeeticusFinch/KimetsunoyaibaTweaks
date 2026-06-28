package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Schedules delayed and repeated actions for breathing technique abilities
 * Works with any LivingEntity (players, mobs, custom entities)
 */
public class AbilityScheduler {
    // Task buckets are separated per-dimension so scheduler behavior is consistent outside overworld.
    private static final Map<ResourceKey<Level>, Map<UUID, List<ScheduledTask>>> entityTasks = new ConcurrentHashMap<>();

    public static class ScheduledTask {
        public final Runnable action;
        public final long executeAtTick;
        public final boolean repeating;
        public final int repeatInterval;
        public final long endAtTick;

        public ScheduledTask(Runnable action, long executeAtTick, boolean repeating, int repeatInterval, long endAtTick) {
            this.action = action;
            this.executeAtTick = executeAtTick;
            this.repeating = repeating;
            this.repeatInterval = repeatInterval;
            this.endAtTick = endAtTick;
        }
    }

    /**
     * Schedule a one-time action to run after a delay
     */
    public static void scheduleOnce(LivingEntity entity, Runnable action, int delayTicks) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        long currentTick = level.getGameTime();
        UUID entityId = entity.getUUID();
        ResourceKey<Level> dimension = level.dimension();

        ScheduledTask task = new ScheduledTask(action, currentTick + delayTicks, false, 0, 0);
        entityTasks
            .computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(entityId, k -> new CopyOnWriteArrayList<>())
            .add(task);
    }

    /**
     * Schedule a repeating action for a duration
     */
    public static void scheduleRepeating(LivingEntity entity, Runnable action, int intervalTicks, int durationTicks) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        long currentTick = level.getGameTime();
        UUID entityId = entity.getUUID();
        ResourceKey<Level> dimension = level.dimension();

        ScheduledTask task = new ScheduledTask(action, currentTick, true, intervalTicks, currentTick + durationTicks);
        entityTasks
            .computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(entityId, k -> new CopyOnWriteArrayList<>())
            .add(task);
    }

    /**
     * Tick all scheduled tasks - should be called from server tick event
     */
    public static void tick(ServerLevel level) {
        Map<UUID, List<ScheduledTask>> tasksByEntity = entityTasks.get(level.dimension());
        if (tasksByEntity == null || tasksByEntity.isEmpty()) {
            return;
        }

        long currentTick = level.getGameTime();
        List<UUID> emptyEntities = new ArrayList<>();

        for (Map.Entry<UUID, List<ScheduledTask>> entry : tasksByEntity.entrySet()) {
            Entity owner = level.getEntity(entry.getKey());
            if (owner == null) {
                owner = level.getServer() != null ? level.getServer().getPlayerList().getPlayer(entry.getKey()) : null;
            }
            if (owner == null) {
                // Entity may be in an unloaded chunk or transiently between states; keep tasks.
                continue;
            }
            if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive() || livingOwner.isDeadOrDying()) {
                // Owner in this dimension is invalid/dead: safe to remove queued tasks.
                emptyEntities.add(entry.getKey());
                continue;
            }

            List<ScheduledTask> tasks = entry.getValue();
            // Iterate over a snapshot so tasks scheduled during callbacks are not lost.
            // Important: do NOT clear+replace the backing list, otherwise newly scheduled
            // tasks in the same tick can be dropped.
            List<ScheduledTask> snapshot = new ArrayList<>(tasks);
            List<ScheduledTask> tasksToRemove = new ArrayList<>();

            for (ScheduledTask task : snapshot) {
                boolean removeTask = false;

                if (task.repeating) {
                    // Repeating task
                    if (currentTick >= task.endAtTick) {
                        removeTask = true; // Task expired
                    } else if ((currentTick - task.executeAtTick) % task.repeatInterval == 0) {
                        try {
                            task.action.run();
                        } catch (Exception e) {
                            // Task failed, remove it
                            removeTask = true;
                        }
                    }
                } else {
                    // One-time task
                    if (currentTick >= task.executeAtTick) {
                        try {
                            task.action.run();
                        } catch (Exception e) {
                            // Task failed
                        }
                        removeTask = true; // Task executed, remove it
                    }
                }

                if (removeTask) {
                    tasksToRemove.add(task);
                }
            }

            // Remove only completed/failed/expired tasks. Newly scheduled tasks remain.
            for (ScheduledTask task : tasksToRemove) {
                tasks.remove(task);
            }

            if (tasks.isEmpty()) {
                emptyEntities.add(entry.getKey());
            }
        }

        // Remove entities with no tasks
        for (UUID entityId : emptyEntities) {
            tasksByEntity.remove(entityId);
        }

        if (tasksByEntity.isEmpty()) {
            entityTasks.remove(level.dimension());
        }
    }

    /**
     * Tick scheduler for every loaded dimension.
     */
    public static void tickAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tick(level);
        }
    }

    /**
     * Cancel all scheduled tasks for an entity
     */
    public static void cancelAll(UUID entityId) {
        for (Map<UUID, List<ScheduledTask>> tasksByEntity : entityTasks.values()) {
            tasksByEntity.remove(entityId);
        }
        entityTasks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Clear all scheduled tasks
     */
    public static void clearAll() {
        entityTasks.clear();
    }
}
