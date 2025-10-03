package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class CrowEnhancementHandler {

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
        public int landingGracePeriod; // Ticks of invulnerability after landing

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
            this.landingGracePeriod = 0;
        }

        public boolean isFlying() {
            return flyingTimer > 0;
        }

        public boolean isFlyingOrLanding() {
            return flyingTimer > 0 || isLanding;
        }

        public boolean isInLandingGracePeriod() {
            return landingGracePeriod > 0;
        }

        public void tick() {
            flyingTimer--;
            ticksSinceLastDamage++;
            if (!reachedFlightHeight) {
                takeoffTicks++;
            }
            if (landingGracePeriod > 0) {
                landingGracePeriod--;
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

        // DEBUG: Print detailed damage source information
        if (Config.logDebug) {
	        Log.info("===== CROW DAMAGE DEBUG =====");
	        Log.info("Crow: {} (UUID: {})", entity.getName().getString(), entity.getUUID());
	        Log.info("Damage Amount: {}", event.getAmount());
	        Log.info("Damage Source ID: {}", source.getMsgId());
	        Log.info("Damage Type: {}", source.type());
        }
        Entity directEntity = source.getDirectEntity();
        Entity causingEntity = source.getEntity();
        if (Config.logDebug)
        Log.info("Direct Entity: {} ({})",
            directEntity != null ? directEntity.getName().getString() : "null",
            directEntity != null ? directEntity.getClass().getSimpleName() : "N/A");
        if (Config.logDebug)
        Log.info("Causing Entity: {} ({})",
            causingEntity != null ? causingEntity.getName().getString() : "null",
            causingEntity != null ? causingEntity.getClass().getSimpleName() : "N/A");
        if (Config.logDebug)
        Log.info("============================");

        // Prevent fall damage for crows ALWAYS (even if not tamed, even if config disabled)
        if (source.getMsgId().equals("fall")) {
            event.setCanceled(true);
            if (Config.logDebug)
            	Log.info("Cancelled fall damage");
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

        if (Config.logDebug)
        Log.info("Crow flying state: {}",
            state != null ? (state.isFlying() ? "FLYING" : state.isLanding ? "LANDING" : state.isInLandingGracePeriod() ? "LANDING_GRACE" : "GROUNDED") : "GROUNDED");

        // If crow is in landing grace period
        if (state != null && state.isInLandingGracePeriod()) {
            // Only allow damage from living entities during grace period
            if (causingEntity instanceof LivingEntity) {
            	if (Config.logDebug)
                Log.info("Crow in landing grace period - damage from living entity, initiating flight");
                event.setCanceled(true);
                initiateCrowFlight(entity);
            } else {
                // Cancel non-living entity damage during grace period
                event.setCanceled(true);
                if (Config.logDebug)
                Log.info("Crow in landing grace period - cancelled damage from non-living source");
            }
            return;
        }

        // If crow is ALREADY flying, allow damage (crow can be killed while flying)
        if (state != null && state.isFlying()) {
            // Don't cancel - let the crow take damage
        	if (Config.logDebug)
            Log.info("Crow is flying - allowing damage (crow can die while flying)");
            return;
        }

        // Crow is on ground - initiate flight and cancel this damage
        event.setCanceled(true);
        if (Config.logDebug)
        Log.info("Crow on ground - initiating flight, cancelled initial damage");
        initiateCrowFlight(entity);
    }

    private static void initiateCrowFlight(Entity crow) {
        // Check if already flying - don't add duplicate
        if (flyingCrows.containsKey(crow.getUUID())) {
            Log.info("Crow {} is already in flying map, skipping duplicate add", crow.getUUID());
            // Just extend the timer
            CrowFlyingState existingState = flyingCrows.get(crow.getUUID());
            existingState.flyingTimer = Math.max(existingState.flyingTimer, 100);
            return;
        }

        // Create flying state
        CrowFlyingState state = new CrowFlyingState(crow.getUUID(), crow.position());
        flyingCrows.put(crow.getUUID(), state);

        if (Config.logDebug) {
	        Log.info("=== INITIATING CROW FLIGHT ===");
	        Log.info("Crow UUID: {}", crow.getUUID());
	        Log.info("Current position: {}", crow.position());
	        Log.info("Circle center will be: {}", state.circleCenter);
	        Log.info("Flight duration: {} ticks", state.flyingTimer);
	        Log.info("Flying crows map now has {} entries", flyingCrows.size());
        }

        // Give the crow upward velocity for takeoff (reduced from 2.0 to 1.0)
        crow.setDeltaMovement(0, 1.0, 0);

        // Disable gravity while flying
        crow.setNoGravity(true);

        // Note: The kasugai_crow entity uses its own animation system from the kimetsunoyaiba mod.
        // Custom flying animations would require creating a GeckoLib model replacement.
        // The fast movement and circular flight pattern provide visual flight behavior regardless.
        
        if (Config.logDebug) {
	        Log.info("Set crow velocity to: {}", crow.getDeltaMovement());
	        Log.info("Set noGravity to true");
        }

        // Spawn particles to indicate takeoff (ONLY ONCE)
        if (crow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    crow.getX(), crow.getY(), crow.getZ(),
                    10, 0.3, 0.1, 0.3, 0.05);

            // Play takeoff sound
            serverLevel.playSound(null, crow.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 0.5f, 1.5f);
        }
    }

    public static void tick(ServerLevel level) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        // Periodically check for crows stuck levitating (every 2 seconds)
        if (level.getGameTime() % 40 == 0) {
            checkForLevitatingCrows(level);
        }

        if (flyingCrows.isEmpty()) {
            return; // No crows to update
        }

        // Debug: log that we're processing flying crows
        if (level.getGameTime() % 20 == 0 && Config.logDebug) { // Every second
            Log.info("Tick handler running with {} flying crows", flyingCrows.size());
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
            	if (Config.logDebug)
                Log.warn("Crow entity {} not found in any level - might be unloaded. Keeping in map for now.", crowId);
                return false; // Keep for now, might reload
            }

            if (!entity.isAlive()) {
            	if (Config.logDebug)
                Log.info("Removing crow from flying map - entity dead");
                return true; // Remove dead crows
            }

            if (!isKasugaiCrow(entity)) {
            	if (Config.logDebug)
                Log.info("Removing from flying map - not a crow anymore");
                return true; // Not a crow anymore somehow
            }

            state.tick();

            if (state.isFlying()) {
                updateFlyingCrow(entity, state, (ServerLevel) entity.level());
                return false; // Keep in map
            } else if (state.isLanding) {
                // Flight duration ended, landing phase
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

                // CRITICAL: Reset fall distance to prevent fall damage
                // Fall damage is calculated from the fallDistance field, so we clear it
                entity.fallDistance = 0.0f;

                if (entity.onGround()) {
                    // Crow has landed - trigger landing animation on mirror
                    triggerLandingAnimation(entity.getUUID());
                    if (Config.logDebug)
                    Log.info("Crow {} has landed", entity.getName().getString());

                    // Play landing sound
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, entity.blockPosition(),
                            SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 0.7f, 0.8f);
                    }

                    entity.noPhysics = false; // Reset physics

                    // Set landing grace period (3 seconds = 60 ticks)
                    state.isLanding = false;
                    state.landingGracePeriod = 60;
                    if (Config.logDebug)
                    Log.info("Crow entered landing grace period (60 ticks / 3 seconds)");

                    // Keep in map during grace period
                    return false;
                }
                return false; // Keep tracking until it lands
            } else if (state.isInLandingGracePeriod()) {
                // Still in grace period, keep tracking
                return false;
            } else {
                // Not flying, not landing, grace period ended - start landing
                state.isLanding = true;
                entity.setNoGravity(false); // Re-enable gravity for landing
                if (Config.logDebug)
                Log.info("Crow {} starting landing phase", entity.getName().getString());
                return false;
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
                if (Config.logDebug)
                Log.info("Crow reached flight height, starting circular pattern");
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

        if (state.flyingTimer % 20 == 0 && Config.logDebug) {
            Log.info("Crow circling: pos={}, target={}, angle={}, timer={}",
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
        if (Config.logDebug)
        Log.info("Cleared all flying crow states");
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

    /**
     * Check for crows that are stuck levitating when they shouldn't be
     */
    private static void checkForLevitatingCrows(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            // Check if this is a kasugai_crow
            if (!isKasugaiCrow(entity)) {
                continue;
            }

            CrowFlyingState state = flyingCrows.get(entity.getUUID());

            // Check if crow is stuck in landing phase for too long (more than 10 seconds)
            if (state != null && state.isLanding && !entity.onGround()) {
                // Check how long it's been landing - if flying timer is deeply negative, it's stuck
                if (state.flyingTimer < -200) { // 200 ticks = 10 seconds past flight end
                	if (Config.logDebug)
                    Log.warn("Crow {} stuck in landing phase for too long! Force removing from flying state", entity.getUUID());

                    // Force remove from flying state
                    entity.setNoGravity(false);
                    entity.setDeltaMovement(entity.getDeltaMovement().x, -0.5, entity.getDeltaMovement().z);
                    entity.fallDistance = 0.0f;

                    // Trigger landing animation on mirror
                    triggerLandingAnimation(entity.getUUID());

                    // This will be removed on next iteration
                    return;
                }
            }

            // Skip if the crow is currently flying/landing (that's expected)
            if (state != null && state.isFlyingOrLanding()) {
                continue;
            }

            // Check if crow is floating/levitating when it shouldn't be
            if (entity.isNoGravity() || (!entity.onGround() && Math.abs(entity.getDeltaMovement().y) < 0.001)) {
            	if (Config.logDebug)
                Log.warn("Crow {} is stuck levitating! Resetting gravity and velocity", entity.getUUID());

                // Fix the levitation
                entity.setNoGravity(false);
                entity.fallDistance = 0.0f;

                // If floating in air, give it a small downward velocity to land
                if (!entity.onGround()) {
                    entity.setDeltaMovement(entity.getDeltaMovement().x, -0.5, entity.getDeltaMovement().z);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (!EntityConfig.crowEnhancementsEnabled || !EntityConfig.crowFlyingDodgeEnabled) {
            return;
        }

        Entity entity = event.getEntity();

        // Check if this is a kasugai_crow that's currently flying OR landing
        if (!isKasugaiCrow(entity)) {
            return;
        }

        CrowFlyingState state = flyingCrows.get(entity.getUUID());
        if (state == null || !state.isFlyingOrLanding()) {
            return;
        }
        
        if (Config.logDebug)
        Log.info("Caught teleport event for flying/landing crow from {} to ({}, {}, {})",
            entity.position(), event.getTargetX(), event.getTargetY(), event.getTargetZ());

        // ALWAYS CANCEL TELEPORTS WHILE FLYING OR LANDING - This prevents the teleport-to-ground issue
        if (Config.logDebug)
        Log.info("Crow is currently flying/landing - CANCELING ALL TELEPORTS");
        event.setCanceled(true);
    }
}