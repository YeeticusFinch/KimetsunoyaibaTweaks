package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.UUID;

public class BonePositionTracker {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static int particlesSpawnedThisTick = 0;
	private static final Map<UUID, Long> lastAnimationTime = new HashMap<>();

	/**
	 * Spawns radial ribbon particles directly for a given entity and animation
	 * 
	 * @param entity        The entity performing the animation
	 * @param animationName The name of the animation being performed
	 * @param animationTick The current animation tick (-1 if unknown)
	 * @param particleType  The type of particles to spawn
	 */
	public static void spawnRadialRibbonParticles(LivingEntity entity, String animationName, int animationTick,
			ParticleOptions particleType) {
		System.out.println("spawnRadialRibbonParticles called: entity=" + entity + ", anim=" + animationName + ", tick="
				+ animationTick);

		// Reset particle counter at start of each animation call
		particlesSpawnedThisTick = 0;

		// Track animation timing to prevent missing fast swings
		UUID entityId = entity.getUUID();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			long currentTime = mc.level.getGameTime();
			Long lastTime = lastAnimationTime.get(entityId);

			// If this is a new animation or there's been a gap, ensure we show particles
			if (lastTime == null || (currentTime - lastTime) > 5) {
				System.out.println("New animation detected or gap found, ensuring particles show");
				// For rapid swings, force progress to show complete arc
				if (animationTick == -1) {
					animationTick = 0; // Convert to tick-based for consistent timing
				}
			}
			lastAnimationTime.put(entityId, currentTime);
		}

		if (entity == null || animationName == null || particleType == null) {
			System.out.println("Early return: null parameters");
			return;
		}

		// Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			System.out.println("Early return: null level");
			return;
		}

		float progress = getAnimationProgress(entity, animationTick);
		System.out.println("Animation progress: " + progress + ", spawning particles...");
		spawnRadialRibbonForAnimation(level, entity, animationName, progress, particleType);
	}

	private static float getAnimationProgress(LivingEntity entity, int animationTick) {
		if (animationTick >= 0) {
			// Calculate total ticks needed for full arc based on config
			double totalSteps = ParticleConfig.particleArcDegrees / ParticleConfig.particleAngleIncrement;
			double totalTicks = totalSteps / ParticleConfig.particleStepsPerTick;

			// For very fast animations (1-2 ticks), ensure we get meaningful progress
			if (totalTicks < 3.0) {
				totalTicks = 3.0; // Minimum animation duration
			}

			return Math.min(1.0f, animationTick / (float) totalTicks);
		} else {
			// Fallback: immediate full animation for attack events
			return 1.0f; // Show complete arc immediately for attack-based triggers
		}
	}

	private static void spawnRadialRibbonForAnimation(ClientLevel level, LivingEntity entity, String animationName,
			float progress, ParticleOptions particleType) {
		
		if (entity.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).map(data -> data.cancelAttackSwing()).orElse(false)) {
        	// We are canceling attack swings and their particles
        	return;
        }
		
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		Vec3 entityPos = entity.position();

		switch (animationName) {
		case "sword_to_right":
			spawnHorizontalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, false);
			break;
		case "sword_to_left":
			spawnHorizontalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, true);
			break;
		case "sword_overhead":
			spawnVerticalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, false);
			break;
		case "sword_to_upper":
			spawnVerticalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, true);
			break;
		case "sword_rotate":
			spawnSpinRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType);
			break;
		case "speed_attack_sword":
			spawnForwardThrust(level, entityPos, yawRad, entityHeight, progress, particleType);
			break;
		default:
			if (animationName.contains("sword") || animationName.contains("breath")) {
				//spawnHorizontalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, false);
			}
			break;
		}
	}

	private static void spawnForwardThrust(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, ParticleOptions particleType) {
		double centerY = entityHeight * 0.75;

// Create continuous particle arc without gaps
		double totalSteps = ParticleConfig.particleArcDegrees / ParticleConfig.particleAngleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps * progress);
		
		// TODO: Spawn particles in a straight line forward to go with the thrust

	}

	private static void spawnHorizontalRadialRibbon(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, ParticleOptions particleType, boolean leftToRight) {
		double centerY = entityHeight * 0.75;

		// Create continuous particle arc without gaps
		double totalSteps = ParticleConfig.particleArcDegrees / ParticleConfig.particleAngleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps * progress);

		for (int stepIdx = 0; stepIdx < stepsToProcess; stepIdx++) {
			double stepProgress = stepIdx / totalSteps;
			if (stepProgress > progress)
				break;

			double arcAngle = Math.toRadians(stepProgress * ParticleConfig.particleArcDegrees);

			// Create radial layers at different radii
			for (int radiusIdx = 0; radiusIdx < ParticleConfig.radialLayers; radiusIdx++) {
				double radius = ParticleConfig.baseRadius + (radiusIdx * ParticleConfig.radiusIncrement);

				// Calculate arc position (proper circular arc)
				double localX = radius * Math.cos(arcAngle) * (leftToRight ? -1 : 1);
				double localZ = radius * Math.sin(arcAngle);
				double localY = (leftToRight ? -0.5 : 0.5)
						* Math.sin(arcAngle - Math.toRadians(ParticleConfig.particleArcDegrees / 2));

				// Rotate to match player facing direction (centered on player)
				double worldX = entityPos.x + (localX * Math.cos(yawRad) - localZ * Math.sin(yawRad));
				double worldY = entityPos.y + centerY + localY;
				double worldZ = entityPos.z + (localX * Math.sin(yawRad) + localZ * Math.cos(yawRad));

				// Spawn particles directly (check max particles per tick)
				if (ParticleConfig.maxParticlesPerTick <= 0
						|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
					for (int i = 0; i < ParticleConfig.particlesPerPosition; i++) {
						if (ParticleConfig.maxParticlesPerTick > 0
								&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
							break;
						//System.out.println("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
						level.addParticle(particleType, worldX, worldY, worldZ, 0.0, 0.0, 0.0);
						incrementParticleCount();
					}
				}
			}
		}
	}

	private static void spawnVerticalRadialRibbon(ClientLevel level, Vec3 entityPos, double yawRad, double entityHeight,
			float progress, ParticleOptions particleType, boolean upward) {
		double centerY = entityHeight * 0.9;

		// Create continuous particle arc without gaps
		double totalSteps = ParticleConfig.particleArcDegrees / ParticleConfig.particleAngleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps * progress);

		for (int stepIdx = 0; stepIdx < stepsToProcess; stepIdx++) {
			double stepProgress = stepIdx / totalSteps;
			if (stepProgress > progress)
				break;

			double arcAngle = Math.toRadians(stepProgress * ParticleConfig.particleArcDegrees);

			// Create radial layers at different radii
			for (int radiusIdx = 0; radiusIdx < ParticleConfig.radialLayers; radiusIdx++) {
				double radius = ParticleConfig.baseRadius + (radiusIdx * ParticleConfig.radiusIncrement);

				// Vertical arc calculation
				double localY = radius * Math.cos(arcAngle);
				double localForward = radius * Math.sin(arcAngle);

				// Apply to world coordinates (centered on player)
				double worldX = entityPos.x + localForward * Math.cos(yawRad + Math.PI / 2);
				double worldY = entityPos.y + centerY + localY * (upward ? -1 : 1);
				double worldZ = entityPos.z + localForward * Math.sin(yawRad + Math.PI / 2);

				// Spawn particles directly (check max particles per tick)
				if (ParticleConfig.maxParticlesPerTick <= 0
						|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
					for (int i = 0; i < ParticleConfig.particlesPerPosition; i++) {
						if (ParticleConfig.maxParticlesPerTick > 0
								&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
							break;
						//System.out.println("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
						level.addParticle(particleType, worldX, worldY, worldZ, 0.0, 0.0, 0.0);
						incrementParticleCount();
					}
				}
			}
		}
	}

	private static void spawnSpinRadialRibbon(ClientLevel level, Vec3 entityPos, double yawRad, double entityHeight,
			float progress, ParticleOptions particleType) {
		double centerY = entityHeight * 0.8;

		// Create continuous particle arc without gaps (360° spin)
		double totalSteps = 360.0 / ParticleConfig.particleAngleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps * progress);

		for (int stepIdx = 0; stepIdx < stepsToProcess; stepIdx++) {
			double stepProgress = stepIdx / totalSteps;
			if (stepProgress > progress)
				break;

			double rotateAngle = yawRad + Math.toRadians(stepProgress * 360.0);

			// Create radial layers at different radii
			for (int radiusIdx = 0; radiusIdx < ParticleConfig.radialLayers; radiusIdx++) {
				double radius = ParticleConfig.baseRadius + (radiusIdx * ParticleConfig.radiusIncrement);

				double worldX = entityPos.x + radius * Math.cos(rotateAngle);
				double worldY = entityPos.y + centerY;
				double worldZ = entityPos.z + radius * Math.sin(rotateAngle);

				// Spawn particles directly (check max particles per tick)
				if (ParticleConfig.maxParticlesPerTick <= 0
						|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
					for (int i = 0; i < ParticleConfig.particlesPerPosition; i++) {
						if (ParticleConfig.maxParticlesPerTick > 0
								&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
							break;
						//System.out.println("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
						level.addParticle(particleType, worldX, worldY, worldZ, 0.0, 0.0, 0.0);
						incrementParticleCount();
					}
				}
			}
		}
	}

	// Legacy compatibility methods (deprecated)
	@Nullable
	@Deprecated
	public static Vec3[] getSwordTipPosition(LivingEntity entity, String animationName) {
		return null; // No longer used
	}

	@Nullable
	@Deprecated
	public static Vec3[] getSwordTipPosition(LivingEntity entity, String animationName, int animationTick) {
		return null; // No longer used
	}

	@Nullable
	@Deprecated
	public static Vec3[] getSwordTipPosition(LivingEntity entity) {
		return null; // No longer used
	}

	public static boolean isValidParticlePosition(Vec3 entityPos, Vec3 calculatedPos) {
		if (entityPos == null || calculatedPos == null) {
			return false;
		}
		double distance = entityPos.distanceTo(calculatedPos);
		return distance <= 5.0;
	}

	public static String getDebugInfo(LivingEntity entity) {
		return String.format("Radial ribbon: %d layers, %.1f base radius, %d particles per position",
				ParticleConfig.radialLayers, ParticleConfig.baseRadius, ParticleConfig.particlesPerPosition);
	}

	private static int getTotalParticlesThisTick() {
		return particlesSpawnedThisTick;
	}

	private static void incrementParticleCount() {
		particlesSpawnedThisTick++;
	}
}