package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks current breathing technique and form for each player.
 * Data is persisted to player NBT for server restarts.
 */
public class PlayerBreathingData {
    private static final Map<UUID, PlayerData> playerData = new HashMap<>();
    private static final String NBT_KEY_FORM_INDEX = "CustomBreathingFormIndex";
    private static final String NBT_KEY_FORM_INDEX_BY_STYLE = "CustomBreathingFormIndices";
    private static final String NBT_KEY_VARIATION_INDEX = "CustomVariationIndex";
    // OLD KEYS - for migration only
    private static final String NBT_KEY_VARIATION_INDEX_OLD = "CustomBreathingVariationIndex";
    private static final String NBT_KEY_BASE_MOD_VARIATION_OLD = "BaseModVariationIndex";

    public static class PlayerData {
        private int currentFormIndex = 0;
        private final Map<String, Integer> currentFormIndicesByStyle = new HashMap<>();
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

        public int getCurrentFormIndex(String styleKey) {
            String normalizedStyleKey = normalizeStyleKey(styleKey);
            if (currentFormIndicesByStyle.containsKey(normalizedStyleKey)) {
                return currentFormIndicesByStyle.get(normalizedStyleKey);
            }
            return currentFormIndicesByStyle.isEmpty() ? currentFormIndex : 0;
        }

        public void setCurrentFormIndex(String styleKey, int index) {
            String normalizedStyleKey = normalizeStyleKey(styleKey);
            int sanitizedIndex = Math.max(0, index);
            currentFormIndicesByStyle.put(normalizedStyleKey, sanitizedIndex);
            currentFormIndex = sanitizedIndex;
        }

        public Map<String, Integer> getCurrentFormIndicesByStyle() {
            return currentFormIndicesByStyle;
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

        public void cycleForm(String styleKey, int maxForms) {
            if (maxForms <= 0) {
                return;
            }
            setCurrentFormIndex(styleKey, (getCurrentFormIndex(styleKey) + 1) % maxForms);
        }

        public void cycleFormBackward(String styleKey, int maxForms) {
            if (maxForms <= 0) {
                return;
            }
            setCurrentFormIndex(styleKey, (getCurrentFormIndex(styleKey) - 1 + maxForms) % maxForms);
        }
    }

    public static String getTechniqueKey(String techniqueName) {
        return normalizeStyleKey(techniqueName);
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
            loadFormIndicesFromNBT(persistentData, data);
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
            saveFormIndicesToNBT(player.getPersistentData(), data);
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
        loadFormIndicesFromNBT(persistentData, data);
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

    private static void loadFormIndicesFromNBT(CompoundTag persistentData, PlayerData data) {
        data.currentFormIndicesByStyle.clear();
        if (!persistentData.contains(NBT_KEY_FORM_INDEX_BY_STYLE)) {
            return;
        }

        CompoundTag formIndicesTag = persistentData.getCompound(NBT_KEY_FORM_INDEX_BY_STYLE);
        for (String key : formIndicesTag.getAllKeys()) {
            data.currentFormIndicesByStyle.put(key, Math.max(0, formIndicesTag.getInt(key)));
        }
    }

    private static void saveFormIndicesToNBT(CompoundTag persistentData, PlayerData data) {
        CompoundTag formIndicesTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : data.currentFormIndicesByStyle.entrySet()) {
            formIndicesTag.putInt(entry.getKey(), Math.max(0, entry.getValue()));
        }
        persistentData.put(NBT_KEY_FORM_INDEX_BY_STYLE, formIndicesTag);
    }

    private static String normalizeStyleKey(String styleKey) {
        if (styleKey == null || styleKey.isBlank()) {
            return "default";
        }
        return styleKey.trim().toLowerCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_');
    }
}
