package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSheathRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

    // Track last combat tick for each entity (for 10-second cooldown)
    private static final Map<LivingEntity, Integer> lastCombatTick = new WeakHashMap<>();

    // Combat cooldown in ticks (10 seconds = 200 ticks, only counts when game is running)
    private static final int COMBAT_COOLDOWN_TICKS = 200;
    // Demon slayers should keep sword drawn briefly after combat ends.
    private static final int DEMON_SLAYER_SHEATH_DELAY_TICKS = 80;
    // Freshly spawned demon slayers wait briefly before first auto-sheath.
    private static final int DEMON_SLAYER_INITIAL_DRAW_TICKS = 20;
    // Sheath animation length for entity transition handling
    private static final int ENTITY_SHEATH_ANIMATION_TICKS = 10;
    // Track entity sheathing transitions: keep sword in hand until animation completes
    private static final Map<LivingEntity, Integer> sheathingUntilTick = new WeakHashMap<>();

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
        int currentTick = entity.tickCount;

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
            //Log.debug("EntityCombatStateTracker: {} recently hurt ({} ticks ago), in combat",
            //    entity.getType().getDescriptionId(), timeSinceHurt);
            activelyFighting = true;
        }

        // For BreathingSlayerEntity, check animation/cooldown
        if (entity instanceof BreathingSlayerEntity breathingEntity) {
            String currentAnimation = breathingEntity.getCurrentAnimation();
            boolean isDrawSheathAnim = currentAnimation != null &&
                (currentAnimation.startsWith("draw_") || currentAnimation.startsWith("sheath_"));
            boolean hasCombatTarget = false;
            if (entity instanceof Mob mob) {
                LivingEntity target = mob.getTarget();
                hasCombatTarget = target != null && target.isAlive() && !target.isRemoved();
            }

            // Do not treat draw/sheath transition animations as combat.
            // Also require an active combat target for breathing-form cooldown to keep combat active.
            if ((!isDrawSheathAnim && breathingEntity.getAnimationTicks() > 0) ||
                (hasCombatTarget && breathingEntity.isBreathingFormOnCooldown())) {
                //Log.debug("EntityCombatStateTracker: {} is breathing/animating, in combat",
                //    entity.getType().getDescriptionId());
                activelyFighting = true;
            }
        }

        // Check for active swing animations (attacks or air swings)
        if (entity.swinging) {
            activelyFighting = true;
        }

        // Update last combat tick if actively fighting
        if (activelyFighting) {
            lastCombatTick.put(entity, currentTick);
            return true;
        }

        // Demon slayers have custom sheath timing:
        // - Keep sword drawn for 80 ticks after combat ends
        // - Before first combat, keep drawn for initial 20 ticks after spawn
        if (entity instanceof DemonSlayerEntity) {
            Integer lastCombat = lastCombatTick.get(entity);
            if (lastCombat != null) {
                int ticksSinceLastCombat = currentTick - lastCombat;
                return ticksSinceLastCombat < DEMON_SLAYER_SHEATH_DELAY_TICKS;
            }
            return currentTick < DEMON_SLAYER_INITIAL_DRAW_TICKS;
        }

        // Other breathing slayers should sheath responsively when combat ends.
        // Skip extended cooldown for these entities.
        if (entity instanceof BreathingSlayerEntity) {
            return false;
        }

        // Check cooldown period - remain "in combat" for 10 seconds (200 ticks) after last activity
        // Using ticks ensures cooldown pauses when game is paused
        Integer lastCombat = lastCombatTick.get(entity);
        if (lastCombat != null) {
            int ticksSinceLastCombat = currentTick - lastCombat;
            if (ticksSinceLastCombat < COMBAT_COOLDOWN_TICKS) {
                //Log.debug("EntityCombatStateTracker: {} in combat cooldown ({} ticks remaining)",
                //    entity.getType().getDescriptionId(), COMBAT_COOLDOWN_TICKS - ticksSinceLastCombat);
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

        // Draw/sheath transitions should not be interrupted by rapid combat-state oscillation.
        if (isDrawSheathAnimationActive(entity)) {
            if (previousState == null) {
                combatStates.put(entity, inCombat);
            }
            return;
        }

        // State changed?
        if (previousState == null || previousState != inCombat) {
            Log.debugEvery("combat-state:" + entity.getStringUUID(), 500,
                "EntityCombatStateTracker: {} combat state changed: {} -> {}",
                entity.getType().getDescriptionId(), previousState, inCombat);
            onCombatStateChanged(entity, inCombat);
            combatStates.put(entity, inCombat);
        }
    }

    /**
     * Handle transition between combat states.
     * When entering combat, spawn particles for temporary sheaths and play draw animation.
     * When exiting combat, play sheath animation (draw in reverse).
     */
    private static void onCombatStateChanged(LivingEntity entity, boolean nowInCombat) {
        if (isDrawSheathAnimationActive(entity)) {
            Log.debug("[EntityCombatStateTracker] Transition ignored for {}: draw/sheath animation in progress",
                entity.getType().getDescriptionId());
            return;
        }

        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
            Log.debug("[EntityCombatStateTracker] Transition ignored for {}: main hand is not a KnY sword ({})",
                entity.getType().getDescriptionId(), mainHand.isEmpty() ? "empty" : mainHand.getItem().toString());
            return;
        }

        // Get sheath position from config
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
        SwordDisplayConfig.SwordDisplayPosition position =
            SwordDisplayConfig.getPositionForSword(itemId.toString());
        if (position == null) {
            position = SwordDisplayConfig.position; // Use default
        }
        if (entity instanceof DemonSlayerEntity slayer) {
            int level = slayer.getPowerLevel();
            if (level >= 1 && level <= 4) {
                position = slayer.isSheatheOnBack()
                    ? SwordDisplayConfig.SwordDisplayPosition.BACK
                    : SwordDisplayConfig.SwordDisplayPosition.HIP;
            } else if (level >= 5) {
                // Super seniors use hip as primary sheath anchor.
                position = SwordDisplayConfig.SwordDisplayPosition.HIP;
            }
        }
        // Primary entity sheath slot is always the LEFT slot.
        boolean isLeft = true;
        Log.debug("[EntityCombatStateTracker] Transition for {}: nowInCombat={}, sword={}, position={}, side={}",
            entity.getType().getDescriptionId(), nowInCombat,
            mainHand.getItem().toString(), position, isLeft ? "left" : "right");

        if (nowInCombat) {
            sheathingUntilTick.remove(entity);
            // Entering combat: draw sword
            SwordSheathRegistry.SheathInfo sheathInfo =
                SwordSheathRegistry.getSheathInfo(mainHand);

            if (sheathInfo != null && !sheathInfo.persistsWhenDrawn()) {
                // Spawn poof particles for temporary sheath (Uzui, Inosuke style)
                spawnSheathDisappearParticles(entity);
            }

            // DRAW ANIMATION: Trigger when entity enters combat
            DrawSheathAnimationHelper.playDrawAnimation(entity, position, isLeft, mainHand);
        } else {
            // Exiting combat: sheath sword
            // SHEATH ANIMATION: Trigger when entity exits combat (reverse draw)
            DrawSheathAnimationHelper.playSheathAnimation(entity, position, isLeft);
            sheathingUntilTick.put(entity, entity.tickCount + ENTITY_SHEATH_ANIMATION_TICKS);
            Log.debugEvery("sheath-start:" + entity.getStringUUID(), 500,
                "[EntityCombatStateTracker] Sheathing transition started for {}: tickNow={}, untilTick={}",
                entity.getType().getDescriptionId(), entity.tickCount, entity.tickCount + ENTITY_SHEATH_ANIMATION_TICKS);
        }
    }

    /**
     * Returns true while entity is in the sheath animation transition window.
     * During this window, the sword should remain visible in hand.
     */
    public static boolean isInSheathingTransition(LivingEntity entity) {
        if (entity == null) return false;
        Integer until = sheathingUntilTick.get(entity);
        if (until == null) {
            return false;
        }
        if (entity.tickCount >= until) {
            sheathingUntilTick.remove(entity);
            Log.debugEvery("sheath-finish:" + entity.getStringUUID(), 500,
                "[EntityCombatStateTracker] Sheathing transition finished for {} at tick {}",
                entity.getType().getDescriptionId(), entity.tickCount);
            return false;
        }
        return true;
    }

    private static boolean isDrawSheathAnimationActive(LivingEntity entity) {
        if (!(entity instanceof BreathingSlayerEntity breathingEntity)) {
            return false;
        }
        if (breathingEntity.getAnimationTicks() <= 0) {
            return false;
        }
        String currentAnimation = breathingEntity.getCurrentAnimation();
        return currentAnimation != null
            && (currentAnimation.startsWith("draw_") || currentAnimation.startsWith("sheath_"));
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
        lastCombatTick.clear();
        sheathingUntilTick.clear();
    }
}
