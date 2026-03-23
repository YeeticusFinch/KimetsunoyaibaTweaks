package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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
    private static final String NBT_KEY_VARIATION_INDEX = "CustomVariationIndex";
    // OLD KEYS - for migration only
    private static final String NBT_KEY_VARIATION_INDEX_OLD = "CustomBreathingVariationIndex";
    private static final String NBT_KEY_BASE_MOD_VARIATION_OLD = "BaseModVariationIndex";

    public static class PlayerData {
        private int currentFormIndex = 0;
        private long lastUsedTick = 0;

        // Cache for base mod sword data (server-side)
        private double baseModBreathesValue = 0.0;  // Cached breathes value (server NBT is often stale)
        private String baseModFormName = null;  // Cached form name for display
        private int currentVariationIndex = 0; // 0 = base form, 1+ = variations
        private String lastSwordKey = "";
        private double lastBreathesValue = 0.0; // For tracking breathes changes
        private boolean wasSprintingWithSword = false; // For tracking sprint animation sync

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

        public double getBaseModBreathesValue() {
            return baseModBreathesValue;
        }

        public void setBaseModBreathesValue(double value) {
            this.baseModBreathesValue = value;
        }

        public String getBaseModFormName() {
            return baseModFormName;
        }

        public void setBaseModFormName(String name) {
            this.baseModFormName = name;
        }

        public int getCurrentVariationIndex() {
            return currentVariationIndex;
        }

        public void setCurrentVariationIndex(int idx) {
            this.currentVariationIndex = Math.max(0, idx);
        }

        public String getLastSwordKey() {
            return lastSwordKey;
        }

        public void setLastSwordKey(String key) {
            this.lastSwordKey = key != null ? key : "";
        }

        public double getLastBreathesValue() {
            return lastBreathesValue;
        }

        public void setLastBreathesValue(double value) {
            this.lastBreathesValue = value;
        }

        public boolean wasSprintingWithSword() {
            return wasSprintingWithSword;
        }

        public void setWasSprintingWithSword(boolean value) {
            this.wasSprintingWithSword = value;
        }

        public void cycleForm(int maxForms) {
            // Custom swords use simple form indices (0, 1, 2...), not encoded values
            // Just cycle directly without decoding
            currentFormIndex = (currentFormIndex + 1) % maxForms;
        }

        public void cycleFormBackward(int maxForms) {
            // Custom swords use simple form indices (0, 1, 2...), not encoded values
            // Just cycle directly without decoding
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
            // CRITICAL FIX: Also load variation index from NBT
            // This ensures that when we reset variation to 0 on form cycle, it's properly loaded
            if (persistentData.contains(NBT_KEY_VARIATION_INDEX)) {
                data.currentVariationIndex = Math.max(0, persistentData.getInt(NBT_KEY_VARIATION_INDEX));
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
            player.getPersistentData().putInt(NBT_KEY_VARIATION_INDEX, data.currentVariationIndex);
        }
    }

    /**
     * Loads player form data from NBT.
     * Call this when player joins server.
     * Includes migration logic to convert old variation indices to encoded breathes values.
     */
    public static void loadFromNBT(Player player) {
        Log.startupProbeOnce("PlayerBreathingData.loadFromNBT.start");
        CompoundTag persistentData = player.getPersistentData();
        PlayerData data = getOrCreate(player.getUUID());

        // Load current form index (still needed for custom swords)
        if (persistentData.contains(NBT_KEY_FORM_INDEX)) {
            data.currentFormIndex = persistentData.getInt(NBT_KEY_FORM_INDEX);
        }
        if (persistentData.contains(NBT_KEY_VARIATION_INDEX)) {
            data.currentVariationIndex = Math.max(0, persistentData.getInt(NBT_KEY_VARIATION_INDEX));
        }

        // MIGRATION: Convert old variation index to encoded breathes
        if (persistentData.contains(NBT_KEY_VARIATION_INDEX_OLD)) {
            int oldVariation = persistentData.getInt(NBT_KEY_VARIATION_INDEX_OLD);

            if (oldVariation > 0) {
                // Get current breathes value
                double breathes = persistentData.getDouble("breathes");

                if (breathes > 0) {
                    // Decode form ID
                    int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathes);

                    // Encode variation into breathes
                    double encodedBreathes = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.encode(formId, oldVariation);
                    persistentData.putDouble("breathes", encodedBreathes);

                    // Cache the new value
                    data.setBaseModBreathesValue(encodedBreathes);
                }
            }

            // Remove old NBT key
            persistentData.remove(NBT_KEY_VARIATION_INDEX_OLD);
        }

        // MIGRATION: Convert old base mod variation index to encoded breathes
        if (persistentData.contains(NBT_KEY_BASE_MOD_VARIATION_OLD)) {
            int oldVariation = persistentData.getInt(NBT_KEY_BASE_MOD_VARIATION_OLD);

            if (oldVariation > 0) {
                // Get current breathes value
                double breathes = persistentData.getDouble("breathes");

                if (breathes > 0) {
                    // Decode form ID
                    int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(breathes);

                    // Encode variation into breathes
                    double encodedBreathes = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.encode(formId, oldVariation);
                    persistentData.putDouble("breathes", encodedBreathes);

                    // Cache the new value
                    data.setBaseModBreathesValue(encodedBreathes);
                }
            }

            // Remove old NBT key
            persistentData.remove(NBT_KEY_BASE_MOD_VARIATION_OLD);
        }
        Log.startupProbeOnce("PlayerBreathingData.loadFromNBT.end");
    }

    public static void clear(UUID playerId) {
        playerData.remove(playerId);
    }

    public static void clearAll() {
        playerData.clear();
    }
}
