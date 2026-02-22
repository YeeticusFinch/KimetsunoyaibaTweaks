package com.lerdorf.kimetsunoyaibamultiplayer.client.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.UUID;

public class BonePositionTracker {
	private static int particlesSpawnedThisTick = 0;
	private static final Map<UUID, Long> lastAnimationTime = new HashMap<>();

	// =====================
	// Sword Slash Render Queue
	// =====================
	private static final List<SlashRenderRequest> renderQueue = new ArrayList<>();
	private static final Map<String, Float> lastProgressByAnimation = new HashMap<>(); // Track progress per entity+animation

	public static List<SlashRenderRequest> getRenderQueue() {
	    return renderQueue;
	}

	public static void clearRenderQueue() {
	    renderQueue.clear();
	    lastProgressByAnimation.clear();
	}

	public static class SlashRenderRequest {
	    public final String modelKey;
	    public final UUID entityId;
	    public final String animationName;
	    public final long startTime;
	    public final LivingEntity entity;
	    public final boolean isHorizontal;
	    public final boolean isVertical;
	    public final boolean isSpin;
	    public final boolean leftToRight;
	    public final boolean upward;
	    public final float angle;
	    public final boolean isRawSlash;
	    public final float arcRange;
	    public final int duration;
	    public final float yawOffset;
	    public final float pitchOffset;
	    public final float rollOffset;
	    public final float radiusScaler;
	    public final float sizeScaler;
	    public final float angleOffset;
	    public final boolean reverse;
		public final Vec3 posOffset;
		public final boolean isRawHorizontal;
		public final boolean isRawVertical;
		public final float vert;
		public final float hor;

	    public SlashRenderRequest(String modelKey, UUID entityId, String animationName, LivingEntity entity,
	                             boolean isHorizontal, boolean isVertical, boolean isSpin,
	                             boolean leftToRight, boolean upward) {
	        this.modelKey = modelKey;
	        this.entityId = entityId;
	        this.animationName = animationName;
	        this.entity = entity;
	        this.isHorizontal = isHorizontal;
	        this.isVertical = isVertical;
	        this.isSpin = isSpin;
	        this.leftToRight = leftToRight;
	        this.upward = upward;
	        this.startTime = System.currentTimeMillis();
	        this.angle = 0;
	        this.isRawSlash = false;
	        this.arcRange = 180f;
	        this.duration = SwordSwingConfig.animationDurationMs;
	        this.yawOffset = 0;
	        this.pitchOffset = 0;
	        this.rollOffset = 0;
	        this.radiusScaler = 1.0f;
	        this.sizeScaler = 1.0f;
	        this.angleOffset = 0;
	        this.reverse = false;
	        this.posOffset = Vec3.ZERO;
	        this.isRawHorizontal = false;
	        this.isRawVertical = false;
	        this.vert = 0;
	        this.hor = 0;
	    }
	    
	    public SlashRenderRequest(String modelKey, UUID entityId, String animationName, LivingEntity entity,
                boolean isHorizontal, boolean isVertical, boolean isSpin,
                float angle, boolean upward) {
			this.modelKey = modelKey;
			this.entityId = entityId;
			this.animationName = animationName;
			this.entity = entity;
			this.isHorizontal = isHorizontal;
			this.isVertical = isVertical;
			this.isSpin = isSpin;
			this.angle = angle;
			this.upward = upward;
			this.leftToRight = false;
			this.startTime = System.currentTimeMillis();
			this.isRawSlash = true;
			this.arcRange = 180f;
			this.duration = SwordSwingConfig.animationDurationMs;
			this.yawOffset = 0;
			this.pitchOffset = 0;
			this.rollOffset = 0;
			this.radiusScaler = 1.0f;
			this.sizeScaler = 1.0f;
			this.angleOffset = 0;
			this.reverse = false;
		this.isRawHorizontal = false;
		this.isRawVertical = false;
		this.vert = 0;
		this.hor = 0;
			this.posOffset = Vec3.ZERO;
		}

		// Full constructor with all raw slash parameters
		public SlashRenderRequest(String modelKey, UUID entityId, String animationName, LivingEntity entity,
				float angle, float arcRange, int duration,
				float yawOffset, float pitchOffset, float rollOffset,
				float radiusScaler, float sizeScaler, float angleOffset, boolean reverse) {
			this.modelKey = modelKey;
			this.entityId = entityId;
			this.animationName = animationName;
			this.entity = entity;
			this.isHorizontal = false;
			this.isVertical = false;
			this.isSpin = false;
			this.angle = angle;
			this.upward = false;
			this.leftToRight = false;
			this.startTime = System.currentTimeMillis();
			this.isRawSlash = true;
			this.arcRange = arcRange;
			this.duration = duration;
			this.yawOffset = yawOffset;
			this.pitchOffset = pitchOffset;
			this.rollOffset = rollOffset;
			this.radiusScaler = radiusScaler;
		this.isRawHorizontal = false;
		this.isRawVertical = false;
		this.vert = 0;
		this.hor = 0;
			this.sizeScaler = sizeScaler;
			this.angleOffset = angleOffset;
			this.reverse = reverse;
			this.posOffset = Vec3.ZERO;
		}

	// Raw horizontal slash constructor
	public SlashRenderRequest(String modelKey, UUID entityId, String animationName, LivingEntity entity,
			float vert, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse, Vec3 posOffset) {
		this.modelKey = modelKey;
		this.entityId = entityId;
		this.animationName = animationName;
		this.entity = entity;
		this.isHorizontal = false;
		this.isVertical = false;
		this.isSpin = false;
		this.angle = 0;
		this.upward = false;
		this.leftToRight = false;
		this.startTime = System.currentTimeMillis();
		this.isRawSlash = false;
		this.arcRange = arcRange;
		this.duration = duration;
		this.yawOffset = yawOffset;
		this.pitchOffset = pitchOffset;
		this.rollOffset = rollOffset;
		this.radiusScaler = radiusScaler;
		this.sizeScaler = sizeScaler;
		this.angleOffset = angleOffset;
		this.reverse = reverse;
		this.posOffset = posOffset;
		this.isRawHorizontal = true;
		this.isRawVertical = false;
		this.vert = vert;
		this.hor = 0;
	}

	// Raw vertical slash constructor
	public SlashRenderRequest(String modelKey, UUID entityId, String animationName, LivingEntity entity,
			float hor, boolean isVertical, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse, Vec3 posOffset, float vert) {
		this.modelKey = modelKey;
		this.entityId = entityId;
		this.animationName = animationName;
		this.entity = entity;
		this.isHorizontal = false;
		this.isVertical = isVertical;  // Marker to distinguish from raw horizontal
		this.isSpin = false;
		this.angle = 0;
		this.upward = false;
		this.leftToRight = false;
		this.startTime = System.currentTimeMillis();
		this.isRawSlash = false;
		this.arcRange = arcRange;
		this.duration = duration;
		this.yawOffset = yawOffset;
		this.pitchOffset = pitchOffset;
		this.rollOffset = rollOffset;
		this.radiusScaler = radiusScaler;
		this.sizeScaler = sizeScaler;
		this.angleOffset = angleOffset;
		this.reverse = reverse;
		this.posOffset = posOffset;
		this.isRawHorizontal = false;
		this.isRawVertical = true;
		this.vert = vert;
		this.hor = hor;
	}

	    public float getCurrentProgress() {
	        long elapsed = System.currentTimeMillis() - startTime;
	        // Use instance duration for animation
	        return Math.min(1.0f, elapsed / (float) duration);
	    }

	    public boolean shouldRemove() {
	        return getCurrentProgress() >= 1.0f;
	    }

	    public boolean matches(UUID entityId, String animationName) {
	        return this.entityId.equals(entityId) && this.animationName.equals(animationName);
	    }
	}
	
	/**
	 * Spawns radial ribbon particles or 3D slash models for a given entity and animation
	 *
	 * @param entity        The entity performing the animation
	 * @param swordItem     The sword item being used
	 * @param animationName The name of the animation being performed
	 * @param animationTick The current animation tick (-1 if unknown)
	 * @param particleType  The type of particles to spawn
	 */
	public static void spawnRadialRibbonParticles(LivingEntity entity, net.minecraft.world.item.ItemStack swordItem,
			String animationName, int animationTick, ParticleOptions particleType) {
		Log.debug("spawnRadialRibbonParticles called: entity=" + entity + ", anim=" + animationName + ", tick="
				+ animationTick);

		// NOTE: Removed early return for Kanroji sword - GeckoLib handles sword MODEL animations,
		// but we still want trail particles to spawn in the air during attacks
		// (GeckoLib is only for the sword item itself, not for the particle trails)

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
				Log.debug("New animation detected or gap found, ensuring particles show");
				// For rapid swings, force progress to show complete arc
				if (animationTick == -1) {
					animationTick = 0; // Convert to tick-based for consistent timing
				}
			}
			lastAnimationTime.put(entityId, currentTime);
		}

		if (entity == null || animationName == null || particleType == null) {
			Log.debug("Early return: null parameters");
			return;
		}

		// Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			Log.debug("Early return: null level");
			return;
		}

		// Check if 3D sword slash models should be used instead of particles
		// Exception: Kanroji sword uses GeckoLib for item model, so skip the swing model but still show particles
		boolean isKanrojiSword = swordItem != null &&
				(swordItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated ||
				 swordItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated);

		if (SwordSwingConfig.useSwordSwingModel && !isKanrojiSword) {
			Log.debug("Using 3D sword slash model rendering");
			float progress = getAnimationProgress(entity, animationTick);
			spawnModelForAnimation(level, entity, swordItem, animationName, progress);
			return; // Skip particle rendering
		}

		float progress = getAnimationProgress(entity, animationTick);
		Log.debug("Animation progress: " + progress + ", spawning particles...");
		spawnRadialRibbonForAnimation(level, entity, animationName, progress, particleType, animationTick);
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
			float progress, ParticleOptions particleType, int animationTick) {

		if (entity.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).map(data -> data.cancelAttackSwing()).orElse(false)) {
        	// We are canceling attack swings and their particles
        	return;
        }

		// Check if entity is holding Kanroji sword
		net.minecraft.world.item.ItemStack heldItem = entity.getMainHandItem();
		boolean isKanrojiSword = heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated ||
				heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated;

		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		Vec3 entityPos = entity.position();

		switch (animationName) {
		case "sword_to_right":
			// Skip radial ribbon particles for Kanroji sword (uses ParticlePositions only)
			if (!isKanrojiSword) {
				spawnHorizontalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, false);
			}
			break;
		case "sword_to_left":
			// Skip radial ribbon particles for Kanroji sword (uses ParticlePositions only)
			if (!isKanrojiSword) {
				spawnHorizontalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, true);
			}
			break;
		case "sword_overhead":
			// Use custom particle positions from ParticlePositions.sword_overhead
			spawnSwordOverheadParticles(level, entity, particleType, animationTick);
			break;
		case "kanroji_sword_overhead":
			// Use custom particle positions from ParticlePositions.kanroji_sword_overhead
			spawnKanrojiSwordOverheadParticles(level, entity, particleType, animationTick);
			break;
		case "sword_to_upper":
			// Skip radial ribbon particles for Kanroji sword (uses ParticlePositions only)
			if (!isKanrojiSword) {
				spawnVerticalRadialRibbon(level, entityPos, yawRad, entityHeight, progress, particleType, true);
			}
			break;
		case "sword_rotate":
			// Use custom particle positions from ParticlePositions.sword_rotate (allowed for all swords)
			spawnSwordRotateParticles(level, entity, particleType, animationTick);
			break;
		case "speed_attack_sword":
			// Skip radial ribbon particles for Kanroji sword (uses ParticlePositions only)
			if (!isKanrojiSword) {
				spawnForwardThrust(level, entityPos, yawRad, entityHeight, progress, particleType);
			}
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
				double localY = (leftToRight ? -0.5 : 1.2)
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
						//Log.debug("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
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
						//Log.debug("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
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
						//Log.debug("Spawning particle at: " + worldX + ", " + worldY + ", " + worldZ);
						level.addParticle(particleType, worldX, worldY, worldZ, 0.0, 0.0, 0.0);
						incrementParticleCount();
					}
				}
			}
		}
	}

	/**
	 * Spawns particles for kanroji_sword_overhead using custom particle positions
	 */
	private static void spawnKanrojiSwordOverheadParticles(ClientLevel level, LivingEntity entity,
			ParticleOptions particleType, int animationTick) {
		// Get the particle positions from ParticlePositions
		float[][][] particlePoints = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.kanroji_sword_overhead.get("point_a");

		if (particlePoints == null || animationTick < 0 || animationTick >= particlePoints.length) {
			return; // No particle data or invalid tick
		}

		if (particlePoints[animationTick].length == 0) {
			return; // No particles for this tick
		}

		// Get entity orientation
		float yaw = entity.getYRot();
		double yawRad = Math.toRadians(-yaw);

		// Build orthonormal basis (forward, right, up) for positioning
		Vec3 forward = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
		Vec3 right = new Vec3(Math.sin(yawRad + Math.PI/2), 0, Math.cos(yawRad + Math.PI/2)).normalize();
		Vec3 up = right.cross(forward).normalize();

		Vec3 entityPos = entity.position();

		// Spawn particles at each position
		for (int i = 0; i < particlePoints[animationTick].length; i++) {
			float x = particlePoints[animationTick][i][0];
			float y = particlePoints[animationTick][i][1];
			float z = particlePoints[animationTick][i][2];

			// Transform particle position from local space to world space
			Vec3 pos = entityPos.add(forward.scale(z).add(right.scale(x)).add(up.scale(y)));

			// Spawn particle
			if (ParticleConfig.maxParticlesPerTick <= 0
					|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
				for (int j = 0; j < ParticleConfig.particlesPerPosition; j++) {
					if (ParticleConfig.maxParticlesPerTick > 0
							&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
						break;
					level.addParticle(particleType, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
					incrementParticleCount();
				}
			}
		}
	}

	/**
	 * Spawns particles for sword_overhead using custom particle positions
	 */
	private static void spawnSwordOverheadParticles(ClientLevel level, LivingEntity entity,
			ParticleOptions particleType, int animationTick) {
		// Get the particle positions from ParticlePositions
		float[][][] particlePoints = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_overhead.get("point_a");

		if (particlePoints == null || animationTick < 0 || animationTick >= particlePoints.length) {
			return; // No particle data or invalid tick
		}

		if (particlePoints[animationTick].length == 0) {
			return; // No particles for this tick
		}

		// Get entity orientation
		float yaw = entity.getYRot();
		double yawRad = Math.toRadians(-yaw);

		// Build orthonormal basis (forward, right, up) for positioning
		Vec3 forward = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
		Vec3 right = new Vec3(Math.sin(yawRad + Math.PI/2), 0, Math.cos(yawRad + Math.PI/2)).normalize();
		Vec3 up = right.cross(forward).normalize();

		Vec3 entityPos = entity.position();

		// Spawn particles at each position
		for (int i = 0; i < particlePoints[animationTick].length; i++) {
			float x = particlePoints[animationTick][i][0];
			float y = particlePoints[animationTick][i][1];
			float z = particlePoints[animationTick][i][2];

			// Transform particle position from local space to world space
			Vec3 pos = entityPos.add(forward.scale(z).add(right.scale(x)).add(up.scale(y)));

			// Spawn particle
			if (ParticleConfig.maxParticlesPerTick <= 0
					|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
				for (int j = 0; j < ParticleConfig.particlesPerPosition; j++) {
					if (ParticleConfig.maxParticlesPerTick > 0
							&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
						break;
					level.addParticle(particleType, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
					incrementParticleCount();
				}
			}
		}
	}

	/**
	 * Spawns particles for sword_rotate using custom particle positions
	 */
	private static void spawnSwordRotateParticles(ClientLevel level, LivingEntity entity,
			ParticleOptions particleType, int animationTick) {
		// Get the particle positions from ParticlePositions
		float[][][] particlePoints = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_rotate.get("point_a");

		if (particlePoints == null || animationTick < 0 || animationTick >= particlePoints.length) {
			return; // No particle data or invalid tick
		}

		if (particlePoints[animationTick].length == 0) {
			return; // No particles for this tick
		}

		// Get entity orientation
		float yaw = entity.getYRot();
		double yawRad = Math.toRadians(-yaw);

		// Build orthonormal basis (forward, right, up) for positioning
		Vec3 forward = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
		Vec3 right = new Vec3(Math.sin(yawRad + Math.PI/2), 0, Math.cos(yawRad + Math.PI/2)).normalize();
		Vec3 up = right.cross(forward).normalize();

		Vec3 entityPos = entity.position();

		// Spawn particles at each position
		for (int i = 0; i < particlePoints[animationTick].length; i++) {
			float x = particlePoints[animationTick][i][0];
			float y = particlePoints[animationTick][i][1];
			float z = particlePoints[animationTick][i][2];

			// Transform particle position from local space to world space
			Vec3 pos = entityPos.add(forward.scale(z).add(right.scale(x)).add(up.scale(y)));

			// Spawn particle
			if (ParticleConfig.maxParticlesPerTick <= 0
					|| getTotalParticlesThisTick() < ParticleConfig.maxParticlesPerTick) {
				for (int j = 0; j < ParticleConfig.particlesPerPosition; j++) {
					if (ParticleConfig.maxParticlesPerTick > 0
							&& getTotalParticlesThisTick() >= ParticleConfig.maxParticlesPerTick)
						break;
					level.addParticle(particleType, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
					incrementParticleCount();
				}
			}
		}
	}

	private static void spawnModelForAnimation(ClientLevel level, LivingEntity entity,
			net.minecraft.world.item.ItemStack swordItem, String animationName, float progress) {

		// Check for cancellation
		if (entity.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
				.map(data -> data.cancelAttackSwing()).orElse(false)) {
			return;
		}

		// Get model key for this sword
		String modelKey = com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry
				.getModelKeyForSword(swordItem);

		UUID entityId = entity.getUUID();
		String progressKey = entityId.toString() + ":" + animationName;

		// Check if this is a new swing by detecting progress reset
		Float lastProgress = lastProgressByAnimation.get(progressKey);
		boolean isNewSwing = lastProgress == null || progress < lastProgress || progress < 0.1f;

		// Update progress tracker
		lastProgressByAnimation.put(progressKey, progress);

		// Only create model on new swing
		if (!isNewSwing) {
			return; // Not a new swing, don't create another model
		}

		Log.debug("New swing detected! Progress: " + progress + ", modelKey: " + modelKey);

		// Get entity position and rotation
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		Vec3 entityPos = entity.position();

		// Create model once - it will self-animate based on animation type
		switch (animationName) {
		case "sword_to_right":
			renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, entityId, animationName, entity);
			break;
		case "sword_to_left":
			renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, true, entityId, animationName, entity);
			break;
		case "sword_overhead":
			renderVerticalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, entityId, animationName, entity);
			break;
		case "sword_to_upper":
			renderVerticalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, true, entityId, animationName, entity);
			break;
		case "sword_rotate":
			renderSpinSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, entityId, animationName, entity);
			break;
		case "speed_attack_sword":
			// Keep using particles for forward thrust
			break;
		default:
			// Unknown animation - could render generic slash or skip
			break;
		}
	}
	
	public static void renderHorizontalSlashModel(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, String modelKey, float angle,
			UUID entityId, String animationName, LivingEntity entity) {

		// Create slash model (calculation will be done during rendering)
		createSlashModel(modelKey, entityId, animationName, entity, true, false, false, angle, false);
	}

	/**
	 * Render a raw slash with full parameter control
	 * @param modelKey The model to render
	 * @param angle The angle of the slash axis (0=horizontal, 90=vertical, 180=flipped horizontal)
	 * @param arcRange The arc range in degrees (default 180)
	 * @param duration The animation duration in milliseconds (default 150)
	 * @param yawOffset Additional yaw offset in degrees
	 * @param pitchOffset Additional pitch offset in degrees
	 * @param rollOffset Additional roll offset in degrees
	 * @param radiusScaler Scales the distance from player to slash (default 1.0)
	 * @param sizeScaler Scales the size of the slash model (default 1.0)
	 * @param angleOffset Offset angle in degrees for positioning/spinning the slash (default 0)
	 * @param entityId The entity UUID
	 * @param animationName The animation name for tracking
	 * @param entity The living entity
	 */
	public static void renderRawSlash(String modelKey, float angle, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse,
			UUID entityId, String animationName, LivingEntity entity, Vec3 posOffset) {
		// Create raw slash with full parameter control
		createRawSlashModel(modelKey, entityId, animationName, entity,
				angle, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, posOffset);
	}

	/**
	 * Render a raw slash with default parameters
	 * @param modelKey The model to render
	 * @param angle The angle of the slash axis (0=horizontal, 90=vertical, 180=flipped horizontal)
	 * @param entityId The entity UUID
	 * @param animationName The animation name for tracking
	 * @param entity The living entity
	 */
	public static void renderRawSlash(String modelKey, float angle,
			UUID entityId, String animationName, LivingEntity entity, Vec3 posOffset) {
		renderRawSlash(modelKey, angle, 180f, SwordSwingConfig.animationDurationMs,
				0f, 0f, 0f, 1.0f, 1.0f, 0f, false, entityId, animationName, entity, posOffset);
	}

	/**
	 * SERVER-SIDE ONLY: Send a raw slash render request to nearby clients
	 * This is the simple method that breathing forms should call
	 *
	 * @param level The server level
	 * @param modelKey The model to render
	 * @param angle The angle of the slash axis (0=horizontal, 90=vertical, 180=flipped horizontal)
	 * @param entityId The entity UUID
	 * @param animationName The animation name for tracking
	 */
	public static void sendRawSlashToClients(net.minecraft.world.level.Level level, String modelKey, float angle, boolean reverse,
			UUID entityId, String animationName, Vec3 posOffset) {
		sendRawSlashToClients(level, posOffset, modelKey, angle, reverse, 180f, SwordSwingConfig.animationDurationMs,
				0f, 0f, 0f, 1.0f, 1.0f, 0f, entityId, animationName);
	}

	/**
	 * SERVER-SIDE ONLY: Send a raw slash render request to nearby clients (full parameters)
	 */
	public static void sendRawSlashToClients(net.minecraft.world.level.Level level, Vec3 pos, String modelKey, float angle, boolean reverse,
			float arcRange, int duration, float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset,
			UUID entityId, String animationName) {
		if (level.isClientSide) return; // Only run on server

		// Create and send packet to all nearby players
		com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket packet =
			new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawSlashRenderPacket(
				modelKey, angle, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, entityId, animationName, pos
			);

		com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);
	}

	/**
	 * SERVER-SIDE ONLY: Send a raw horizontal slash render request to nearby clients
	 */
	public static void sendRawHorizontalSlashToClients(net.minecraft.world.level.Level level, Vec3 posOffset, String modelKey, float vert, boolean reverse,
			float arcRange, int duration, float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset,
			UUID entityId, String animationName) {
		if (level.isClientSide) return; // Only run on server

		// Create and send packet to all nearby players
		com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket packet =
			new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawHorizontalSlashRenderPacket(
				modelKey, vert, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, entityId, animationName, posOffset
			);

		com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);
	}

	/**
	 * SERVER-SIDE ONLY: Send a raw vertical slash render request to nearby clients
	 */
	public static void sendRawVerticalSlashToClients(net.minecraft.world.level.Level level, Vec3 posOffset, String modelKey, float vert, boolean reverse,
			float arcRange, int duration, float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset,
			UUID entityId, String animationName) {
		if (level.isClientSide) return; // Only run on server

		// Create and send packet to all nearby players
		com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket packet =
			new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RawVerticalSlashRenderPacket(
				modelKey, vert, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, entityId, animationName, posOffset
			);

		com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);
	}

	/**
	 * CLIENT-SIDE ONLY: Render a raw horizontal slash with full parameter control
	 */
	public static void renderRawHorizontalSlash(String modelKey, float vert, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse,
			UUID entityId, String animationName, LivingEntity entity, Vec3 posOffset) {
		// Create raw horizontal slash with full parameter control
		createRawHorizontalSlashModel(modelKey, entityId, animationName, entity,
				vert, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, posOffset);
	}

	/**
	 * CLIENT-SIDE ONLY: Render a raw vertical slash with full parameter control
	 */
	public static void renderRawVerticalSlash(String modelKey, float vert, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse,
			UUID entityId, String animationName, LivingEntity entity, Vec3 posOffset) {
		// Create raw vertical slash with full parameter control
		createRawVerticalSlashModel(modelKey, entityId, animationName, entity,
				vert, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, posOffset);
	}

	private static void renderHorizontalSlashModel(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, String modelKey, boolean leftToRight,
			UUID entityId, String animationName, LivingEntity entity) {

		// Create slash model (calculation will be done during rendering)
		createSlashModel(modelKey, entityId, animationName, entity, true, false, false, leftToRight, false);
	}

	private static void renderVerticalSlashModel(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, String modelKey, boolean upward,
			UUID entityId, String animationName, LivingEntity entity) {

		// Create slash model (calculation will be done during rendering)
		createSlashModel(modelKey, entityId, animationName, entity, false, true, false, false, upward);
	}

	private static void renderSpinSlashModel(ClientLevel level, Vec3 entityPos, double yawRad,
			double entityHeight, float progress, String modelKey, UUID entityId, String animationName,
			LivingEntity entity) {

		// Create slash model (calculation will be done during rendering)
		createSlashModel(modelKey, entityId, animationName, entity, false, false, true, false, false);
	}

	private static void createSlashModel(String modelKey, UUID entityId, String animationName,
			LivingEntity entity, boolean isHorizontal, boolean isVertical, boolean isSpin,
			float angle, boolean upward) {
		// All swords now use slash models (old whip rendering removed)

		// Check if we already have a model for this entity+animation
				for (SlashRenderRequest req : renderQueue) {
					if (req.matches(entityId, animationName)) {
						return; // Model already exists, don't create another
					}
				}

				// Create new model - it will self-animate based on elapsed time
				renderQueue.add(new SlashRenderRequest(modelKey, entityId, animationName, entity,
						isHorizontal, isVertical, isSpin, angle, upward));
	}
	
	private static void createSlashModel(String modelKey, UUID entityId, String animationName,
			LivingEntity entity, boolean isHorizontal, boolean isVertical, boolean isSpin,
			boolean leftToRight, boolean upward) {

		// All swords now use slash models (old whip rendering removed)

		// Check if we already have a model for this entity+animation
		for (SlashRenderRequest req : renderQueue) {
			if (req.matches(entityId, animationName)) {
				return; // Model already exists, don't create another
			}
		}

		// Create new model - it will self-animate based on elapsed time
		renderQueue.add(new SlashRenderRequest(modelKey, entityId, animationName, entity,
				isHorizontal, isVertical, isSpin, leftToRight, upward));
	}

	private static void createRawSlashModel(String modelKey, UUID entityId, String animationName,
			LivingEntity entity, float angle, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse, Vec3 posOffset) {

		// All swords now use slash models (old whip rendering removed)

		// Check if we already have a model for this entity+animation
		for (SlashRenderRequest req : renderQueue) {
			if (req.matches(entityId, animationName)) {
				return; // Model already exists, don't create another
			}
		}

		// Create new raw slash model with full parameter control
		renderQueue.add(new SlashRenderRequest(modelKey, entityId, animationName, entity,
				angle, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse));
	}

	private static void createRawHorizontalSlashModel(String modelKey, UUID entityId, String animationName,
			LivingEntity entity, float vert, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse, Vec3 posOffset) {

		// All swords now use slash models (old whip rendering removed)

		// Check if we already have a model for this entity+animation
		for (SlashRenderRequest req : renderQueue) {
			if (req.matches(entityId, animationName)) {
				return; // Model already exists, don't create another
			}
		}

		// Create new raw horizontal slash model with full parameter control
		renderQueue.add(new SlashRenderRequest(modelKey, entityId, animationName, entity,
				vert, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, posOffset));
	}

	private static void createRawVerticalSlashModel(String modelKey, UUID entityId, String animationName,
			LivingEntity entity, float vert, float arcRange, int duration,
			float yawOffset, float pitchOffset, float rollOffset,
			float radiusScaler, float sizeScaler, float angleOffset, boolean reverse, Vec3 posOffset) {

		// All swords now use slash models (old whip rendering removed)

		// Check if we already have a model for this entity+animation
		for (SlashRenderRequest req : renderQueue) {
			if (req.matches(entityId, animationName)) {
				return; // Model already exists, don't create another
			}
		}

		// Create new raw vertical slash model with full parameter control
		renderQueue.add(new SlashRenderRequest(modelKey, entityId, animationName, entity,
				0, true, arcRange, duration, yawOffset, pitchOffset, rollOffset,
				radiusScaler, sizeScaler, angleOffset, reverse, posOffset, vert));
	}

	// Helper methods to calculate position based on animation type and progress
	public static Vec3 calculateHorizontalPosition(LivingEntity entity, float progress, boolean leftToRight) {
		Vec3 entityPos = entity.position();
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		double centerY = entityHeight * 0.75;

		double totalArcDegrees = ParticleConfig.particleArcDegrees;
		double arcAngle = progress * totalArcDegrees + (leftToRight ? SwordSwingConfig.rightArcOffset : SwordSwingConfig.leftArcOffset);

		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * (leftToRight ? SwordSwingConfig.rightRadiusMult : SwordSwingConfig.leftRadiusMult);
		double localX = radius * Math.cos(Math.toRadians(arcAngle)) * (leftToRight ? -1 : 1);
		double localZ = radius * Math.sin(Math.toRadians(arcAngle));
		double localY = (leftToRight ? -0.5 : 1.2) * Math.sin(Math.toRadians(arcAngle - totalArcDegrees / 2));

		double worldX = entityPos.x + (localX * Math.cos(yawRad) - localZ * Math.sin(yawRad));
		double worldY = entityPos.y + centerY + localY;
		double worldZ = entityPos.z + (localX * Math.sin(yawRad) + localZ * Math.cos(yawRad));

		return new Vec3(worldX, worldY, worldZ);
	}

	public static float[] calculateHorizontalRotation(LivingEntity entity, float progress, boolean leftToRight) {
		float yaw = entity.getYRot();
		double totalArcDegrees = ParticleConfig.particleArcDegrees;
		double arcAngle = progress * totalArcDegrees + (leftToRight ? SwordSwingConfig.rightArcOffset : SwordSwingConfig.leftArcOffset);

		// Model should rotate around the player as it sweeps
		// Yaw rotates the model around the vertical axis to follow the arc
		float modelYaw = yaw + (float) arcAngle * (leftToRight ? -1 : 1) + (float)(leftToRight ? SwordSwingConfig.rightYawOffset : SwordSwingConfig.leftYawOffset);

		// Pitch tilts the slash plane slightly for visual effect
		float modelPitch = leftToRight ? SwordSwingConfig.rightPitch : SwordSwingConfig.leftPitch;

		// Roll rotates the slash around its own forward axis
		float modelRoll = leftToRight ? SwordSwingConfig.rightRoll : SwordSwingConfig.leftRoll;

		return new float[]{modelYaw, modelPitch, modelRoll};
	}

	public static Vec3 calculateVerticalPosition(LivingEntity entity, float progress, boolean upward) {
		Vec3 entityPos = entity.position();
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		double centerY = entityHeight * 0.9;

		double totalArcDegrees = ParticleConfig.particleArcDegrees;
		double arcAngle = progress * totalArcDegrees + (float)(upward ? SwordSwingConfig.upperArcOffset : SwordSwingConfig.overheadArcOffset);

		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * (upward ? SwordSwingConfig.upperRadiusMult : SwordSwingConfig.overheadRadiusMult);
		double localY = radius * Math.cos(Math.toRadians(arcAngle));
		double localForward = radius * Math.sin(Math.toRadians(arcAngle));

		double worldX = entityPos.x + localForward * Math.cos(yawRad + Math.PI / 2);
		double worldY = entityPos.y + centerY + localY * (upward ? -1 : 1);
		double worldZ = entityPos.z + localForward * Math.sin(yawRad + Math.PI / 2);

		return new Vec3(worldX, worldY, worldZ);
	}

	public static float[] calculateVerticalRotation(LivingEntity entity, float progress, boolean upward) {
		float yaw = entity.getYRot();
		double totalArcDegrees = ParticleConfig.particleArcDegrees;
		double arcAngle = progress * totalArcDegrees + (float)(upward ? SwordSwingConfig.upperArcOffset : SwordSwingConfig.overheadArcOffset);

		// Model rotates around player in vertical plane
		float modelYaw = yaw + (float)(upward ? SwordSwingConfig.upperYawOffset : SwordSwingConfig.overheadYawOffset);
		float modelPitch = (float) arcAngle * (upward ? -1 : 1) + (float)(upward ? SwordSwingConfig.upperPitch : SwordSwingConfig.overheadPitch);
		float modelRoll = (float)(upward ? SwordSwingConfig.upperRoll : SwordSwingConfig.overheadRoll);;

		return new float[]{modelYaw, modelPitch, modelRoll};
	}

	public static Vec3 calculateSpinPosition(LivingEntity entity, float progress) {
		Vec3 entityPos = entity.position();
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		double centerY = entityHeight * 0.8;

		double rotateAngle = yawRad + (progress * 2 * Math.PI) + Math.toRadians(SwordSwingConfig.rotateArcOffset);
		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * SwordSwingConfig.rotateRadiusMult;

		double worldX = entityPos.x + radius * Math.cos(rotateAngle);
		double worldY = entityPos.y + centerY;
		double worldZ = entityPos.z + radius * Math.sin(rotateAngle);

		return new Vec3(worldX, worldY, worldZ);
	}

	public static float[] calculateSpinRotation(LivingEntity entity, float progress) {
		float yaw = entity.getYRot();
		double yawRad = Math.toRadians(yaw);
		double rotateAngle = yawRad + (progress * 2 * Math.PI) + Math.toRadians(SwordSwingConfig.rotateArcOffset);

		float modelYaw = (float) Math.toDegrees(rotateAngle) + (float) SwordSwingConfig.rotateYawOffset;
		float modelPitch = 0f + (float)SwordSwingConfig.rotatePitch;
		float modelRoll = 0f + (float)SwordSwingConfig.rotateRoll;

		return new float[]{modelYaw, modelPitch, modelRoll};
	}

	public static Vec3 calculateRawSlashPosition(LivingEntity entity, float progress, SlashRenderRequest req) {
		Vec3 entityPos = entity.position().add( req.posOffset);
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);

		// Center height varies based on angle
		// For horizontal slashes (angle~0), use 0.75
		// For vertical slashes (angle~90), use 0.9
		double angleRad = Math.toRadians(req.angle);
		double centerY = entityHeight * (0.75 + 0.15 * Math.abs(Math.sin(angleRad)));

		// Calculate arc progress using custom arc range
		// Apply angleOffset to rotate the slash around the player
		// Reverse flag flips the sweep direction (like leftToRight vs rightToLeft)
		double effectiveProgress = req.reverse ? (1.0 - progress) : progress;
		double arcAngle = effectiveProgress * req.arcRange + req.angleOffset;

		// Base radius from config, scaled by radiusScaler
		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * req.radiusScaler;

		// The angle parameter controls the tilt of the rotation axis
		// angle=0: horizontal slash (rotating around vertical axis)
		// angle=90: vertical slash (rotating around horizontal axis)
		// angle=180: horizontal slash but flipped

		// Calculate position in local space
		// We rotate the arc position by the angle to tilt the slash plane
		double arcRad = Math.toRadians(arcAngle);

		// Base arc in horizontal plane
		double baseX = radius * Math.cos(arcRad);
		double baseZ = radius * Math.sin(arcRad);
		double baseY = 0;

		// Apply the angle tilt (rotate around the forward axis)
		// This transforms horizontal slash → vertical slash as angle goes 0→90
		double tiltedX = baseX;
		double tiltedY = baseZ * Math.sin(angleRad);
		double tiltedZ = baseZ * Math.cos(angleRad);

		// Rotate to match player facing direction
		double worldX = entityPos.x + (tiltedX * Math.cos(yawRad) - tiltedZ * Math.sin(yawRad));
		double worldY = entityPos.y + centerY + tiltedY;
		double worldZ = entityPos.z + (tiltedX * Math.sin(yawRad) + tiltedZ * Math.cos(yawRad));
		
		return new Vec3(worldX, worldY, worldZ);
		
		/*if (Math.abs(req.angle) < 45) {
			double localX = radius * Math.cos(Math.toRadians(arcAngle)) * (req.reverse ? -1 : 1);
			double localZ = radius * Math.sin(Math.toRadians(arcAngle));
			double localY = (req.angle/45) * Math.sin(Math.toRadians(arcAngle - req.arcRange / 2));
	
			double worldX = entityPos.x + (localX * Math.cos(yawRad) - localZ * Math.sin(yawRad));
			double worldY = entityPos.y + centerY + localY;
			double worldZ = entityPos.z + (localX * Math.sin(yawRad) + localZ * Math.cos(yawRad));
	
			return new Vec3(worldX, worldY, worldZ);
		}
		else
		{
			//double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * (upward ? SwordSwingConfig.upperRadiusMult : SwordSwingConfig.overheadRadiusMult);
			double localY = radius * Math.cos(Math.toRadians(arcAngle));
			double localForward = radius * Math.sin(Math.toRadians(arcAngle));
			double localRight = radius * ((90-req.angle%90)/45);

			double worldX = entityPos.x + localForward * Math.cos(yawRad + Math.PI / 2) + localRight * Math.cos(yawRad);
			double worldY = entityPos.y + centerY + localY * (req.reverse ? -1 : 1);
			double worldZ = entityPos.z + localForward * Math.sin(yawRad + Math.PI / 2) + localRight * Math.sin(yawRad);
			
			return new Vec3(worldX, worldY, worldZ);
		}*/
	}

	public static float[] calculateRawSlashRotation(LivingEntity entity, float progress, SlashRenderRequest req) {
		float yaw = entity.getYRot();

		// Calculate base rotation based on arc progress
		// Reverse flag flips the sweep direction (like leftToRight vs rightToLeft)
		double effectiveProgress = req.reverse ? (1.0 - progress) : progress;
		double arcAngle = effectiveProgress * req.arcRange + req.angleOffset;

		// The angle parameter controls the tilt of the slash plane
		// angle=0: horizontal slash (yaw rotates, pitch=0)
		// angle=90: vertical slash (yaw fixed, pitch rotates)
		// angle=45: diagonal slash (both yaw and pitch rotate)

		double angleRad = Math.toRadians(req.angle);
		double arcRad = Math.toRadians(arcAngle);

		// Decompose the arc rotation into yaw and pitch based on the angle
		// For horizontal slashes (angle=0), arc affects yaw
		// For vertical slashes (angle=90), arc affects pitch
		float arcYawContribution = (float) (arcAngle * Math.cos(angleRad));
		float arcPitchContribution = (float) (arcAngle * Math.sin(angleRad));

		// Calculate final rotations
		float modelYaw = yaw + arcYawContribution + req.yawOffset;
		float modelPitch = arcPitchContribution + req.pitchOffset;
		// angleOffset spins the slash around its own axis (added to roll)
		float modelRoll = req.angle + req.rollOffset;

		return new float[]{modelYaw, modelPitch, modelRoll};

}
	public static Vec3 calculateRawHorizontalPosition(LivingEntity entity, float progress, SlashRenderRequest req) {

		Vec3 entityPos = entity.position().add(req.posOffset);
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		double centerY = entityHeight * 0.75;

		double totalArcDegrees = req.arcRange;
		double arcAngle = progress * totalArcDegrees + (req.angleOffset);

		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * (req.radiusScaler);
		double localX = radius * Math.cos(Math.toRadians(arcAngle)) * (req.reverse ? -1 : 1);
		double localZ = radius * Math.sin(Math.toRadians(arcAngle));
		double localY = (req.hor) * Math.sin(Math.toRadians(arcAngle - totalArcDegrees / 2));

		double worldX = entityPos.x + (localX * Math.cos(yawRad) - localZ * Math.sin(yawRad));
		double worldY = entityPos.y + centerY + localY;
		double worldZ = entityPos.z + (localX * Math.sin(yawRad) + localZ * Math.cos(yawRad));

		return new Vec3(worldX, worldY, worldZ);
	}

	public static float[] calculateRawHorizontalRotation(LivingEntity entity, float progress, SlashRenderRequest req) {
		float yaw = entity.getYRot();
		double totalArcDegrees = req.arcRange;
		double arcAngle = progress * totalArcDegrees + (req.angleOffset);

		// Model should rotate around the player as it sweeps
		// Yaw rotates the model around the vertical axis to follow the arc
		float modelYaw = yaw + (float) (arcAngle-90) * (req.reverse ? -1 : 1) + (float)(req.yawOffset);

		// Pitch tilts the slash plane slightly for visual effect
		float modelPitch = req.pitchOffset;

		// Roll rotates the slash around its own forward axis
		float modelRoll = req.rollOffset;

		return new float[]{modelYaw, modelPitch, modelRoll};
	}

	public static Vec3 calculateRawVerticalPosition(LivingEntity entity, float progress, SlashRenderRequest req) {
		Vec3 entityPos = entity.position().add(req.posOffset);
		float yaw = entity.getYRot();
		double entityHeight = entity.getBbHeight();
		double yawRad = Math.toRadians(yaw);
		double centerY = entityHeight * 0.9;
		
		double totalArcDegrees = req.arcRange;
		double arcAngle = progress * totalArcDegrees + req.angleOffset + (req.reverse ? -20 : 0);
		double arcAngleRad = Math.toRadians(arcAngle);

		double radius = ParticleConfig.baseRadius * SwordSwingConfig.globalRadiusMult * req.radiusScaler;
		double localY = radius * Math.cos(arcAngleRad);
		double localForward = radius * Math.sin(arcAngleRad);
		double localRight = radius * (req.vert*0.5) * Math.cos(arcAngleRad);

		double worldX = entityPos.x + localForward * Math.cos(yawRad + Math.PI / 2) + localRight * Math.sin(yawRad + Math.PI / 2);
		double worldY = entityPos.y + centerY + localY * (req.reverse ? -1 : 1);
		double worldZ = entityPos.z + localForward * Math.sin(yawRad + Math.PI / 2) - localRight * Math.cos(yawRad + Math.PI / 2);

		return new Vec3(worldX, worldY, worldZ);
	}

	public static float[] calculateRawVerticalRotation(LivingEntity entity, float progress, SlashRenderRequest req) {
		
		float yaw = entity.getYRot();
		double totalArcDegrees = req.arcRange;
		double arcAngle = progress * totalArcDegrees + req.angleOffset + (req.reverse ? -20 : 0);

		// Model rotates around player in vertical plane
		float modelYaw = yaw + (float)(req.yawOffset);
		float modelPitch = (float) arcAngle * (req.reverse ? -1 : 1) + (float)(req.pitchOffset) + (req.reverse ? 90 : -90);
		float modelRoll = (float)(-req.rollOffset)+90;

		return new float[]{modelYaw, modelPitch, modelRoll};
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
