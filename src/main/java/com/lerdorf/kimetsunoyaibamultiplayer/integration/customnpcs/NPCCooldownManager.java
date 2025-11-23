package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Manages cooldowns for NPC abilities.
 * Stores cooldown data in entity persistent NBT.
 */
public class NPCCooldownManager {

    private static final String COOLDOWN_PREFIX = "kny_npc_cooldown_";
    private static final String LAST_FORM_PREFIX = "kny_npc_last_form_";
    private static final String LAST_ABILITY_USE_TIME = "kny_npc_last_ability_use_time";

    /**
     * Check if an NPC can use an ability (not on cooldown)
     * @param npc The NPC entity
     * @param abilityId Unique ability identifier (e.g., "mist_breathing", "akaza_art")
     * @return true if the ability is off cooldown
     */
    public static boolean canUseAbility(LivingEntity npc, String abilityId) {
        if (npc == null || npc.level().isClientSide) {
            return false;
        }

        // Check if max cooldown time has passed and auto-clear if enabled
        checkAndClearMaxCooldown(npc);

        CompoundTag persistentData = npc.getPersistentData();
        String cooldownKey = COOLDOWN_PREFIX + abilityId;

        if (!persistentData.contains(cooldownKey)) {
            return true; // No cooldown set, ability is available
        }

        long lastUseTime = persistentData.getLong(cooldownKey);
        long currentTime = npc.level().getGameTime();

        return currentTime >= lastUseTime;
    }

    /**
     * Check if the maximum cooldown time has passed and clear all cooldowns if enabled
     * @param npc The NPC entity
     */
    private static void checkAndClearMaxCooldown(LivingEntity npc) {
        if (!CustomNPCConfig.isMaxCooldownClearEnabled()) {
            return;
        }

        CompoundTag persistentData = npc.getPersistentData();

        // Check if we have a last ability use time
        if (!persistentData.contains(LAST_ABILITY_USE_TIME)) {
            return; // No abilities used yet
        }

        long lastAbilityUseTime = persistentData.getLong(LAST_ABILITY_USE_TIME);
        long currentTime = npc.level().getGameTime();
        int maxCooldownTicks = CustomNPCConfig.getMaxCooldownSeconds() * 20;

        // If max cooldown time has passed, clear all cooldowns
        if (currentTime - lastAbilityUseTime >= maxCooldownTicks) {
            clearAllCooldowns(npc);

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] Max cooldown time passed for " + npc.getName().getString() + " - cleared all cooldowns");
            }
        }
    }

    /**
     * Set a cooldown for an ability
     * @param npc The NPC entity
     * @param abilityId Unique ability identifier
     * @param baseCooldownTicks Base cooldown in ticks (before multiplier)
     */
    public static void setCooldown(LivingEntity npc, String abilityId, int baseCooldownTicks) {
        if (npc == null || npc.level().isClientSide) {
            return;
        }

        // Apply config cooldown multiplier
        double multiplier = CustomNPCConfig.getCooldownMultiplier();
        int adjustedCooldown = (int) (baseCooldownTicks * multiplier);

        CompoundTag persistentData = npc.getPersistentData();
        String cooldownKey = COOLDOWN_PREFIX + abilityId;

        long currentTime = npc.level().getGameTime();
        long cooldownEndTime = currentTime + adjustedCooldown;

        persistentData.putLong(cooldownKey, cooldownEndTime);

        // Update last ability use time for max cooldown tracking
        persistentData.putLong(LAST_ABILITY_USE_TIME, currentTime);

        if (CustomNPCConfig.isDebugEnabled()) {
            Log.debug("[KnY Custom NPCs] Set cooldown for " + npc.getName().getString()
                + " - Ability: " + abilityId
                + " - Ticks: " + adjustedCooldown
                + " (" + (adjustedCooldown / 20.0) + "s)");
        }
    }

    /**
     * Get remaining cooldown time in ticks
     * @param npc The NPC entity
     * @param abilityId Unique ability identifier
     * @return Remaining cooldown ticks, or 0 if off cooldown
     */
    public static int getRemainingCooldown(LivingEntity npc, String abilityId) {
        if (npc == null || npc.level().isClientSide) {
            return 0;
        }

        CompoundTag persistentData = npc.getPersistentData();
        String cooldownKey = COOLDOWN_PREFIX + abilityId;

        if (!persistentData.contains(cooldownKey)) {
            return 0;
        }

        long lastUseTime = persistentData.getLong(cooldownKey);
        long currentTime = npc.level().getGameTime();
        long remaining = lastUseTime - currentTime;

        return (int) Math.max(0, remaining);
    }

    /**
     * Clear cooldown for an ability
     * @param npc The NPC entity
     * @param abilityId Unique ability identifier
     */
    public static void clearCooldown(LivingEntity npc, String abilityId) {
        if (npc == null || npc.level().isClientSide) {
            return;
        }

        CompoundTag persistentData = npc.getPersistentData();
        String cooldownKey = COOLDOWN_PREFIX + abilityId;
        persistentData.remove(cooldownKey);
    }

    /**
     * Clear all NPC ability cooldowns for an entity
     * @param npc The NPC entity
     */
    public static void clearAllCooldowns(LivingEntity npc) {
        if (npc == null || npc.level().isClientSide) {
            return;
        }

        CompoundTag persistentData = npc.getPersistentData();

        // Collect keys to remove first to avoid ConcurrentModificationException
        java.util.List<String> keysToRemove = persistentData.getAllKeys().stream()
            .filter(key -> key.startsWith(COOLDOWN_PREFIX))
            .collect(java.util.stream.Collectors.toList());

        // Now remove them
        keysToRemove.forEach(persistentData::remove);
    }

    /**
     * Store the last form used by an NPC for a breathing style
     * @param npc The NPC entity
     * @param breathingStyle The breathing style identifier
     * @param formIndex The form index that was used (0-based)
     */
    public static void setLastFormUsed(LivingEntity npc, String breathingStyle, int formIndex) {
        if (npc == null || npc.level().isClientSide) {
            return;
        }

        CompoundTag persistentData = npc.getPersistentData();
        String lastFormKey = LAST_FORM_PREFIX + breathingStyle;
        persistentData.putInt(lastFormKey, formIndex);
    }

    /**
     * Get the last form used by an NPC for a breathing style
     * @param npc The NPC entity
     * @param breathingStyle The breathing style identifier
     * @return The last form index used, or -1 if none
     */
    public static int getLastFormUsed(LivingEntity npc, String breathingStyle) {
        if (npc == null || npc.level().isClientSide) {
            return -1;
        }

        CompoundTag persistentData = npc.getPersistentData();
        String lastFormKey = LAST_FORM_PREFIX + breathingStyle;

        if (!persistentData.contains(lastFormKey)) {
            return -1;
        }

        return persistentData.getInt(lastFormKey);
    }
}
