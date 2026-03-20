package com.lerdorf.kimetsunoyaibamultiplayer.raids;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

/**
 * Controls the daylight cycle for Final Selection.
 * While active, this controller drives a single global time across all loaded dimensions.
 */
public class MtFujikasaneDaylightController {
    private static boolean paused = false;
    private static long frozenTime = 0L;

    /**
     * Pause the daylight cycle for Mt Fujikasane.
     * The dimension's time will be frozen at its current value.
     */
    public static void pauseDaylightCycle(ServerLevel level) {
        if (level == null) {
            return;
        }

        if (paused) {
            return;
        }

        paused = true;
        frozenTime = level.getDayTime();
        setGlobalDaylightCycle(level, false);
        applyFrozenTimeToAllLoadedLevels(level);
        Log.debug("[MtFujikasane] Daylight cycle paused at time " + frozenTime);
    }

    /**
     * Force a specific absolute day time while keeping the Mt Fujikasane cycle paused.
     * This enables scripted time progression for raids without affecting other dimensions.
     */
    public static void setPausedTime(ServerLevel level, long absoluteDayTime) {
        if (level == null) {
            return;
        }

        paused = true;
        frozenTime = absoluteDayTime;
        setGlobalDaylightCycle(level, false);
        applyFrozenTimeToAllLoadedLevels(level);
    }

    /**
     * Resume the daylight cycle for Mt Fujikasane.
     */
    public static void resumeDaylightCycle(ServerLevel level) {
        if (level == null) {
            return;
        }

        paused = false;
        frozenTime = 0L;
        setGlobalDaylightCycle(level, true);
        Log.debug("[MtFujikasane] Daylight cycle resumed");
    }

    public static void resetRuntimeState(MinecraftServer server) {
        paused = false;
        frozenTime = 0L;

        if (server == null) {
            return;
        }

        for (ServerLevel loadedLevel : server.getAllLevels()) {
            loadedLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, server);
        }
    }

    /**
     * Called every tick for the Mt Fujikasane dimension.
     * If paused, resets the time to the frozen value.
     */
    public static void tick(ServerLevel level) {
        if (!paused || level == null) {
            return;
        }

        // Guard against other systems flipping this gamerule during an active Final Selection timeline.
        setGlobalDaylightCycle(level, false);
        applyFrozenTimeToAllLoadedLevels(level);
    }

    public static boolean isPaused() {
        return paused;
    }

    public static long getFrozenTime() {
        return frozenTime;
    }

    private static void applyFrozenTimeToAllLoadedLevels(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            level.setDayTime(frozenTime);
            return;
        }

        for (ServerLevel loadedLevel : server.getAllLevels()) {
            loadedLevel.setDayTime(frozenTime);
        }
    }

    private static void setGlobalDaylightCycle(ServerLevel level, boolean enabled) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(enabled, null);
            return;
        }

        for (ServerLevel loadedLevel : server.getAllLevels()) {
            loadedLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(enabled, server);
        }
    }
}
