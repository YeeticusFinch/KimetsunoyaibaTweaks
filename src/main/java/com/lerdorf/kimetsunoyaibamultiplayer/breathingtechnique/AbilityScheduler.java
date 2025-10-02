package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Schedules delayed and repeated actions for breathing technique abilities
 */
public class AbilityScheduler {
    private static final Map<UUID, List<ScheduledTask>> playerTasks = new HashMap<>();

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
        playerTasks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(task);
    }

    /**
     * Schedule a repeating action for a duration
     */
    public static void scheduleRepeating(Player player, Runnable action, int intervalTicks, int durationTicks) {
        if (!(player.level() instanceof ServerLevel)) return;

        long currentTick = player.level().getGameTime();
        UUID playerId = player.getUUID();

        ScheduledTask task = new ScheduledTask(action, currentTick, true, intervalTicks, currentTick + durationTicks);
        playerTasks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(task);
    }

    /**
     * Tick all scheduled tasks - should be called from server tick event
     */
    public static void tick(ServerLevel level) {
        long currentTick = level.getGameTime();

        Iterator<Map.Entry<UUID, List<ScheduledTask>>> playerIterator = playerTasks.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, List<ScheduledTask>> entry = playerIterator.next();
            List<ScheduledTask> tasks = entry.getValue();

            Iterator<ScheduledTask> taskIterator = tasks.iterator();
            while (taskIterator.hasNext()) {
                ScheduledTask task = taskIterator.next();

                if (task.repeating) {
                    // Repeating task
                    if (currentTick >= task.endAtTick) {
                        taskIterator.remove();
                    } else if ((currentTick - task.executeAtTick) % task.repeatInterval == 0) {
                        try {
                            task.action.run();
                        } catch (Exception e) {
                            // Task failed, remove it
                            taskIterator.remove();
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
                        taskIterator.remove();
                    }
                }
            }

            if (tasks.isEmpty()) {
                playerIterator.remove();
            }
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
