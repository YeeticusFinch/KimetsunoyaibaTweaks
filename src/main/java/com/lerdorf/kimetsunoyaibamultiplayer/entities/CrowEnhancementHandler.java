package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

        public CrowFlyingState(UUID crowId, Vec3 startPos) {
            this.crowId = crowId;
            this.circleCenter = startPos.add(0, EntityConfig.crowFlightHeight, 0);
            this.circleAngle = (float) (Math.random() * Math.PI * 2); // Random starting angle
            this.flyingTimer = EntityConfig.crowFlightDuration;
            this.ticksSinceLastDamage = 0;
            this.reachedFlightHeight = false;
            this.takeoffTicks = 0;
        }

        public boolean isFlying() {
            return flyingTimer > 0;
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
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        Entity entity = event.getEntity();
        DamageSource source = event.getSource();

        // Check if this is a kasugai_crow entity
        if (!isKasugaiCrow(entity)) {
            return;
        }

        LOGGER.info("Crow {} taking damage from {}, amount: {}",
            entity.getName().getString(), source.getMsgId(), event.getAmount());

        // Prevent fall damage for crows
        if (source.getMsgId().equals("fall")) {
            event.setCanceled(true);
            LOGGER.info("Cancelled fall damage for crow");
            return;
        }

        // Check if it's tamed
        if (entity instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() != null) {
            CrowFlyingState state = flyingCrows.get(entity.getUUID());

            LOGGER.info("Crow is tamed, owner: {}, flying state: {}",
                tamable.getOwner().getName().getString(),
                state != null ? "exists, timer=" + state.flyingTimer : "null");

            // If crow is on ground or coming down from flight
            if (state == null || !state.isFlying()) {
                // Cancel damage
                event.setCanceled(true);
                LOGGER.info("Cancelling damage and initiating flight for crow");

                // Initiate flight
                initiateCrowFlight(entity);
            } else if (state.isFlying()) {
                // Crow is already flying - extend flight duration if taking damage
                state.flyingTimer = Math.max(state.flyingTimer, 100); // At least 5 seconds more
                state.ticksSinceLastDamage = 0;

                // Cancel damage while flying
                event.setCanceled(true);

                LOGGER.info("Kasugai crow {} extended flight due to continued danger", entity.getName().getString());
            }
        } else {
            LOGGER.info("Crow is not tamed or has no owner");
        }
    }

    private static void initiateCrowFlight(Entity crow) {
        // Create flying state
        CrowFlyingState state = new CrowFlyingState(crow.getUUID(), crow.position());
        flyingCrows.put(crow.getUUID(), state);

        LOGGER.info("=== INITIATING CROW FLIGHT ===");
        LOGGER.info("Crow UUID: {}", crow.getUUID());
        LOGGER.info("Current position: {}", crow.position());
        LOGGER.info("Circle center will be: {}", state.circleCenter);
        LOGGER.info("Flight duration: {} ticks", state.flyingTimer);
        LOGGER.info("Flying crows map now has {} entries", flyingCrows.size());

        // Give the crow strong upward velocity for fast takeoff
        crow.setDeltaMovement(0, 2.0, 0);

        // Disable gravity while flying
        crow.setNoGravity(true);

        // Make the crow look like it's flying (spreads wings if possible)
        if (crow instanceof Mob mob) {
            // Set the mob to think it's in "no-clip" mode which often triggers flying animations
            crow.noPhysics = false; // Keep physics but set flying flag
        }

        LOGGER.info("Set crow velocity to: {}", crow.getDeltaMovement());
        LOGGER.info("Set noGravity to true");

        // Spawn particles to indicate takeoff
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

            if (entity == null || !entity.isAlive()) {
                LOGGER.info("Removing crow from flying map - entity not found or dead");
                return true; // Remove dead or unloaded crows
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
                // Flight duration ended, let crow land naturally
                entity.setNoGravity(false); // Re-enable gravity for landing

                if (entity.onGround()) {
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

        // Phase 1: Fast takeoff until reaching flight height
        if (!state.reachedFlightHeight) {
            if (currentPos.y >= state.circleCenter.y - 2) {
                state.reachedFlightHeight = true;
                LOGGER.info("Crow reached flight height, starting circular pattern");
            } else {
                // Fast upward movement
                crow.setDeltaMovement(0, 2.5, 0);
                crow.setNoGravity(true);

                if (state.takeoffTicks % 5 == 0) {
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

        // Fast circular motion - 0.2 radians per tick = full circle in ~31 ticks (~1.5 seconds)
        state.circleAngle += 0.2f;

        // Calculate circular flight path
        double x = state.circleCenter.x + Math.cos(state.circleAngle) * EntityConfig.crowCircleRadius;
        double z = state.circleCenter.z + Math.sin(state.circleAngle) * EntityConfig.crowCircleRadius;
        double y = state.circleCenter.y + Math.sin(state.circleAngle * 3) * 2; // Slight vertical variation

        Vec3 targetPos = new Vec3(x, y, z);
        Vec3 direction = targetPos.subtract(currentPos).normalize();

        // Fast horizontal movement
        double speed = 0.6;
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

        // Wing flap particles more frequently during flight
        if (state.flyingTimer % 5 == 0) {
            level.sendParticles(ParticleTypes.CLOUD,
                    crow.getX(), crow.getY(), crow.getZ(),
                    2, 0.3, 0.1, 0.3, 0.02);
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
        return state != null && state.isFlying();
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

        // Crow is flying - determine if we should cancel or redirect
        Vec3 currentPos = entity.position();
        Vec3 originalTarget = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());

        // Get owner position
        Vec3 ownerPos = null;
        if (entity instanceof TamableAnimal tamable && tamable.getOwner() != null) {
            ownerPos = tamable.getOwner().position();
        }

        if (ownerPos != null) {
            // Calculate target position (20 blocks above owner)
            Vec3 desiredFlyPos = new Vec3(ownerPos.x, ownerPos.y + 20, ownerPos.z);
            double distanceToDesiredPos = currentPos.distanceTo(desiredFlyPos);

            // If crow is within 20 blocks of the desired flying position, CANCEL the teleport
            if (distanceToDesiredPos <= 20.0) {
                LOGGER.info("Crow is within 20 blocks of desired fly position ({}), CANCELING teleport", distanceToDesiredPos);
                event.setCanceled(true);
                return;
            }

            // If crow is far away, redirect teleport to above owner instead of to ground
            LOGGER.info("Crow is {} blocks from desired position, redirecting teleport to above owner", distanceToDesiredPos);
            double newY = ownerPos.y + 20 + Math.random() * 10; // 20-30 blocks above owner
            event.setTargetX(ownerPos.x + (Math.random() - 0.5) * 5); // Small randomness
            event.setTargetY(newY);
            event.setTargetZ(ownerPos.z + (Math.random() - 0.5) * 5);

            LOGGER.info("Redirected flying crow teleport to ({}, {}, {})",
                event.getTargetX(), event.getTargetY(), event.getTargetZ());

            // Update circle center to new position
            state.circleCenter = new Vec3(ownerPos.x, newY, ownerPos.z);
            state.reachedFlightHeight = true; // Already at flight height after teleport
        } else {
            // No owner, just cancel all teleports while flying
            LOGGER.info("Crow has no owner, canceling teleport");
            event.setCanceled(true);
        }
    }
}