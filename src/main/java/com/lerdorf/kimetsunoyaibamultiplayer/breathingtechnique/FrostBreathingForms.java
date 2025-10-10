package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.FancyMath;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
// import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.SwordParticleHandler; // REMOVED: Client-only class, causes server crash
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlyingSwordEntity;

/**
 * Implementation of all Frost Breathing forms (6 forms + 7th for Komorebi)
 */
public class FrostBreathingForms {

    /**
     * Unified animation helper that works with both players and GeckoLib entities
     */
    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, 20);
        }
    }
    
    /**
	 * Unified animation helper with layer and speed control
	 */
	private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName, int maxTicks, float speed, int layer) {
		if (entity instanceof Player player) {
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
     * First Form: Lavish Tundra
     * Fast horizontal dash with left-right swings, multiple attacks
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
            "First Form: Lavish Tundra",
            "Dash forward with flowing horizontal strikes",
            5, // 5 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_to_left");

                // Apply speed and dash forward
                Vec3 lookVec = entity.getLookAngle();
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));

                final int totalTicks = 60; // 3 seconds
                final int attackInterval = 10; // Attack every 0.5 seconds

                MovementHelper.setStepHeight(entity, 1.8F);
                final float originalStepHeight = 0.6F;

                for (int tick = 0; tick < totalTicks; tick++) {
                    final int currentTick = tick;

                    AbilityScheduler.scheduleOnce(entity, () -> {
                    	boolean left = (currentTick/attackInterval) % 2 == 0;
                        // Force entity to sprint forward
                    	Vec3 horizontalVelocity = FancyMath.rotateYaw(entity.getLookAngle(), left ? 30 : -30).scale(1F - ((float)(currentTick % attackInterval))/((float)attackInterval));
						MovementHelper.setVelocity(entity, horizontalVelocity.x, entity.getDeltaMovement().y,
								horizontalVelocity.z);

						MovementHelper.stepUp(entity, entity.getX() + horizontalVelocity.x, entity.getY(),
								entity.getZ() + horizontalVelocity.z);

                        // Alternate between left and right swing animations and attacks
                        if (currentTick % attackInterval == 0) {
                            // Always play attack animation
                            playEntityAnimation(entity, left ? "sword_to_left" : "sword_to_right");

                            // AOE damage
                            Vec3 attackPos = entity.position().add(lookVec.scale(2.0));
                            AABB hitBox = new AABB(attackPos, attackPos).inflate(4.0);
                            List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                                e -> e != entity && e.isAlive());

                            for (LivingEntity target : targets) {
                                float damage = DamageCalculator.calculateScaledDamage(entity, 7.0F);
                                Damager.hurt(entity, target, damage);
                            }

                            if (level instanceof ServerLevel serverLevel) {
                                spawnParticleLine(serverLevel, entity.position().add(0, 1, 0),
                                    entity.position().add(0, 1, 0).add(lookVec.scale(3.0)),
                                    ParticleTypes.SNOWFLAKE, 15);
                                //spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 2.0, ParticleTypes.CLOUD, 8);

                                double yawRad = Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 20);
								double pitchRad = Math.toRadians(10 + Math.random() * 10);

								Vec3 pos = entity.position().add(Math.random() - 0.5, (Math.random() + 0.5) * 2,
										Math.random() - 0.5);

								int arcLength = (int) (90 + Math.random() * 60);
								double angle = (Math.random() - 0.5) * 10;
                                ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
    									3 + Math.random() * 1.5, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE,
    									80);
                            }

                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }, tick);
                }

                // Schedule step height reset AFTER all scheduled tasks complete
                AbilityScheduler.scheduleOnce(entity, () -> {
                    MovementHelper.setStepHeight(entity, originalStepHeight);
                    if (Config.logDebug) {
                        Log.debug("First Form: Resetting step height to {}", originalStepHeight);
                    }
                }, totalTicks + 1);

                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Second Form: Snowing Point
     * Quick jab that immobilizes opponent
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            "Second Form: Snowing Point",
            "Impactful jab that immobilizes",
            2, // 2 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "speed_attack_sword");

                // Launch entity forward slightly
                Vec3 lookVec = entity.getLookAngle();
                MovementHelper.setVelocity(entity, lookVec.scale(0.5));

                // Apply effects to targets in front
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(3.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
                    Damager.hurt(entity, target, damage);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4)); // 8 seconds, extreme slowness
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 4)); // 8 seconds, mining fatigue
                }

                // Spawn particles - forward thrust straight line
                if (level instanceof ServerLevel serverLevel) {
                    spawnForwardThrust(serverLevel, startPos, lookVec, 3.0, ParticleTypes.SNOWFLAKE, 20);
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Third Form: Icicle through Snowfall
     * Leap up, hover for a bit with lots of snowfall, then shoot forward with a powerful stab
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            "Third Form: Icicle through Snowfall",
            "Gentle snowfall followed by a quick and painless stab",
            6, // 6 second cooldown
            (entity, level) -> {
				// Enable attack animations during this ability
				/*
				 * entity.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(
				 * data -> { data.setCancelAttackSwing(false); });
				 */

				// Initial leap
				MovementHelper.addVelocity(entity, 0, 1.2, 0);

				playEntityAnimation(entity, "kimetsunoyaibamultiplayer:sword_spin"); // spin the sword

				entity.setNoGravity(true);

				final int totalTicks = 30; // 1.5 seconds
				final int[] tickCounter = { 0 };
				final double targetY = entity.getY() + 4.0; // Target hover height (4 blocks up)
				final double[] columnPos = { 0, 0 };


				level.playSound(null, entity.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 2.0F, 0.6F);
				
				// Single repeating task instead of 80 individual tasks
				AbilityScheduler.scheduleRepeating(entity, () -> {
					int currentTick = tickCounter[0]++;

					// Hover at target height - stop vertical movement
					double currentY = entity.getY();
					if (currentY < targetY && currentTick < 15) {
						// Still ascending to target height (first 15 ticks)
						MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, 0.3,
								entity.getDeltaMovement().z);
					} else {
						// At target height - completely stop vertical movement
						MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, 0,
								entity.getDeltaMovement().z);
					}
					
					// Spawn particles - cloud with snowfall
	                if (level instanceof ServerLevel serverLevel) {
	                	serverLevel.sendParticles(ParticleTypes.CLOUD, entity.getX() + 10*(Math.random()-0.5),
								entity.getY() + entity.getEyeHeight() + 2*(Math.random()-0.5), entity.getZ() + 10*(Math.random()-0.5), 50, 0.5, 0.0,
								0.5, 0.01);
	                	
	                	serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f),
								(float) (Math.random() + 1.5f)), entity.getX() + 10*(Math.random()-0.5),
								entity.getY() + entity.getEyeHeight() + 2*(Math.random()-0.5), entity.getZ() + 10*(Math.random()-0.5), 50, 0.5, 0.0,
								0.5, 0.01);
	                	
	                	serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX() + 10*(Math.random()-0.5),
								entity.getY() + entity.getEyeHeight() - 3*(Math.random()), entity.getZ() + 10*(Math.random()-0.5), 100, 0.6, 0.2,
								0.6, 0.5);
	                }

					if (currentTick >= totalTicks - 2) {
						entity.setNoGravity(false);
					}
				}, 1, totalTicks);
				
				AbilityScheduler.scheduleOnce(entity, () -> {
					 playEntityAnimation(entity, "speed_attack_sword");

		                // Launch entity forward slightly
		                Vec3 lookVec = entity.getLookAngle();
		                MovementHelper.setVelocity(entity, lookVec.scale(1.8));

		                // Apply effects to targets in front
		                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
		                Vec3 endPos = startPos.add(lookVec.scale(9.0));

		                AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
		                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
		                    e -> e != entity && e.isAlive());

		                for (LivingEntity target : targets) {
		                    float damage = DamageCalculator.calculateScaledDamage(entity, 12.0F);
		                    Damager.hurt(entity, target, damage);
		                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 4)); // 5 seconds, blindness
		                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 4)); // 5 seconds, mining fatigue
		                }

		                // Spawn particles - forward thrust straight line
		                if (level instanceof ServerLevel serverLevel) {
		                    spawnForwardThrust(serverLevel, startPos, lookVec, 18.0, ParticleTypes.SNOWFLAKE, 300);
		                    spawnForwardThrust(serverLevel, startPos, lookVec, 18.0, new DustParticleOptions(new Vector3f(0.5f, 0.8f, 1.0f),
									(float) (Math.random() + 0.2f)), 150);
		                }

		                level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK,
		                    SoundSource.PLAYERS, 1.0F, 1.0F);
		                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
			                    SoundSource.PLAYERS, 2.0F, 2.0F);
		                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
				}, totalTicks+1);

				// Play rain sound at start
				level.playSound(null, entity.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 0.5F,
						1.0F);
			});
    }

    /**
     * Fourth Form: Freezing Cold
     * Vertical slash sending cold air blast 30 blocks forward
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            "Fourth Form: Frostbite Gale",
            "Send a blast of freezing air",
            4, // 4 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_overhead");
                float range = 30;
                float width = 2;
                // Send blast forward
                Vec3 lookVec = entity.getLookAngle();
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(range));
                
                setCancelAttackSwing(entity, true);
                

                // Spawn particles
                if (level instanceof ServerLevel serverLevel) {
                    //spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 60);
                    //spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.CLOUD, 40);

                    // Extra particles for the blast wave
                    for (int i = 0; i < 30; i++) {
                        double progress = i / 30.0;
                        Vec3 particlePos = startPos.add(lookVec.scale(20.0 * progress));
                        double spread = progress * 2.0;
                        serverLevel.sendParticles(ParticleTypes.CLOUD,
                            particlePos.x + (level.random.nextDouble() - 0.5) * spread,
                            particlePos.y + (level.random.nextDouble() - 0.5) * spread,
                            particlePos.z + (level.random.nextDouble() - 0.5) * spread,
                            1, 0, 0, 0, 0);
                    }
                }
                
                final int totalTicks = 40;
                final int[] tickCounter = {0};

                AbilityScheduler.scheduleRepeating(entity, () -> {
					int currentTick = tickCounter[0]++;				
					// Spawn particles - cloud with snowfall
	                if (level instanceof ServerLevel serverLevel) {
	                	ParticleHelper.spawnVerticalArc(serverLevel, startPos, Math.toRadians(entity.getYRot()), Math.toRadians(entity.getXRot()),
								6 + Math.random() * 3, 0.1, 160, 1, 1, ParticleTypes.SNOWFLAKE,
								100);
	                }
	                
	                List<LivingEntity> targets = new ArrayList<LivingEntity>();
	                
	                Vec3 pos = startPos.add(lookVec.scale(currentTick*3/4));
                	AABB hitBox = new AABB(pos.add(0, -1, 0), pos.add(0, 1, 0)).inflate(width);
                	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive()));
	                
	                for (LivingEntity target : targets) {
	                    float damage = DamageCalculator.calculateScaledDamage(entity, 7.0F);
	                    Damager.hurt(entity, target, damage);
	                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2)); // 20 seconds slowness
	                    target.setTicksFrozen(target.getTicksFrozen() + 400); // Freeze visual effect
	                }

				}, 1, totalTicks);
                
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, entity.blockPosition(), SoundEvents.SHULKER_SHOOT,
                    SoundSource.PLAYERS, 0.8F, 0.8F);
                
                AbilityScheduler.scheduleOnce(entity, () -> {
					// We can swing swords normally again
					setCancelAttackSwing(entity, false);
				}, 5); // Run this 5 ticks later
            }
        );
    }

    /**
     * Fifth Form: Numbing Arctic Dance
     * Speed + invisibility for up to 6 seconds, ends on attack, then 3 jabs
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            "Fifth Form: Numbing Arctic Dance",
            "Flicker in and out, then strike",
            7, // 7 second cooldown
            (entity, level) -> {

                int duration = 120;
                
                playEntityAnimationOnLayer(entity, "invisibility", duration, 1.0f, 2000);
                
                // Apply speed and invisibility
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 3)); // 6 seconds
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0)); // 6 seconds

                // Schedule automatic attack at end if player doesn't attack
                final boolean[] hasAttacked = {false};
                
                entity.addTag("CheckForAttack");
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
					
                	if (!hasAttacked[0] && entity.getTags().contains("DidAttack")) {
                		playEntityAnimationOnLayer(entity, "invisibility", 0, 1.0f, 2000);
                		entity.removeTag("DidAttack");
                		hasAttacked[0] = true;
                		executeThreeJabs(entity, level);
                	}
                	
				}, 5, duration);
                
                AbilityScheduler.scheduleOnce(entity, () -> {
                    if (!hasAttacked[0]) {
                    	entity.removeTag("CheckForAttack");
                        executeThreeJabs(entity, level);
                    }
                }, duration+1);

                if (level instanceof ServerLevel serverLevel) {
                    spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.0, ParticleTypes.SNOWFLAKE, 30);
                    spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.0, ParticleTypes.CLOUD, 15);
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Helper method for Fifth Form: Execute 3 jabs
     */
    private static void executeThreeJabs(LivingEntity entity, Level level) {
    	
    	{
            
    		playEntityAnimation(entity, "sword_to_left");

            Vec3 lookVec = entity.getLookAngle();
            Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
            Vec3 endPos = startPos.add(lookVec.scale(3.0));

            MovementHelper.setVelocity(entity, lookVec.scale(0.5));

            AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
            List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != entity && e.isAlive());

            for (LivingEntity target : targets) {
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                Damager.hurt(entity, target, damage);
            }

            if (level instanceof ServerLevel serverLevel) {
                //spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 20);
            	double yawRad = Math.toRadians(entity.getYRot());
				double pitchRad = 0;

				Vec3 pos = entity.getEyePosition();

				int arcLength = 120;
				double angle = 10;
                ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
						3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE,
						80);
            }

            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    	}
    	
    	AbilityScheduler.scheduleOnce(entity, () -> {
    		playEntityAnimation(entity, "sword_to_right");

            Vec3 lookVec = entity.getLookAngle();
            Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
            Vec3 endPos = startPos.add(lookVec.scale(3.0));

            MovementHelper.setVelocity(entity, lookVec.scale(0.5));

            AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
            List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != entity && e.isAlive());

            for (LivingEntity target : targets) {
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                Damager.hurt(entity, target, damage);
            }

            if (level instanceof ServerLevel serverLevel) {
                //spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 20);
            	double yawRad = Math.toRadians(entity.getYRot());
				double pitchRad = 0;

				Vec3 pos = entity.getEyePosition();

				int arcLength = 120;
				double angle = 16;
                ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
						3, 0.1, arcLength, 1, angle, ParticleTypes.SNOWFLAKE,
						80);
            }

            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    	}, 8);
    	
    	AbilityScheduler.scheduleOnce(entity, () -> {
    		playEntityAnimation(entity, "speed_attack_sword");

            Vec3 lookVec = entity.getLookAngle();
            Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
            Vec3 endPos = startPos.add(lookVec.scale(3.0));

            MovementHelper.setVelocity(entity, lookVec.scale(0.5));
            
            AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
            List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != entity && e.isAlive());

            for (LivingEntity target : targets) {
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                Damager.hurt(entity, target, damage);
            }

            if (level instanceof ServerLevel serverLevel) {
                spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 20);
            }

            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        }, 16);
    	
        
    }

    /**
     * Sixth Form: Polar Mark
     * Throw sword as projectile using FlyingSwordEntity
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Polar Mark",
            "Throw your sword forward",
            9, // 9 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_overhead");

                // Only players can throw swords (need inventory)
                if (!(entity instanceof Player player)) {
                    if (Config.logDebug) {
                        Log.debug("Sixth Form: Entity is not a player, cannot throw sword");
                    }
                    return;
                }

                // Get the held sword item
                ItemStack heldSword = player.getMainHandItem();
                if (heldSword.isEmpty()) {
                    if (Config.logDebug) {
                        Log.debug("Sixth Form: No sword in hand");
                    }
                    return;
                }

                // Create flying sword ItemDisplay (it will return the sword automatically)
                FlyingSwordEntity.create(level, player, heldSword);

                // Remove sword from player's hand temporarily (FlyingSwordEntity will return it)
                player.getMainHandItem().shrink(1);

                level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

                if (Config.logDebug) {
                    Log.debug("Sixth Form: Spawned flying sword entity");
                }
            }
        );
    }

    /**
     * Seventh Form: Golden Senses (Komorebi's sword only)
     * Temporarily switch sword to golden model and enhance stats with golden slashing particles
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            "Seventh Form: Golden Senses",
            "Sword glows golden, empowering you",
            40, // 40 second cooldown
            (entity, level) -> {
                // Play kaishin3 animation if available, otherwise sword_overhead
                playEntityAnimation(entity, "kaishin3");

                // Only works for players (needs capability system)
                if (!(entity instanceof Player player)) {
                    if (Config.logDebug) {
                        Log.debug("Seventh Form: Entity is not a player, skipping model override");
                    }
                }

                final int duration = 400; // 20 seconds
                final ResourceLocation goldenModel = ResourceLocation.fromNamespaceAndPath(
                    KimetsunoyaibaMultiplayer.MODID, "item/nichirinsword_golden");

                // Change sword model to golden
                if (entity instanceof Player player) {
                    player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
                        .ifPresent(data -> {
                            data.setSwordModelOverride(goldenModel);

                            // Sync to all clients if on server
                            if (player instanceof ServerPlayer serverPlayer) {
                                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket(
                                        player.getUUID(), goldenModel));
                            }

                            if (Config.logDebug) {
                                Log.debug("Seventh Form: Set golden sword model for player {}",
                                    player.getName().getString());
                            }
                        });
                }

                // Get current effect levels and add 1
                int hasteLevel = entity.hasEffect(MobEffects.DIG_SPEED) ?
                    entity.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 1 : 0;
                int speedLevel = entity.hasEffect(MobEffects.MOVEMENT_SPEED) ?
                    entity.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
                int strengthLevel = entity.hasEffect(MobEffects.DAMAGE_BOOST) ?
                    entity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1 : 0;

                // Apply enhanced effects
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, hasteLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strengthLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));

                // Spawn initial golden particle burst
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 50; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 3;
                        double offsetY = level.random.nextDouble() * 2;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 3;

                        // Golden yellow dust particles
                        serverLevel.sendParticles(
                            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.0f), 1.5f),
                            entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                            1, 0, 0.1, 0, 0.05);
                    }

                    // Continue spawning golden ambient particles
                    for (int tick = 0; tick < duration; tick += 10) {
                        AbilityScheduler.scheduleOnce(entity, () -> {
                            for (int i = 0; i < 3; i++) {
                                double offsetX = (level.random.nextDouble() - 0.5) * 2;
                                double offsetY = level.random.nextDouble() * 2;
                                double offsetZ = (level.random.nextDouble() - 0.5) * 2;
                                serverLevel.sendParticles(
                                    new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.0f), 1.0f),
                                    entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                                    1, 0, 0.05, 0, 0.02);
                            }
                        }, tick);
                    }

                    // Enable golden slashing particles during attacks
                    if (entity instanceof Player player) {
                        player.addTag("GoldenSlashParticles");
                        if (Config.logDebug) {
                            Log.debug("Seventh Form: Enabled golden slash particles for player {}",
                                player.getName().getString());
                        }
                    }
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);

                // After duration, change sword model back and disable golden particles
                AbilityScheduler.scheduleOnce(entity, () -> {
                    if (entity instanceof Player player) {
                        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
                            .ifPresent(data -> {
                                data.setSwordModelOverride(null);

                                // Sync model clear to all clients
                                if (player instanceof ServerPlayer serverPlayer) {
                                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordModelOverridePacket(
                                            player.getUUID(), null));
                                }

                                if (Config.logDebug) {
                                    Log.debug("Seventh Form: Cleared golden sword model for player {}",
                                        player.getName().getString());
                                }
                            });

                        // Disable golden slashing particles
                        player.removeTag("GoldenSlashParticles");
                        if (Config.logDebug) {
                            Log.debug("Seventh Form: Disabled golden slash particles for player {}",
                                player.getName().getString());
                        }
                    }
                }, duration);
            }
        );
    }

    // Helper methods for particle effects
    private static void spawnParticleLine(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.ParticleOptions particle, int count) {
        Vec3 direction = end.subtract(start);
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            Vec3 pos = start.add(direction.scale(t));
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn particles in a straight line forward (for thrust attacks like speed_attack_sword)
     */
    private static void spawnForwardThrust(ServerLevel level, Vec3 start, Vec3 direction, double distance, net.minecraft.core.particles.ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            Vec3 pos = start.add(direction.scale(distance * t));
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnCircleParticles(ServerLevel level, Vec3 center, double radius, net.minecraft.core.particles.ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Create the complete Frost Breathing technique with 6 forms
     */
    public static BreathingTechnique createFrostBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        return new BreathingTechnique("Frost Breathing", forms);
    }

    /**
     * Create Frost Breathing with 7th form for Komorebi's sword
     */
    public static BreathingTechnique createFrostBreathingWithSeventh() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        forms.add(seventhForm());
        return new BreathingTechnique("Frost Breathing", forms);
    }
}
