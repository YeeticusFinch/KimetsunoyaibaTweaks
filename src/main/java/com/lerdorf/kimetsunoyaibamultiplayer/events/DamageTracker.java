package com.lerdorf.kimetsunoyaibamultiplayer.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks damage history between entities to provide redundancy for anger/hostility detection.
 * This helps resolve cases where isAngry() returns false even when entities are actively fighting.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class DamageTracker {

    // Maps entity UUID -> Set of UUIDs that have damaged this entity
    private static final Map<UUID, Set<UUID>> damageHistory = new ConcurrentHashMap<>();

    // How long to remember damage history (in milliseconds)
    private static final long DAMAGE_MEMORY_DURATION = 30000; // 30 seconds

    // Maps entity UUID -> Map of attacker UUID -> last damage timestamp
    private static final Map<UUID, Map<UUID, Long>> damageTimestamps = new ConcurrentHashMap<>();

    /**
     * Records when one entity damages another.
     * This creates a bidirectional damage relationship.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        try {
            LivingEntity target = event.getEntity();

            // Get the source of the damage
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                UUID targetUuid = target.getUUID();
                UUID attackerUuid = attacker.getUUID();
                long currentTime = System.currentTimeMillis();

                // Record that attacker damaged target
                damageHistory.computeIfAbsent(targetUuid, k -> ConcurrentHashMap.newKeySet()).add(attackerUuid);
                damageTimestamps.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>()).put(attackerUuid, currentTime);

                // Also record the reverse relationship (target has been damaged by attacker)
                damageHistory.computeIfAbsent(attackerUuid, k -> ConcurrentHashMap.newKeySet()).add(targetUuid);
                damageTimestamps.computeIfAbsent(attackerUuid, k -> new ConcurrentHashMap<>()).put(targetUuid, currentTime);
            }
        } catch (Exception e) {
            // Never use log4j in exception handlers (causes LinkageError crashes)
            System.err.println("[DamageTracker] Error tracking damage: " + e.getMessage());
        }
    }

    /**
     * Checks if one entity has recently damaged another (within DAMAGE_MEMORY_DURATION).
     * This provides redundancy for the isAngry() check.
     *
     * @param entity The entity to check
     * @param other The other entity
     * @return true if entity has damaged or been damaged by other recently
     */
    public static boolean hasDamageHistory(LivingEntity entity, LivingEntity other) {
        if (entity == null || other == null) {
            return false;
        }

        UUID entityUuid = entity.getUUID();
        UUID otherUuid = other.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if entity has damage history with other
        Map<UUID, Long> entityTimestamps = damageTimestamps.get(entityUuid);
        if (entityTimestamps != null) {
            Long lastDamageTime = entityTimestamps.get(otherUuid);
            if (lastDamageTime != null && (currentTime - lastDamageTime) < DAMAGE_MEMORY_DURATION) {
                return true;
            }
        }

        // Check the reverse (other has damage history with entity)
        Map<UUID, Long> otherTimestamps = damageTimestamps.get(otherUuid);
        if (otherTimestamps != null) {
            Long lastDamageTime = otherTimestamps.get(entityUuid);
            if (lastDamageTime != null && (currentTime - lastDamageTime) < DAMAGE_MEMORY_DURATION) {
                return true;
            }
        }

        return false;
    }

    /**
     * Cleans up old damage history entries to prevent memory leaks.
     * Should be called periodically (e.g., every few seconds).
     */
    public static void cleanupOldHistory() {
        long currentTime = System.currentTimeMillis();

        damageTimestamps.entrySet().removeIf(entry -> {
            Map<UUID, Long> timestamps = entry.getValue();
            timestamps.entrySet().removeIf(e -> (currentTime - e.getValue()) > DAMAGE_MEMORY_DURATION);
            return timestamps.isEmpty();
        });

        damageHistory.entrySet().removeIf(entry -> {
            UUID entityUuid = entry.getKey();
            Map<UUID, Long> timestamps = damageTimestamps.get(entityUuid);
            return timestamps == null || timestamps.isEmpty();
        });
    }

    /**
     * Clears all damage history for an entity.
     * Useful when an entity dies or is removed from the world.
     *
     * @param entity The entity whose history should be cleared
     */
    public static void clearHistory(LivingEntity entity) {
        if (entity != null) {
            UUID uuid = entity.getUUID();
            damageHistory.remove(uuid);
            damageTimestamps.remove(uuid);
        }
    }
}
