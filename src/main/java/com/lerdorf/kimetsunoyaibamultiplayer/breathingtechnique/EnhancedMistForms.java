package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.FancyMath;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

/**
 * Enhanced implementation of Mist Breathing forms.
 * These enhanced forms feature improved particle effects, better mechanics,
 * and work with both players and mobs (like Muichiro).
 *
 * This class defines enhanced versions of the base kimetsunoyaiba mod's Mist Breathing,
 * with special focus on the 7th Form: Obscuring Clouds.
 */
public class EnhancedMistForms {

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
        }
    }

    /**
     * Helper method to set cancel attack swing state and sync to client
     * Only works for Player entities
     */
    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return; // Skip for non-player entities
        }

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
     * First Form: Low Clouds, Distant Haze
     * The user lunges forward at high speeds to perform a singular and powerful frontal thrust.
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
            "First Form: Low Clouds, Distant Haze",
            "Dash forward through a veil of mist with a singular stab",
            2, // 2 second cooldown
            (entity, level) -> {
            	
                // Set guard state - defensive power 9.0 to match offensive damage
				GuardStateHelper.setGuardState(entity, 9.0, 20001); // ID 20001 for Mist Breathing

				// Use default player step height (0.6f) for reset
				final float originalStepHeight = 0.6f;

				MovementHelper.setStepHeight(entity, 3);
				
				entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 3)); // Slow Falling

				// Play animation
				playEntityAnimation(entity, "speed_attack_sword");

				// Prevent the attacks from triggering unwanted sword swings and particles (like
				// from the left click attacks)
				setCancelAttackSwing(entity, true);

				// Launch player forward a little bit
				Vec3 lookVec = entity.getLookAngle();
				entity.setDeltaMovement(lookVec.scale(3).multiply(1, 0.3f, 1));

				// Set attack state for damage dealing
				GuardStateHelper.setAttackState(entity, 9.0);

				// Apply effects to targets in front - INCREASED RANGE to 5 blocks
				Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
				Vec3 endPos = startPos.add(lookVec.scale(6.0));

				AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
				List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
						e -> e != entity && e.isAlive());
				
				if (level instanceof ServerLevel serverLevel) {
					for (int i = 0; i <= 10; i ++) {
	                    double x = entity.getX() + 3*(Math.random()-0.5);
	                    double y = entity.getY() + 2*(Math.random()-0.5);
	                    double z = entity.getZ() + 3*(Math.random()-0.5);
	
	                    // Spawn a burst of particles at this point
	                    serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
	                        x, y, z,
	                        3, 0.5, 0.3, 0.5, 0.03);
	                }
				}

				for (LivingEntity target : targets) {
					float damage = DamageCalculator.calculateScaledDamage(entity, 9.0F);
					Damager.hurt(entity, target, damage);
					//target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
					//target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 4));

					/*
					// Apply immvable effect from KnY mod
					net.minecraft.world.effect.MobEffect immEffect = KnYEffects.getImmvableEffect();
					if (immEffect != null) {
						target.addEffect(new MobEffectInstance(immEffect, 160, 0));
					}*/
				}

				// Spawn particles - forward thrust straight line
				if (level instanceof ServerLevel serverLevel) {
					ParticleHelper.spawnForwardThrust(serverLevel, startPos, lookVec, 5.0, ModParticles.SMALL_MIST_PARTICLE.get(),
							30);
					ParticleHelper.spawnForwardThrust(serverLevel, startPos, lookVec, 5.0, new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
							(float) (Math.random()*0.7f + 0.2f)),
							30);
				}

                // Restore step height and reset NBT tags after form
                AbilityScheduler.scheduleOnce(entity, () -> {
                    MovementHelper.setStepHeight(entity, originalStepHeight);

                    // CRITICAL: Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    
                    GuardStateHelper.clearGuardState(entity);

                    // Re-enable normal attack swings and particles
                    setCancelAttackSwing(entity, false);
                }, 20);
                
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
						1.0F, 1.0F);

                level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK,
                    SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        );
    }
    
    static String modelKey = "mist";
    
    /**
     * Second Form: Eight-Layered Mist
     * The user performs eight slashes in super quick succession.
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            "Second Form: Eight-Layered Mist",
            "Perform eight slashes in super quick succession",
            5, // 5 second cooldown
            (entity, level) -> {
            	float damage = DamageCalculator.calculateScaledDamage(entity, 7F);
            	GuardStateHelper.setGuardState(entity, damage*2, 20002);
                //playEntityAnimation(entity, "sword_to_right");
                
                // Perform 8 rapid slashes
                final int slashCount = 8;
                final int slashInterval = 2; // 2 ticks between slashes
                final int totalTicks = slashCount*slashInterval+1;
                
                //final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_spin" };
                final String[] animations = { "sword_to_left", "sword_to_right" };

                final int[] tickCounter = { 0 };
                final int[] slashCounter = { 0 };

				AbilityScheduler.scheduleRepeating(entity, () -> {
					Vec3 lookVec = entity.getLookAngle();
					int currentTick = tickCounter[0]++;

					// Attack every slashInterval ticks
					if (currentTick % slashInterval == 0) {
						int slashNumber = slashCounter[0]++;

						// Stop after 8 slashes
						if (slashNumber > slashCount) {
							return;
						}

						int animIndex = slashNumber % animations.length;

						if (Config.logDebug) {
							Log.debug("Second Form: Slash {}/{} - Playing attack animation '{}' on layer 4000",
									slashNumber + 1, slashCount, animations[animIndex]);
						}

						// Use layer 4000 with 1.5x speed for ultra-fast attacks
						playEntityAnimationOnLayer(entity, animations[animIndex], 2, 1.5f, 4000);

						// Large AOE in front of entity

						float boxSize = 6;
						Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0);
						// Vec3 lookVec = entity.getLookAngle().normalize();
						Vec3 frontPos = attackerPos.add(lookVec.scale(boxSize / 2f));

						AABB attackBox = new AABB(frontPos.add(-boxSize / 2f, -boxSize / 2f, -boxSize / 2f),
								frontPos.add(boxSize / 2f, boxSize / 2f, boxSize / 2f));

						// AABB attackBox = entity.getBoundingBox().inflate(4.5);
						List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
								attackBox, e -> e != entity && e.isAlive());

						for (LivingEntity target : targets) {
							Damager.hurt(entity, target, damage);
						}
						
						
						// Spawn particles and slash models (server-side only)
						if (level instanceof ServerLevel serverLevel) {

							double yawRad = Math.toRadians(entity.getYRot()+20);
							float pitchDeg = (float)((Math.random()-0.5) * 20);
							double pitchRad = Math.toRadians(pitchDeg);
							
							float heightRand = (float)( (Math.random() + 0.3) * 2 + pitchRad/Math.PI);
							
							Vec3 pos = entity.position().add(Math.random() - 0.5, heightRand,
									Math.random() - 0.5);
							
							// Send raw slash render request to all clients
							float slashAngle = (float)pitchRad;
							boolean directionFlag = animIndex == 0;
							int arcLength = (int) (100 + Math.random() * 40);
							/*
							BonePositionTracker.sendRawSlashToClients(
									level, // level
									new Vec3(0, heightRand, 0),
									modelKey, // model key
									slashAngle, // angle
									directionFlag, // reverse
									arcLength, // arc range
									(int)(arcLength/1.4f), // duration
									-90, // yaw offset
									(float)pitchDeg, // pitch offset
									0, // roll offset 
									1.2f, // radius scalar
									1.1f, // size scalar
									directionFlag ? -15 : 15, // angle offset
									entity.getUUID(), // entity id
									animations[animIndex]); // animation name
									*/
							BonePositionTracker.sendRawHorizontalSlashToClients(
									level, // level
									new Vec3(3, heightRand-1, 0),
									modelKey, // model key
									(float)0, // hor
									directionFlag, // reverse
									arcLength, // arc range
									(int)(arcLength/1.4f), // duration
									0, // yaw offset
									(float)pitchDeg, // pitch offset
									0, // roll offset 
									1.2f, // radius scalar
									1.1f, // size scalar
									directionFlag ? -15 : 15, // angle offset
									entity.getUUID(), // entity id
									animations[animIndex]); // animation name

							double angle = 0;
							{
								for (int i = 0; i < 2; i++) {
									ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
											2+i*0.3f, 0.2, arcLength, Math.random()*10+10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
											1);

									ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
											2+i*0.3f, 0.2, arcLength, Math.random()*15+15, angle, new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
													(float) (Math.random()*0.9f + 0.2f)), 1
												);
								}
							}
						}

						// if (currentTick % (attackInterval * 3) == 0) {
						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
								SoundSource.PLAYERS, 0.7F, 1.4F);
						level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
								1.9F);
						// }
					}

				}, 1, totalTicks+1);
				
				// CRITICAL: Schedule cleanup after all slashes complete
                AbilityScheduler.scheduleOnce(entity, () -> {
                    // Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    GuardStateHelper.clearGuardState(entity);
                }, (totalTicks) + 5);
            }
        );
                
    }

    /**
     * Third Form: Scattering Mist Splash (ENHANCED VERSION)
     * A wide sweeping attack that scatters mist in all directions
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            "Third Form: Scattering Mist Splash",
            "Scatter enemies with a wide mist wave",
            4, // 4 second cooldown
            (entity, level) -> {
                float damage = DamageCalculator.calculateScaledDamage(entity, 7.0F);
            	GuardStateHelper.setGuardState(entity, damage, 20003);
                playEntityAnimation(entity, "sword_rotate");

                // Spinning 360-degree attack
                Vec3 centerPos = entity.position();

                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 3)); // Slow Falling

                final int totalTicks = 30; // 1.5 seconds
                final double[] currentAngle = {0};
                
                if (level instanceof ServerLevel serverLevel) {
                	BonePositionTracker.sendRawSlashToClients(
							level, // level
							Vec3.ZERO, // position offset
							modelKey, // model key
							0, // angle
							false, // reverse
							360, // arc range
							(int)(360/1.4f), // duration
							-90, // yaw offset
							0, // pitch offset
							0, // roll offset 
							1.6f, // radius scalar
							1.4f, // size scalar
							0, // angle offset
							entity.getUUID(), // entity id
							"sword_rotate"); // animation name
                	BonePositionTracker.sendRawSlashToClients(
							level, // level
							Vec3.ZERO, // position offset
							modelKey, // model key
							0, // angle
							false, // reverse
							360, // arc range
							(int)(360/1.4f), // duration
							-90, // yaw offset
							0, // pitch offset
							0, // roll offset 
							1.6f, // radius scalar
							1.4f, // size scalar
							180, // angle offset
							entity.getUUID(), // entity id
							"sword_rotate"); // animation name
                }

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    currentAngle[0] += 24; // Rotate 24 degrees per tick

                    // Spawn mist in a spiral pattern
                    if (level instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 5; i++) {
                            double angle = Math.toRadians(currentAngle[0] + (i * 72));
                            double radius = 2.0 + (currentAngle[0] / 360.0) * 3.0; // Expanding spiral
                            double x = centerPos.x + Math.cos(angle) * radius;
                            double z = centerPos.z + Math.sin(angle) * radius;

                            serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
                                x, centerPos.y + 1, z,
                                2, 0.2, 0.3, 0.2, 0.03);
                        }
                    }

                    // Damage and knockback in area
                    AABB hitBox = entity.getBoundingBox().inflate(5.0);
                    List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);

                    for (Entity target : targets) {
                    	if (target instanceof LivingEntity livingTarget) {
	                        Damager.hurt(entity, livingTarget, damage);
	
	                        // Brief confusion
	                        //livingTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    	}
                    	// Knockback away from center
                        Vec3 knockbackDir = target.position().subtract(centerPos).normalize();
                        target.setDeltaMovement(target.getDeltaMovement().add(knockbackDir.scale(1.0)));
                    }
                }, 1, totalTicks);

                // CRITICAL: Schedule cleanup after form completes
                AbilityScheduler.scheduleOnce(entity, () -> {
                    // Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    GuardStateHelper.clearGuardState(entity);
                }, totalTicks + 1);

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.2F, 0.8F);
            }
        );
    }

    /**
     * Fourth Form: Shifting Flow Slash
     * The user takes a low stance before dashing toward the target and slashing them.
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            "Fourth Form: Shifting Flow Slash",
            "Take a low stance and dash toward target with a powerful slash",
            4, // 4 second cooldown
            (entity, level) -> {
            	float damage = DamageCalculator.calculateScaledDamage(entity, 12.0F);
            	GuardStateHelper.setGuardState(entity, damage, 20004);
				shiftingFlowSlash(level, entity, 40, damage);
                
                // CRITICAL: Schedule cleanup after dash completes
                AbilityScheduler.scheduleOnce(entity, () -> {
                    // Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    GuardStateHelper.clearGuardState(entity);
                }, 20);
            }
        );
    }
    
    public static void shiftingFlowSlash(Level level, LivingEntity entity, float range, float damage) {
    	playEntityAnimation(entity, "kamusari3");
		// Find safe teleport position up to 40 blocks away
		Vec3 lookVec = entity.getLookAngle();
		if (lookVec.y > 0)
			lookVec = lookVec.multiply(1, 0.3f, 1);
		Vec3 startPos = entity.getEyePosition();
		Vec3 targetPos = startPos.add(lookVec.scale(range));

		// Raycast to find first non-passable block
		BlockHitResult hitResult = level.clip(new ClipContext(startPos.add(0, entity.getEyeHeight(), 0),
				targetPos.add(0, entity.getEyeHeight(), 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, entity));

		if (hitResult.getType() == HitResult.Type.BLOCK) {
			// Hit a block, teleport just before it
			Vec3 hitPos = hitResult.getLocation();
			targetPos = startPos.add(hitPos.subtract(startPos).normalize()
					.scale(Math.max(0, startPos.distanceTo(hitPos) - 1.0)));
		}

		List<LivingEntity> nearbyEntities = new ArrayList<LivingEntity>();
		float width = 2.5f;
        
        for (float i = 0; i < range; i+=width*1.8f) {
        	Vec3 pos = startPos.add(lookVec.scale(i));
        	AABB hitBox = new AABB(pos, pos).inflate(width);
        	nearbyEntities.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox,
            e -> e != entity && e.isAlive()));
        }

		if (!nearbyEntities.isEmpty()) {
			Vec3 entityPos = nearbyEntities.get(0).position();
			if (startPos.distanceTo(entityPos) < startPos.distanceTo(targetPos)) {
				targetPos = entityPos;
			}
		}
		
		if (level instanceof ServerLevel serverLevel) {
			ParticleHelper.spawnParticleLine(serverLevel, startPos, targetPos, ModParticles.MIST_PARTICLE.get(), 40);
		}
			
		// Teleport entity
		entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);

		// Spawn particles
		if (level instanceof ServerLevel serverLevel) {
			ParticleHelper.spawnCircleParticles(serverLevel, targetPos.add(0, 1, 0), 3.0,
					new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
							(float) (Math.random()*0.7f + 0.2f)), 30);
			ParticleHelper.spawnCircleParticles(serverLevel, targetPos.add(0, 1, 0), 3.0,
					ModParticles.SMALL_MIST_PARTICLE.get(), 40);
		}

		level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
				0.8F);
		level.playSound(null, entity.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 1.0F,
				1.5F);

		entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 3)); // Slow Falling
		
		// Damage nearby entities (AOE)
		AABB area = entity.getBoundingBox().inflate(3.0);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != entity && e.isAlive());

		for (LivingEntity target : targets) {
			Damager.hurt(entity, target, damage);
		}

		// After 2 ticks spawn the particles again after the entity has been teleported
		AbilityScheduler.scheduleOnce(entity, () -> {
			// Spawn particles
			if (level instanceof ServerLevel serverLevel) {
				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.0,
						new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
								(float) (Math.random()*0.7f + 0.2f)), 30);
				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.0,
						ModParticles.SMALL_MIST_PARTICLE.get(), 40);
				level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
						0.8F);
				level.playSound(null, entity.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 1.0F,
						1.5F);
				
				BonePositionTracker.sendRawSlashToClients(
						level, // level
						Vec3.ZERO,
						modelKey, // model key
						0, // angle
						false, // reverse
						240, // arc range
						150, // duration
						-90, // yaw offset
						0, // pitch offset
						0, // roll offset 
						1.3f, // radius scalar
						1.2f, // size scalar
						15, // angle offset
						entity.getUUID(), // entity id
						"sword_rotate"); // animation name
			}
		}, 2);
    }

    /**
     * Fifth Form: Sea of Clouds and Haze
     * The user charges at their target at blistering speeds in a zig-zag motion while performing 
     * a barrage of continuous slashes in front and/or around them.
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            "Fifth Form: Sea of Clouds and Haze",
            "Charge at target in zig-zag motion with barrage of slashes",
            6, // 6 second cooldown
            (entity, level) -> {
            	double damage = DamageCalculator.calculateScaledDamage(entity, 5F);
            	GuardStateHelper.setGuardState(entity, damage, 20005); // ID 20001 for Mist Breathing
            	GuardStateHelper.setAttackState(entity, damage);
				
				final float originalStepHeight = 0.6f;
				MovementHelper.setStepHeight(entity, 3);
                
                final int totalTicks = 50; // 2.5 seconds
                final int attackInterval = 1; 
                
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 3)); // Slow Falling
                
                final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
						"sword_to_left_reverse", "sword_to_right_reverse" };
                
                final int[] tickCounter = {0};

                //entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, totalTicks, 3)); // High speed
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
					Vec3 lookVec = entity.getLookAngle();
					if (lookVec.y > 0)
						lookVec = lookVec.multiply(1, 0, 1);
					int currentTick = tickCounter[0]++;
					
					// Launch player forward a little bit
					//entity.setDeltaMovement(lookVec.scale(1.7));
					MovementHelper.setVelocity(entity, lookVec.scale(1.7));
					
					if (currentTick % attackInterval == 1) {
						// Spawn mist clouds during the charge
	                    if (level instanceof ServerLevel serverLevel) {
	                        serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
	                            entity.getX(), entity.getY() + 1, entity.getZ(),
	                            6, 1.0, 1.0, 1.0, 0.05);
	                    }
					}

					// Attack every attackInterval ticks
					if (currentTick % attackInterval == 0) {
						int animIndex = (currentTick / attackInterval) % 4;

						if (Config.logDebug) {
							Log.debug("Fifth Form: Playing attack animation '{}' on layer 4000",
									animations[animIndex]);
						}

						// Use layer 4000 with 3x speed for ultra-fast attacks
						playEntityAnimationOnLayer(entity, animations[animIndex], 10, 3.0f, 4000);

						Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0);
						// Vec3 lookVec = entity.getLookAngle().normalize();

						AABB attackBox = entity.getBoundingBox().inflate(5);

						// AABB attackBox = entity.getBoundingBox().inflate(4.5);
						List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
								attackBox, e -> e != entity && e.isAlive());

						for (LivingEntity target : targets) {
							Damager.hurt(entity, target, (float)damage);
						}

						// Spawn particles
						if (level instanceof ServerLevel serverLevel) {

							double yawRad = Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 30);
							double pitchRad = Math.toRadians((Math.random()-0.3) * 5);

							Vec3 pos = entity.position().add(Math.random() - 0.5, (Math.random() + 0.5) * 2,
									Math.random() - 0.5);

							int arcLength = (int) (100 + Math.random() * 70);
							double angle = (Math.random() - 0.5) * 10;
							boolean particle = false;
							if (Math.random() > 0.5) {
								particle = true;
								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);

								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);
								
								BonePositionTracker.sendRawHorizontalSlashToClients(
										level, // level
										Vec3.ZERO,
										modelKey, // model key
										(float)angle/10, // vert
										Math.random() > 0.5, // reverse
										120, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										15, // angle offset
										entity.getUUID(), // entity id
										"sword_to_left"); // animation name
							}
							if (Math.random() > 0.5 || !particle) {
								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										4 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);

								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);

								BonePositionTracker.sendRawVerticalSlashToClients(
										level, // level
										Vec3.ZERO,
										modelKey, // model key
										(float)angle/10, // angle
										false, // reverse
										120, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										0, // angle offset
										entity.getUUID(), // entity id
										"sword_overhead"); // animation name
								
							}
						}

						// Sound every 3rd attack to avoid spam
						// if (currentTick % (attackInterval * 3) == 0) {
						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
								SoundSource.PLAYERS, 0.7F, 1.4F);
						level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
								1.9F);
						// }
					}

				}, 1, totalTicks);
                
                // CRITICAL: Schedule cleanup after form completes
                AbilityScheduler.scheduleOnce(entity, () -> {
                	MovementHelper.setStepHeight(entity, originalStepHeight);
                    // Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    GuardStateHelper.clearGuardState(entity);
                    GuardStateHelper.clearAttackFlag(entity);
                }, totalTicks + 5);
            }
        );
    }

    /**
     * Sixth Form: Lunar Dispersing Mist
     * The user leaps into the air and charges forward, performs a multitude of slashes before somersaulting as they deliver a singular vertical slash in a circular motion, decimating dozens of targets at once.
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Lunar Dispersing Mist",
            "Leap into air, charge forward with multiple slashes, then deliver circular vertical slash",
            7, // 7 second cooldown
            (entity, level) -> {
            	double damage = DamageCalculator.calculateScaledDamage(entity, 12F);
            	GuardStateHelper.setGuardState(entity, damage*2, 20006); // ID 20001 for Mist Breathing
                playEntityAnimation(entity, "backstep");
                
                final float originalStepHeight = 0.6f;
				MovementHelper.setStepHeight(entity, 3);
                
                final int leapDelay = 5; // 0.25 seconds to prepare jump
                final int forwardChargeTicks = 15; // 0.75 seconds of forward charge
                final int verticalSlashDelay = 20; // 1 second to execute vertical slash
                
                final int totalDuration = 60;
                final int rangedDuration = 30;
                final int interval = 1;
                final int[] currentTick = {0};
                final boolean[] bools = {false, false, false, false};
                final Vec3[] vecs = new Vec3[2];
                final float[] nums = {0, 0};
                
                
                final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
						"sword_to_left_reverse", "sword_to_right_reverse" };
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
                	
                	if (currentTick[0] < 7) { // Jump upwards and backwards
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.2).add(0, 0.5f, 0));
                	}
                	else if (currentTick[0] < 17) { // Hover
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                		if (currentTick[0] >= 11-interval && currentTick[0] <= 11+interval && !bools[3]) {
                			playEntityAnimationOnLayer(entity, "kimetsunoyaibamultiplayer:front_flip", 15, 1.5f, 4000);
            				bools[3] = true;
            			}
                	}
                	else if (currentTick[0] < 50) { // Charge forward with multiple slashes
                		if (currentTick[0] < 22) { // Forward movement 
                			
                			MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(1.2).multiply(1, 0.5, 1));

                		} else { // Hover
                			MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                			if (!bools[0]) {
                				bools[0] = true;
                				// give slowfall
                				 entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, totalDuration, 3)); // Slow Falling
                			}
                		}
                		// SLASHES
                		if (currentTick[0] < 37) { 
	                		int animIndex = (currentTick[0]/interval) % 4;
	
							if (Config.logDebug) {
								Log.debug("Fifth Form: Playing attack animation '{}' on layer 4000",
										animations[animIndex]);
							}
	
							// Use layer 4000 with 3x speed for ultra-fast attacks
							playEntityAnimationOnLayer(entity, animations[animIndex], 10, 3.0f, 4000);
	
							Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0).add(entity.getLookAngle().normalize().scale(3));
							// Vec3 lookVec = entity.getLookAngle().normalize();
	
							AABB attackBox = entity.getBoundingBox().inflate(5);
	
							// AABB attackBox = entity.getBoundingBox().inflate(4.5);
							List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
									attackBox, e -> e != entity && e.isAlive());
	
							for (LivingEntity target : targets) {
								Damager.hurt(entity, target, (float)damage);
							}
	
							// Spawn particles
							if (level instanceof ServerLevel serverLevel) {
	
								double yawRad = Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 30);
								double pitchRad = Math.toRadians((Math.random()-0.6) * 20);
	
								Vec3 pos = entity.position().add(Math.random() - 0.5, (Math.random() + 0.5) * 2,
										Math.random() - 0.5);
	
								int arcLength = (int) (100 + Math.random() * 70);
								double angle = (Math.random() - 0.5) * 10;
								boolean particle = false;
								if (Math.random() > 0.5) {
									particle = true;
									ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
											3 + Math.random() * 2, 0.1, arcLength, 1, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
											40);
	
									ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
											3 + Math.random() * 2, 0.1, arcLength, 1, angle,
											new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
													(float) (Math.random()*0.7f + 0.2f)),
											20);
									/*
									ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
											3 + Math.random(), 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK,
											3);
											*/
								}
								if (Math.random() > 0.5 || !particle) {
									ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
											4 + Math.random() * 2, 0.1, arcLength, 1, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
											80);
	
									ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
											3 + Math.random() * 2, 0.1, arcLength, 1, angle,
											new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
													(float) (Math.random()*0.7f + 0.2f)),
											30);
									/*
									ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
											3 + Math.random(), 0.1, arcLength, 1, angle, ParticleTypes.SWEEP_ATTACK,
											5);*/
								}
							}
	
							// Sound every 3rd attack to avoid spam
							// if (currentTick % (attackInterval * 3) == 0) {
							level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
									SoundSource.PLAYERS, 0.7F, 1.4F);
							level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
									1.9F);
							// }
                		} else {
                			if (currentTick[0] <= 37+interval)
                				playEntityAnimationOnLayer(entity, "ragnaraku2", 10, 0.7f, 4000);
                		}
                		// SLASHES END
                	}
                	else { // Hover
                		if (currentTick[0] < totalDuration)
                			MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                		if (!bools[1]) {
                			bools[1] = true;
                			// Huge vertical slash ranged attack
                			vecs[0] = entity.getEyePosition();
                			vecs[1] = entity.getLookAngle().normalize();
                			nums[0] =  (float)Math.toRadians(entity.getYRot());
                			nums[1] =  (float)Math.toRadians(entity.getXRot());
                			
                			playEntityAnimationOnLayer(entity, animations[2], 10, 1.0f, 4000);
                			
                			level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                    SoundSource.PLAYERS, 1.0F, 0.8F);
                                level.playSound(null, entity.blockPosition(), SoundEvents.CANDLE_EXTINGUISH,
                                    SoundSource.PLAYERS, 1.0F, 1.0F);
                                level.playSound(null, entity.blockPosition(), SoundEvents.SHULKER_SHOOT,
                                    SoundSource.PLAYERS, 0.8F, 0.8F);
                		}
                			
                			// Ranged attack START
                			
                			List<LivingEntity> targets = new ArrayList<LivingEntity>();
        	                
        	                Vec3 pos = vecs[0].add(vecs[1].scale((currentTick[0]-40)*3/4));
                        	AABB hitBox = new AABB(pos.add(0, -1, 0), pos.add(0, 1, 0)).inflate(5);
                        	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox,
                            e -> e != entity && e.isAlive()));

                        	if (level.getBlockState(BlockPos.containing(pos)).getCollisionShape(level, BlockPos.containing(pos)).isEmpty() || level.getBlockState(BlockPos.containing(pos)).canBeReplaced() || level.getBlockState(BlockPos.containing(pos)).isAir()) {
                        		bools[2] = true;
                        	}
                        	
                        	if (level instanceof ServerLevel serverLevel) {
        	                	ParticleHelper.spawnVerticalArc(serverLevel, pos, nums[0], nums[1],
        								8 + Math.random() * 3, 0.1, 160, 1, -1, new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
    											(float) (Math.random()*1.5f + 0.2f)),
        								80);
        	                	ParticleHelper.spawnVerticalArc(serverLevel, pos, nums[0], nums[1],
        								8 + Math.random() * 3, 0.1, 160, 1, -1, ModParticles.SMALL_MIST_PARTICLE.get(),
        								80);
        	                	ParticleHelper.spawnVerticalArc(serverLevel, pos, nums[0], nums[1],
        								8 + Math.random() * 3, 0.1, 160, 1, -1, ParticleTypes.SWEEP_ATTACK,
        								5);
        	                }
        	                
        	                for (LivingEntity target : targets) {
        	                    Damager.hurt(entity, target, (float)damage*1.5f);
        	                }
        	                
        	                // Ranged attack END
                		
                	}
                	
                	currentTick[0] += interval;
                }, interval, totalDuration + rangedDuration);
                
                // CRITICAL: Schedule cleanup after form completes
                AbilityScheduler.scheduleOnce(entity, () -> {
                	MovementHelper.setStepHeight(entity, originalStepHeight);
                    // Reset breathes and cnt tags to allow next ability use
                    // breathes is cleared immediately by dispatcher - this is defensive backup
                    // Cooldown is handled by EnhancedMistDispatcher - do not reset skill here
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    GuardStateHelper.clearGuardState(entity);
                }, totalDuration + 5);
            }
        );
    }
    
    /**
     * Seventh Form: Obscuring Clouds (ENHANCED VERSION)
     * Creates a massive mist field with ghostly clones that fade in and out.
     * Allows teleportation on attack through enemies.
     *
     * Enhanced features:
     * - Dense cloud of cyan mist particles
     * - 3-5 ghostly clones that wander and fade in/out
     * - Teleport on left-click/attack up to 15 blocks forward (safe, doesn't go through blocks)
     * - Works for both players and Muichiro entity
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            "Seventh Form: Obscuring Clouds",
            "Blanket the battlefield in thick mist, spawn ghostly clones, and teleport through enemies",
            15, // 10 second cooldown
            (entity, level) -> {
                if (Config.logDebug) {
                    Log.debug("Enhanced Mist 7th Form: Starting for {}", entity.getName().getString());
                }
                
                GuardStateHelper.setGuardState(entity, 10, 20007);

                // Play initial animation
                playEntityAnimation(entity, "sword_overhead");

                final int formDuration = 200;

                // ===== PHASE 1: DENSE MIST CLOUD GENERATION =====
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 centerPos = entity.position();

                    // Cyan mist particle (RGB: 138, 195, 194)
                    DustParticleOptions mistParticle = new DustParticleOptions(
                        new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
                        (float) (Math.random() * 0.7f + 0.2f)
                    );

                    // Create initial dense mist burst
                    for (int layer = 0; layer < 4; layer++) {
                        final double layerHeight = centerPos.y + (layer * 1.5);
                        final int finalLayer = layer;

                        AbilityScheduler.scheduleOnce(entity, () -> {
                            // Dense ring pattern - every 15 degrees
                            for (int angle = 0; angle < 360; angle += 15) {
                                double radians = Math.toRadians(angle);

                                // Multiple radii for density: 3, 6, 9, 12, 15 blocks
                                for (int r = 3; r <= 15; r += 3) {
                                    double x = centerPos.x + Math.cos(radians) * r;
                                    double z = centerPos.z + Math.sin(radians) * r;

                                    // Spawn dense particles
                                    serverLevel.sendParticles(mistParticle,
                                        x, layerHeight, z,
                                        5, 0.5, 0.3, 0.5, 0.02);

                                    // Add cloud particles for extra density
                                    if (level.random.nextFloat() < 0.5) {
                                        serverLevel.sendParticles(ParticleTypes.CLOUD,
                                            x, layerHeight + 0.5, z,
                                            2, 0.4, 0.3, 0.4, 0.01);
                                        
                                        serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
                                                x, layerHeight + 0.5, z,
                                                4, 0.4, 0.3, 0.4, 0.01);
                                    }
                                }
                            }
                        }, finalLayer * 2); // Stagger layers
                    }

                    // Continue spawning ambient mist throughout duration
                    for (int tick = 0; tick < formDuration; tick += 5) {
                        final int currentTick = tick;
                        AbilityScheduler.scheduleOnce(entity, () -> {
                            // Random dense mist clouds
                            for (int i = 0; i < 8; i++) {
                                double offsetX = (level.random.nextDouble() - 0.5) * 30;
                                double offsetY = level.random.nextDouble() * 6;
                                double offsetZ = (level.random.nextDouble() - 0.5) * 30;

                                serverLevel.sendParticles(mistParticle,
                                    centerPos.x + offsetX, centerPos.y + offsetY, centerPos.z + offsetZ,
                                    4, 0.6, 0.4, 0.6, 0.03);

                                // Cloud particles for texture
                                if (level.random.nextFloat() < 0.4) {
                                    serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
                                        centerPos.x + offsetX, centerPos.y + offsetY, centerPos.z + offsetZ,
                                        4, 0.5, 0.3, 0.5, 0.02);
                                }
                            }
                        }, currentTick);
                    }
                }

                // Sound effects for mist generation
                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 2.0F, 0.6F);
                level.playSound(null, entity.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.PLAYERS, 1.5F, 1.2F);

                // ===== PHASE 2: SPAWN GHOSTLY CLONES =====
                Vec3 centerPos = entity.position();
                System.out.println("[DEBUG 7TH FORM] Starting clone spawn phase...");
                System.out.println("[DEBUG 7TH FORM] Level type: " + level.getClass().getSimpleName());
                System.out.println("[DEBUG 7TH FORM] Is ServerLevel: " + (level instanceof ServerLevel));

                if (level instanceof ServerLevel serverLevel) {
                    int numClones = 6 + level.random.nextInt(5); // INCREASED: 6-10 clones
                    System.out.println("[DEBUG 7TH FORM] Spawning " + numClones + " clones");
                    System.out.println("[DEBUG 7TH FORM] Center position: " + centerPos);
                    System.out.println("[DEBUG 7TH FORM] Player: " + entity.getName().getString());

                    for (int i = 0; i < numClones; i++) {
                        // Spawn clones at random positions around the user
                        double angle = Math.toRadians(level.random.nextInt(360));
                        double distance = 5 + level.random.nextDouble() * 10; // 5-15 blocks away

                        double spawnX = entity.getX() + Math.cos(angle) * distance;
                        double spawnZ = entity.getZ() + Math.sin(angle) * distance;
                        double spawnY = entity.getY();

                        System.out.println("[DEBUG 7TH FORM] Clone #" + i + " at: " + spawnX + ", " + spawnY + ", " + spawnZ);

                        try {
                            // Create clone with center position for circular motion
                            com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity clone =
                                new com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity(
                                    serverLevel, entity, formDuration, centerPos);

                            clone.setPos(spawnX, spawnY, spawnZ);
                            boolean success = serverLevel.addFreshEntity(clone);
                            System.out.println("[DEBUG 7TH FORM] Clone #" + i + " added: " + success + " (ID: " + clone.getId() + ")");
                        } catch (Exception e) {
                            System.err.println("[ERROR 7TH FORM] Failed to spawn clone #" + i);
                            e.printStackTrace();
                        }
                    }
                    System.out.println("[DEBUG 7TH FORM] Finished spawning " + numClones + " clones");
                } else {
                    System.out.println("[DEBUG 7TH FORM] NOT spawning clones - level is client-side");
                }

                // ===== PHASE 3: TELEPORT ON ATTACK MECHANIC =====
                // Set a flag to enable teleport on next attack
                entity.getPersistentData().putBoolean("mist_7th_teleport_active", true);
                // Store the end time using tickCount (works on both client and server)
                entity.getPersistentData().putInt("mist_7th_teleport_duration", formDuration);
                // Add initial cooldown to prevent immediate teleport on activation (1 second = 20 ticks)
                entity.getPersistentData().putInt("mist_7th_teleport_cooldown", entity.tickCount + 10);

                // Speed boost while in mist
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, formDuration, 2));
                entity.addEffect(new MobEffectInstance(MobEffects.JUMP, formDuration, 1));

                // Apply fading invisibility effect to make player blend with clones
                // This creates a pulsing invisibility effect (fade in/out) similar to clones
                final int[] fadeTickCounter = {0};
                final boolean[] currentlyInvisible = {false};
                final int[] invisibleDuration = {60 + level.random.nextInt(40)}; // Start invisible 3-5 seconds

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    fadeTickCounter[0]++;

                    if (currentlyInvisible[0]) {
                        // Stay invisible
                        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
                        playEntityAnimationOnLayer(entity, "invisibility", 5, 1.0f, 4000);

                        // Count down invisible duration
                        invisibleDuration[0]--;
                        if (invisibleDuration[0] <= 0) {
                            // Switch to visible period
                            currentlyInvisible[0] = false;
                            invisibleDuration[0] = 40 + level.random.nextInt(40); // Visible for 2-4 seconds
                        }
                    } else {
                        // Stay visible (no invisibility effect)
                        // Count down visible duration
                        invisibleDuration[0]--;
                        if (invisibleDuration[0] <= 0) {
                            // Switch to invisible period
                            currentlyInvisible[0] = true;
                            invisibleDuration[0] = 60 + level.random.nextInt(40); // Invisible for 3-5 seconds
                        }
                    }

                }, 1, formDuration);

                // Apply blindness to nearby enemies
                AABB effectArea = new AABB(entity.position(), entity.position()).inflate(15.0);
                List<LivingEntity> nearbyEnemies = entity.level().getEntitiesOfClass(LivingEntity.class, effectArea,
                    e -> e != entity && e.isAlive() && !(e instanceof Player) && !(e instanceof BreathingSlayerEntity));

                for (LivingEntity enemy : nearbyEnemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, formDuration, 0));
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, formDuration / 2, 1));
                }

                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("mist_7th_teleport_active");
                    entity.getPersistentData().remove("mist_7th_teleport_duration");

                    // Reset ability counters
                    entity.getPersistentData().putDouble("cnt1", 0.0);
                    entity.getPersistentData().putDouble("cnt2", 0.0);
                    entity.getPersistentData().putDouble("cnt3", 0.0);
                    GuardStateHelper.clearGuardState(entity);

                    if (Config.logDebug) {
                        Log.debug("Enhanced Mist 7th Form: Cleanup complete for {}", entity.getName().getString());
                    }
                }, formDuration + 1);

                if (Config.logDebug) {
                    Log.debug("Enhanced Mist 7th Form: All phases scheduled for {}", entity.getName().getString());
                }
            }
        );
    }

    /**
     * Create the complete Enhanced Mist Breathing technique (Muichiro's Sword)
     * All 7 forms including the special 7th Form: Obscuring Clouds
     */
    public static BreathingTechnique createMuichiroMistBreathing() {
        List<BreathingForm> forms = new ArrayList<>();

        // Add all 7 Mist Breathing forms
        forms.add(firstForm());      // 1st Form: Low Clouds, Distant Haze
        forms.add(secondForm());    // 2nd Form: Eight-Layered Mist
        forms.add(thirdForm());      // 3rd Form: Scattering Mist Splash
        forms.add(fourthForm());    // 4th Form: Shifting Flow Slash
        forms.add(fifthForm());    // 5th Form: Sea of Clouds and Haze
        forms.add(sixthForm());    // 6th Form: Lunar Dispersing Mist
        forms.add(seventhForm());    // 7th Form: Obscuring Clouds (Muichiro's signature move)

        return new BreathingTechnique("Mist Breathing", forms, "§7", "§b");
    }

    /**
     * Create the generic Enhanced Mist Breathing technique (Generic Mist Sword)
     * Only 6 forms - excludes the 7th Form which is Muichiro's signature technique
     */
    public static BreathingTechnique createGenericMistBreathing() {
        List<BreathingForm> forms = new ArrayList<>();

        // Add first 6 Mist Breathing forms
        forms.add(firstForm());      // 1st Form: Low Clouds, Distant Haze
        forms.add(secondForm());    // 2nd Form: Eight-Layered Mist
        forms.add(thirdForm());      // 3rd Form: Scattering Mist Splash
        forms.add(fourthForm());    // 4th Form: Shifting Flow Slash
        forms.add(fifthForm());    // 5th Form: Sea of Clouds and Haze
        forms.add(sixthForm());    // 6th Form: Lunar Dispersing Mist
        // 7th Form excluded - Muichiro's signature technique

        return new BreathingTechnique("Mist Breathing", forms, "§7", "§b");
    }
}
