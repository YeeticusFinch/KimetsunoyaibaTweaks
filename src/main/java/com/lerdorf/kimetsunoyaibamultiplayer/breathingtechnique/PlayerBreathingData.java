package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks current breathing technique and form for each player.
 * Data is persisted to player NBT for server restarts.
 */
public class PlayerBreathingData {
    private static final Map<UUID, PlayerData> playerData = new HashMap<>();
    private static final String NBT_KEY_FORM_INDEX = "CustomBreathingFormIndex";

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

    /**
     * Gets player data and loads it from NBT if not in cache.
     * Use this method when you have access to the Player object.
     *
     * On server: Always syncs from NBT (authoritative source)
     * On client: Uses cached value
     */
    public static PlayerData getOrCreate(Player player) {
        PlayerData data = getOrCreate(player.getUUID());

        // On server, always load from NBT to ensure we have the latest value
        if (!player.level().isClientSide) {
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains(NBT_KEY_FORM_INDEX)) {
                data.currentFormIndex = persistentData.getInt(NBT_KEY_FORM_INDEX);
            }
        }

        return data;
    }

    /**
     * Saves player form data to NBT for persistence.
     * Call this after updating form index on the server.
     */
    public static void saveToNBT(Player player) {
        PlayerData data = playerData.get(player.getUUID());
        if (data != null) {
            player.getPersistentData().putInt(NBT_KEY_FORM_INDEX, data.currentFormIndex);
        }
    }

    /**
     * Loads player form data from NBT.
     * Call this when player joins server.
     */
    public static void loadFromNBT(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(NBT_KEY_FORM_INDEX)) {
            PlayerData data = getOrCreate(player.getUUID());
            data.currentFormIndex = persistentData.getInt(NBT_KEY_FORM_INDEX);
        }
    }

    public static void clear(UUID playerId) {
        playerData.remove(playerId);
    }

    public static void clearAll() {
        playerData.clear();
    }
}
