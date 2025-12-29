package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Kanroji sword animations per-entity instead of per-ItemStack.
 * This avoids issues with ItemStack identity and ensures each entity's
 * sword animates independently.
 */
public class KanrojiSwordEntityAnimationTracker {

    /**
     * Maps entity UUID -> current animation name
     */
    private static final Map<UUID, String> entityAnimations = new ConcurrentHashMap<>();

    /**
     * Maps entity UUID -> animation start time
     */
    private static final Map<UUID, Long> animationStartTimes = new ConcurrentHashMap<>();

    /**
     * Set the current animation for an entity's Kanroji sword.
     *
     * @param entityUUID The UUID of the entity holding the sword
     * @param animationName The animation to play
     */
    public static void setAnimation(UUID entityUUID, String animationName) {
        String previousAnim = entityAnimations.get(entityUUID);
        entityAnimations.put(entityUUID, animationName);
        animationStartTimes.put(entityUUID, System.currentTimeMillis());

        Log.debug("[KanrojiSwordEntityAnimationTracker] Entity {} animation: {} -> {}",
                  entityUUID, previousAnim, animationName);
    }

    /**
     * Get the current animation for an entity's Kanroji sword.
     *
     * @param entityUUID The UUID of the entity holding the sword
     * @return The animation name, or "idle" if none set
     */
    public static String getAnimation(UUID entityUUID) {
        return entityAnimations.getOrDefault(entityUUID, "idle");
    }

    /**
     * Get the time when the current animation started.
     *
     * @param entityUUID The UUID of the entity holding the sword
     * @return Start time in milliseconds, or current time if none set
     */
    public static long getAnimationStartTime(UUID entityUUID) {
        return animationStartTimes.getOrDefault(entityUUID, System.currentTimeMillis());
    }

    /**
     * Check if an entity has an animation set.
     *
     * @param entityUUID The UUID of the entity
     * @return true if the entity has an animation tracked
     */
    public static boolean hasAnimation(UUID entityUUID) {
        return entityAnimations.containsKey(entityUUID);
    }

    /**
     * Clear animation tracking for an entity (e.g., when entity is removed).
     *
     * @param entityUUID The UUID of the entity
     */
    public static void clearAnimation(UUID entityUUID) {
        entityAnimations.remove(entityUUID);
        animationStartTimes.remove(entityUUID);
        Log.debug("[KanrojiSwordEntityAnimationTracker] Cleared animation for entity {}", entityUUID);
    }

    /**
     * Clear all tracked animations (e.g., on world unload).
     */
    public static void clearAll() {
        entityAnimations.clear();
        animationStartTimes.clear();
        Log.debug("[KanrojiSwordEntityAnimationTracker] Cleared all tracked animations");
    }
}
