package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Schedules delayed and repeated actions for breathing technique abilities
 */
public class AbilityScheduler {
    private static final Map<UUID, List<ScheduledTask>> playerTasks = new ConcurrentHashMap<>();

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
    public static void scheduleOnce(Player player, Runnable action, int delayTicks) {
        if (!(player.level() instanceof ServerLevel)) return;

        long currentTick = player.level().getGameTime();
        UUID playerId = player.getUUID();

        ScheduledTask task = new ScheduledTask(action, currentTick + delayTicks, false, 0, 0);
        playerTasks.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()).add(task);
    }

    /**
     * Schedule a repeating action for a duration
     */
    public static void scheduleRepeating(Player player, Runnable action, int intervalTicks, int durationTicks) {
        if (!(player.level() instanceof ServerLevel)) return;

        long currentTick = player.level().getGameTime();
        UUID playerId = player.getUUID();

        ScheduledTask task = new ScheduledTask(action, currentTick, true, intervalTicks, currentTick + durationTicks);
        playerTasks.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()).add(task);
    }

    /**
     * Tick all scheduled tasks - should be called from server tick event
     */
    public static void tick(ServerLevel level) {
        long currentTick = level.getGameTime();
        List<UUID> emptyPlayers = new ArrayList<>();

        for (Map.Entry<UUID, List<ScheduledTask>> entry : playerTasks.entrySet()) {
            List<ScheduledTask> tasks = entry.getValue();
            List<ScheduledTask> tasksToKeep = new ArrayList<>();

            for (ScheduledTask task : tasks) {
                boolean keepTask = true;

                if (task.repeating) {
                    // Repeating task
                    if (currentTick >= task.endAtTick) {
                        keepTask = false; // Task expired
                    } else if ((currentTick - task.executeAtTick) % task.repeatInterval == 0) {
                        try {
                            task.action.run();
                        } catch (Exception e) {
                            // Task failed, remove it
                            keepTask = false;
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
                        keepTask = false; // Task executed, remove it
                    }
                }

                if (keepTask) {
                    tasksToKeep.add(task);
                }
            }

            // Replace the task list entirely (thread-safe for CopyOnWriteArrayList)
            tasks.clear();
            tasks.addAll(tasksToKeep);

            if (tasks.isEmpty()) {
                emptyPlayers.add(entry.getKey());
            }
        }

        // Remove players with no tasks
        for (UUID playerId : emptyPlayers) {
            playerTasks.remove(playerId);
        }
    }

    /**
     * Cancel all scheduled tasks for a player
     */
    public static void cancelAll(UUID playerId) {
        playerTasks.remove(playerId);
    }

    /**
     * Clear all scheduled tasks
     */
    public static void clearAll() {
        playerTasks.clear();
    }
}
