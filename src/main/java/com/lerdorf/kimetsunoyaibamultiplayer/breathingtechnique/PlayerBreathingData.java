package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks current breathing technique and form for each player
 */
public class PlayerBreathingData {
    private static final Map<UUID, PlayerData> playerData = new HashMap<>();

    public static class PlayerData {
        private int currentFormIndex = 0;
        private long lastUsedTick = 0;

        public int getCurrentFormIndex() {
            return currentFormIndex;
        }

        public void setCurrentFormIndex(int index) {
            this.currentFormIndex = index;
        }

        public long getLastUsedTick() {
            return lastUsedTick;
        }

        public void setLastUsedTick(long tick) {
            this.lastUsedTick = tick;
        }

        public void cycleForm(int maxForms) {
            currentFormIndex = (currentFormIndex + 1) % maxForms;
        }

        public void cycleFormBackward(int maxForms) {
            currentFormIndex = (currentFormIndex - 1 + maxForms) % maxForms;
        }
    }

    public static PlayerData getOrCreate(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerData());
    }

    public static void clear(UUID playerId) {
        playerData.remove(playerId);
    }

    public static void clearAll() {
        playerData.clear();
    }
}
