package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.WhipDamageHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.LoveLineSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModMobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import org.joml.Vector3f;

/**
 * Enhanced Love Breathing forms for Mitsuri Kanroji's whip sword.
 *
 * Features:
 * - Flexible whip-like attacks with physics-based motion
 * - Multi-target damage along whip curve
 * - Heart particle effects
 * - Dynamic whip extension during forms
 *
 * This is a PLACEHOLDER implementation. Full forms will be added later.
 */
public class EnhancedLoveForms {
    private static final int LOVE_FAIRY_DUST_MOTES = 5;
    private static final int LOVE_RING_COUNT = 3;
    private static final int LOVE_RING_POINTS = 18;
    private static final String FOURTH_FORM_ANIMATION = "love_fourth_form_short";
    private static final int FOURTH_FORM_DURATION_TICKS = 40;
    private static final String SIXTH_FORM_ANIMATION = "love_sixth_form_short";
    private static final int SIXTH_FORM_DURATION_TICKS = 40;

    static ParticleOptions createLoveParticleOptions(Vector3f color, float size) {
        return "dust".equals(EnhancedBreathingConfig.loveParticleMode)
                ? new DustParticleOptions(color, size)
                : new EnergyParticleOptions(color, size);
    }

    /** Renders one point frame either as legacy particles or as a synced curve. */
    static void renderLovePointFrame(ServerLevel serverLevel, LivingEntity entity,
                                     float[][][] particlePoints, int frame,
                                     Vec3 forward, Vec3 right, Vec3 up,
                                     boolean isDemon, float pinkSize, float whiteSize) {
        if (serverLevel == null) {
            return;
        }

        if ("lines".equals(EnhancedBreathingConfig.loveParticleMode)) {
            if (particlePoints == null || frame < 0 || frame >= particlePoints.length
                || particlePoints[frame].length < 2) {
                ModNetworking.sendToTrackingAndSelf(
                    new LoveLineSyncPacket(entity.getUUID(), entity.getId(), false, null), entity);
                return;
            }

            float[][] framePoints = particlePoints[frame];
            Vec3[] worldPoints = new Vec3[framePoints.length];
            for (int i = 0; i < framePoints.length; i++) {
                float x = framePoints[i][0];
                float y = framePoints[i][1];
                float z = framePoints[i][2];
                worldPoints[i] = entity.position()
                    .add(forward.scale(z).add(right.scale(x)).add(up.scale(y)));
            }
            ModNetworking.sendToTrackingAndSelf(
                new LoveLineSyncPacket(entity.getUUID(), entity.getId(), true, worldPoints), entity);
            for (Vec3 point : worldPoints) {
                spawnLoveFairyDust(serverLevel, point);
            }
            return;
        }

        if (particlePoints == null || frame < 0 || frame >= particlePoints.length
            || particlePoints[frame].length == 0) {
            return;
        }
        for (int i = 0; i < particlePoints[frame].length; i++) {
            float x = particlePoints[frame][i][0];
            float y = particlePoints[frame][i][1];
            float z = particlePoints[frame][i][2];
            Vec3 pos = entity.position()
                .add(forward.scale(z).add(right.scale(x)).add(up.scale(y)));

            serverLevel.sendParticles(
                createLoveParticleOptions(
                    isDemon ? new Vector3f(0.8196f, 0, 0.286f) : new Vector3f(1.0f, 0.4f, 0.7f),
                    pinkSize),
                pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            serverLevel.sendParticles(
                createLoveParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), whiteSize),
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    static void clearLovePointFrame(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel) {
            ModNetworking.sendToTrackingAndSelf(
                new LoveLineSyncPacket(entity.getUUID(), entity.getId(), false, null), entity);
            for (int ring = 0; ring < LOVE_RING_COUNT; ring++) {
                ModNetworking.sendToTrackingAndSelf(
                    new LoveLineSyncPacket(loveRingUuid(entity.getUUID(), ring), entity.getId(), false, null),
                    entity);
            }
        }
    }

    /** Renders the three wide, swirling rings used by Fifth Form's impact. */
    private static void renderLoveRingFrame(ServerLevel serverLevel, LivingEntity entity, Vec3 center,
                                            Vec3 forward, Vec3 right, Vec3 up, int tick, boolean isDemon) {
        if (serverLevel == null) {
            return;
        }

        double spin = tick * 0.22D;
        Vector3f pink = isDemon
            ? new Vector3f(0.8196f, 0.0f, 0.286f)
            : new Vector3f(1.0f, 0.4f, 0.7f);

        for (int ring = 0; ring < LOVE_RING_COUNT; ring++) {
            double ringSpin = spin * (ring % 2 == 0 ? 1.0D : -1.0D) + ring * (Math.PI * 2.0D / 3.0D);
            double radius = 4.0D + ring * 1.25D;
            Vec3 ringCenter = center.add(forward.scale((ring - 1) * 1.15D
                + Math.sin(spin + ring) * 0.35D));
            Vec3[] points = new Vec3[LOVE_RING_POINTS + 1];

            for (int pointIndex = 0; pointIndex <= LOVE_RING_POINTS; pointIndex++) {
                double angle = Math.PI * 2.0D * pointIndex / LOVE_RING_POINTS;
                double wobble = Math.sin(angle * 3.0D + ringSpin) * 0.35D;
                double pointRadius = radius + wobble;
                points[pointIndex] = ringCenter
                    .add(right.scale(Math.cos(angle + ringSpin) * pointRadius))
                    .add(up.scale(Math.sin(angle + ringSpin) * pointRadius));
            }

            if ("lines".equals(EnhancedBreathingConfig.loveParticleMode)) {
                ModNetworking.sendToTrackingAndSelf(
                    new LoveLineSyncPacket(loveRingUuid(entity.getUUID(), ring), entity.getId(), true, points),
                    entity);
            } else {
                for (int pointIndex = 0; pointIndex < LOVE_RING_POINTS; pointIndex++) {
                    Vec3 point = points[pointIndex];
                    serverLevel.sendParticles(createLoveParticleOptions(pink, 1.4f),
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(createLoveParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 0.8f),
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
            }

            // Keep the dust sparse enough that the three rings remain readable.
            for (int pointIndex = 0; pointIndex < LOVE_RING_POINTS; pointIndex += 2) {
                spawnLoveFairyDust(serverLevel, points[pointIndex]);
            }
        }
    }

    private static UUID loveRingUuid(UUID entityUuid, int ring) {
        return UUID.nameUUIDFromBytes((entityUuid.toString() + ":love_ring:" + ring)
            .getBytes(StandardCharsets.UTF_8));
    }

    /** Spawns the tiny pink/green dust motes used to decorate Love line effects. */
    private static void spawnLoveFairyDust(ServerLevel serverLevel, Vec3 center) {
        spawnLoveFairyDust(serverLevel, center, new Vector3f(1.0f, 0.2f, 0.7f));
        spawnLoveFairyDust(serverLevel, center, new Vector3f(0.6f, 1.0f, 0.4f));
    }

    private static void spawnLoveFairyDust(ServerLevel serverLevel, Vec3 center, Vector3f color) {
        double x;
        double y;
        double z;
        do {
            x = Math.random() * 2.0D - 1.0D;
            y = Math.random() * 2.0D - 1.0D;
            z = Math.random() * 2.0D - 1.0D;
        } while (x * x + y * y + z * z > 1.0D);

        Vec3 position = center.add(x, y, z);
        for (int i = 0; i < LOVE_FAIRY_DUST_MOTES; i++) {
            serverLevel.sendParticles(
                new DustParticleOptions(color, 0.1f + (float) Math.random() * 0.3f),
                position.x, position.y, position.z, 1, 0, 0, 0, 0);
        }
    }

    private static boolean isLoveLineMode() {
        return "lines".equals(EnhancedBreathingConfig.loveParticleMode);
    }

    private static boolean isWithinLoveLine(Vec3 point, Vec3 start, Vec3 end, double radius) {
        Vec3 line = end.subtract(start);
        double lineLengthSquared = line.lengthSqr();
        if (lineLengthSquared <= 1.0E-6D) {
            return point.distanceToSqr(start) <= radius * radius;
        }

        double projection = point.subtract(start).dot(line) / lineLengthSquared;
        projection = Math.max(0.0D, Math.min(1.0D, projection));
        Vec3 closestPoint = start.add(line.scale(projection));
        return point.distanceToSqr(closestPoint) <= radius * radius;
    }

    /** Applies the Fifth Form tornado field independently of its damage ticks. */
    private static void applyLoveTornadoMovement(Level level, LivingEntity source, Vec3 center,
                                                 Vec3 orbitRight, int tick) {
        double tornadoRadius = tick < 45
            ? 6.0D
            : Math.min(12.0D, 6.0D + (tick - 45) * 0.2D);
        AABB tornadoBox = new AABB(center, center).inflate(tornadoRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            tornadoBox,
            target -> target != source && target.isAlive() && isTargetable(source, target));

        double progress = Math.max(0.0D, Math.min(1.0D, (tick - 26) / 49.0D));
        double orbitStrength = 0.18D + progress * 0.16D;
        double upwardStrength = 0.025D + progress * 0.045D;
        Vec3 fallbackRadial = new Vec3(orbitRight.x, 0.0D, orbitRight.z).normalize();

        for (LivingEntity target : targets) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
            Vec3 horizontalOffset = new Vec3(offset.x, 0.0D, offset.z);
            Vec3 radialDirection = horizontalOffset.lengthSqr() > 1.0E-6D
                ? horizontalOffset.normalize()
                : fallbackRadial;
            Vec3 tangentDirection = new Vec3(-radialDirection.z, 0.0D, radialDirection.x);
            double inwardStrength = Math.min(0.2D, Math.max(0.025D, horizontalOffset.length() * 0.035D));

            MovementHelper.addVelocity(target,
                tangentDirection.scale(orbitStrength)
                    .add(radialDirection.scale(-inwardStrength))
                    .add(0.0D, upwardStrength, 0.0D));

            if (tick == 75) {
                MovementHelper.addVelocity(target,
                    tangentDirection.scale(0.25D).add(0.0D, 1.35D, 0.0D));
            }
        }
    }

	/**
	 * Determines if a target should be hit by AOE attacks from the source.
	 *
	 * This is a smart targeting system that prevents accidental friendly fire:
	 * - Demons and non-demons always target each other
	 * - Demon slayers won't hit other demon slayers unless actively fighting
	 * - Neutral/passive mobs are protected unless source has combat history with them
	 * - Players targeting other players requires active combat
	 *
	 * @param source The entity performing the attack
	 * @param target The potential target
	 * @return true if the target should be hit by AOE, false otherwise
	 */
	public static boolean isTargetable(LivingEntity source, LivingEntity target) {
		if (source == null || target == null) {
			return false;
		}

		// Don't target self
		if (source == target) {
			return false;
		}

		if (CrowEnhancementHandler.isKasugaiCrow(target)) {
			return false;
		}

		boolean sourceIsDemon = Damager.isDemon(source);
		boolean targetIsDemon = Damager.isDemon(target);
		boolean sourceIsDemonSlayer = Damager.isDemonSlayer(source);
		boolean targetIsDemonSlayer = Damager.isDemonSlayer(target);

		// Check combat history - if they've fought recently, always allow targeting
		boolean hasRecentCombat = DamageTracker.hasDamageHistory(source, target);
		boolean traditionallyAngry = Damager.isAngry(source, target) || Damager.isAngry(target, source);
		boolean inCombat = hasRecentCombat || traditionallyAngry;

		// RULE 1: Demons and non-demons always target each other
		if (sourceIsDemon != targetIsDemon) {
			return true;
		}

		// RULE 2: If actively in combat with each other, allow targeting
		if (inCombat) {
			return true;
		}

		// From here on, we're dealing with same-faction entities
		// We want to be conservative to prevent accidental friendly fire

		// RULE 3: Demon slayers shouldn't hit other demon slayers by accident
		if (sourceIsDemonSlayer && targetIsDemonSlayer) {
			return false; // Not in combat, so don't target
		}

		// RULE 4: Players shouldn't hit other players by accident
		if (source instanceof Player && target instanceof Player) {
			return false; // Not in combat, so don't target
		}

		// RULE 5: Don't target neutral or passive mobs unless in combat
		// This includes animals, villagers, and NPCs without targets
		if (target instanceof net.minecraft.world.entity.animal.Animal) {
			return false; // Passive animals - never auto-target
		}

		if (target instanceof net.minecraft.world.entity.npc.Villager) {
			return false; // Villagers - never auto-target
		}

		// Check if target is a neutral mob (has no target/isn't aggressive)
		if (target instanceof net.minecraft.world.entity.Mob mob) {
			if (mob.getTarget() == null) {
				// Neutral mob - don't auto-target
				return false;
			}
			// Hostile mob with a target - check if they're targeting source or allies
			LivingEntity mobTarget = mob.getTarget();
			if (mobTarget == source) {
				return true; // They're attacking us
			}
			// If they're attacking someone else, only target if they're hostile
			// (we could add ally detection here later)
		}

		// RULE 6: Non-demon slayer players vs demon slayer entities
		// If source is a non-demon-slayer (regular mob or demon player),
		// and target is a demon slayer, allow targeting
		if (!sourceIsDemonSlayer && targetIsDemonSlayer) {
			return true;
		}

		// Default: don't target unknown entities to be safe
		return false;
	}
	
	/**
     * Unified animation helper that works with both players and GeckoLib entities.
     *
     * IMPORTANT: This is a SAFE version that doesn't rely on SwordRegistry lookups,
     * making it compatible with base mod swords (which aren't registered in our registry).
     */
    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            // Use safePlayAnimation instead of AnimationHelper.playAnimation
            // to avoid SwordRegistry null pointer issues with base mod swords
            safePlayAnimation(player, animationName);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, 20);
        } else {
            // Generic mobs/NPCs (e.g., CustomNPCs) — route through AnimationHelper
            AnimationHelper.playAnimation(entity, animationName);
        }
    }

    /**
     * Safe animation playback that doesn't rely on SwordRegistry.
     * This allows animations to work with base mod swords.
     */
    private static void safePlayAnimation(Player player, String animationName) {
        // Use AnimationHelper which handles null sword registrations gracefully
        AnimationHelper.playAnimation(player, animationName);
    }

    private static void playSynchronizedAnimation(LivingEntity entity, String animationName,
                                                  int durationTicks) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName, -1, 1.0F);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, durationTicks);
        } else {
            AnimationHelper.playAnimationOnLayer(entity, animationName, durationTicks, 1.0F, 3000);
        }
    }

    private static void playSixthFormAnimation(LivingEntity entity) {
        playSynchronizedAnimation(entity, SIXTH_FORM_ANIMATION, SIXTH_FORM_DURATION_TICKS);
    }

    /**
     * Unified animation helper with layer and speed control.
     * Safe version that doesn't require SwordRegistry lookups.
     */
    private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName, int maxTicks, float speed, int layer) {
        if (entity instanceof Player player) {
            // Use AnimationHelper which handles null sword registrations gracefully
            AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, maxTicks);
        } else {
            // Generic mobs/NPCs (e.g., CustomNPCs) — route through AnimationHelper
            AnimationHelper.playAnimationOnLayer(entity, animationName, maxTicks, speed, layer);
        }
    }

    /**
     * Helper method to set cancel attack swing state and sync to client.
     * Only works for Player entities.
     */
    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return;
        }

        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
            data.setCancelAttackSwing(value);
        });

        if (player instanceof ServerPlayer serverPlayer) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket(
                    player.getUUID(), value
                ),
                serverPlayer
            );
        }
    }

    /**
     * Trigger whip extension animation via network packet.
     * Sends to all clients so they can apply the whip physics animation.
     * Works for any LivingEntity holding a Kanroji sword (players, mobs, etc.)
     */
    public static void triggerWhipAnimation(LivingEntity entity, String animationName, double extension) {
        // Send whip animation packet to all clients
        if (!entity.level().isClientSide) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket packet =
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket(
                    entity.getUUID(),
                    animationName,
                    extension,
                    0 // Start at tick 0
                );
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);
        } else {
            // If called on client (shouldn't happen for forms), apply directly
            // Use DistExecutor to avoid loading client class on server
            final LivingEntity finalEntity = entity;
            final String finalAnimName = animationName;
            final double finalExtension = extension;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics.playAnimation(
                    finalEntity, finalAnimName, finalExtension, 10
                );
            });
        }
    }

    /**
     * First Form: Shivers of First Love (PLACEHOLDER)
     * Multi-directional whip slashes.
     *
     * TODO: Implement full form with proper whip animations and damage.
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
            22001, // Love First Form ID
            "First Form: Shivers of First Love",
            "Multi-directional rapid slashes with the whip",
            5, // 5 second cooldown
				(entity, level, formId) -> {
					boolean isDemon = Damager.isDemon(entity);
            	final ServerLevel serverLevel = level instanceof ServerLevel ? ((ServerLevel)level) : null;

                // Set guard state (formId auto-injected as 22001)
                GuardStateHelper.setGuardState(entity, 8.0, formId);
                
                // Use default player step height (0.6f) for reset
				final float originalStepHeight = 0.6f;

				MovementHelper.setStepHeight(entity, 3);

                // Play player animation
                playEntityAnimation(entity, "love_first_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_first_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 90; // 4.5 seconds
                final int[] currentTick = {0};
                final int interval = 1;

                

                final ArrayList<LivingEntity> targets = new ArrayList<>();
                // Use DamageCalculator for proper scaling with strength potions and attributes
                // (same as Enhanced Mist Breathing)

                final Vec3[] vectors = new Vec3[2];
                // Get initial forward and right vectors using yaw (handles looking up/down)
                float yawRad = (float) Math.toRadians(-entity.getYRot());
                 vectors[0] = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
                 // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                 vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);

                // Give player strong slowness to immobilize
        		//if (currentTick[0] == 0) {
        			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
        		//}
        			
        		float [][][] particlePoints = ParticlePositions.first_form.get("point_a");
                
                 final boolean[] hitThisTick = {false};
                 AbilityScheduler.scheduleRepeating(entity, () -> {

                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}

                	
                    renderLovePointFrame(serverLevel, entity, particlePoints, currentTick[0],
                        vectors[0], vectors[1], new Vec3(0, 1, 0), isDemon, 1.2f, 1.0f);

                  	if (currentTick[0] >= 26 && currentTick[0] < 52) { // Sprinting fast (2.6 seconds)
                 		if (currentTick[0] == 26) {
                 			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                 				    ModSounds.LOVE_LAUNCH.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 		}
                  		if (currentTick[0] < 31 && serverLevel != null) {
                  			serverLevel.sendParticles(
            						ModParticles.LOVE_IMPACT.get(),
            						entity.getX(), entity.getY(), entity.getZ(),
                  					3, 0.3, 0.3, 0.3, 0
                  				);
                  		}
                  		if (serverLevel != null && isLoveLineMode()) {
                  			spawnLoveFairyDust(serverLevel,
                  				entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
                  		}
                 		// Make the player zoom forward
                		Vec3 forward = vectors[0].scale(1.4); // Speed boost
                		MovementHelper.setVelocity(entity, new Vec3(forward.x, entity.getDeltaMovement().y, forward.z));
                		//entity.setDeltaMovement(forward.x, entity.getDeltaMovement().y, forward.z);

                		
                	}
                 	else if (currentTick[0] < 55) { // Floating and spinning (2.75 seconds)
                 		if (currentTick[0] == 52) {
                 			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                 				    ModSounds.LOVE_LANDS.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 		}
                 		// Give the player slow falling
                		if (currentTick[0] == 52) {
                			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
                		}
                	}
                	else if (currentTick[0] > 55 && currentTick[0] % 2 == 1 && currentTick[0] < 80) { // Sword swing - damage all targets

                 	}
                	
                	if (currentTick[0] >= 27 && currentTick[0] < 70) {
                		// Collect targets within 6 blocks that are targetable
                		
                			AABB searchBox = entity.getBoundingBox().inflate(7.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				if (!targets.contains(target)) {
                					targets.add(target);
                					// Spawn love_slash particles on newly added target
                					double particleY = target.getY() + target.getBbHeight() * 0.5;
                  					if (serverLevel != null) {
                  					if (isLoveLineMode() && currentTick[0] < 52) {
                  						spawnLoveFairyDust(serverLevel,
                  							target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                  					} else {
                  						serverLevel.sendParticles(
                  							ModParticles.LOVE_SLASH.get(),
                  							target.getX(), particleY, target.getZ(),
                  							3, 0.3, 0.3, 0.3, 0
                  						);
                  					}
                  				}
                			}
                		}
                	}
                	if (currentTick[0] > 50 && currentTick[0] < 80 && currentTick[0] % 2 == 0) {
                		if (Config.logDebug)
                			Log.debugEvery("enhanced-love-damage:" + entity.getUUID(), 1000,
                			    "Damaging {} targets on tick {}", targets.size(), currentTick[0]);
                  		hitThisTick[0] = false;
                 		for (LivingEntity target : targets) {
                			if (Math.random() < 0.4) {
	                			//System.out.println("Trying to damage " + target.getName().getString());
	                			float damage = (8.0F);
	                			boolean damaged = Damager.hurt(entity, target, damage);
	                			//System.out.println("Damaging " + target.getName().getString() + " " + damaged);
	                			if (serverLevel != null) {
	                				double targetHeight = target.getEyeHeight();
	                				double offsetX = (Math.random() - 0.5) * 0.8;
	        						double offsetY = Math.random() * targetHeight;
	        						double offsetZ = (Math.random() - 0.5) * 0.8;
	        						serverLevel.sendParticles(
	        							ModParticles.LOVE_IMPACT.get(),
	        							target.getX() + offsetX,
	        							target.getY() + offsetY,
	        							target.getZ() + offsetZ,
	        							1, 0, 0, 0, 0
	        						);
	
	        						serverLevel.sendParticles(
	            							ParticleTypes.FLASH,
	            							target.getX() - offsetX,
	            							target.getY() - offsetY,
	            							target.getZ() - offsetZ,
	            							1, 0, 0, 0, 0
	            						);
	                			}
	
//	    						level.playSound(null, target.getX(), target.getY(), target.getZ(),
//	                				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "punch1")),
//	                				    SoundSource.PLAYERS, 1.0f, 1.5f);
	
                 					if (damaged) {
                  						hitThisTick[0] = true;
                 						level.playSound(null, target.getX(), target.getY(), target.getZ(),
                 										    ModSounds.LOVE_HIT_SPARKLES.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 									}
                 							}
                 						}
                 	}
                   		if (hitThisTick[0] && currentTick[0] > 50 && currentTick[0] < 80
                   			&& currentTick[0] % 2 == 0) {
                 			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                 				ModSounds.LOVE_HIT_SPARKLES.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 		}

	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                 AbilityScheduler.scheduleOnce(entity, () -> {
                    clearLovePointFrame(entity);
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    
                    MovementHelper.setStepHeight(entity, originalStepHeight);
                }, totalDuration+2);
            }
        );
    }

    /**
     * Second Form: Love Pangs (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            22002, // Love Second Form ID
            "Second Form: Love Pangs",
            "Rapid extension strikes",
            3,
            (entity, level, formId) -> {
                // TODO: Implement second form

				final float damage = (10.0F);
            	 // Set guard state (formId auto-injected as 22002)
					GuardStateHelper.setGuardState(entity, 8.0, formId);
				
					boolean isDemon = Damager.isDemon(entity);
					final ServerLevel loveServerLevel = level instanceof ServerLevel ? (ServerLevel) level : null;

                // Play player animation
                playEntityAnimation(entity, "love_second_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_second_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 80; // 4 seconds
                final int[] currentTick = {0};
                final int interval = 1;
                
                final Vec3[] vectors = new Vec3[2];
                // Get initial forward and right vectors using yaw (handles looking up/down)
                float yawRad = (float) Math.toRadians(-entity.getYRot());
                vectors[0] = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                
                float [][][] particlePoints = ParticlePositions.second_form.get("point_a");
                
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 50, false, false));
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}

                	
                    renderLovePointFrame(loveServerLevel, entity, particlePoints, currentTick[0],
                        vectors[0], vectors[1], new Vec3(0, 1, 0), isDemon, 1.2f, 1.0f);

                    // Keep a fairy-dust trail around the user for the full form duration.
                    if (loveServerLevel != null) {
                        spawnLoveFairyDust(loveServerLevel,
                            entity.position().add(0, entity.getBbHeight() * 0.5, 0));
                    }
                 	
                 	if (currentTick[0] < 12) {
                		// Jump up (0.6 seconds)
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.2).add(0, 0.5f, 0));
                	}
                	else if (currentTick[0] < 40) {
                		// Launch forward and attack (2.9 seconds)
                		MovementHelper.lookAtTarget(entity);
                		Vec3 vel = new Vec3(entity.getLookAngle().x, Math.min(entity.getLookAngle().y, -0.01), entity.getLookAngle().z);
                		MovementHelper.setVelocity(entity, vel.scale(0.8f));
                		
                		if (level instanceof ServerLevel serverLevel && currentTick[0] % 2 == 0) {
                			AABB searchBox = entity.getBoundingBox().inflate(6.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				if (Math.random() < 0.8) {
                					Damager.hurt(entity, target, damage);
                					// Spawn love_slash particles on newly added target
                					double particleY = target.getY() + target.getBbHeight() * 0.5;
                					serverLevel.sendParticles(
                						ModParticles.LOVE_SLASH.get(),
                						target.getX(), particleY, target.getZ(),
                						3, 0.3, 0.3, 0.3, 0
                					);
                					level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
                        				    SoundSource.PLAYERS, 1.0f, 1.5f);
                				}

                			}
                		}
                	}
            	
            	
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                 AbilityScheduler.scheduleOnce(entity, () -> {
                     clearLovePointFrame(entity);
                     GuardStateHelper.clearGuardState(entity);
                     setCancelAttackSwing(entity, false);
                     if (entity instanceof ServerPlayer serverPlayer) {
                         ModNetworking.sendToAllClients(
                             AnimationSyncPacket.createStopPacket(serverPlayer.getUUID()));
                     }
                 }, totalDuration+2);
            }
        );
    }

    /**
     * Third Form: Catlove Shower (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            22003, // Love Third Form ID
            "Third Form: Catlove Shower",
            "Overhead rain of whip slashes",
            4,
            (entity, level, formId) -> {
            	 // Set guard state (formId auto-injected as 22003)
                GuardStateHelper.setGuardState(entity, 8.0, formId);

                // Play player animation
                playEntityAnimation(entity, "love_third_form");

                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_third_form", 1.0);

					boolean isDemon = Damager.isDemon(entity);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                // Spawn love sword slashes visual effect
                Vec3 spawnPos = entity.position().add(0, entity.getEyeHeight(), 0); // Eye level
                Vec3 lookVec = entity.getLookAngle();

                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                final String modelKey = "love";
                final String[] slashAnimations = {
                    "sword_to_left", "sword_to_right", "sword_to_upper", "sword_to_left_reverse", "sword_to_right_reverse"
                };

                
                boolean[] spawnedLoveSlashes = {false};

                final int totalDuration = 40; // 2 seconds
                final int[] currentTick = {0};
                final int interval = 1;

                final Vec3[] vectors = new Vec3[3];
                // Get initial forward and right vectors using yaw (handles looking up/down)
                float yawRad = (float) Math.toRadians(-entity.getYRot());
                vectors[0] = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                
                 float [][][] particlePoints = ParticlePositions.third_form.get("point_a");
                 final Vec3 activationEyePosition = entity.getEyePosition();
                 final Vec3[] targetDirection = {vectors[0].scale(-1.0D)};
                 
                 double ogY = entity.position().y; // TODO: Measure the delta Y and add that to the Y position of the love sword slashes
                
                 MovementHelper.setVelocity(entity, vectors[0].scale(-0.75).add(0, 1.5, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 5, 10, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, totalDuration+10, 10, false, false));
                
                HashMap<Integer, List<LivingEntity>> targets = new HashMap<Integer, List<LivingEntity>>();
                ArrayList<LivingEntity> reversePropagationTargets = new ArrayList<LivingEntity>();
                HashMap<UUID, Integer> damageHitCounts = new HashMap<UUID, Integer>();
                Vec3[] damageLine = new Vec3[2];
                boolean[] damageSnapshotCaptured = {false};
                
                //LoveSwordSlashesEntity[] swordSlashes = {null};
                
                int range = 30;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                 	if (currentTick[0] % 4 == 0 || currentTick[0] == 30) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                		
                  		// Raycast back toward where the form was activated, stopping at the first block.
                        Vec3 eyePos = entity.getEyePosition();
                        Vec3 directionToActivation = activationEyePosition.subtract(eyePos);
                        targetDirection[0] = directionToActivation.lengthSqr() > 1.0E-6D
                            ? directionToActivation.normalize()
                            : vectors[0].scale(-1.0D);
                        Vec3 lookAngle = targetDirection[0];
                  		vectors[2] = eyePos.add(lookAngle.scale(range)); // Default to max range
                  		
                        MovementHelper.lookInDirection(entity, targetDirection[0]);

                		for (int i = 1; i < range; i++) {
                			Vec3 vec = eyePos.add(lookAngle.scale(i));

                  			// Check block collision
                  			BlockPos blockPos = new BlockPos((int)Math.floor(vec.x), (int)Math.floor(vec.y), (int)Math.floor(vec.z));
                  			if (!level.getBlockState(blockPos).isAir()) {
                  				vectors[2] = vec;
                  				break; // Stop at first block hit
                  			}
                  		}

                        // Query one broad box, then keep only entities inside the 6-block-radius cylinder.
                        targets.clear();
                        for (int i = 1; i < range; i++) {
                            targets.put(i, new ArrayList<LivingEntity>());
                        }
                        Vec3 lineEnd = vectors[2];
                        AABB lineBox = new AABB(
                            Math.min(eyePos.x, lineEnd.x) - 6.0D,
                            Math.min(eyePos.y, lineEnd.y) - 6.0D,
                            Math.min(eyePos.z, lineEnd.z) - 6.0D,
                            Math.max(eyePos.x, lineEnd.x) + 6.0D,
                            Math.max(eyePos.y, lineEnd.y) + 6.0D,
                            Math.max(eyePos.z, lineEnd.z) + 6.0D);
                        List<LivingEntity> entitiesAlongLine = level.getEntitiesOfClass(
                            LivingEntity.class, lineBox,
                            e -> e != entity && e.isAlive() && isTargetable(entity, e)
                                && isWithinLoveLine(
                                    e.position().add(0, e.getBbHeight() * 0.5, 0), eyePos, lineEnd, 6.0D));
                        Vec3 line = lineEnd.subtract(eyePos);
                        double lineLengthSquared = line.lengthSqr();
                        for (LivingEntity target : entitiesAlongLine) {
                            Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
                            double projection = lineLengthSquared <= 1.0E-6D
                                ? 0.0D
                                : targetCenter.subtract(eyePos).dot(line) / lineLengthSquared;
                            int segmentIndex = Math.max(1, Math.min(range - 1,
                                (int) (projection * (range - 1)) + 1));
                            targets.get(segmentIndex).add(target);
                        }

                        if (currentTick[0] == 30 && !damageSnapshotCaptured[0]) {
                            damageLine[0] = eyePos;
                            damageLine[1] = lineEnd;
                            for (int segment = range - 1; segment >= 1; segment--) {
                                List<LivingEntity> segmentTargets = targets.get(segment);
                                if (segmentTargets != null) {
                                    reversePropagationTargets.addAll(segmentTargets);
                                }
                            }
                            damageSnapshotCaptured[0] = true;
                        }
                   	}

                	
                    renderLovePointFrame(serverLevel, entity, particlePoints, currentTick[0],
                        vectors[0], vectors[1], new Vec3(0, 1, 0), isDemon, 1.2f, 1.0f);
                	
                 	if (currentTick[0] > 15 && currentTick[0] < 35) {
                 		if (currentTick[0] == 16) {
                 			level.playSound(null, vectors[2].x, vectors[2].y, vectors[2].z,
                 				    ModSounds.LOVE_CATS_AND_CUTS.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                 				    ModSounds.LOVE_CATS_AND_CUTS.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                 		}
                        MovementHelper.lookInDirection(entity, targetDirection[0]);
                        Vec3 localVelocity = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.velocity(entity);
                        MovementHelper.setVelocity(entity, localVelocity.x, Math.max(localVelocity.y, 0), localVelocity.z);
                		if (serverLevel != null) {
                			for (int i = 0; i < 5; i++) {
                				String slashAnim = slashAnimations[(int) (Math.random() * slashAnimations.length)];
 	                 			// Spawn local slash animations without a target-point particle cloud.

                                double slashAngle = (Math.random() - 0.5) * 12.0;
                                int arcLength = (int) (100 + Math.random() * 80);
                                Vec3 posOffset = new Vec3(
                                    (Math.random() - 0.5) * 1.2,
                                    0.6 + Math.random() * 1.6,
                                    (Math.random() - 0.5) * 1.2
                                );
                                if (Math.random() > 0.5) {
                                    BonePositionTracker.sendRawHorizontalSlashToClients(
                                        level,
                                        posOffset.add(0, -1, 0),
                                        modelKey,
                                        (float) slashAngle,
                                        Math.random() > 0.5,
                                        arcLength,
                                        100,
                                        0,
                                        0,
                                        (float) slashAngle * 10,
                                        1.5f,
                                        2.1f,
                                        15,
                                        entity.getUUID(),
                                        slashAnim
                                    );
                                } else {
                                    BonePositionTracker.sendRawVerticalSlashToClients(
                                        level,
                                        posOffset.add(0, -1, 0),
                                        modelKey,
                                        (float) slashAngle,
                                        false,
                                        arcLength,
                                        100,
                                        0,
                                        0,
                                        (float) slashAngle * 10,
                                        1.5f,
                                        2.1f,
                                        0,
                                        entity.getUUID(),
                                        slashAnim
                                    );
                                }
		        				
                			}
                		}
                	}
                	if (currentTick[0] >= 30) {
                        MovementHelper.lookInDirection(entity, targetDirection[0]);
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, 0), entity.getDeltaMovement().z);
                		// Launch the sword slashes forward
                		if (spawnedLoveSlashes[0] == false) {
                			spawnedLoveSlashes[0] = true;

                			// Calculate direction vector from eye position to target
                            Vec3 direction = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(
                                vectors[2].subtract(entity.getEyePosition())).normalize();

                			// Calculate yaw from direction (Minecraft uses -Z as north, rotation is counterclockwise from south)
                			// atan2(x, z) gives angle from north axis
                            float yawSlashRot = (float) Math.toDegrees(Math.atan2(direction.x, -direction.z));

                            // Calculate pitch from normalized direction (negative because Minecraft pitch is inverted)
                            float pitchSlashRot = (float) -Math.toDegrees(Math.asin(direction.y));

                            // Spawn the love sword slashes entity
                            LoveSwordSlashesSpawner.spawnLoveSwordSlashes(
                                level, com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity),
                                yawSlashRot, pitchSlashRot, "love_third_form", 40
                            );
                		}
                        if (serverLevel != null && damageSnapshotCaptured[0]) {
                            // Keep the slash impact effect moving from the farthest target to the nearest.
                            int propagationTick = currentTick[0] - 30;
                            for (int index = 0; index < reversePropagationTargets.size(); index++) {
                                int targetWave = Math.min(9,
                                    (int) ((long) index * 10 / reversePropagationTargets.size()));
                                if (targetWave != propagationTick) {
                                    continue;
                                }

                                LivingEntity target = reversePropagationTargets.get(index);
                                if (!target.isAlive()
                                    || !isWithinLoveLine(
                                        target.position().add(0, target.getBbHeight() * 0.5, 0),
                                        damageLine[0], damageLine[1], 6.0D)) {
                                    continue;
                                }

                                float x = (float) (target.position().x
                                    + (Math.random() - 0.5) * target.getEyeHeight());
                                float y = (float) (target.position().y
                                    + (Math.random() - 0.5) * target.getEyeHeight());
                                float z = (float) (target.position().z
                                    + (Math.random() - 0.5) * target.getEyeHeight());
                                serverLevel.sendParticles(
                                    ModParticles.LOVE_IMPACT.get(), x, y, z,
                                    3, 0.3, 0.3, 0.3, 0);
                                level.playSound(null, x, y, z,
                                    SoundEvents.PLAYER_ATTACK_STRONG,
                                    SoundSource.PLAYERS, 1.0f, 1f);
                            }

                            // Four evenly spaced hits over the ten-tick slash propagation window.
                            if (currentTick[0] == 30 || currentTick[0] == 33
                                || currentTick[0] == 36 || currentTick[0] == 39) {
                                for (LivingEntity target : reversePropagationTargets) {
                                    int hitCount = damageHitCounts.getOrDefault(target.getUUID(), 0);
                                    if (hitCount >= 4 || !target.isAlive()
                                        || !isWithinLoveLine(
                                            target.position().add(0, target.getBbHeight() * 0.5, 0),
                                            damageLine[0], damageLine[1], 6.0D)) {
                                        continue;
                                    }

                                    if (Damager.hurt(entity, target, 8.0F, true, false, true)) {
                                        damageHitCounts.put(target.getUUID(), hitCount + 1);
                                    }
                                }
                            }
                        }
                	}

	            	currentTick[0] += interval;
	            }, interval, totalDuration);


                // Schedule cleanup
                 AbilityScheduler.scheduleOnce(entity, () -> {
                    clearLovePointFrame(entity);
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2); // 3.2 seconds
            }
        );
    }
    
    /**
     * Fourth Form: The Heartbreak of Unrequited Love.
     *
     * The moving 3x3x3 capture box reaches a fixed point 15 blocks ahead in
     * ten ticks. Captured targets are pulled toward that box until the impact
     * resolves at tick 18.
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            22004,
            "Fourth Form: The Heartbreak of Unrequited Love",
            "Drag targets along a forward Love line and detonate the gathered group",
            6,
            (entity, level, formId) -> {
                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;
                final float damage = 36.0F;
                final boolean isDemon = Damager.isDemon(entity);
                final float originalYaw = entity.getYRot();
                final float originalPitch = entity.getXRot();
                final float originalHeadYaw = entity.getYHeadRot();
                final float originalBodyYaw = entity.yBodyRot;
                final Vec3 origin = entity.getEyePosition();
                Vec3 forward = entity.getLookAngle();
                if (forward.lengthSqr() < 1.0E-6D) {
                    forward = new Vec3(0.0D, 0.0D, 1.0D);
                } else {
                    forward = forward.normalize();
                }
                final Vec3 lockedForward = forward;
                final Vec3 targetPoint = origin.add(lockedForward.scale(15.0D));
                final Vec3 worldUp = Math.abs(lockedForward.y) > 0.98D
                    ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(0.0D, 1.0D, 0.0D);
                final Vec3 right = lockedForward.cross(worldUp).normalize();
                final Vec3 up = right.cross(lockedForward).normalize();
                final float lockedYaw = (float) Math.toDegrees(Math.atan2(-lockedForward.x, lockedForward.z));
                final float lockedPitch = (float) Math.toDegrees(-Math.asin(lockedForward.y));
                final float[][][] particlePoints = ParticlePositions.love_fourth_form_short.get("point_a");
                final List<LivingEntity> collectedTargets = new ArrayList<>();
                final boolean[] impactResolved = {false};

                GuardStateHelper.setGuardState(entity, damage, formId);
                setCancelAttackSwing(entity, true);

                // Start the player/entity, item, first-person, and curve timelines together.
                playSynchronizedAnimation(entity, FOURTH_FORM_ANIMATION, FOURTH_FORM_DURATION_TICKS);
                triggerWhipAnimation(entity, FOURTH_FORM_ANIMATION, 1.0D);
                MovementHelper.setRotation(entity, lockedYaw, lockedPitch);

                final int[] currentTick = {0};
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = currentTick[0];
                    MovementHelper.setRotation(entity, lockedYaw, lockedPitch);

                    Vec3 boxCenter;
                    if (tick <= 10) {
                        double progress = tick / 10.0D;
                        boxCenter = origin.add(lockedForward.scale(15.0D * progress));

                        AABB scanBox = new AABB(
                            boxCenter.x - 1.5D, boxCenter.y - 1.5D, boxCenter.z - 1.5D,
                            boxCenter.x + 1.5D, boxCenter.y + 1.5D, boxCenter.z + 1.5D);
                        List<LivingEntity> newlyCollected = level.getEntitiesOfClass(
                            LivingEntity.class,
                            scanBox,
                            target -> target != entity
                                && target.isAlive()
                                && isTargetable(entity, target)
                                && !collectedTargets.contains(target));
                        collectedTargets.addAll(newlyCollected);
                    } else {
                        boxCenter = targetPoint;
                    }

                    // Add only the difference between the box and each target's
                    // current center so the pull remains responsive to movement.
                    for (LivingEntity target : collectedTargets) {
                        if (target.isAlive()) {
                            MovementHelper.addVelocity(target,
                                boxCenter.subtract(target.getBoundingBox().getCenter()));
                        }
                    }

                    if (tick == 18 && !impactResolved[0]) {
                        impactResolved[0] = true;
                        if (serverLevel != null) {
                            serverLevel.sendParticles(ParticleTypes.FLASH,
                                targetPoint.x, targetPoint.y, targetPoint.z,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                            serverLevel.sendParticles(ModParticles.LOVE_SLASH.get(),
                                targetPoint.x, targetPoint.y, targetPoint.z,
                                32, 1.5D, 1.5D, 1.5D, 0.08D);
                            serverLevel.sendParticles(ModParticles.LOVE_IMPACT.get(),
                                targetPoint.x, targetPoint.y, targetPoint.z,
                                24, 1.2D, 1.2D, 1.2D, 0.08D);
                        }

                        for (LivingEntity target : collectedTargets) {
                            if (target.isAlive()
                                && target.getBoundingBox().getCenter().distanceTo(targetPoint) <= 3.0D) {
                                Damager.hurt(entity, target, damage);
                            }
                        }
                    }

                    int pointFrame = particlePoints == null || particlePoints.length <= 1
                        ? 0
                        : Math.min(particlePoints.length - 1,
                            Math.round(tick * (particlePoints.length - 1)
                                / (float) (FOURTH_FORM_DURATION_TICKS - 1)));
                    renderLovePointFrame(serverLevel, entity, particlePoints, pointFrame,
                        lockedForward, right, up, isDemon, 1.8F, 1.5F);

                    currentTick[0]++;
                }, 1, FOURTH_FORM_DURATION_TICKS);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    clearLovePointFrame(entity);
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    MovementHelper.setRotation(entity, originalYaw, originalPitch);
                    entity.setYHeadRot(originalHeadYaw);
                    entity.setYBodyRot(originalBodyYaw);
                    entity.yHeadRotO = originalHeadYaw;
                    entity.yBodyRotO = originalBodyYaw;
                }, FOURTH_FORM_DURATION_TICKS + 2);
            }
        );
    }

    /**
     * Legacy Fourth Form implementation, now preserved by LoveVariations.
     */
    private static BreathingForm legacyFourthForm() {
     	return new BreathingForm(
    			22004, // Love Fourth Form ID
    			"Fourth Form: The Heartbreak of Unrequited Love",
    			"The slayer catches an oncoming attack with their whip-blade, tossing it aside leaving the target open for a counter, or a retreat",
    			6,
    			(entity, level, formId) -> {
    				// TODO: Implement fourth form
    				ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
                	float [][][] particlePoints = ParticlePositions.fourth_form.get("point_a");
                	
                	float damage = (24.0F);

					boolean isDemon = Damager.isDemon(entity);

                	// Set guard state
                	GuardStateHelper.setGuardState(entity, damage, formId);

                	// Play player animation - try multiple approaches to ensure it works
                    //System.out.println("[FourthForm] Playing player animation: love_fourth_form");
                    if (entity instanceof Player player) {
                        // Direct animation call to bypass SwordRegistry lookups
                        AnimationHelper.playAnimationOnLayer(player, "love_fourth_form", -1, 1.0f, 3000);
                    } else {
                        playEntityAnimation(entity, "love_fourth_form");
                    }

                    // Trigger whip animation (client-side)
                    //System.out.println("[FourthForm] Triggering whip animation: love_fourth_form");
                    triggerWhipAnimation(entity, "love_fourth_form", 1.0);

                    // ALSO trigger Kanroji sword animation directly
                    AnimationHelper.triggerKanrojiSwordAnimation(entity, "love_fourth_form");

                    // Prevent normal attack swing
                    setCancelAttackSwing(entity, true);
                    
                    int interval = 1;
                    int totalDuration = 60;
                    
                    int[] currentTick = {0};
                    
                    List<LivingEntity> targets = new ArrayList<LivingEntity>();
                    
                    final Vec3[] vecs = new Vec3[5]; // 0=position, 1=forward, 2=right, 3=up, 4=target
                    float yawStart = (float) Math.toRadians(-entity.getYRot());

            		// Build orthonormal basis (forward, right, up)
            		vecs[0] = entity.position();
            		vecs[1] = entity.getLookAngle(); // Forward vector
            		vecs[2] = new Vec3(Math.sin(yawStart + Math.PI/2), 0, Math.cos(yawStart + Math.PI/2)).normalize(); // Right vector (90° from forward on horizontal plane)
            		vecs[3] = vecs[2].cross(vecs[1]).normalize(); // Up vector (right × forward, perpendicular to both)
                    
                    // Catch entities in front and tie them in the whip, then toss them forward
                    
                    AbilityScheduler.scheduleRepeating(entity, () -> {
                    	
                    	if (currentTick[0] == 38 || (currentTick[0] < 38 && currentTick[0] % 3 == 0)) {
                    		float yaw = (float) Math.toRadians(-entity.getYRot());

                    		// Build orthonormal basis (forward, right, up)
                    		vecs[0] = entity.position();
                    		vecs[1] = entity.getLookAngle(); // Forward vector
                    		if (vecs[1].y > 0) vecs[1] = new Vec3(vecs[1].x, 0, vecs[1].z);
                    		vecs[2] = new Vec3(Math.sin(yaw + Math.PI/2), 0, Math.cos(yaw + Math.PI/2)).normalize(); // Right vector (90° from forward on horizontal plane)
                    		vecs[3] = vecs[2].cross(vecs[1]).scale(-1).normalize(); // Up vector (right × forward, perpendicular to both)
                    	}
                    	
                    	if (currentTick[0] <= 15) { // Spiral around and gather targets
                    		if (currentTick[0] % 2 == 0) {
                    			int r = 4;
                    			AABB rayBox = new AABB(vecs[0].subtract(r, r, r), vecs[0].add(r, r, r));
                    			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                    				LivingEntity.class, rayBox,
                    				e -> e != entity && e.isAlive() && isTargetable(entity, e) && !targets.contains(e)
                    			);
                    			targets.addAll(entitiesInRay);
                    			
                    			rayBox = new AABB(vecs[0].add(vecs[1].scale(5)).subtract(r, r, r), vecs[0].add(vecs[1].scale(5)).add(r, r, r));
                    			entitiesInRay = level.getEntitiesOfClass(
                    				LivingEntity.class, rayBox,
                    				e -> e != entity && e.isAlive() && isTargetable(entity, e) && !targets.contains(e)
                    			);
                    			targets.addAll(entitiesInRay);
                    			
                    			for (LivingEntity le : entitiesInRay) {
                    				// Play fishing rod sound
                    				level.playSound(null, le.getX(), le.getY(), le.getZ(),
                    					SoundEvents.FISHING_BOBBER_THROW,
                    					SoundSource.PLAYERS, 1.0f, 1.2f);

                    				// Give the entity 3 seconds (60 ticks) of immovable effect
                    				//le.addEffect(new MobEffectInstance(KimetsunoyaibaModMobEffects.IMMVABLE.get(), 60, 0));
                    				
                    				// Launch entity into the air
                    				MovementHelper.setVelocity(le, new Vec3(0, 0.8f, 0));

                    				// Small circle of pink dust particles around le
                    				for (float i = 0; i < Math.PI*2; i += 0.2f) {
                    					Vec3 pos = le.position().add(1.5f*Math.sin(i), 1, 1.5f*Math.cos(i));
                    					serverLevel.sendParticles(
                             createLoveParticleOptions(
                            		                isDemon ? new Vector3f(0.8196f, 0, 0.286f) : new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                            		                1.8f                           // scale
                            		            ),
                            		            pos.x, pos.y, pos.z,
                            		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                            		        );
                    				}
                    			}
                    		}
                    	}
                    	else if (currentTick[0] < 35) { // Hold entities in front
                    		Vec3 point = vecs[0].add(vecs[1].scale(7)).add(0, 5, 0);
                    		for (LivingEntity le : targets) {
                    			MovementHelper.setVelocity(le, point.subtract(le.position()));
                    		}
                    	}
                    	else if (currentTick[0] < 38) { // Flick to the right
                    		Vec3 point = vecs[0].add(vecs[1].scale(6)).add(0, 7, 0).add(vecs[2].scale(-8));
                    		for (LivingEntity le : targets) {
                    			MovementHelper.setVelocity(le, point.subtract(le.position()));
                    		}
                    	}
                    	else if (currentTick[0] < 41) { // Launch entities forward
                    		if (currentTick[0] == 39) {
                    			// Play the projectile launch sound effect
                    			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    				SoundEvents.ARROW_SHOOT,
                    				SoundSource.PLAYERS, 1.5f, 0.8f);
                    			for (LivingEntity le : targets)
                    				Damager.hurt(entity, le, damage);
                    		}
                    		for (LivingEntity le : targets) {
                    			MovementHelper.setVelocity(le, vecs[1].scale(1.5f).add(0, 0.8f, 0));
                    		}
                    	}
                    	
                        renderLovePointFrame(serverLevel, entity, particlePoints, currentTick[0],
                            vecs[1], vecs[2], vecs[3], isDemon, 1.8f, 1.5f);
                    	
                    	currentTick[0] += interval;
    	            }, interval, totalDuration);
                    
                   
                    // Schedule cleanup
                     AbilityScheduler.scheduleOnce(entity, () -> {
                        clearLovePointFrame(entity);
                        GuardStateHelper.clearGuardState(entity);
                        setCancelAttackSwing(entity, false);
                    }, totalDuration+2);
    			}
    			);
    			
    }

    /**
     * Fifth Form: Swaying Love, Wildclaw
     * 
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            22005, // Love Fifth Form ID
            "Fifth Form: Swaying Love, Wildclaw",
            "Wide sweeping arc attack",
            10,
            (entity, level, formId) -> {
            	
            	ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
            	float [][][] particlePoints = ParticlePositions.fifth_form.get("point_a");

				float damage = (10.0F);
            	 // Set guard state (formId auto-injected as 22005)
                GuardStateHelper.setGuardState(entity, damage * 0.8f, formId);

					boolean isDemon = Damager.isDemon(entity);

                // Play player animation
                playEntityAnimation(entity, "love_fifth_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_fifth_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);
                
                final Vec3 mainTargetPos = entity.position().add(entity.getLookAngle().scale(15));
                LivingEntity mainTargetEntity = null;
                final net.minecraft.world.entity.Mob mob = entity instanceof net.minecraft.world.entity.Mob ? (net.minecraft.world.entity.Mob)entity : null;
                
                if (mob != null) {
                    mainTargetEntity = mob.getTarget();
                    if (mainTargetEntity == null || !mainTargetEntity.isAlive()) {
                        return;
                    }
                    
                    // Look at target's eye position for a natural head tilt
                    MovementHelper.lookAt(entity, mainTargetEntity.getEyePosition());
                }
                
                final LivingEntity mainTargetEntityFinal = mainTargetEntity;

                final int totalDuration = 96; // 5.8 seconds
                final int[] currentTick = {0};
                final int interval = 1;
                
                final Vec3[] vecs = new Vec3[5]; // 0=position, 1=forward, 2=right, 3=up, 4=target
                float yawStart = (float) Math.toRadians(-entity.getYRot());

        		// Build orthonormal basis (forward, right, up)
        		vecs[0] = entity.position();
        		vecs[1] = entity.getLookAngle(); // Forward vector
        		vecs[2] = new Vec3(Math.sin(yawStart + Math.PI/2), 0, Math.cos(yawStart + Math.PI/2)).normalize(); // Right vector (90° from forward on horizontal plane)
        		vecs[3] = vecs[2].cross(vecs[1]).normalize(); // Up vector (right × forward, perpendicular to both)
        		
        		final float[] yawPitch = {0,0};
        		
        		final int range = 60;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
            	
                	if (currentTick[0] == 25 || (currentTick[0] < 25 && currentTick[0] %4 == 0)) {
                		float yaw = (float) Math.toRadians(-entity.getYRot());

                		// Build orthonormal basis (forward, right, up)
                		vecs[0] = entity.position();
                		vecs[1] = entity.getLookAngle(); // Forward vector
                		if (vecs[1].y > 0) vecs[1] = new Vec3(vecs[1].x, 0, vecs[1].z);
                		vecs[2] = new Vec3(Math.sin(yaw + Math.PI/2), 0, Math.cos(yaw + Math.PI/2)).normalize(); // Right vector (90° from forward on horizontal plane)
                		vecs[3] = vecs[2].cross(vecs[1]).scale(-1).normalize(); // Up vector (right × forward, perpendicular to both)
                		if (currentTick[0] == 25) {
                			yawPitch[0] = (float) Math.toDegrees(Math.atan2(-vecs[1].x, vecs[1].z));
                			yawPitch[1] = (float) Math.toDegrees(Math.atan2(-vecs[1].y, Math.sqrt(vecs[1].x*vecs[1].x + vecs[1].z*vecs[1].z)));
                			
                			Vec3 eyePos = entity.getEyePosition();
                			// Find the target block
                			for (int i = 1; i < range; i++) {
                    			Vec3 vec = eyePos.add(vecs[1].scale(i));
                    			
                    			vecs[4] = vec;

                    			// Check block collision
                    			BlockPos blockPos = new BlockPos((int)Math.floor(vec.x), (int)Math.floor(vec.y), (int)Math.floor(vec.z));
                    			if (!level.getBlockState(blockPos).isAir()) {
                    				
                    				break; // Stop at first block hit
                    			}
                			}
                		}
                	}
                	
                    renderLovePointFrame(serverLevel, entity, particlePoints, currentTick[0],
                        vecs[1], vecs[2], vecs[3], isDemon, 1.8f, 1.5f);

                    if (serverLevel != null && currentTick[0] >= 26 && currentTick[0] <= 75) {
                        applyLoveTornadoMovement(level, entity, vecs[4], vecs[2], currentTick[0]);
                    }
                 	
                 	if (currentTick[0] < 14) {
                		// Jump upwards and backwards (do the backflip)
                		if (mob != null) {
                    		if (mainTargetEntityFinal != null && mainTargetEntityFinal.isAlive())
                    			MovementHelper.lookAt(entity, mainTargetEntityFinal.getEyePosition());
                    		else
                    			MovementHelper.lookAt(entity,  mainTargetPos);
                    	}
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.65).add(0, 1.5f, 0));
                	}
                	else if (currentTick[0] < 25) {
                		// Hover and aim
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                		if (mob != null) {
                    		if (mainTargetEntityFinal != null && mainTargetEntityFinal.isAlive())
                    			MovementHelper.lookAt(entity, mainTargetEntityFinal.getEyePosition());
                    		else
                    			MovementHelper.lookAt(entity,  mainTargetPos);
                    	}
                	}
                	else if (currentTick[0] == 26) {
                		if (mob != null) {
                    		if (mainTargetEntityFinal != null && mainTargetEntityFinal.isAlive())
                    			MovementHelper.lookAt(entity, mainTargetEntityFinal.getEyePosition());
                    		else
                    			MovementHelper.lookAt(entity,  mainTargetPos);
                    	}
                		// Start the sword slashes and tornado
                		LoveTornadoSpawner.spawnLoveTornado(
                				level,
                				vecs[4],
                				yawPitch[0]+180,
                				yawPitch[1],
                				"love_fifth_form",
                				85);
                		LoveSwordSlashesSpawner.spawnLoveSwordSlashes(
                				level,
                				vecs[4],
                				yawPitch[0]+180,
                				yawPitch[1],
                				"love_fifth_form",
                				85);
                		entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, totalDuration+2-currentTick[0], 3)); // To stop the fall damage
                	}
                	else if (currentTick[0] < 31) {
                		if (mob != null) {
                    		if (mainTargetEntityFinal != null && mainTargetEntityFinal.isAlive())
                    			MovementHelper.lookAt(entity, mainTargetEntityFinal.getEyePosition());
                    		else
                    			MovementHelper.lookAt(entity,  mainTargetPos);
                    	}
                		// Tornado is starting in the thin phase
                		// Launch in the direction
                		MovementHelper.setRotation(entity, yawPitch[0], yawPitch[1]);
                		float percent = (float)(currentTick[0]-25f)/(31f-25f);
                		Vec3 targetPos = vecs[0].scale(1-percent).add(vecs[4].scale(percent)); // Go from position vecs[0] to position vecs[4]
                		MovementHelper.setVelocity(entity, targetPos.subtract(entity.position())); // Set the velocity to the difference in position to target position
                		
                		int r = 6;
                		AABB rayBox = new AABB(targetPos.subtract(r, r, r), targetPos.add(r, r, r));
            			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
            				LivingEntity.class, rayBox,
            				e -> e != entity && e.isAlive() && isTargetable(entity, e)
            			);
            			for (LivingEntity le : entitiesInRay) {
            				float x = (float)(le.position().x + (Math.random()-0.5)*le.getEyeHeight());
                			float y = (float)(le.position().y + (Math.random()-0.5)*le.getEyeHeight());
                			float z = (float)(le.position().z + (Math.random()-0.5)*le.getEyeHeight());
	                		serverLevel.sendParticles(
	        						ModParticles.LOVE_IMPACT.get(),
	        						x, y, z,
	        						1, 0.3, 0.3, 0.3, 0
	        					);
	                		
	                		
	        				level.playSound(null, x, y, z,
	                				   SoundEvents.PLAYER_ATTACK_STRONG,
	                				    SoundSource.PLAYERS, 1.0f, 1f);
            				Damager.hurt(entity, le, damage*0.9f);
            			}
                	}
                	else if (currentTick[0] < 41) {
                		// Landed on the ground
                		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position())); // entity is supposed to be at vecs[4] position
                	}
                	else if (currentTick[0] < 45) {
                		// Big cloud of slashes at the end of the tornado
                		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position())); // entity is supposed to be at vecs[4] position
                		if (serverLevel != null) {
                			for (int i = 0; i < 5; i++) {
	                			// Spawn a cloud of sword slashes at the target point
	                			float x = (float)(vecs[4].x + (Math.random()-0.5)*15);
	                			float y = (float)(vecs[4].y + (Math.random()-0.5)*15);
	                			float z = (float)(vecs[4].z + (Math.random()-0.5)*15);
		                		serverLevel.sendParticles(
		        						ModParticles.LOVE_SLASH.get(),
		        						x, y, z,
		        						3, 0.3, 0.3, 0.3, 0
		        					);
		                		
		        				level.playSound(null, x, y, z,
		                				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
		                				    SoundSource.PLAYERS, 1.0f, 1.5f);
                			}
                		}
                		if (currentTick[0] % 2 == 0) {
                			int r = (10 + currentTick[0]-41);
                    		AABB rayBox = new AABB(vecs[4].subtract(r, r, r), vecs[4].add(r, r, r));
                			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                				LivingEntity.class, rayBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);
                			for (LivingEntity le : entitiesInRay) {
                				if (Math.random() > 0.5) continue;
                				float x = (float)(le.position().x + (Math.random()-0.5)*le.getEyeHeight());
                    			float y = (float)(le.position().y + (Math.random()-0.5)*le.getEyeHeight());
                    			float z = (float)(le.position().z + (Math.random()-0.5)*le.getEyeHeight());
    	                		serverLevel.sendParticles(
    	        						ModParticles.LOVE_IMPACT.get(),
    	        						x, y, z,
    	        						1, 0.3, 0.3, 0.3, 0
    	        					);
    	                		
    	        				level.playSound(null, x, y, z,
    	                				   SoundEvents.PLAYER_ATTACK_STRONG,
    	                				    SoundSource.PLAYERS, 1.0f, 1f);
                				Damager.hurt(entity, le, damage*0.9f, true);
                			}
                		}
                	}
                 	else if (currentTick[0] < 55) {
                 		// The impact expands into three wide, swirling Love rings.
                 		renderLoveRingFrame(serverLevel, entity, vecs[4], vecs[1], vecs[2], vecs[3],
                 			currentTick[0], isDemon);
                 		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position())); // entity is supposed to be at vecs[4] position
                		
                		if (currentTick[0] % 2 == 0) {
                			int r = (10 + currentTick[0]-45);
                    		AABB rayBox = new AABB(vecs[4].subtract(r, r, r), vecs[4].add(r, r, r));
                			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                				LivingEntity.class, rayBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);
                			for (LivingEntity le : entitiesInRay) {
                				float x = (float)(le.position().x + (Math.random()-0.5)*le.getEyeHeight());
                    			float y = (float)(le.position().y + (Math.random()-0.5)*le.getEyeHeight());
                    			float z = (float)(le.position().z + (Math.random()-0.5)*le.getEyeHeight());
    	                		serverLevel.sendParticles(
    	        						ModParticles.LOVE_IMPACT.get(),
    	        						x, y, z,
    	        						1, 0.3, 0.3, 0.3, 0
    	        					);
    	                		
    	        				level.playSound(null, x, y, z,
    	                				   SoundEvents.PLAYER_ATTACK_STRONG,
    	                				    SoundSource.PLAYERS, 1.0f, 1f);
                				Damager.hurt(entity, le, damage * 0.9f);
                			}
                		}
                		
                 	}
                 	else if (currentTick[0] < 75) {
                 		renderLoveRingFrame(serverLevel, entity, vecs[4], vecs[1], vecs[2], vecs[3],
                 			currentTick[0], isDemon);
                 		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position()));
                 	}
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                     AbilityScheduler.scheduleOnce(entity, () -> {
                        clearLovePointFrame(entity);
                        GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2); // 3.2 seconds
            }
        );
    }

	/**
     * Sixth Form: Cat-Legged Winds of Love
     * The base form uses the shortened attack animation; the longer opening
     * sequence remains available through LoveVariations.
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            22006, // Love Sixth Form ID
            "Sixth Form: Cat-Legged Winds of Love",
            "Spinning tornado whip attack",
            5,
            (entity, level, formId) -> {
				boolean isDemon = Damager.isDemon(entity);
            	float damage = (10.0F);
            	ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
             	float [][][] particlePoints = ParticlePositions.sixth_form_short.get("point_a");
            	 // Set guard state (formId auto-injected as 22006)
                GuardStateHelper.setGuardState(entity, damage*1.75f, formId); // extra protection

                // Start the user, sword, and whip timelines from the same form tick.
                playSixthFormAnimation(entity);
                triggerWhipAnimation(entity, SIXTH_FORM_ANIMATION, 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = SIXTH_FORM_DURATION_TICKS;
                final int[] currentTick = {0};
                final int interval = 1;
                
                final Vec3[] vectors = new Vec3[3];
                // Get initial forward and right vectors using yaw (handles looking up/down)
                float yawRad = (float) Math.toRadians(-entity.getYRot());
                vectors[0] = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
            	
                  if (currentTick[0] == 0) {
                    	level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                  				ModSounds.LOVE_BIG_SLASH_MEOW.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                  		MovementHelper.setVelocity(entity, entity.getLookAngle().scale(0.5f).add(0, 1, 0));
                		entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
                	}
                	
                 	if (currentTick[0] == 12 || currentTick[0] == 20 || currentTick[0] == 28) {
                		// Damage the idiots in front of you
                		if (serverLevel != null) {
                			Vec3 centerPos = entity.getEyePosition().add(entity.getLookAngle().scale(6));
                			AABB searchBox = new AABB(centerPos, centerPos).inflate(6.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				//if (Math.random() < 0.8) {
                					Damager.hurt(entity, target, damage, true, false, true);
                					// Spawn love_slash particles on newly added target
                					double particleY = target.getY() + target.getBbHeight() * 0.5;
                					serverLevel.sendParticles(
                						ModParticles.LOVE_SLASH.get(),
                						target.getX(), particleY, target.getZ(),
                						3, 0.3, 0.3, 0.3, 0
                					);
                					level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
                        				    SoundSource.PLAYERS, 1.0f, 1.5f);
                				//}

                			}
                		}
                	}
                	
                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}
                	
                	if (currentTick[0] > 10 && currentTick[0] % 2 == 0) {
                		Vec3 hitBoxCenter = entity.getEyePosition().add(entity.getLookAngle().scale(6));
                		AABB hitBox = new AABB(hitBoxCenter, hitBoxCenter).inflate(6.0);
                        List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);

                        for (Entity target : targets) {
                        	/*if (target instanceof LivingEntity livingTarget) {
    	                        Damager.hurt(entity, livingTarget, damage/2);
    	
    	                        // Brief confusion
    	                        //livingTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        	}*/
                        	// Knockback away from center
                            Vec3 knockbackDir = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                                .local(target.position()).subtract(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity));
                            if (knockbackDir.lengthSqr() > 1.0E-4D && target instanceof LivingEntity livingTarget) {
                                MovementHelper.addVelocity(livingTarget, knockbackDir.normalize());
                            }
                        }
                	}
                	
                    int pointFrame = particlePoints == null || particlePoints.length <= 1
                        ? 0
                        : Math.min(particlePoints.length - 1,
                            Math.round(currentTick[0] * (particlePoints.length - 1)
                                / (float) (totalDuration - 1)));
                    renderLovePointFrame(serverLevel, entity, particlePoints, pointFrame,
                            vectors[0], vectors[1], new Vec3(0, 1, 0), isDemon, 1.2f, 1.0f);
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
               
                
                // Schedule cleanup
                 AbilityScheduler.scheduleOnce(entity, () -> {
                    clearLovePointFrame(entity);
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2);
            }
        );
    }
    
    /**
     * Create the base Love Breathing technique.
     */
    public static BreathingTechnique createLoveBreathingBase() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());

        // Pink colors for Love Breathing (§d = light purple/pink)
        return new BreathingTechnique("Love Breathing", forms, "§d", "§d");
    }

    /**
     * Create the complete Love Breathing technique.
     */
    public static BreathingTechnique createLoveBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());

        // Pink colors for Love Breathing (§d = light purple/pink)
        return new BreathingTechnique("Love Breathing", forms, "§d", "§d");
    }

    /**
     * Generic Love Breathing (same as main technique).
     */
    public static BreathingTechnique createGenericLoveBreathing() {
        return createLoveBreathing();
    }
}
