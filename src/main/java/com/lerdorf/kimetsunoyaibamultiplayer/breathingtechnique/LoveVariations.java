package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joml.Vector3f;

/**
 * Enhanced Love Breathing variations (22000s range).
 * These variations are for the Enhanced Love Breathing forms added by Kimetsunoyaiba-Tweaks.
 */
public class LoveVariations {

    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, 20);
        } else {
            AnimationHelper.playAnimation(entity, animationName);
        }
    }

    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return;
        }

        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
            data.setCancelAttackSwing(value);
        });

        if (player instanceof ServerPlayer serverPlayer) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket(player.getUUID(), value),
                    serverPlayer);
        }
    }

    public static void register() {
        Log.info("Registering Love Breathing variations from Tweaks...");

        registerFirstFormVariation();
        registerFifthFormVariation();

        Log.info("Finished registering Love Breathing variations from Tweaks");
    }

    /**
     * Love Breathing First Form: Shivers of First Love, Entwined
     * Enhanced Love First Form (22001)
     */
    private static void registerFirstFormVariation() {
        BreathingFormVariation variation = new BreathingFormVariation(
            "First Form: Shivers of First Love, Entwined",
            "Attack the targets continuously, launching them into the air, and then pounce on them",
            14,
            (entity, level, formId) -> {
            	final ServerLevel serverLevel = level instanceof ServerLevel ? ((ServerLevel)level) : null;

                // Set guard state (formId auto-injected as 22001)
                GuardStateHelper.setGuardState(entity, 8.0, formId);
                
                // Use default player step height (0.6f) for reset
				final float originalStepHeight = 0.6f;

				MovementHelper.setStepHeight(entity, 3);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);
                
                final int firstFormStart = 80; // 4 seconds

                final int totalDuration = 90 + firstFormStart; // 4.5 seconds + first form start
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
        			
        		float [][][] particlePointsFirstForm = ParticlePositions.first_form.get("point_a");
        		float [][][] particlePointsSwordLeft = ParticlePositions.sword_to_left.get("point_a");
        		float [][][] particlePointsSwordRight = ParticlePositions.sword_to_right.get("point_a");
        		
        		// Find target - check for entity within 12 blocks on crosshair
				Vec3 lookVec = entity.getLookAngle();
				Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
				Vec3 endPos = startPos.add(lookVec.scale(12.0));

				// Raycast to find entity
				AABB searchBox = new AABB(startPos, endPos).inflate(1.0);
				List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
						e -> e != entity && e.isAlive());

				// Determine circle center - either targeted entity or default position
				Vec3 targetPos;
				LivingEntity targetEntity = null;

				if (!nearbyEntities.isEmpty()) {
					nearbyEntities
							.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(entity.position())));
					targetEntity = nearbyEntities.get(0);
					targetPos = targetEntity.position();
				} else {
					targetPos = entity.position().add(lookVec.scale(10.0));
				}

				// entity.setNoGravity(true);

				final Vec3 finalTargetPos = targetPos;
				final LivingEntity finalTargetEntity = targetEntity;
				final double ogCircleRadius = 10.0;
				final int totalTicks = 80; // 4 seconds
				final int attackInterval = 5; // ~4 attacks per second
				final double angularVelocity = (Math.PI * 2) / totalTicks; // Radians per tick

				// Store player's starting angle
				Vec3 toPlayer = entity.position().subtract(finalTargetPos);
				final double startAngle = Math.atan2(toPlayer.z, toPlayer.x);
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                	
                	
                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}
                	
                	float[][][] particlePoints = null;
                	int particlePointsOffset = 0;
                	
                	boolean leftSwing = (currentTick[0]/10) % 2 == 0; // is it a swing to the left or a swing to the right (alternate every 10 ticks)
                	
                	if (currentTick[0] >= firstFormStart) { // Play the particles from the og first form
                		particlePoints = particlePointsFirstForm;
                		particlePointsOffset = -firstFormStart;
                	}
                	else if (leftSwing) { // Left Swing 
                		particlePoints = particlePointsSwordLeft;
                		particlePointsOffset = -((int)(currentTick[0]/10))*10; // start it at the nearest 10
                	}
                	else { // Right Swing
                		particlePoints = particlePointsSwordRight;
                		particlePointsOffset = -((int)(currentTick[0]/10))*10; // start it at the nearest 10
                	}
                	
                	if (serverLevel != null && particlePoints != null && currentTick[0]+particlePointsOffset < particlePoints.length && particlePoints[currentTick[0]+particlePointsOffset].length > 0) {
                		for (int i = 0; i < particlePoints[currentTick[0]+particlePointsOffset].length; i++) {
                			float x = particlePoints[currentTick[0]+particlePointsOffset][i][0];
                			float y = particlePoints[currentTick[0]+particlePointsOffset][i][1];
                			float z = particlePoints[currentTick[0]+particlePointsOffset][i][2];

                			Vec3 pos = entity.position().add(vectors[0].scale(z).add(vectors[1].scale(x)).add(0, y, 0));

                			// Spawn pink dust particle at pos
                			serverLevel.sendParticles(
                		            new EnergyParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.2f                           // scale
                		            ),
                		            pos.x, pos.y, pos.z,
                		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                		        );
                			
                			// Spawn white dust particle at pos
                			serverLevel.sendParticles(
                					new EnergyParticleOptions(
                    		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                    		                1f                           // scale
                    		            ),
                		            pos.x, pos.y, pos.z,
                		            1, 0, 0, 0, 0 // count, velocity
                		        );
                		}
                	}
                	
                	// START OF CIRCLING
                	if (currentTick[0] < firstFormStart) {
                		// Circle around the target and continuously do some sword slashes
                		if (currentTick[0] % 10 == 0) {
	                		if (leftSwing) {
	                			playEntityAnimation(entity, "sword_to_left");
	                			EnhancedLoveForms.triggerWhipAnimation(entity, "sword_to_left", 1.0);
	                		}
	                		else {
	                			playEntityAnimation(entity, "sword_to_right");
	                			EnhancedLoveForms.triggerWhipAnimation(entity, "sword_to_right", 1.0);
	                		}
                		}
                		
                		
                		double circleRadius = Math.min(Math.max(ogCircleRadius + (currentTick[0] / 20), 3.5),
								ogCircleRadius);

						// Calculate current target angle (3x faster rotation)
						double currentAngle = startAngle + (currentTick[0] * angularVelocity * 3.0);

						// Get current center position (follow target entity if available)
						Vec3 currentCenter = finalTargetEntity != null ? finalTargetEntity.position() : finalTargetPos;

						// Calculate where entity SHOULD be on the circle
						Vec3 targetPosition = MovementHelper.calculateCirclePosition(currentCenter, circleRadius,
								currentAngle);

						// Calculate next position (slightly ahead for smoother motion)
						double nextAngle = currentAngle + (angularVelocity * 3.0);
						Vec3 nextPosition = MovementHelper.calculateCirclePosition(currentCenter, circleRadius,
								nextAngle);

						// Calculate velocity to move from current position towards next position
						Vec3 playerPos = entity.position();
						Vec3 toNextPosition = nextPosition.subtract(playerPos);

						// Also add correction to pull player towards the circle if they're off-path
						Vec3 toTargetPosition = targetPosition.subtract(playerPos);

						// Weighted combination: 70% forward motion, 30% position correction
						Vec3 forwardVelocity = toNextPosition.scale(0.4); // Move towards next position
						Vec3 correctionVelocity = toTargetPosition.scale(0.3); // Correct towards current position
						Vec3 combinedVelocity = forwardVelocity.add(correctionVelocity);

						// Preserve some Y velocity for terrain following, but dampen falling
						double yVelocity = entity.getDeltaMovement().y;
						if (yVelocity < 0) {
							yVelocity = Math.max(yVelocity, -0.2); // Limit falling speed
						}

						// Set velocity with synchronization
						MovementHelper.setVelocity(entity, combinedVelocity.x, yVelocity, combinedVelocity.z);

						// Make entity look at circle center
						MovementHelper.lookAt(entity, currentCenter);

						// Spawn tornado-like particles
						if (serverLevel != null) {
							// Spiral pattern around entity
							int particleCount = 8;
							for (int i = 0; i < particleCount; i++) {
								double particleAngle = (currentTick[0] * 0.5 + i * (Math.PI * 2 / particleCount))
										% (Math.PI * 2);
								double particleRadius = 1.0 + Math.sin(currentTick[0] * 0.3) * 0.5;
								double px = entity.getX() + Math.cos(particleAngle) * particleRadius;
								double pz = entity.getZ() + Math.sin(particleAngle) * particleRadius;
								double py = entity.getY() + 0.5 + (currentTick[0] % 20) * 0.1;

								serverLevel.sendParticles( new EnergyParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.2f                           // scale
                		            ), px, py, pz, 1, 0, 0.1, 0, 0.02);
							}

							// Sweep attack particles
							if (currentTick[0] % 3 == 0) {
								serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
										(entity.getX() + currentCenter.x) / 2 + 3 * (Math.random() - 0.5),
										(entity.getY() + currentCenter.y) / 2 + 1 + 3 * (Math.random() - 0.5),
										(entity.getZ() + currentCenter.z) / 2 + 3 * (Math.random() - 0.5), 1, 0, 0, 0,
										0);
							}

							// Circular path particles
							for (int i = 0; i < 12; i++) {
								double pathAngle = currentAngle + (i * Math.PI / 6);
								double pathX = currentCenter.x + Math.cos(pathAngle) * circleRadius;
								double pathZ = currentCenter.z + Math.sin(pathAngle) * circleRadius;
								serverLevel.sendParticles( new EnergyParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.2f                           // scale
                		            ), pathX, currentCenter.y + 0.5, pathZ,
										1, 0, 0.05, 0, 0.01);
							}
						}

						if (currentTick[0] % 3 == 0) {
							level.playSound(null, entity.blockPosition(), SoundEvents.BAMBOO_WOOD_STEP, SoundSource.PLAYERS, 1.0F,
									0.9F);
						}

						// Attack every attackInterval ticks
						if (currentTick[0] % attackInterval == 0 && currentTick[0] > attackInterval) {
							try {
								// Set attack state for this specific attack
								GuardStateHelper.setAttackState(entity, 3.0);

								MovementHelper.stepUp(entity, combinedVelocity.x, yVelocity, combinedVelocity.z);
								

								AABB attackBox = AABB.of(BoundingBox.fromCorners(
										new Vec3i((int) (currentCenter.x - circleRadius),
												(int) (currentCenter.y - circleRadius),
												(int) (currentCenter.z - circleRadius)),
										new Vec3i((int) (currentCenter.x + circleRadius),
												(int) (currentCenter.y + circleRadius),
												(int) (currentCenter.z + circleRadius))));
								// AABB attackBox = entity.getBoundingBox().inflate(3.0);
								List<LivingEntity> circleTargets = entity.level().getEntitiesOfClass(LivingEntity.class,
										attackBox, e -> e != entity && e.isAlive());

								float damage = DamageCalculator.calculateScaledDamage(entity, 3.0F);
								
								for (LivingEntity target : circleTargets) {
									boolean success = Damager.hurt(entity, target, damage);
									if (success) { // Knock the target into the air
										float targetY = (float)entity.position().y() + 10 + (((float)currentTick[0])/(float)(firstFormStart));
										Vec3 curVel =  target.getDeltaMovement();
										if (target.position().y() < targetY)
											MovementHelper.setVelocity(target, curVel.add(0, Math.min(Math.max(targetY-target.position().y(), 0.7), 1.1), 0));
									}
								}

								level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
										SoundSource.PLAYERS, 1.1F, 1.2F);

								// Clear attack flag after attack completes, but keep defense active
								GuardStateHelper.clearAttackFlag(entity);
								GuardStateHelper.enableContinuousDefense(entity);
							} catch (Exception e) {
								Log.error("Love Breathing First Form attack error: " + e.getMessage());
								e.printStackTrace();
							}
						}
                		
                		
                	} // END OF CIRCLING
                	
                	if (currentTick[0] == firstFormStart) { // Start the original first form

                        // Play player animation
                        playEntityAnimation(entity, "love_first_form");
                        
                        // Trigger whip animation (client-side)
                        EnhancedLoveForms.triggerWhipAnimation(entity, "love_first_form", 1.0);
                	}

                	if (currentTick[0]-firstFormStart < 26) { // Preparing for the pounce (1.31 seconds)
                		if (currentTick[0] % 5 == 0 && currentTick[0]-firstFormStart < 10) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
                				    SoundSource.PLAYERS, 1.0f, 1.5f);
                		}
                	}
                	else if (currentTick[0]-firstFormStart < 52) { // Sprinting fast (2.6 seconds)
                		if (currentTick[0]-firstFormStart < 27) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 2.0f);
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
                		}
                		if (currentTick[0]-firstFormStart < 31 && serverLevel != null) {
                			serverLevel.sendParticles(
            						ModParticles.LOVE_IMPACT.get(),
            						entity.getX(), entity.getY(), entity.getZ(),
            						3, 0.3, 0.3, 0.3, 0
            					);
                		}
                		if (currentTick[0]-firstFormStart % 2 == 0) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.HORSE_STEP, SoundSource.PLAYERS, 1.0f, 2f);
                		}
                		// Make the player zoom forward
                		Vec3 forward = vectors[0].scale(1.4); // Speed boost
                		MovementHelper.setVelocity(entity, new Vec3(forward.x, entity.getDeltaMovement().y, forward.z));
                		//entity.setDeltaMovement(forward.x, entity.getDeltaMovement().y, forward.z);

                		
                	}
                	else if (currentTick[0]-firstFormStart < 55) { // Floating and spinning (2.75 seconds)
                		// Give the player slow falling
                		if (currentTick[0]-firstFormStart == 52) {
                			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
                		}
                	}
                	else if (currentTick[0]-firstFormStart > 55 && currentTick[0] % 2 == 1 && currentTick[0]-firstFormStart < 80) { // Sword swing - damage all targets

                		if (currentTick[0] % 3 == 0) {
                			level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        			SoundSource.PLAYERS, 1.5F, 1.3F);
                		}

                	}
                	
                	if (currentTick[0]-firstFormStart >= 27 && currentTick[0]-firstFormStart < 70) {
                		// Collect targets within 6 blocks that are targetable
                		
                			AABB searchBox2 = entity.getBoundingBox().inflate(7.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox2,
                				e -> e != entity && e.isAlive() && EnhancedLoveForms.isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				if (!targets.contains(target)) {
                					targets.add(target);
                					// Spawn love_slash particles on newly added target
                					double particleY = target.getY() + target.getBbHeight() * 0.5;
                					if (serverLevel != null) {
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
                	if (currentTick[0]-firstFormStart > 50 && currentTick[0]-firstFormStart < 80 && currentTick[0] % 2 == 0) {
                		if (Config.logDebug)
                			Log.debugEvery("love-variation-damage:" + entity.getUUID(), 1000,
                			    "Damaging {} targets on tick {}", targets.size(), currentTick[0]);
                		for (LivingEntity target : targets) {
                			if (Math.random() < 0.4) {
	                			//System.out.println("Trying to damage " + target.getName().getString());
	                			float damage = DamageCalculator.calculateScaledDamage(entity, 12.0F);
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
	
	    						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
	                				    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
	
	    						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
	                				    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 2f);
                			}
                		}
                	}

	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    
                    MovementHelper.setStepHeight(entity, originalStepHeight);
                }, totalDuration+2);
            },
            Collections.emptySet()  // Applies to all Love Breathing swords
        );

        VariationRegistry.register(22001, variation);
        Log.debug("DEBUG: Registered '" + variation.getName() + "' for form ID 22001");
    }
    
    /**
     * Fifth Form: Swaying Love, Wildclaw, Everlasting Beauty
     * Enhanced Love Fifth Form (22005)
     */
    private static void registerFifthFormVariation() {
        BreathingFormVariation variation = new BreathingFormVariation(
            "Fifth Form: Swaying Love, Wildclaw, Everlasting Beauty",
            "Whip the target and do a spin to knock them back, and then hit them with the love tornado",
            16,
            (entity, level, formId) -> {
            	final int fifthFormStart = 30; // Start the og fifth form 3 seconds later
            	
            	ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
            	float [][][] particlePointsFifthForm = ParticlePositions.fifth_form.get("point_a");
            	float [][][] particlePointsSwingLeft = ParticlePositions.sword_to_left.get("point_a");
            	float [][][] particlePointsSwingRight = ParticlePositions.sword_to_right.get("point_a");
            	float [][][] particlePointsRotate = ParticlePositions.sword_rotate.get("point_a");

				float damage = DamageCalculator.calculateScaledDamage(entity, 10.0F);
            	 // Set guard state (formId auto-injected as 22005)
                GuardStateHelper.setGuardState(entity, damage*0.75f, formId);

                

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 96+fifthFormStart; // 5.8 seconds
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
            	
                	if (currentTick[0] == fifthFormStart) {
                		// Play player animation
                        playEntityAnimation(entity, "love_fifth_form");
                        
                        // Trigger whip animation (client-side)
                        EnhancedLoveForms.triggerWhipAnimation(entity, "love_fifth_form", 1.0);
                	}
                	
                	if (currentTick[0]-fifthFormStart == 25 || (currentTick[0]-fifthFormStart < 25 && currentTick[0] %4 == 0)) {
                		float yaw = (float) Math.toRadians(-entity.getYRot());

                		// Build orthonormal basis (forward, right, up)
                		vecs[0] = entity.position();
                		vecs[1] = entity.getLookAngle(); // Forward vector
                		if (vecs[1].y > 0) vecs[1] = new Vec3(vecs[1].x, 0, vecs[1].z);
                		vecs[2] = new Vec3(Math.sin(yaw + Math.PI/2), 0, Math.cos(yaw + Math.PI/2)).normalize(); // Right vector (90° from forward on horizontal plane)
                		vecs[3] = vecs[2].cross(vecs[1]).scale(-1).normalize(); // Up vector (right × forward, perpendicular to both)
                		if (currentTick[0]-fifthFormStart == 25) {
                			yawPitch[0] = (float) Math.toDegrees(Math.atan2(-vecs[1].x, vecs[1].z));
                			yawPitch[1] = (float) Math.toDegrees(Math.atan2(-vecs[1].y, Math.sqrt(vecs[1].x*vecs[1].x + vecs[1].z*vecs[1].z)));
                			
                			Vec3 eyePos = entity.getEyePosition();
                			// Find the target block
                			for (int i = 1; i < range; i++) {
                    			Vec3 vec = eyePos.add(vecs[1].scale(i));
                    			
                    			vecs[4] = vec; // Set the target position

                    			// Check block collision
                    			BlockPos blockPos = new BlockPos((int)Math.floor(vec.x), (int)Math.floor(vec.y), (int)Math.floor(vec.z));
                    			if (!level.getBlockState(blockPos).isAir()) {
                    				
                    				break; // Stop at first block hit
                    			}
                			}
                		}
                	}
                	
                	float[][][] particlePoints = null;
                	int particlePointsOffset = 0;
                	
                	if (currentTick[0] >= fifthFormStart) {
                		particlePoints = particlePointsFifthForm;
                		particlePointsOffset = -fifthFormStart;
                	}
                	else if (currentTick[0] < fifthFormStart / 3) { // right
                		particlePoints = particlePointsSwingRight;
                		particlePointsOffset = 0;
                		if (currentTick[0] == 0) {
                			// Play player animation
                            playEntityAnimation(entity, "sword_to_right");
                            
                            // Trigger whip animation (client-side)
                            EnhancedLoveForms.triggerWhipAnimation(entity, "sword_to_right", 1.0);
                            
                            // Hit all the targets in front of you forward (big sweep with knockback)
                            
                            AABB hitBox = new AABB(entity.position().add(entity.getLookAngle().scale(9)).add(-9, -4, -9), entity.position().add(entity.getLookAngle().scale(5).add(9, 4, 9)));
                            List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);
                            
                            for (Entity target : targets) {
                            	if (target instanceof LivingEntity le && EnhancedLoveForms.isTargetable(entity, le)) {
                            		MovementHelper.setVelocity(le, le.getDeltaMovement().add((le.position().subtract(entity.position())).normalize().scale(1.5f)));
                            		Damager.hurt(entity, le, damage * 0.7f);
                            		if (serverLevel != null)
	                            		serverLevel.sendParticles(
	        	        						ModParticles.LOVE_IMPACT.get(),
	        	        						le.position().x, le.position().y, le.position().z,
	        	        						1, 0.3, 0.3, 0.3, 0
	        	        					);
                            	}
                            }
                		}
                	}
                	else if (currentTick[0] < fifthFormStart * 2 / 3) { // left
                		particlePoints = particlePointsSwingLeft;
                		particlePointsOffset = fifthFormStart / 3;
                		if (currentTick[0] == fifthFormStart / 3) {
                			// Play player animation
                            playEntityAnimation(entity, "sword_to_left");
                            
                            // Trigger whip animation (client-side)
                            EnhancedLoveForms.triggerWhipAnimation(entity, "sword_to_left", 1.0);
                            
                            // Hit all the targets in front of you forward (big sweep with knockback)
                            
                            AABB hitBox = new AABB(entity.position().add(entity.getLookAngle().scale(9)).add(-9, -4, -9), entity.position().add(entity.getLookAngle().scale(5).add(9, 4, 9)));
                            List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);
                            
                            for (Entity target : targets) {
                            	if (target instanceof LivingEntity le && EnhancedLoveForms.isTargetable(entity, le)) {
                            		MovementHelper.setVelocity(le, le.getDeltaMovement().add((le.position().subtract(entity.position())).normalize().scale(1.5f)));
                            		Damager.hurt(entity, le, damage*0.7f);
                            		if (serverLevel != null)
	                            		serverLevel.sendParticles(
	        	        						ModParticles.LOVE_IMPACT.get(),
	        	        						le.position().x, le.position().y, le.position().z,
	        	        						1, 0.3, 0.3, 0.3, 0
	        	        					);
                            	}
                            }
                		}
                	}
                	else if (currentTick[0] < fifthFormStart) { // rotate
                		particlePoints = particlePointsRotate;
                		particlePointsOffset = fifthFormStart * 2 / 3;
                		if (currentTick[0] == fifthFormStart * 2 / 3) {
                			// Play player animation
                            playEntityAnimation(entity, "sword_rotate");
                            
                            // Trigger whip animation (client-side)
                            EnhancedLoveForms.triggerWhipAnimation(entity, "sword_rotate", 1.0);
                            
                            // Hit all the targets around you forward (big sweep with knockback)
                            
                            AABB hitBox = entity.getBoundingBox().inflate(12.0);
                            List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);
                            
                            for (Entity target : targets) {
                            	if (target instanceof LivingEntity le && EnhancedLoveForms.isTargetable(entity, le)) {
                            		MovementHelper.setVelocity(le, le.getDeltaMovement().add((le.position().subtract(entity.position())).normalize().scale(1.5f)));
                            		Damager.hurt(entity, le, damage*0.7f);
                            		if (serverLevel != null)
	                            		serverLevel.sendParticles(
	        	        						ModParticles.LOVE_IMPACT.get(),
	        	        						le.position().x, le.position().y, le.position().z,
	        	        						1, 0.3, 0.3, 0.3, 0
	        	        					);
                            	}
                            }
                		}
                	}
                	
                	if (serverLevel != null && particlePoints != null && currentTick[0]+particlePointsOffset < particlePoints.length && particlePoints[currentTick[0]+particlePointsOffset].length > 0) {
                		for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                			float x = particlePoints[currentTick[0]+particlePointsOffset][i][0];
                			float y = particlePoints[currentTick[0]+particlePointsOffset][i][1];
                			float z = particlePoints[currentTick[0]+particlePointsOffset][i][2];

                			Vec3 pos = entity.position().add(vecs[1].scale(z).add(vecs[2].scale(x)).add(vecs[3].scale(y)));

                			// Spawn pink dust particle at pos
                			serverLevel.sendParticles(
                		            new EnergyParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.8f                           // scale
                		            ),
                		            pos.x, pos.y, pos.z,
                		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                		        );
                			
                			// Spawn white dust particle at pos
                			serverLevel.sendParticles(
                					new EnergyParticleOptions(
                    		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                    		                1.5f                           // scale
                    		            ),
                		            pos.x, pos.y, pos.z,
                		            1, 0, 0, 0, 0 // count, velocity
                		        );
                		}
                	}
                	
                	if (currentTick[0] >= fifthFormStart && currentTick[0]-fifthFormStart < 14) {
                		// Jump upwards and backwards (do the backflip)
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.65).add(0, 1.5f, 0));
                	}
                	else if (currentTick[0] >= fifthFormStart && currentTick[0]-fifthFormStart < 25) {
                		// Hover and aim
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                		MovementHelper.lookAtTarget(entity);
                	}
                	else if (currentTick[0] >= fifthFormStart && currentTick[0]-fifthFormStart == 26) {
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
                		entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, totalDuration+2-currentTick[0]-fifthFormStart, 3)); // To stop the fall damage
                	}
                	else if (currentTick[0] >= fifthFormStart && currentTick[0]-fifthFormStart < 31) {
                		// Tornado is starting in the thin phase
                		// Launch in the direction
                		MovementHelper.setRotation(entity, yawPitch[0], yawPitch[1]);
                		float percent = (float)(currentTick[0]-fifthFormStart-25f)/(31f-25f);
                		Vec3 targetPos = vecs[0].scale(1-percent).add(vecs[4].scale(percent)); // Go from position vecs[0] to position vecs[4]
                		MovementHelper.setVelocity(entity, targetPos.subtract(entity.position())); // Set the velocity to the difference in position to target position
                		
                		int r = 6;
                		AABB rayBox = new AABB(targetPos.subtract(r, r, r), targetPos.add(r, r, r));
            			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
            				LivingEntity.class, rayBox,
            				e -> e != entity && e.isAlive() && EnhancedLoveForms.isTargetable(entity, e)
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
            				Damager.hurt(entity, le, damage*0.5f, true);
            			}
                	}
                	else if (currentTick[0] > fifthFormStart && currentTick[0]-fifthFormStart < 41) {
                		// Landed on the ground
                		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position())); // entity is supposed to be at vecs[4] position
                	}
                	else if (currentTick[0] > fifthFormStart && currentTick[0]-fifthFormStart < 45) {
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
                		if (currentTick[0] > fifthFormStart && currentTick[0] % 2 == 0) {
                			int r = (10 + currentTick[0]-fifthFormStart-41);
                    		AABB rayBox = new AABB(vecs[4].subtract(r, r, r), vecs[4].add(r, r, r));
                			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                				LivingEntity.class, rayBox,
                				e -> e != entity && e.isAlive() && EnhancedLoveForms.isTargetable(entity, e)
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
                				Damager.hurt(entity, le, damage*0.5f, true);
                			}
                		}
                	}
                	else if (currentTick[0] > fifthFormStart && currentTick[0]-fifthFormStart < 55) {
                		// Big explosion
                		MovementHelper.setVelocity(entity, vecs[4].subtract(entity.position())); // entity is supposed to be at vecs[4] position
                		
                		if (currentTick[0] % 2 == 0) {
                			int r = (10 + currentTick[0]-fifthFormStart-45);
                    		AABB rayBox = new AABB(vecs[4].subtract(r, r, r), vecs[4].add(r, r, r));
                			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                				LivingEntity.class, rayBox,
                				e -> e != entity && e.isAlive() && EnhancedLoveForms.isTargetable(entity, e)
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
                				Damager.hurt(entity, le, damage);
                			}
                		}
                		
                		if (serverLevel != null) {
                			level.playSound(null, vecs[4].x, vecs[4].y, vecs[4].z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                			for (int i = 0; i < 25; i++) {
	                			// Spawn an explosion that gets bigger and bigger (from tick 41 to 54)
                				float explosionRadius = 2+(currentTick[0]-fifthFormStart - 41) * 1.4f; // Scale from 0 to ~10 blocks
                				Vec3 pos = vecs[4].add((new Vec3(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5)).normalize().scale(explosionRadius));
		                		
                				if (Math.random() > 0.8) {
                					level.playSound(null, vecs[4].x, vecs[4].y, vecs[4].z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
                				}
                				
                				if (Math.random() < 0.3) {
                					serverLevel.sendParticles(
                        		            new EnergyParticleOptions(
                        		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                        		                2.5f                           // scale
                        		            ),
                        		            pos.x, pos.y, pos.z,
                        		            8, 0.5, 0.5, 0.5, 0 // count, velocity
                        		        );
                				} else {
                				
			                		serverLevel.sendParticles(
			        						Math.random() > 0.5 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.EXPLOSION,
			        						pos.x, pos.y, pos.z,
			        						8, 0.5, 0.5, 0.5, 0
			        					);
                				}
                			}
                		}
                	}
                	else if (currentTick[0] > fifthFormStart && currentTick[0]-fifthFormStart < 75) {
                		// Post explosion rings
                		
                		int r = Math.min(currentTick[0]-fifthFormStart-55, 15);
                		
                		for (float f = 0.2f; f < 0.9f; f += 0.3f) {
                			Vec3 ringPos = vecs[4].subtract(vecs[1].scale(f*50));
                			//Vec3 ringPos = vecs[0].scale(1-f).add(vecs[4].scale(f)); // Go from position vecs[0] to position vecs[4]
                			
                			for (float t = 0; t < 2*Math.PI; t+=2f/r) {
                				// Create proper circle: right*cos(t)*r + up*sin(t)*r
                				Vec3 diff = vecs[2].scale(Math.cos(t) * r).add(vecs[3].scale(Math.sin(t) * r));
                				Vec3 pos = ringPos.add(diff);
                				Vec3 pos2 = ringPos.subtract(diff);
                				serverLevel.sendParticles(
		        						ParticleTypes.EXPLOSION,
		        						pos.x, 
		        						pos.y, 
		        						pos.z,
		        						2, 0.3, 0.3, 0.3, 0
		        					);

                				serverLevel.sendParticles(
		        						ParticleTypes.EXPLOSION,
		        						pos2.x, 
		        						pos2.y, 
		        						pos2.z,
		        						2, 0.3, 0.3, 0.3, 0
		        					);
                			}
                		}
                	}
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2); // 3.2 seconds
            },
            Collections.emptySet()  // Applies to all Love Breathing swords
        );

        VariationRegistry.register(22005, variation);
        Log.debug("DEBUG: Registered '" + variation.getName() + "' for form ID 22005");
    }

}
