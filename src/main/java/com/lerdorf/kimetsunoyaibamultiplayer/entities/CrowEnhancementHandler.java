package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class CrowEnhancementHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Store flying state for each crow entity
    private static final Map<UUID, CrowFlyingState> flyingCrows = new HashMap<>();

    public static class CrowFlyingState {
        public final UUID crowId;
        public Vec3 circleCenter;
        public float circleAngle;
        public int flyingTimer;
        public int ticksSinceLastDamage;
        public boolean reachedFlightHeight;
        public int takeoffTicks;
        public Vec3 lastKnownPos;
        public boolean isLanding; // Crow is descending with slow falling

        public CrowFlyingState(UUID crowId, Vec3 startPos) {
            this.crowId = crowId;
            this.circleCenter = startPos.add(0, EntityConfig.crowFlightHeight, 0);
            this.circleAngle = (float) (Math.random() * Math.PI * 2); // Random starting angle
            this.flyingTimer = EntityConfig.crowFlightDuration;
            this.ticksSinceLastDamage = 0;
            this.reachedFlightHeight = false;
            this.takeoffTicks = 0;
            this.lastKnownPos = startPos;
            this.isLanding = false;
        }

        public boolean isFlying() {
            return flyingTimer > 0;
        }

        public boolean isFlyingOrLanding() {
            return flyingTimer > 0 || isLanding;
        }

        public void tick() {
            flyingTimer--;
            ticksSinceLastDamage++;
            if (!reachedFlightHeight) {
                takeoffTicks++;
            }
        }
    }

    @SubscribeEvent
    public static void onCrowHurt(LivingHurtEvent event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();

        // Check if this is a kasugai_crow entity
        if (!isKasugaiCrow(entity)) {
            return;
        }

        // Prevent fall damage for crows ALWAYS (even if not tamed, even if config disabled)
        if (source.getMsgId().equals("fall")) {
            event.setCanceled(true);
            return;
        }

        // Check if enhancements are enabled before handling flying dodge
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        // Check if it's tamed
        if (!(entity instanceof TamableAnimal tamable) || !tamable.isTame() || tamable.getOwner() == null) {
            return; // Only handle tamed crows
        }

        CrowFlyingState state = flyingCrows.get(entity.getUUID());

        LOGGER.info("Crow {} taking damage from {}, flying state: {}",
            entity.getName().getString(),
            source.getMsgId(),
            state != null ? "FLYING (timer=" + state.flyingTimer + ")" : "ON GROUND");

        // If crow is ALREADY flying, just extend timer and cancel damage
        if (state != null && state.isFlying()) {
            state.flyingTimer = Math.max(state.flyingTimer, 100); // At least 5 seconds more
            state.ticksSinceLastDamage = 0;
            event.setCanceled(true);
            LOGGER.info("Crow already flying - extended timer, cancelled damage");
            return;
        }

        // Crow is on ground - initiate flight
        event.setCanceled(true);
        LOGGER.info("Crow on ground - initiating flight");
        initiateCrowFlight(entity);
    }

    private static void initiateCrowFlight(Entity crow) {
        // Check if already flying - don't add duplicate
        if (flyingCrows.containsKey(crow.getUUID())) {
            LOGGER.info("Crow {} is already in flying map, skipping duplicate add", crow.getUUID());
            // Just extend the timer
            CrowFlyingState existingState = flyingCrows.get(crow.getUUID());
            existingState.flyingTimer = Math.max(existingState.flyingTimer, 100);
            return;
        }

        // Create flying state
        CrowFlyingState state = new CrowFlyingState(crow.getUUID(), crow.position());
        flyingCrows.put(crow.getUUID(), state);

        LOGGER.info("=== INITIATING CROW FLIGHT ===");
        LOGGER.info("Crow UUID: {}", crow.getUUID());
        LOGGER.info("Current position: {}", crow.position());
        LOGGER.info("Circle center will be: {}", state.circleCenter);
        LOGGER.info("Flight duration: {} ticks", state.flyingTimer);
        LOGGER.info("Flying crows map now has {} entries", flyingCrows.size());

        // Give the crow upward velocity for takeoff (reduced from 2.0 to 1.0)
        crow.setDeltaMovement(0, 1.0, 0);

        // Disable gravity while flying
        crow.setNoGravity(true);

        // Note: The kasugai_crow entity uses its own animation system from the kimetsunoyaiba mod.
        // Custom flying animations would require creating a GeckoLib model replacement.
        // The fast movement and circular flight pattern provide visual flight behavior regardless.

        LOGGER.info("Set crow velocity to: {}", crow.getDeltaMovement());
        LOGGER.info("Set noGravity to true");

        // Spawn particles to indicate takeoff (ONLY ONCE)
        if (crow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    crow.getX(), crow.getY(), crow.getZ(),
                    10, 0.3, 0.1, 0.3, 0.05);
        }
    }

    public static void tick(ServerLevel level) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        if (flyingCrows.isEmpty()) {
            return; // No crows to update
        }

        // Debug: log that we're processing flying crows
        if (level.getGameTime() % 20 == 0) { // Every second
            LOGGER.info("Tick handler running with {} flying crows", flyingCrows.size());
        }

        // Update all flying crows across ALL levels
        flyingCrows.entrySet().removeIf(entry -> {
            UUID crowId = entry.getKey();
            CrowFlyingState state = entry.getValue();

            // Try to find crow in ANY level (it might have switched dimensions)
            Entity entity = null;
            for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                entity = serverLevel.getEntity(crowId);
                if (entity != null) {
                    break;
                }
            }

            if (entity == null) {
                LOGGER.warn("Crow entity {} not found in any level - might be unloaded. Keeping in map for now.", crowId);
                return false; // Keep for now, might reload
            }

            if (!entity.isAlive()) {
                LOGGER.info("Removing crow from flying map - entity dead");
                return true; // Remove dead crows
            }

            if (!isKasugaiCrow(entity)) {
                LOGGER.info("Removing from flying map - not a crow anymore");
                return true; // Not a crow anymore somehow
            }

            state.tick();

            if (state.isFlying()) {
                updateFlyingCrow(entity, state, (ServerLevel) entity.level());
                return false; // Keep in map
            } else {
                // Flight duration ended, start landing phase
                if (!state.isLanding) {
                    state.isLanding = true;
                    entity.setNoGravity(false); // Re-enable gravity for landing
                    LOGGER.info("Crow {} starting landing phase", entity.getName().getString());
                }

                // Apply slow falling effect during landing
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        5, // 5 ticks duration (re-applied every tick)
                        0,
                        false,
                        false
                    ));
                }

                if (entity.onGround()) {
                    // Crow has landed - trigger landing animation on mirror
                    triggerLandingAnimation(entity.getUUID());
                    LOGGER.info("Crow {} has landed", entity.getName().getString());
                    entity.noPhysics = false; // Reset physics
                    return true; // Remove from flying map
                }
                return false; // Keep tracking until it lands
            }
        });
    }

    private static void updateFlyingCrow(Entity crow, CrowFlyingState state, ServerLevel level) {
        Vec3 currentPos = crow.position();

        // Detect if crow was forcibly teleported (distance > 10 blocks from last known position)
        if (state.lastKnownPos != null) {
            double distanceMoved = currentPos.distanceTo(state.lastKnownPos);
            if (distanceMoved > 10.0) {
                // Kimetsunoyaiba mod is trying to teleport crow to ground - counter it!

                // Calculate where the crow SHOULD be in the circular pattern
                double targetX = state.circleCenter.x + Math.cos(state.circleAngle) * EntityConfig.crowCircleRadius;
                double targetZ = state.circleCenter.z + Math.sin(state.circleAngle) * EntityConfig.crowCircleRadius;
                double targetY = state.circleCenter.y + Math.sin(state.circleAngle * 3) * 2;

                // Force crow to correct position
                crow.teleportTo(targetX, targetY, targetZ);

                // Calculate direction and set velocity to continue circular motion
                Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
                state.circleAngle += 0.05f; // Advance angle (reduced from 0.2 to 0.05 to slow down)

                double nextX = state.circleCenter.x + Math.cos(state.circleAngle) * EntityConfig.crowCircleRadius;
                double nextZ = state.circleCenter.z + Math.sin(state.circleAngle) * EntityConfig.crowCircleRadius;
                double nextY = state.circleCenter.y + Math.sin(state.circleAngle * 3) * 2;

                Vec3 nextPos = new Vec3(nextX, nextY, nextZ);
                Vec3 direction = nextPos.subtract(targetPos).normalize();
                crow.setDeltaMovement(direction.scale(0.4)); // Reduced from 0.6 to 0.4
                crow.setNoGravity(true);

                // Update last known position to new correct position
                state.lastKnownPos = targetPos;
                return;
            }
        }

        // Update last known position
        state.lastKnownPos = currentPos;

        // Phase 1: Fast takeoff until reaching flight height
        if (!state.reachedFlightHeight) {
            if (currentPos.y >= state.circleCenter.y - 2) {
                state.reachedFlightHeight = true;
                LOGGER.info("Crow reached flight height, starting circular pattern");
            } else {
                // Upward movement (reduced from 2.5 to 1.2 for smoother takeoff)
                crow.setDeltaMovement(0, 1.2, 0);
                crow.setNoGravity(true);

                // Stop navigation while taking off
                if (crow instanceof Mob mob) {
                    mob.getNavigation().stop();
                }

                // Particles during takeoff (less frequent)
                if (state.takeoffTicks % 10 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD,
                            crow.getX(), crow.getY(), crow.getZ(),
                            3, 0.2, 0.1, 0.2, 0.02);
                }
                return;
            }
        }

        // Phase 2: Circular flight pattern (fast)
        // Update circle center to follow owner if they move
        if (crow instanceof TamableAnimal tamable && tamable.getOwner() != null) {
            Vec3 ownerPos = tamable.getOwner().position();
            state.circleCenter = new Vec3(ownerPos.x, state.circleCenter.y, ownerPos.z);
        }

        // Slower circular motion - 0.1 radians per tick = full circle in ~63 ticks (~3 seconds)
        state.circleAngle += 0.1f;

        // Calculate circular flight path
        double x = state.circleCenter.x + Math.cos(state.circleAngle) * EntityConfig.crowCircleRadius;
        double z = state.circleCenter.z + Math.sin(state.circleAngle) * EntityConfig.crowCircleRadius;
        double y = state.circleCenter.y + Math.sin(state.circleAngle * 3) * 2; // Slight vertical variation

        Vec3 targetPos = new Vec3(x, y, z);
        Vec3 direction = targetPos.subtract(currentPos).normalize();

        // Slower horizontal movement (reduced from 0.6 to 0.4)
        double speed = 0.4;
        crow.setDeltaMovement(direction.scale(speed));
        crow.setNoGravity(true); // Make sure gravity stays off

        if (state.flyingTimer % 20 == 0) {
            LOGGER.info("Crow circling: pos={}, target={}, angle={}, timer={}",
                currentPos, targetPos, state.circleAngle, state.flyingTimer);
        }

        // Face movement direction
        if (crow instanceof Mob mob) {
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            mob.setYRot(yaw);
            mob.yHeadRot = yaw;
            mob.yBodyRot = yaw;
        }

        // Don't spawn wing flap particles during circular flight
        // (removed to reduce visual clutter)

        // Disable crow AI goals while flying to prevent it trying to pathfind back
        if (crow instanceof Mob mob) {
            // Clear the navigation so it stops trying to pathfind
            mob.getNavigation().stop();
        }
    }

    private static boolean isKasugaiCrow(Entity entity) {
        // Check entity type
        String entityType = entity.getType().toString();
        return entityType.contains("kasugai_crow");
    }

    public static void clearFlyingCrows() {
        flyingCrows.clear();
        LOGGER.info("Cleared all flying crow states");
    }

    public static boolean isCrowFlying(UUID crowId) {
        CrowFlyingState state = flyingCrows.get(crowId);
        return state != null && state.isFlyingOrLanding();
    }

    private static void triggerLandingAnimation(UUID crowId) {
        // Find the mirror crow and trigger landing animation
        for (com.lerdorf.kimetsunoyaibamultiplayer.entities.GeckolibCrowEntity mirror :
             com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowMirrorHandler.getAllMirrors()) {
            if (mirror.getOriginalCrowUUID() != null &&
                mirror.getOriginalCrowUUID().equals(crowId)) {
                mirror.triggerLanding();
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        Entity entity = event.getEntity();

        // Check if this is a kasugai_crow that's currently flying
        if (!isKasugaiCrow(entity)) {
            return;
        }

        CrowFlyingState state = flyingCrows.get(entity.getUUID());
        if (state == null || !state.isFlying()) {
            return;
        }

        LOGGER.info("Caught teleport event for flying crow from {} to ({}, {}, {})",
            entity.position(), event.getTargetX(), event.getTargetY(), event.getTargetZ());

        // ALWAYS CANCEL TELEPORTS WHILE FLYING - This prevents the constant teleporting issue
        LOGGER.info("Crow is currently flying - CANCELING ALL TELEPORTS to prevent takeoff loop");
        event.setCanceled(true);
    }
}