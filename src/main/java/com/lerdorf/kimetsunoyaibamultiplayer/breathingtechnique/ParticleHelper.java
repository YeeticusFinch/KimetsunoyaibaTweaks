package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;

import net.minecraft.core.particles.ParticleOptions;

public class ParticleHelper {
	// Helper methods for particle effects
	public static void spawnParticleLine(ServerLevel level, Vec3 start, Vec3 end,
			net.minecraft.core.particles.ParticleOptions particle, int count) {
		Vec3 direction = end.subtract(start);
		for (int i = 0; i < count; i++) {
			double t = i / (double) count;
			Vec3 pos = start.add(direction.scale(t));
			level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * Spawn particles in a straight line forward (for thrust attacks like
	 * speed_attack_sword)
	 */
	public static void spawnForwardThrust(ServerLevel level, Vec3 start, Vec3 direction, double distance,
			net.minecraft.core.particles.ParticleOptions particle, int count) {
		for (int i = 0; i < count; i++) {
			double t = i / (double) count;
			Vec3 pos = start.add(direction.scale(distance * t));
			level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	public static void spawnCircleParticles(ServerLevel level, Vec3 center, double radius, ParticleOptions particle,
			int count) {
		for (int i = 0; i < count; i++) {
			double angle = (i / (double) count) * Math.PI * 2;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
		}
	}

	public static void spawnHorizontalArc(ServerLevel level, Vec3 center, double yaw, double pitch, double baseRadius,
			double radiusIncrement, int arcDegrees, double angleIncrement, double vert, ParticleOptions particle,
			int count) {

		// Create continuous particle arc without gaps
		double totalSteps = arcDegrees / angleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps);

		for (int stepIdx = 0; stepIdx < stepsToProcess; stepIdx++) {
			double stepProgress = stepIdx / totalSteps;

			double arcAngle = Math.toRadians(stepProgress * arcDegrees);

			// Create radial layers at different radii
			for (int radiusIdx = 0; radiusIdx < ParticleConfig.radialLayers; radiusIdx++) {
				double radius = baseRadius + (radiusIdx * radiusIncrement);
				// Local coordinates before rotation
				double localX = radius * Math.cos(arcAngle);
				double localZ = radius * Math.sin(arcAngle);
				double localY = vert * Math.sin(arcAngle - Math.toRadians(ParticleConfig.particleArcDegrees / 2));

				// ---- Apply yaw rotation (around Y axis)
				double xYaw = localX * Math.cos(yaw) - localZ * Math.sin(yaw);
				double zYaw = localX * Math.sin(yaw) + localZ * Math.cos(yaw);
				double yYaw = localY;

				// ---- Apply pitch rotation (around X axis)
				double yPitch = yYaw * Math.cos(pitch) - zYaw * Math.sin(pitch);
				double zPitch = yYaw * Math.sin(pitch) + zYaw * Math.cos(pitch);

				// ---- Translate to world coordinates
				double worldX = center.x + xYaw;
				double worldY = center.y + yPitch;
				double worldZ = center.z + zPitch;

				// Spawn particles directly
				if (true) {
					for (int i = 0; i < ParticleConfig.particlesPerPosition; i++) {
						// System.out.println("Spawning particle at: " + worldX + ", " + worldY + ", " +
						// worldZ);
						level.sendParticles(particle, worldX, worldY, worldZ, 1, 0, 0, 0, 0);
					}
				}
			}
		}
	}

	public static void spawnVerticalArc(ServerLevel level, Vec3 center, double yaw, double pitch, double baseRadius, double radiusIncrement, int arcDegrees, double angleIncrement, double hori,
			ParticleOptions particle, int count) {

		// Create continuous particle arc without gaps
		double totalSteps = ParticleConfig.particleArcDegrees / ParticleConfig.particleAngleIncrement;
		int stepsToProcess = (int) Math.ceil(totalSteps);

		for (int stepIdx = 0; stepIdx < stepsToProcess; stepIdx++) {
			double stepProgress = stepIdx / totalSteps;

			double arcAngle = Math.toRadians(stepProgress * ParticleConfig.particleArcDegrees);

			// Create radial layers at different radii
			for (int radiusIdx = 0; radiusIdx < ParticleConfig.radialLayers; radiusIdx++) {
				double radius = ParticleConfig.baseRadius + (radiusIdx * ParticleConfig.radiusIncrement);

				// Vertical arc calculation
				double localX = hori * Math.cos(arcAngle);
				double localZ = hori * Math.sin(arcAngle);
				double localY = radius * Math.cos(arcAngle);
				double localForward = radius * Math.sin(arcAngle);

				// ---- Apply pitch rotation (around X axis)
				double yPitch = localY * Math.cos(pitch) - localForward * Math.sin(pitch);
				double forwardPitch = localY * Math.sin(pitch) + localForward * Math.cos(pitch);

				// ---- Apply yaw rotation (around Y axis)
				double xYaw = localX * Math.cos(yaw) - localZ * Math.sin(yaw);
				double zYaw = localX * Math.sin(yaw) + localZ * Math.cos(yaw);

				// ---- Combine forward motion (player look direction)
				double worldX = center.x + forwardPitch * Math.cos(yaw + Math.PI / 2) + xYaw;
				double worldY = center.y + yPitch;
				double worldZ = center.z + forwardPitch * Math.sin(yaw + Math.PI / 2) + zYaw;

				// Spawn particles directly
				if (true) {
					for (int i = 0; i < ParticleConfig.particlesPerPosition; i++) {
						//System.out.println("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
						level.sendParticles(particle, worldX, worldY, worldZ, 1, 0, 0, 0, 0);
					}
				}
			}
		}
	}
}
