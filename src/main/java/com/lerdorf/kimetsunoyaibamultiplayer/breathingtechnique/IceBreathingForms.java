package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Vector3f;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;

import net.minecraft.server.level.ServerPlayer;

/**
 * Implementation of all Ice Breathing forms (6 forms + 7th for Hanazawa)
 */
public class IceBreathingForms {

	/**
	 * Helper method to set cancel attack swing state and sync to client
	 */
	private static void setCancelAttackSwing(Player player, boolean value) {
		player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
			data.setCancelAttackSwing(value);
		});

		// Sync to client if on server
		if (player instanceof ServerPlayer serverPlayer) {
			com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
					new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket(player.getUUID(),
							value),
					serverPlayer);
		}
	}

	/**
	 * First Form: Paralyzing Icicle Speed stab with slowness/mining fatigue -
	 * INCREASED RANGE
	 */
	public static BreathingForm firstForm() {
		return new BreathingForm("First Form: Paralyzing Icicle", "Stab forward with incredible speed", 3, // 3 second
																											// cooldown
				(player, level) -> {
					// Play animation
					AnimationHelper.playAnimation(player, "speed_attack_sword");

					// Prevent the attacks from triggering unwanted sword swings and particles (like
					// from the left click attacks)
					setCancelAttackSwing(player, true);

					// Launch player forward a little bit
					Vec3 lookVec = player.getLookAngle();
					player.setDeltaMovement(lookVec.scale(1.2));

					// Apply effects to targets in front - INCREASED RANGE to 5 blocks
					Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
					Vec3 endPos = startPos.add(lookVec.scale(6.0));

					AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
					List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
							e -> e != player && e.isAlive());

					for (LivingEntity target : targets) {
						float damage = DamageCalculator.calculateScaledDamage(player, 8.0F);
						target.hurt(level.damageSources().playerAttack(player), damage);
						target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
						target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 4));
					}

					// Spawn particles - forward thrust straight line
					if (level instanceof ServerLevel serverLevel) {
						ParticleHelper.spawnForwardThrust(serverLevel, startPos, lookVec, 5.0, ParticleTypes.SNOWFLAKE,
								30);
					}

					// Play sounds
					level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
							1.0F, 1.0F);
					level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F,
							1.2F);

					AbilityScheduler.scheduleOnce(player, () -> {
						// We can swing swords normally again
						setCancelAttackSwing(player, false);
					}, 5); // Run this 5 ticks later

				});
	}

	/**
	 * Second Form: Winter Wrath Circle target for 5 seconds, 3 attacks/second,
	 * always facing center Uses velocity-based movement with tornado-like particle
	 * effects
	 */
	public static BreathingForm secondForm() {
		return new BreathingForm("Second Form: Winter Wrath", "Circle and deliver rotational slashes", 8, // 8 second
																											// cooldown
				(player, level) -> {
					// Enable attack animations during this ability
					player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
						data.setCancelAttackSwing(false);
						if (Config.logDebug) {
							Log.debug("Second Form: Enabled attack animations (cancelAttackSwing = false)");
						}
					});

					AnimationHelper.playAnimationOnLayer(player, "ragnaraku1", 10, 1.0f, 3000);

					// Find target - check for entity within 6 blocks on crosshair
					Vec3 lookVec = player.getLookAngle();
					Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
					Vec3 endPos = startPos.add(lookVec.scale(6.0));

					// Raycast to find entity
					AABB searchBox = new AABB(startPos, endPos).inflate(1.0);
					List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
							e -> e != player && e.isAlive());

					// Determine circle center - either targeted entity or default position
					Vec3 targetPos;
					LivingEntity targetEntity = null;

					if (!nearbyEntities.isEmpty()) {
						nearbyEntities
								.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(player.position())));
						targetEntity = nearbyEntities.get(0);
						targetPos = targetEntity.position();
					} else {
						targetPos = player.position().add(lookVec.scale(5.0));
					}

					player.setNoGravity(true);

					final Vec3 finalTargetPos = targetPos;
					final LivingEntity finalTargetEntity = targetEntity;
					final double ogCircleRadius = 6.0;
					final int totalTicks = 80; // 4 seconds
					final int attackInterval = 5; // ~4 attacks per second
					final double angularVelocity = (Math.PI * 2) / totalTicks; // Radians per tick

					// Store player's starting angle
					Vec3 toPlayer = player.position().subtract(finalTargetPos);
					final double startAngle = Math.atan2(toPlayer.z, toPlayer.x);

					// Store original step height
					final float originalStepHeight = player.maxUpStep();

					// Use a counter array to track current tick
					final int[] tickCounter = { 0 };

					// Schedule a single repeating task that runs every tick for 100 ticks
					AbilityScheduler.scheduleRepeating(player, () -> {
						int currentTick = tickCounter[0]++;
						double circleRadius = Math.min(Math.max(ogCircleRadius - (currentTick / 20), 3.5),
								ogCircleRadius);

						// Enable step-up for blocks during ability (1.8 blocks high)
						MovementHelper.setStepHeight(player, 1.8F);

						// Calculate current target angle (3x faster rotation)
						double currentAngle = startAngle + (currentTick * angularVelocity * 3.0);

						// Get current center position (follow target entity if available)
						Vec3 currentCenter = finalTargetEntity != null ? finalTargetEntity.position() : finalTargetPos;

						// Calculate where player SHOULD be on the circle
						Vec3 targetPosition = MovementHelper.calculateCirclePosition(currentCenter, circleRadius,
								currentAngle);

						// Calculate next position (slightly ahead for smoother motion)
						double nextAngle = currentAngle + (angularVelocity * 3.0);
						Vec3 nextPosition = MovementHelper.calculateCirclePosition(currentCenter, circleRadius,
								nextAngle);

						// Calculate velocity to move from current position towards next position
						Vec3 playerPos = player.position();
						Vec3 toNextPosition = nextPosition.subtract(playerPos);

						// Also add correction to pull player towards the circle if they're off-path
						Vec3 toTargetPosition = targetPosition.subtract(playerPos);

						// Weighted combination: 70% forward motion, 30% position correction
						Vec3 forwardVelocity = toNextPosition.scale(0.4); // Move towards next position
						Vec3 correctionVelocity = toTargetPosition.scale(0.3); // Correct towards current position
						Vec3 combinedVelocity = forwardVelocity.add(correctionVelocity);

						// Preserve some Y velocity for terrain following, but dampen falling
						double yVelocity = player.getDeltaMovement().y;
						if (yVelocity < 0) {
							yVelocity = Math.max(yVelocity, -0.2); // Limit falling speed
						}

						// Set velocity with synchronization
						MovementHelper.setVelocity(player, combinedVelocity.x, yVelocity, combinedVelocity.z);

						// Make player look at circle center
						MovementHelper.lookAt(player, currentCenter);

						// Spawn tornado-like particles
						if (level instanceof ServerLevel serverLevel) {
							// Spiral pattern around player
							int particleCount = 8;
							for (int i = 0; i < particleCount; i++) {
								double particleAngle = (currentTick * 0.5 + i * (Math.PI * 2 / particleCount))
										% (Math.PI * 2);
								double particleRadius = 1.0 + Math.sin(currentTick * 0.3) * 0.5;
								double px = player.getX() + Math.cos(particleAngle) * particleRadius;
								double pz = player.getZ() + Math.sin(particleAngle) * particleRadius;
								double py = player.getY() + 0.5 + (currentTick % 20) * 0.1;

								serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0.1, 0, 0.02);
							}

							// Sweep attack particles
							if (currentTick % 3 == 0) {
								serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
										(player.getX() + currentCenter.x) / 2 + 3 * (Math.random() - 0.5),
										(player.getY() + currentCenter.y) / 2 + 1 + 3 * (Math.random() - 0.5),
										(player.getZ() + currentCenter.z) / 2 + 3 * (Math.random() - 0.5), 1, 0, 0, 0,
										0);
							}

							// Circular path particles
							for (int i = 0; i < 12; i++) {
								double pathAngle = currentAngle + (i * Math.PI / 6);
								double pathX = currentCenter.x + Math.cos(pathAngle) * circleRadius;
								double pathZ = currentCenter.z + Math.sin(pathAngle) * circleRadius;
								serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, pathX, currentCenter.y + 0.5, pathZ,
										1, 0, 0.05, 0, 0.01);
							}
						}

						// Attack every attackInterval ticks
						if (currentTick % attackInterval == 0 && currentTick > attackInterval) {
							try {

								MovementHelper.stepUp(player, combinedVelocity.x, yVelocity, combinedVelocity.z);

								String anim = (currentTick / attackInterval) % 2 == 0 ? "sword_to_left"
										: "sword_to_right";

								if (Config.logDebug) {
									Log.debug("Second Form: Playing attack animation '{}' on layer 4000", anim);
								}

								// Use layer 4000 (higher priority) so attack animations overlay on top of
								// ability animation
								AnimationHelper.playAnimationOnLayer(player, anim, 10, 2.0f, 4000);

								AABB attackBox = AABB.of(BoundingBox.fromCorners(
										new Vec3i((int) (currentCenter.x - circleRadius),
												(int) (currentCenter.y - circleRadius),
												(int) (currentCenter.z - circleRadius)),
										new Vec3i((int) (currentCenter.x + circleRadius),
												(int) (currentCenter.y + circleRadius),
												(int) (currentCenter.z + circleRadius))));
								// AABB attackBox = player.getBoundingBox().inflate(3.0);
								List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
										attackBox, e -> e != player && e.isAlive());

								for (LivingEntity target : targets) {
									float damage = DamageCalculator.calculateScaledDamage(player, 6.0F);
									target.hurt(level.damageSources().playerAttack(player), damage);
								}

								level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
										SoundSource.PLAYERS, 1.0F, 1.2F);
							} catch (Exception e) {
								Log.error("Ice Breathing Second Form attack error: " + e.getMessage());
								e.printStackTrace();
							}
						}

						// Last tick - restore step height
						if (currentTick >= totalTicks - 2) {
							MovementHelper.setStepHeight(player, originalStepHeight);
							player.setNoGravity(false);
							Log.debug("Setting no gravity");
						}
					}, 1, totalTicks); // Run every tick for 100 ticks
				});
	}

	/**
	 * Third Form: Merciful Hail Fall Hover and attack for 4 seconds, 3
	 * attacks/second, ragnaraku2 and ragnaraku3
	 */
	public static BreathingForm thirdForm() {
		return new BreathingForm("Third Form: Merciful Hail Fall", "Leap and deliver powerful downward slashes", 7, // 7
																													// second
																													// cooldown
				(player, level) -> {
					// Enable attack animations during this ability
					/*
					 * player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(
					 * data -> { data.setCancelAttackSwing(false); });
					 */

					// Initial leap
					MovementHelper.addVelocity(player, 0, 1.2, 0);

					AnimationHelper.playAnimation(player, "ragnaraku2"); // raise sword

					player.setNoGravity(true);

					final int totalTicks = 60; // 3 seconds
					final int attackInterval = 5; // ~4 attacks per second
					final int[] tickCounter = { 0 };
					final double targetY = player.getY() + 4.0; // Target hover height (4 blocks up)
					final double[] columnPos = { 0, 0 };

					// Single repeating task instead of 80 individual tasks
					AbilityScheduler.scheduleRepeating(player, () -> {
						int currentTick = tickCounter[0]++;

						// Hover at target height - stop vertical movement
						double currentY = player.getY();
						if (currentY < targetY && currentTick < 15) {
							// Still ascending to target height (first 15 ticks)
							MovementHelper.setVelocity(player, player.getDeltaMovement().x, 0.3,
									player.getDeltaMovement().z);
						} else {
							// At target height - completely stop vertical movement
							MovementHelper.setVelocity(player, player.getDeltaMovement().x, 0,
									player.getDeltaMovement().z);
						}

						// Attack every attackInterval ticks
						if (currentTick % attackInterval == 0 && currentTick > attackInterval) {
							// String anim = (currentTick / attackInterval) % 2 == 0 ? "sword_to_left" :
							// "sword_to_right";
							String anim = "sword_overhead";
							// Use layer 4000 so attacks show without being overridden
							AnimationHelper.playAnimationOnLayer(player, anim, 10, 1.0f, 4000);

							Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(3));
							AABB area = new AABB(pos.x - 4, player.getY() - 8, pos.z - 4, pos.x + 4, player.getY(),
									pos.z + 4);
							List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
									e -> e != player && e.isAlive() && e.getY() < player.getY() + 2);

							for (LivingEntity target : targets) {
								float damage = DamageCalculator.calculateScaledDamage(player, 5.0F);
								target.hurt(level.damageSources().playerAttack(player), damage);
							}

							if (level instanceof ServerLevel serverLevel) {

								for (int i = 0; i < 10; i++) {
									double offsetX = (level.random.nextDouble() - 0.5) * 8;
									double offsetZ = (level.random.nextDouble() - 0.5) * 8;
									serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, player.getX() + offsetX,
											player.getY(), player.getZ() + offsetZ, 1, 0, -0.5, 0, 0.1);

									serverLevel.sendParticles(
											new BlockParticleOption(ParticleTypes.BLOCK,
													Blocks.PACKED_ICE.defaultBlockState()),
											pos.x + columnPos[0],
											player.getY() + player.getEyeHeight() - 0.3 * (i + 10),
											pos.z + columnPos[1], 10, 0, 0, 0, 0.1);
								}
								serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x + columnPos[0],
										player.getY() + player.getEyeHeight() - 0.3 * 20, pos.z + columnPos[1], 1, 0, 0,
										0, 0.1);

								SwordParticleHandler.spawnSwordParticles(player, player.getMainHandItem(),
										"sword_overhead", 10);
							}

							level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
									SoundSource.PLAYERS, 1.0F, 0.8F);
						} else if ((currentTick - attackInterval / 2) % attackInterval == 0
								&& (currentTick - attackInterval / 2) > attackInterval) {
							columnPos[0] = (level.random.nextDouble() - 0.5) * 4;
							columnPos[1] = (level.random.nextDouble() - 0.5) * 4;
							String anim = "sword_to_upper";
							// Use layer 4000 so attacks show without being overridden
							AnimationHelper.playAnimationOnLayer(player, anim, 10, 1.5f, 4000);

							if (level instanceof ServerLevel serverLevel) {

								Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(3));
								for (int i = 0; i < 10; i++) {
									serverLevel.sendParticles(
											new BlockParticleOption(ParticleTypes.BLOCK,
													Blocks.PACKED_ICE.defaultBlockState()),
											pos.x + columnPos[0], player.getY() + player.getEyeHeight() - 0.3 * i,
											pos.z + columnPos[1], 10, 0, 0, 0, 0.1);
								}
							}
						}

						if (currentTick >= totalTicks - 2) {
							player.setNoGravity(false);
						}
					}, 1, totalTicks);

					// Play rain sound at start
					level.playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.5F,
							1.0F);
				});
	}

	/**
	 * Fourth Form: Silent Avalanche Teleport forward without going through blocks,
	 * increased range
	 */
	public static BreathingForm fourthForm() {
		return new BreathingForm("Fourth Form: Silent Avalanche", "Dash forward with incredible speed", 4, // 4 second
																											// cooldown
				(player, level) -> {
					AnimationHelper.playAnimation(player, "kamusari3");

					// Find safe teleport position up to 40 blocks away
					Vec3 lookVec = player.getLookAngle();
					Vec3 startPos = player.getEyePosition();
					Vec3 targetPos = startPos.add(lookVec.scale(40.0));

					// Raycast to find first non-passable block
					BlockHitResult hitResult = level.clip(new ClipContext(startPos.add(0, player.getEyeHeight(), 0),
							targetPos.add(0, player.getEyeHeight(), 0), ClipContext.Block.COLLIDER,
							ClipContext.Fluid.NONE, player));

					if (hitResult.getType() == HitResult.Type.BLOCK) {
						// Hit a block, teleport just before it
						Vec3 hitPos = hitResult.getLocation();
						targetPos = startPos.add(hitPos.subtract(startPos).normalize()
								.scale(Math.max(0, startPos.distanceTo(hitPos) - 1.0)));
					}

					// Check for entity target
					AABB searchBox = new AABB(startPos, targetPos).inflate(2.0);
					List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
							e -> e != player && e.isAlive());

					if (!nearbyEntities.isEmpty()) {
						Vec3 entityPos = nearbyEntities.get(0).position();
						if (startPos.distanceTo(entityPos) < startPos.distanceTo(targetPos)) {
							targetPos = entityPos;
						}
					}

					// Teleport player
					player.teleportTo(targetPos.x, targetPos.y, targetPos.z);

					// Damage nearby entities (AOE)
					AABB area = player.getBoundingBox().inflate(3.0);
					List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
							e -> e != player && e.isAlive());

					for (LivingEntity target : targets) {
						float damage = DamageCalculator.calculateScaledDamage(player, 12.0F);
						target.hurt(level.damageSources().playerAttack(player), damage);
					}

					// Spawn particles
					if (level instanceof ServerLevel serverLevel) {
						ParticleHelper.spawnCircleParticles(serverLevel, player.position().add(0, 1, 0), 3.0,
								ParticleTypes.CLOUD, 30);
						ParticleHelper.spawnCircleParticles(serverLevel, player.position().add(0, 1, 0), 3.0,
								ParticleTypes.SNOWFLAKE, 40);
					}

					level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.0F,
							0.8F);
				});
	}

	/**
	 * Fifth Form: Cold Blue Assault Fast dash with 3 attacks/second, forced
	 * movement
	 */
	public static BreathingForm fifthForm() {
		return new BreathingForm("Fifth Form: Cold Blue Assault", "Swift dash with continuous slashes", 6, // 5 second
																											// cooldown
				(player, level) -> {
					AnimationHelper.playAnimation(player, "kamusari3");
					player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 3));

					final Vec3 dashDirection = player.getLookAngle();
					final int totalTicks = 40; // 2 seconds
					final int attackInterval = 5;
					final int[] tickCounter = { 0 };

					AbilityScheduler.scheduleRepeating(player, () -> {
						int currentTick = tickCounter[0]++;

						// Force player to move forward (preserve Y velocity for gravity/jumping)
						Vec3 horizontalVelocity = dashDirection.scale(0.75);
						MovementHelper.setVelocity(player, horizontalVelocity.x, player.getDeltaMovement().y,
								horizontalVelocity.z);

						MovementHelper.stepUp(player, player.getX() + horizontalVelocity.x, player.getY(),
								player.getZ() + horizontalVelocity.z);

						// Attack every attackInterval ticks
						if (currentTick % attackInterval == 0) {
							AnimationHelper.playAnimation(player, "sword_rotate");

							Vec3 attackPos = player.position().add(dashDirection.scale(2.0));
							AABB hitBox = new AABB(attackPos, attackPos).inflate(2.0);
							List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
									e -> e != player && e.isAlive());

							for (LivingEntity target : targets) {
								float damage = DamageCalculator.calculateScaledDamage(player, 5.0F);
								target.hurt(level.damageSources().playerAttack(player), damage);
							}

							if (level instanceof ServerLevel serverLevel) {
								ParticleHelper.spawnParticleLine(serverLevel, player.position().add(0, 1, 0),
										player.position().add(0, 1, 0).add(dashDirection.scale(3.0)),
										ParticleTypes.SNOWFLAKE, 10);
							}

							level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
									SoundSource.PLAYERS, 1.0F, 1.2F);
						}
					}, 1, totalTicks);

					level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F,
							1.0F);
				});
	}

	/**
	 * Sixth Form: Snowflake Cycle Two parts: Jump with ragnaraku1, then AOE slash
	 * with sword_rotate
	 */
	public static BreathingForm sixthForm() {
		return new BreathingForm("Sixth Form: Snowflake Cycle", "Spin and deliver a devastating slash", 4, // 4 second
																											// cooldown
				(player, level) -> {
					// Part 1: Jump up with ragnaraku1
					AnimationHelper.playAnimation(player, "ragnaraku1");

					Vec3 lookVec = player.getLookAngle();
					player.setDeltaMovement(lookVec.scale(0.5).add(0, 0.8, 0));

					// Spawn spinning particles around player
					if (level instanceof ServerLevel serverLevel) {
						ParticleHelper.spawnCircleParticles(serverLevel, player.position().add(0, 1, 0), 3.0,
								ParticleTypes.SNOWFLAKE, 30);
					}

					// Part 2: After 10 ticks, do the AOE slash with sword_rotate
					AbilityScheduler.scheduleOnce(player, () -> {
						AnimationHelper.playAnimation(player, "sword_rotate");

						// Large AOE damage around player
						AABB area = player.getBoundingBox().inflate(5.0);
						List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
								e -> e != player && e.isAlive());

						for (LivingEntity target : targets) {
							float damage = DamageCalculator.calculateScaledDamage(player, 11.0F);
							target.hurt(level.damageSources().playerAttack(player), damage);
							target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)); // Nausea
						}

						// Spawn more particles for the slash
						if (level instanceof ServerLevel serverLevel) {
							ParticleHelper.spawnCircleParticles(serverLevel, player.position().add(0, 1, 0), 5.0,
									ParticleTypes.SNOWFLAKE, 50);
							ParticleHelper.spawnCircleParticles(serverLevel, player.position().add(0, 1, 0), 5.0,
									ParticleTypes.SWEEP_ATTACK, 30);
						}

						level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
								SoundSource.PLAYERS, 1.0F, 1.0F);
						level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.0F,
								1.0F);
					}, 10);
				});
	}

	/**
	 * Seventh Form: Icicle Claws (Hanazawa's sword only) Thrust that blinds, then 5
	 * seconds of ultra-fast attacks (10 attacks/second)
	 */
	public static BreathingForm seventhForm() {
		return new BreathingForm("Seventh Form: Icicle Claws", "Blind and strike from all directions", 8, // 8 second
																											// cooldown
				(player, level) -> {
					// Enable attack animations during this ability
					setCancelAttackSwing(player, false);

					// Part 1: Initial thrust with blinding effect
					AnimationHelper.playAnimation(player, "speed_attack_sword");

					// Launch player forward slightly
					Vec3 lookVec = player.getLookAngle();
					MovementHelper.setVelocity(player, lookVec.scale(0.8));

					// Apply blindness to targets in front
					Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
					Vec3 endPos = startPos.add(lookVec.scale(6.0));

					AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
					List<LivingEntity> initialTargets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
							e -> e != player && e.isAlive());

					for (LivingEntity target : initialTargets) {
						float damage = DamageCalculator.calculateScaledDamage(player, 8.0F);
						target.hurt(level.damageSources().playerAttack(player), damage);
						target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0)); // 15 seconds blind
					}

					// Spawn thrust particles
					if (level instanceof ServerLevel serverLevel) {
						ParticleHelper.spawnForwardThrust(serverLevel, startPos, lookVec, 6.0, ParticleTypes.SNOWFLAKE,
								40);
					}

					level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
							1.0F, 1.0F);
					level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F,
							1.5F);

					// Part 2: Ultra-fast attack barrage (10 attacks per second = 1 attack per 2
					// ticks)
					final int totalTicks = 80; // 4 seconds
					final int attackInterval = 2; // 10 attacks per second
					final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
							"sword_to_left_reverse", "sword_to_right_reverse" };
					final int[] tickCounter = { 0 };

					// Delay barrage start by 10 ticks
					AbilityScheduler.scheduleRepeating(player, () -> {
						int currentTick = tickCounter[0]++;

						// Attack every attackInterval ticks
						if (currentTick % attackInterval == 0 && currentTick > 12) {
							int animIndex = (currentTick / attackInterval) % 4;

							if (Config.logDebug) {
								Log.debug("Seventh Form: Playing attack animation '{}' on layer 4000",
										animations[animIndex]);
							}

							// Use layer 4000 with 3x speed for ultra-fast attacks
							AnimationHelper.playAnimationOnLayer(player, animations[animIndex], 10, 3.0f, 4000);

							// Large AOE in front of player

							float boxSize = 10;
							Vec3 attackerPos = player.position().add(0, player.getEyeHeight(), 0);
							// Vec3 lookVec = player.getLookAngle().normalize();
							Vec3 frontPos = attackerPos.add(lookVec.scale(boxSize / 2f));

							AABB attackBox = new AABB(frontPos.add(-boxSize / 2f, -boxSize / 2f, -boxSize / 2f),
									frontPos.add(boxSize / 2f, boxSize / 2f, boxSize / 2f));

							// AABB attackBox = player.getBoundingBox().inflate(4.5);
							List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
									attackBox, e -> e != player && e.isAlive());

							for (LivingEntity target : targets) {
								float damage = DamageCalculator.calculateScaledDamage(player, 5F);
								target.hurt(level.damageSources().playerAttack(player), damage);
							}

							// Spawn particles
							if (level instanceof ServerLevel serverLevel) {

								double yawRad = Math.toRadians(player.getYRot());
								double pitchRad = Math.toRadians(40 + Math.random()*20);

								int arcLength = (int) (100 + Math.random() * 70);
								double angle = (Math.random() - 0.5) * 10;
								boolean particle = false;
								if (Math.random() > 0.5) {
									particle = true;
									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 6+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE, 80);
									
									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 5+Math.random()*3, 0.1, arcLength, 1, angle, new DustParticleOptions(new Vector3f(200f/255f, 210f/255f, 1f), (float)(Math.random()+0.2f)), 30);

									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 4+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK, 10);
								} 
								if (Math.random() > 0.5 || !particle) {
									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											6+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE, 80);
									
									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											5+Math.random()*3, 0.1, arcLength, 1, angle, new DustParticleOptions(new Vector3f(200f/255f, 210f/255f, 1f), (float)(Math.random()+0.2f)), 30);

									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											4+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK, 10);
								}
							}

							// Sound every 3rd attack to avoid spam
							// if (currentTick % (attackInterval * 3) == 0) {
							level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
									SoundSource.PLAYERS, 0.7F, 1.4F);
							// }
						}

						// Final tick - play ending sound and effects
						if (currentTick >= totalTicks - 1) {
							if (level instanceof ServerLevel serverLevel) {
								// Large final burst of particles

								double yawRad = Math.toRadians(player.getYRot());
								double pitchRad = Math.toRadians(40 + Math.random()*20);

								int arcLength = (int) (100 + Math.random() * 70);
								double angle = (Math.random() - 0.5) * 4;

								boolean particle = false;
								if (Math.random() > 0.5) {
									particle = true;
									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 6+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE, 80);
									
									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 5+Math.random()*3, 0.1, arcLength, 1, angle, new DustParticleOptions(new Vector3f(200f/255f, 210f/255f, 1f), (float)(Math.random()+0.2f)), 30);

									ParticleHelper.spawnHorizontalArc(serverLevel, player.position().add(0, 2, 0),
											yawRad,pitchRad, 4+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK, 10);
								} 
								if (Math.random() > 0.5 || !particle) {
									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											6+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE, 80);
									
									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											5+Math.random()*3, 0.1, arcLength, 1, angle, new DustParticleOptions(new Vector3f(200f/255f, 210f/255f, 1f), (float)(Math.random()+0.2f)), 30);

									ParticleHelper.spawnVerticalArc(serverLevel, player.position().add(0, 2, 0), yawRad,pitchRad,
											4+Math.random()*3, 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK, 10);
								}
							}
							level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
									1.0F, 1.0F);
						}
					}, 1, totalTicks);
				});
	}

	/**
	 * Create the complete Ice Breathing technique with 6 forms
	 */
	public static BreathingTechnique createIceBreathing() {
		List<BreathingForm> forms = new ArrayList<>();
		forms.add(firstForm());
		forms.add(secondForm());
		forms.add(thirdForm());
		forms.add(fourthForm());
		forms.add(fifthForm());
		forms.add(sixthForm());
		return new BreathingTechnique("Ice Breathing", forms);
	}

	/**
	 * Create Ice Breathing with 7th form for Hanazawa's sword
	 */
	public static BreathingTechnique createIceBreathingWithSeventh() {
		List<BreathingForm> forms = new ArrayList<>();
		forms.add(firstForm());
		forms.add(secondForm());
		forms.add(thirdForm());
		forms.add(fourthForm());
		forms.add(fifthForm());
		forms.add(sixthForm());
		forms.add(seventhForm());
		return new BreathingTechnique("Ice Breathing", forms);
	}
}
