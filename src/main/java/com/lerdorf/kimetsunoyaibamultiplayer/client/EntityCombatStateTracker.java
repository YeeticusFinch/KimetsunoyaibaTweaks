package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSheathRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tracks combat state for living entities to determine when to display swords on their back/hip.
 * Uses WeakHashMap to automatically clean up entries when entities are garbage collected.
 *
 * Combat state includes a 10-second cooldown - entity remains "in combat" for 10 seconds
 * after their last combat activity.
 */
public class EntityCombatStateTracker {

    // CRITICAL: Use WeakHashMap to auto-cleanup when entities are garbage collected
    private static final Map<LivingEntity, Boolean> combatStates = new WeakHashMap<>();

    // Track last combat time for each entity (for 10-second cooldown)
    private static final Map<LivingEntity, Long> lastCombatTime = new WeakHashMap<>();

    // Combat cooldown in milliseconds (10 seconds)
    private static final long COMBAT_COOLDOWN_MS = 10000;

    /**
     * Check if entity is currently in combat.
     *
     * An entity is considered "in combat" if:
     * - Has an active attack target (for Mob entities)
     * - Recently took damage (within 1 second)
     * - Is executing a breathing form ability (for BreathingSlayerEntity)
     * - Within 10 seconds of last combat activity (cooldown period)
     */
    public static boolean isInCombat(LivingEntity entity) {
        if (entity == null) return false;

        boolean activelyFighting = false;
        long currentTime = System.currentTimeMillis();

        // Check active target for Mob entities (ensure it's alive and not removed)
        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive() && !target.isRemoved()) {
                Log.debug("EntityCombatStateTracker: {} has target {}, in combat",
                    entity.getType().getDescriptionId(), target.getType().getDescriptionId());
                activelyFighting = true;
            }
        }

        // Check if recently damaged (within 1 second = 20 ticks)
        int timeSinceHurt = entity.tickCount - entity.getLastHurtByMobTimestamp();
        if (timeSinceHurt < 20) {
            Log.debug("EntityCombatStateTracker: {} recently hurt ({} ticks ago), in combat",
                entity.getType().getDescriptionId(), timeSinceHurt);
            activelyFighting = true;
        }

        // For BreathingSlayerEntity, check animation/cooldown
        if (entity instanceof BreathingSlayerEntity breathingEntity) {
            if (breathingEntity.getAnimationTicks() > 0 ||
                breathingEntity.isBreathingFormOnCooldown()) {
                Log.debug("EntityCombatStateTracker: {} is breathing/animating, in combat",
                    entity.getType().getDescriptionId());
                activelyFighting = true;
            }
        }

        // Check for active swing animations (attacks or air swings)
        if (entity.swinging) {
            activelyFighting = true;
        }

        // Update last combat time if actively fighting
        if (activelyFighting) {
            lastCombatTime.put(entity, currentTime);
            return true;
        }

        // Check cooldown period - remain "in combat" for 10 seconds after last activity
        Long lastCombat = lastCombatTime.get(entity);
        if (lastCombat != null) {
            long timeSinceLastCombat = currentTime - lastCombat;
            if (timeSinceLastCombat < COMBAT_COOLDOWN_MS) {
                Log.debug("EntityCombatStateTracker: {} in combat cooldown ({} ms remaining)",
                    entity.getType().getDescriptionId(), COMBAT_COOLDOWN_MS - timeSinceLastCombat);
                return true;
            }
        }

        return false;
    }

    /**
     * Update combat state for an entity (called during rendering).
     * Detects state transitions and triggers appropriate effects.
     */
    public static void updateCombatState(LivingEntity entity) {
        if (entity == null) return;

        boolean inCombat = isInCombat(entity);
        Boolean previousState = combatStates.get(entity);

        // State changed?
        if (previousState == null || previousState != inCombat) {
            Log.debug("EntityCombatStateTracker: {} combat state changed: {} -> {}",
                entity.getType().getDescriptionId(), previousState, inCombat);
            onCombatStateChanged(entity, inCombat);
            combatStates.put(entity, inCombat);
        }
    }

    /**
     * Handle transition between combat states.
     * When entering combat, spawn particles for temporary sheaths.
     */
    private static void onCombatStateChanged(LivingEntity entity, boolean nowInCombat) {
        if (nowInCombat) {
            // Entering combat: handle temporary sheath removal
            ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
            if (SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
                SwordSheathRegistry.SheathInfo sheathInfo =
                    SwordSheathRegistry.getSheathInfo(mainHand);

                if (sheathInfo != null && !sheathInfo.persistsWhenDrawn()) {
                    // Spawn poof particles for temporary sheath (Uzui, Inosuke style)
                    spawnSheathDisappearParticles(entity);
                }
            }
        }
        // Exiting combat: sword goes back to sheath (handled by renderer)
    }

    /**
     * Spawn particles when a temporary sheath disappears (entity entering combat).
     * Simpler version than player sheath particles - just spawns at entity position.
     */
    private static void spawnSheathDisappearParticles(LivingEntity entity) {
        if (entity == null || entity.level() == null || !entity.level().isClientSide) {
            return;
        }

        // Spawn 6 POOF particles at entity position (chest height)
        for (int i = 0; i < 6; i++) {
            entity.level().addParticle(
                ParticleTypes.POOF,
                entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                entity.getY() + 1.0 + entity.getRandom().nextDouble() * 0.5,
                entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                0, 0.05, 0
            );
        }
    }

    /**
     * Clear all tracked combat states.
     * Should be called when disconnecting from server.
     */
    public static void clearAll() {
        combatStates.clear();
        lastCombatTime.clear();
    }
}
