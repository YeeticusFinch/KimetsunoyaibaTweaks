package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlowerPetalSlashEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

/**
 * Enhanced implementation of Flower Breathing forms.
 *
 * Flower Breathing is derived from Water Breathing, featuring graceful, flowing attacks
 * with beautiful petal effects. The style emphasizes speed, agility, and multi-hit combos.
 *
 * Form ID Ranges:
 * - 1301-1306: Basic forms (1st through 6th)
 * - 1307-1309: Hashira-exclusive forms (7th through 9th)
 * - 1310: Final Form
 */
public class EnhancedFlowerForms {

    // Flower petal particles - pink/magenta colors for flower aesthetics
    private static final Vector3f PINK_PETAL = new Vector3f(255f / 255f, 182f / 255f, 193f / 255f);
    private static final Vector3f MAGENTA_PETAL = new Vector3f(255f / 255f, 105f / 255f, 180f / 255f);
    private static final Vector3f PEACH_PETAL = new Vector3f(255f / 255f, 218f / 255f, 185f / 255f);
    private static final Vector3f LAVENDAR = new Vector3f(221f/255f, 195f/255f, 255f/255f);
    private static final Vector3f WHITE = new Vector3f(255f/255f, 255f/255f, 255f/255f);
    private static final Vector3f TIGER_ORANGE = new Vector3f(255f/255f, 111f/255f, 0f/255f);

    /**
     * Unified animation helper that works with both players and GeckoLib entities.
     */
    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, 20);
        } else {
            AnimationHelper.playAnimation(entity, animationName);
        }
    }

    /**
     * Unified animation helper with layer and speed control.
     */
    private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName, int maxTicks, float speed, int layer) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, maxTicks);
        } else {
            AnimationHelper.playAnimationOnLayer(entity, animationName, maxTicks, speed, layer);
        }
    }

    /**
     * Helper method to set cancel attack swing state and sync to client
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
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket(player.getUUID(), value),
                serverPlayer);
        }
    }
    
    static SoundEvent SWORD_SWEEP_SOUND = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep"));
    static SoundEvent CRACK2 = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "crack2"));

    public static String modelKey = "flower";
    
    /**
     * First Form: Flowing Tiger Lily Petals
     * The user readies their katana before flowing forward with agility and speed,
     * striking repeatedly with six-fold symmetry like a blooming tiger lily,
     * finishing with a speedy slash to the neck.
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
        	24001,
            "First Form: Flowing Tiger Lily Petals",
            "Six rapid slashes in symmetrical pattern, finishing with a neck strike",
            3, // 3 second cooldown
            (entity, level, formId) -> {
                float damage = (16.0F);
                GuardStateHelper.setGuardState(entity, 7.0, formId);
                playEntityAnimation(entity, "sword_to_left");
                setCancelAttackSwing(entity, true);

                // Launch forward with agility
                Vec3 lookVec = entity.getLookAngle();
        		if (lookVec.y < 0.3) 
        			lookVec = new Vec3(lookVec.x, 0.3, lookVec.z);
        		final Vec3 movement = lookVec.scale(2);
                //MovementHelper.setVelocity(entity, lookVec.scale(1.8).multiply(1, 0.2f, 1));

                // Six-fold symmetry attacks
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                AABB hitBox = new AABB(startPos.add(lookVec.scale(-1.0)), startPos.add(lookVec.scale(5.0))).inflate(2.0);

                final int totalDuration = 30;
                
                final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
						"sword_to_left_reverse", "sword_to_right_reverse" };

                final int[] petalCount = {6};
                final int[] tickCounter = {0};
                
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                final double petalLength = 6;
                
                MovementHelper.setStepHeight(entity, 3);
                
                final boolean[] finalSlash = {false};
                
                //final Vec3[] vectors = new Vec3[2];
                // Get initial forward and right vectors using yaw (handles looking up/down)
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5));
                playEntityAnimation(entity, "kaishin3");
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
	
                	if (tickCounter[0] == 10 && serverLevel != null)
                	{
                		
                        MovementHelper.setVelocity(entity, movement);
                        
                		double initRad = 4;
                        Vec3 start = entity.position().add(new Vec3(0, entity.getBbHeight()*0.75f, 0));
                        for (int i = 0; i < 12; i++) {
                        	double angle = (((double)i)/12) * Math.PI * 2; 
                        	Vec3 center = new Vec3(0, initRad * Math.sin(angle), 0).add(right.scale(initRad * Math.cos(angle))); 
                        	double randMax = Math.random()*2 + 3;
                        	double scaler = Math.random()*0.2; 
                        	if (i % 2 == 0) { 
                        		for (double j = 0; j < randMax; j += 0.05) { 
                        			Vec3 p = start.add(center).subtract(center.scale(Math.cos(j))).add(forward.scale(initRad*Math.sin(j))); 
                        			serverLevel.sendParticles( new DustParticleOptions(TIGER_ORANGE, 1.4f), p.x, p.y, p.z, 2, 0, 0, 0, 0.01 ); 
                        			}
                        		}
                        	else {
                        		for (double j = 0; j < randMax; j += 0.05) { 
                        			Vec3 p = start.add(center).subtract(center.scale(Math.cos(j))).add(forward.scale(initRad*j)); 
                        			//Vec3 p = start.add(forward.scale(j)).add(center.normalize().scale((scaler)*j*j)); 
                        			serverLevel.sendParticles( new DustParticleOptions(WHITE, 0.8f), p.x, p.y, p.z, 2, 0, 0, 0, 0.01 ); 
                        			}
                        		}
                        	}
                        }
                        		
					if (!finalSlash[0] && tickCounter[0] > totalDuration - 5) {
						finalSlash[0] = true;
						playEntityAnimationOnLayer(entity, "sword_rotate", 10, 3.0f, 4000);
						Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0);
						// Vec3 lookVec = entity.getLookAngle().normalize();

						AABB attackBox = entity.getBoundingBox().inflate(4);

						// AABB attackBox = entity.getBoundingBox().inflate(4.5);
						List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
								attackBox, e -> e != entity && e.isAlive());

						for (LivingEntity target : targets) {
							if (Damager.hurt(entity, target, damage, true, false, true))
								MovementHelper.setVelocity(target, movement.scale(0.4f));
						}
						
						if (serverLevel != null) {
							yawRad[0] = (float)Math.toRadians(entity.getYRot());
							double pitchRad = 0;
							Vec3 posOffset = new Vec3(0, 1.5f, 0);
							Vec3 pos = entity.position().add(posOffset);
	
							int arcLength = 160;
							double angle = 0;
							ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad[0], pitchRad,
							4, 0.8, arcLength, 10, angle, ParticleTypes.SWEEP_ATTACK,
							4);

							ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad[0], pitchRad,
									5, 0.1, arcLength, 10, angle,
									new DustParticleOptions(PINK_PETAL,
											(float) (Math.random()*0.7f + 0.2f)),
									3);
							BonePositionTracker.sendRawHorizontalSlashToClients(
									level, // level
									posOffset.add(0, -1, 0),
									modelKey, // model key
									(float)angle, // vert
									false, // reverse
									arcLength, // arc range
									100, // duration
									0, // yaw offset
									0, // pitch offset
									(float)angle*10, // roll offset 
									1.5f, // radius scalar
									2.1f, // size scalar
									15, // angle offset
									entity.getUUID(), // entity id
									"sword_to_right"); // animation name
						}
						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            				    SWORD_SWEEP_SOUND,
            				    SoundSource.PLAYERS, 1.0f, 0.8f);
						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
			                    SoundSource.PLAYERS, 1.0F, 1.2F);
						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
			                    SoundSource.PLAYERS, 1.0F, 1.2F);
						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
			                    SoundSource.PLAYERS, 1.0F, 1.2F);
					}
					
					// Spawn particles
					if (tickCounter[0] > 10 && petalCount[0] > 0)
					{
						// Sound
		                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
		                
						int animIndex = (int)(Math.random() * animations.length);
		                
		                playEntityAnimationOnLayer(entity, animations[animIndex], 10, 3.0f, 4000);

						Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0);
						// Vec3 lookVec = entity.getLookAngle().normalize();

						AABB attackBox = entity.getBoundingBox().inflate(4);

						// AABB attackBox = entity.getBoundingBox().inflate(4.5);
						List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
								attackBox, e -> e != entity && e.isAlive());

						for (LivingEntity target : targets) {
							if (Damager.hurt(entity, target, (damage/2)-1, false, false, true))
								MovementHelper.setVelocity(target, movement.scale(0.2f).add(0, 0.1f, 0));
						}
						if (serverLevel != null) {
	
							//yawRad[0] = (float)Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 30);
							double pitchRad = Math.toRadians((Math.random()-0.3) * 5);
							Vec3 posOffset = new Vec3(Math.random() - 0.5, (Math.random() + 0.5) * 2,
									Math.random() - 0.5);
							Vec3 pos = entity.position().add(posOffset);
	
							int arcLength = (int) (100 + Math.random() * 70);
							double angle = (Math.random() - 0.5) * 10;
							boolean particle = false;
							if (Math.random() > 0.5) {
								particle = true;
								/*ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);
	
								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);*/
								
								BonePositionTracker.sendRawHorizontalSlashToClients(
										level, // level
										posOffset.add(0, -1, 0),
										modelKey, // model key
										(float)angle, // vert
										Math.random() > 0.5, // reverse
										arcLength, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle*10, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										15, // angle offset
										entity.getUUID(), // entity id
										"sword_to_left"); // animation name
							}
							if (Math.random() > 0.5 || !particle) {
								/*
								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										4 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);
	
								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);*/
	
								BonePositionTracker.sendRawVerticalSlashToClients(
										level, // level
										posOffset.add(0, -1, 0) ,
										modelKey, // model key
										(float)angle, // angle
										false, // reverse
										arcLength, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle*10, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										0, // angle offset
										entity.getUUID(), // entity id
										"sword_overhead"); // animation name
								
							}
							
							 // Spawn tiger lily petal particles
			                if (true) {
			                    //yawRad = Math.toRadians(entity.getYRot());
			                    pos = entity.position().add(0, 1.5, 0);
			                    Vec3 rand = new Vec3(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5).normalize().scale(0.6f);
			                    // Six-petaled pattern
			
			                        for (double r = 0.5; r <= petalLength; r += 0.3) {
			                            Vec3 p = pos.add(startPos.add(entity.getLookAngle().add(rand).normalize().scale(r)));
			                            serverLevel.sendParticles(
			                                new DustParticleOptions(PINK_PETAL, 1.0f),
			                                p.x, p.y + Math.sin(r * Math.PI / (2 * petalLength)), p.z, 2, 0.1, 0.1, 0.1, 0.02
			                            );
			                        }
			
			                    // Trail particles as entity moves
			                    ParticleHelper.spawnForwardThrust(serverLevel, pos, movement.normalize().add(entity.getLookAngle().add(rand).normalize().scale(petalLength)), 4.0,
			                        new DustParticleOptions(MAGENTA_PETAL, 0.8f), 15);
			                }
			                
						}

	                    petalCount[0]--;
					}
					tickCounter[0]++;
                }, 1, totalDuration);
                
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, 20);
            }
        );
    }

    /**
     * Second Form: Honorable Shadow Plum
     * The user releases several rotating sword slashes that deflect incoming physical attacks.
     * This is a defensive form that creates a rotating barrier of slashes around the user.
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            24002,
            "Second Form: Honorable Shadow Plum",
            "Rotating sword slashes that deflect incoming physical attacks",
            4, // 4 second cooldown
            (entity, level, formId) -> {
                GuardStateHelper.setGuardState(entity, 12.0, formId);
                final String[] animations = { "sword_rotate", "guard", "sword_to_upper",
						"sword_to_left_reverse", "sword_to_right_reverse" };
                playEntityAnimation(entity, "sword_rotate");
                setCancelAttackSwing(entity, true);

                // Defensive stance - high damage resistance
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3, false, false));

                Vec3 centerPos = entity.position();
                final int totalDuration = 35; // 1.5 seconds
                
                final Vec3[] circleVec = new Vec3[9];
                final int[] circleCounter = {0};
                final int maxCircleCount = 3;
                final float radius = 3.75f;

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = (int) entity.getPersistentData().getDouble("flower_form2_tick");
                    double progress = tick / (double) totalDuration;
                    double rotAngle = progress * Math.PI * 6; // 3 full rotations

                    // Deflect/knockback enemies that get close
                    AABB deflectBox = entity.getBoundingBox().inflate(radius);
                    List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, deflectBox,
                        e -> e != entity && e.isAlive());
                    
                    List<Projectile> nearbyProjectiles = level.getEntitiesOfClass(Projectile.class, deflectBox);

                    if (tick % 5 == 0)
                    {
                    	playEntityAnimationOnLayer(entity, animations[(int)(Math.random()*animations.length)], 10, 2.0f, 4000);
                    	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 0.8F, 1.3F);
                    }
                    
                    for (LivingEntity target : nearbyTargets) {
                        // Push enemies away with rotating slashes
                        Vec3 knockbackDir = target.position().subtract(centerPos).normalize();
                        MovementHelper.setVelocity(target, knockbackDir.scale(0.8).add(0, 0.2, 0));

                        // Deal moderate damage on contact
                        if (tick % 5 == 0) {
                            float damage = (6.0F);
                            Damager.hurt(entity, target, damage);
                        }
                    }
                    
                    for (Projectile target : nearbyProjectiles) { // deflect projectiles
                    	Vec3 knockbackDir = target.position().subtract(centerPos).normalize();
                    	Vec3 velocity = knockbackDir.scale(1.2).add(0, 0.2, 0);
                        //MovementHelper.setVelocity(target, knockbackDir.scale(1.2).add(0, 0.2, 0));
                    	target.setDeltaMovement(velocity);
                    	target.hasImpulse = true;
                    	target.hurtMarked = true; // Force velocity sync to clients
                    }

                    // Rotating plum blossom petal particles
                    if (level instanceof ServerLevel serverLevel) {
                        if (circleCounter[0] <= 0) {
                        	circleVec[0] = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	Vec3 helper = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	while (helper.dot(circleVec[0]) > 0.99) {
                        		helper = Vec3.directionFromRotation(
                            		    (float)(Math.random() * 360f),
                            		    (float)(Math.random() * 360f)
                            		).normalize();
                        	}

                        	circleVec[1] = circleVec[0].cross(helper).normalize();
                        	circleVec[2] = circleVec[0].cross(circleVec[1]).normalize();
                        	
                        	// AGAIN
                        	circleVec[3] = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	helper = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	while (helper.dot(circleVec[3]) > 0.99) {
                        		helper = Vec3.directionFromRotation(
                            		    (float)(Math.random() * 360f),
                            		    (float)(Math.random() * 360f)
                            		).normalize();
                        	}

                        	circleVec[4] = circleVec[3].cross(helper).normalize();
                        	circleVec[5] = circleVec[3].cross(circleVec[4]).normalize();
                        	
                        	// AGAIN
                        	circleVec[6] = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	helper = Vec3.directionFromRotation(
                        		    (float)(Math.random() * 360f),
                        		    (float)(Math.random() * 360f)
                        		).normalize();
                        	
                        	while (helper.dot(circleVec[6]) > 0.99) {
                        		helper = Vec3.directionFromRotation(
                            		    (float)(Math.random() * 360f),
                            		    (float)(Math.random() * 360f)
                            		).normalize();
                        	}

                        	circleVec[7] = circleVec[6].cross(helper).normalize();
                        	circleVec[8] = circleVec[6].cross(circleVec[7]).normalize();
                        }
                        	
                        	
                        	for (float theta = 0; theta < Math.PI * 2 * (((float)circleCounter[0]) / maxCircleCount); theta += 0.05) {
                        		Vec3 d =
                        		        circleVec[1].scale(radius*Math.cos(theta))
                        		        .add(circleVec[2].scale(radius*Math.sin(theta)));
                        		Vec3 p = entity.position().add(0, 1, 0).add(d);
                        		
                        		serverLevel.sendParticles(
                                        new DustParticleOptions(LAVENDAR, 1.2f),
                                        p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01
                                    );
                        		
                        		d =
                        		        circleVec[4].scale(radius*Math.cos(theta))
                        		        .add(circleVec[5].scale(radius*Math.sin(theta)));
                        		
                        		p = entity.position().add(0, 1, 0).add(d);
                        		
                        		serverLevel.sendParticles(
                                        new DustParticleOptions(WHITE, 1.2f),
                                        p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01
                                    );
                        		
                        		d =
                        		        circleVec[7].scale(radius*Math.cos(theta))
                        		        .add(circleVec[8].scale(radius*Math.sin(theta)));
                        		
                        		p = entity.position().add(0, 1, 0).add(d);
                        		
                        		serverLevel.sendParticles(
                                        new DustParticleOptions(LAVENDAR, 1.2f),
                                        p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01
                                    );
                        	}
                        	
                        circleCounter[0]++;
                        if (circleCounter[0] > maxCircleCount) circleCounter[0] = 0;
                    }

                    entity.getPersistentData().putDouble("flower_form2_tick", tick + 1);
                }, 1, totalDuration);

                // Sound
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("flower_form2_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration + 5);
            }
        );
    }

    /**
     * Third Form: Scattering Rose-Peach Thorns
     * The user performs a long sweeping slash before launching forward, executing diagonal
     * and horizontal strikes simultaneously, then leaps out while maneuvering into a high
     * guard strike. This leads into a spiral of slashes and stabs, twisting and bending
     * with momentum while performing a downward spiral attack that takes the form of a rose
     * and its thorned vines.
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
        	24003,
            "Third Form: Scattering Rose-Peach Thorns",
            "Sweeping slashes evolving into a spiraling rose-thorned attack",
            6, // 6 second cooldown
            (entity, level, formId) -> {
                float damage = (16.0F);
                GuardStateHelper.setGuardState(entity, 4.0, formId);
                playEntityAnimation(entity, "sword_rotate");
                setCancelAttackSwing(entity, true);

                Vec3 lookVec = entity.getLookAngle();
                
                final Vec3[] dir = {lookVec};

                // Initial sweep - 360 degree attack
                AABB sweepBox = entity.getBoundingBox().inflate(5.5);
                List<LivingEntity> sweepTargets = level.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : sweepTargets) {
                    Damager.hurt(entity, target, damage);
                    MovementHelper.setVelocity(target, lookVec);
                }
             // Sound
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 0.9F);
                
                final int[] tickCounter = {0};
                final int firstSlash = 15;
                final int launchingForward = 10;
                final int leapOut = 15;
                final int downwardSpiral = 15;
                final int totalTicks = firstSlash + launchingForward + leapOut + downwardSpiral;
                
                // Get initial forward and right vectors using yaw (handles looking up/down)
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
             // Spawn particles
    			if (serverLevel != null) {
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 4.0,
    						new DustParticleOptions(PINK_PETAL,
    								(float) (Math.random()*0.9f + 0.4f)), 30);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.75,
    						new DustParticleOptions(PINK_PETAL,
    								(float) (Math.random()*0.7f + 0.2f)), 30);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 3.5,
    						new DustParticleOptions(PINK_PETAL,
    								(float) (Math.random()*0.7f + 0.2f)), 20);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 2.5,
    						new DustParticleOptions(WHITE,
    								(float) (Math.random()*1.5f + 0.5f)), 20);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 1.5,
    						new DustParticleOptions(WHITE,
    								(float) (Math.random()*1.4f + 0.3f)), 20);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 4.5,
    						ParticleTypes.CHERRY_LEAVES, 30);
    				ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 4.75,
    						ParticleTypes.CHERRY_LEAVES, 30);
    				level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F,
    						0.8F);
    				
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
    			
    			final Vec3[] leapPos = new Vec3[1];
                
    			final float hor = 4f; // forward displacement in the direction that the user was looking
    			
                AbilityScheduler.scheduleRepeating(entity, () -> {
                	
                	if (tickCounter[0] < firstSlash)
                	{
                		// First slash
                		
                	}
                	else if (tickCounter[0] >= firstSlash && tickCounter[0] < firstSlash + launchingForward)
                	{
                		// Launching forward with diagonal and horizontal slashes
                		dir[0] = entity.getLookAngle();
                		if (tickCounter[0] == firstSlash) {
                			Vec3 m = lookVec.scale(3f);
                			if (m.y < 0.05f)
                				m = new Vec3(m.x, 0.05, m.z);
                			MovementHelper.setVelocity(entity, m);
                		}
                		if (tickCounter[0] % 2 == 0) {
                			level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                    SoundSource.PLAYERS, 1.0F, 1.4F);
                            AABB bbox = entity.getBoundingBox().inflate(5.5);
                            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bbox,
                                e -> e != entity && e.isAlive());

                            for (LivingEntity target : targets) {
                                boolean hit = Damager.hurt(entity, target, damage/3);
                                if (hit) MovementHelper.setVelocity(target, lookVec);
                            }
                		}
                		if (serverLevel != null) {
                			
							//yawRad[0] = (float)Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 30);
							double pitchRad = Math.toRadians((Math.random()-0.3) * 5);
							Vec3 posOffset = new Vec3(Math.random() - 0.5, (Math.random() + 0.5) * 2,
									Math.random() - 0.5);
							Vec3 pos = entity.position().add(posOffset);
	
							int arcLength = (int) (100 + Math.random() * 70);
							double angle = (Math.random() - 0.5) * 10;
							boolean particle = false;
							if (Math.random() > 0.5) {
								playEntityAnimationOnLayer(entity, Math.random() > 0.5 ? "sword_to_right" : "sword_to_left", 10, 3.0f, 4000);
								particle = true;
								/*ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);
	
								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);*/
								
								BonePositionTracker.sendRawHorizontalSlashToClients(
										level, // level
										posOffset.add(0, -1, 0),
										modelKey, // model key
										(float)angle, // vert
										Math.random() > 0.5, // reverse
										arcLength, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle*10, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										15, // angle offset
										entity.getUUID(), // entity id
										"sword_to_left"); // animation name
							}
							if (Math.random() > 0.5 || !particle) {
								/*
								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										4 + Math.random() * 2, 0.2, arcLength, 10, angle, ModParticles.SMALL_MIST_PARTICLE.get(),
										4);
	
								ParticleHelper.spawnVerticalArc(serverLevel, pos, yawRad, pitchRad,
										3 + Math.random() * 2, 0.2, arcLength, 10, angle,
										new DustParticleOptions(new Vector3f(138f / 255f, 195f / 255f, 194f / 255f),
												(float) (Math.random()*0.7f + 0.2f)),
										2);*/
								playEntityAnimationOnLayer(entity, Math.random() > 0.5 ? "sword_to_upper" : "sword_overhead", 10, 3.0f, 4000);
	
								BonePositionTracker.sendRawVerticalSlashToClients(
										level, // level
										posOffset.add(0, -1, 0) ,
										modelKey, // model key
										(float)angle, // angle
										false, // reverse
										arcLength, // arc range
										100, // duration
										0, // yaw offset
										0, // pitch offset
										(float)angle*10, // roll offset 
										1.5f, // radius scalar
										2.1f, // size scalar
										0, // angle offset
										entity.getUUID(), // entity id
										"sword_overhead"); // animation name
								
							}
                		}
                	}
                	else if (tickCounter[0] >= firstSlash + launchingForward && tickCounter[0] < firstSlash + launchingForward + leapOut)
                	{
                		// Leap upwards and backwards, get defensive
                		if (tickCounter[0] == firstSlash + launchingForward)
                		{
                			GuardStateHelper.setGuardState(entity, damage*2, formId);
                			//MovementHelper.setVelocity(entity, forward.scale(-1.6).add(0, 3, 0));
                			playEntityAnimation(entity, "backstep");
                			leapPos[0] = entity.position().add(0, 11, 0).subtract(forward.scale(4));
                		}
                		//if (entity.getDeltaMovement().y < 0.001)
                		//	MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, 0.005, entity.getDeltaMovement().z);
                		MovementHelper.setVelocity(entity, leapPos[0].subtract(entity.position()));
                		if (tickCounter[0] == firstSlash + launchingForward + leapOut/2)
                		{
                			playEntityAnimation(entity, "guard");
                			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, leapOut + downwardSpiral, 1));
                		}
                	}
                	else if (tickCounter[0] >= firstSlash + launchingForward + leapOut && tickCounter[0] < firstSlash + launchingForward + leapOut + downwardSpiral)
                	{
                		if (tickCounter[0] == firstSlash + launchingForward + leapOut)
                		{
                			GuardStateHelper.setGuardState(entity, 4.0, formId);
                			level.playSound(null, entity.blockPosition(), CRACK2,
                                    SoundSource.PLAYERS, 1.0F, 1.0F);
                		}
                		if (tickCounter[0] % 5 == 0) 
                		{
                			playEntityAnimation(entity, "sword_rotate");
                			level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                                    SoundSource.PLAYERS, 1.0F, 0.9F);
                		}
                		// Downward Spiral
                		//double progress = entity.getPersistentData().getDouble("flower_form3_tick") / 30.0;
                		double spiralTick =
                		        tickCounter[0] - (firstSlash + launchingForward + leapOut);

                		double progress = spiralTick / downwardSpiral;
                		progress = Math.min(Math.max(progress, 0.0), 1.0);
                		
                        double angle = progress * Math.PI * 6; // number of twists
                        double radius = 6 * (1.4 - progress); // shrinking spiral
                        Vec3 up = new Vec3(0, 1, 0);
                        
                        // Horizontal spiral
                        Vec3 spiralOffset =
                                right.scale(Math.cos(angle) * radius)
                                .add(forward.scale(Math.sin(angle) * radius))
                                .add(dir[0].scale(progress * hor));

                        // Downward motion
                        Vec3 downward = new Vec3(0, -0.9, 0);

                        // Tangential velocity (THIS makes it curve)
                        Vec3 tangential =
                                right.scale(-Math.sin(angle))
                                .add(forward.scale(Math.cos(angle)))
                                .scale(1.4);

                        Vec3 velocity =
                                tangential
                                .add(downward);

                        MovementHelper.setVelocity(entity, velocity);
                        
                        /*
                        Vec3 spiralVec = lookVec.scale(2.0).add(
                            Math.cos(progress * Math.PI * 4) * 0.5,
                            Math.sin(progress * Math.PI * 2) * 0.3,
                            Math.sin(progress * Math.PI * 4) * 0.5
                        );
                        MovementHelper.setVelocity(entity, spiralVec);
                        */

                        // Spiral damage hitbox
                        AABB spiralBox = entity.getBoundingBox().inflate(2.0);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, spiralBox,
                            e -> e != entity && e.isAlive());

                        for (LivingEntity target : targets) {
                            //float damage = (6.0F);
                            Damager.hurt(entity, target, damage/3);
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        }

                        // Spiral rose particle effect
                        if (serverLevel != null) {
                            Vec3 pos = entity.position().add(0, 1.0, 0);
                            //angle = progress * Math.PI * 6;
                            //radius = 5.0 + progress * 0.5;

                            for (int i = 0; i < 3; i++) {
                                double offsetAngle = angle + (i * Math.PI * 2 / 3);
                                double x = pos.x + Math.cos(offsetAngle) * radius;
                                double z = pos.z + Math.sin(offsetAngle) * radius;
                                double y = pos.y + Math.sin(progress * Math.PI * 2) * 1.5;

                                serverLevel.sendParticles(
                                    new DustParticleOptions(PEACH_PETAL, 1.2f),
                                    x, y, z, 2, 0.1, 0.1, 0.1, 0.01
                                );
                                
                                serverLevel.sendParticles(
                                		ParticleTypes.CHERRY_LEAVES,
                                        x, y, z, 1, 0.1, 0.1, 0.1, 0.01
                                    );

                                // Thorn particles
                                if (i == 0) {
                                    serverLevel.sendParticles(ParticleTypes.CRIT,
                                        x, y, z, 1, 0, 0, 0, 0.05);
                                }
                            }
                            
                            for (float prog = 0; prog <= progress; prog += 0.02f) {
                            	double a = prog * Math.PI * 6;
                            	double r = 6 * (1.4 - progress);

                            	double x = pos.x + right.x * Math.cos(a) * r + forward.x * Math.sin(a) * r + dir[0].x * prog * hor;
                            	double z = pos.z + right.z * Math.cos(a) * r + forward.z * Math.sin(a) * r + dir[0].z * prog * hor;
                            	double y = pos.y + Math.sin(a * 0.5) * 0.6 + dir[0].y * prog * hor;
                            	serverLevel.sendParticles(
                                        new DustParticleOptions(PEACH_PETAL, 1.6f),
                                        x, y, z, 5, 0.1, 0.1, 0.1, 0.01
                                    );
                                    
                                    serverLevel.sendParticles(
                                    		ParticleTypes.CHERRY_LEAVES,
                                            x, y, z, 5, 0.1, 0.1, 0.1, 0.01
                                        );
                            }
                        }
                	}

                	tickCounter[0]++;
                }, 1, totalTicks); // Tick every 2 ticks for 30 iterations

                AbilityScheduler.scheduleOnce(entity, () -> {
                    //entity.getPersistentData().remove("flower_form3_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    
                }, totalTicks+2);
            }
        );
    }

    /**
     * Fourth Form: Crimson Hanagoromo
     * The user unleashes a single sword slash that curves and twists gracefully.
     * A precise, elegant strike with a sweeping curved trajectory.
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            24004,
            "Fourth Form: Crimson Hanagoromo",
            "A single sword slash that curves and twists gracefully",
            3, // 3 second cooldown
            (entity, level, formId) -> {
                float damage = (17.0F);
                GuardStateHelper.setGuardState(entity, damage, formId);
                playEntityAnimation(entity, "sword_to_right");
                setCancelAttackSwing(entity, true);

                Vec3 lookVec = entity.getLookAngle();

                // Graceful forward lunge
                //MovementHelper.setVelocity(entity, lookVec.scale(1.5).multiply(1, 0.2f, 1));

                // The slash curves in a wide arc - hits a broad area in front
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                double yawRad = Math.toRadians(entity.getYRot());

                // Curved hitbox - sweep from right to left in a graceful arc
                AABB hitBox = new AABB(
                    startPos.add(lookVec.scale(-1.0)),
                    startPos.add(lookVec.scale(6.0))
                ).inflate(3.0);

                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    Damager.hurt(entity, target, damage);
                    // Graceful knockback - push sideways and back
                    Vec3 toTarget = target.position().subtract(entity.position()).normalize();
                    MovementHelper.setVelocity(target, toTarget.scale(0.8).add(0, 0.3, 0));
                }
                
                level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
                        SoundSource.PLAYERS, 1.0F, 0.7F);

                // Curved crimson slash particle trail
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 pos = entity.position().add(0, 1.5, 0);

                    // Draw a graceful curving arc of crimson petals
                    for (int deg = -60; deg <= 60; deg += 3) {
                        double angle = yawRad + Math.toRadians(deg);
                        double radius = 3.0 + Math.sin(Math.toRadians(deg) * 2) * 1.0; // Curve oscillation
                        double heightOscillation = Math.sin(Math.toRadians(deg) * 3) * 0.5;

                        double x = pos.x - Math.sin(angle) * radius;
                        double z = pos.z + Math.cos(angle) * radius;
                        double y = pos.y + heightOscillation;
                        double lx = entity.getLookAngle().x;
                        double ly = entity.getLookAngle().y;
                        double lz = entity.getLookAngle().z;

                        // Crimson (deep red) petals for hanagoromo
                        serverLevel.sendParticles(
                            new DustParticleOptions(new Vector3f(220f/255f, 20f/255f, 60f/255f), 1.2f),
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.01
                        );
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(220f/255f, 20f/255f, 60f/255f), 1.2f),
                                x + lx*0.2, y + ly*0.2, z+lz*0.2, 1, 0.05, 0.05, 0.05, 0.01
                            );
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(220f/255f, 20f/255f, 60f/255f), 1.2f),
                                x + lx*0.4, y + ly*0.4, z+lz*0.4, 1, 0.05, 0.05, 0.05, 0.01
                            );
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(220f/255f, 20f/255f, 60f/255f), 1.2f),
                                x + lx*0.6, y + ly*0.6, z+lz*0.6, 1, 0.05, 0.05, 0.05, 0.01
                            );
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(220f/255f, 20f/255f, 60f/255f), 1.2f),
                                x + lx*0.8, y + ly*0.8, z+lz*0.8, 1, 0.05, 0.05, 0.05, 0.01
                            );

                        // Pink accent petals
                        if (deg % 9 == 0) {
                            serverLevel.sendParticles(
                                new DustParticleOptions(PINK_PETAL, 0.9f),
                                x, y + 0.2, z, 2, 0.1, 0.1, 0.1, 0.02
                            );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(PINK_PETAL, 0.9f),
                                    x + lx*0.2, y + ly*0.2 + 0.2, z+lz*0.2, 1, 0.1, 0.1, 0.1, 0.02
                                );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(PINK_PETAL, 0.9f),
                                    x + lx*0.4, y + ly*0.4 + 0.2, z+lz*0.4, 1, 0.1, 0.1, 0.1, 0.02
                                );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(PINK_PETAL, 0.9f),
                                    x + lx*0.6, y + ly*0.6 + 0.2, z+lz*0.6, 1, 0.1, 0.1, 0.1, 0.02
                                );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(PINK_PETAL, 0.9f),
                                    x + lx*0.8, y + ly*0.8 + 0.2, z+lz*0.8, 1, 0.1, 0.1, 0.1, 0.02
                                );
                        }
                    }
                }

                // Sound - single powerful slash
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.1F);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, 25);
            }
        );
    }

    /**
     * Fifth Form: Peonies of Futility
     * The user unleashes a flurry of nine consecutive attacks that flow and weave
     * in on themselves. Each strike flows into the next in a continuous chain.
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            24005,
            "Fifth Form: Peonies of Futility",
            "A flurry of nine consecutive attacks that flow and weave together",
            6, // 6 second cooldown
            (entity, level, formId) -> {

                float damage = (2.0F);
                GuardStateHelper.setGuardState(entity, 8.0, formId);
                setCancelAttackSwing(entity, true);

                Vec3 lookVec = entity.getLookAngle();
                final int totalSlashes = 12;
                final int slashInterval = 3; // 3 ticks between each slash
                final int totalDuration = totalSlashes * slashInterval + 10;
                final int[] currentTick = {0};

                final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead" };

                // Slight forward momentum throughout
                //MovementHelper.setVelocity(entity, lookVec.scale(1.0).multiply(1, 0.2f, 1));
                final float range = 4;
                final Vec3[] tPoint = {entity.getEyePosition().add(lookVec.scale(range))};
                AABB bb = new AABB(tPoint[0].subtract(3, 3, 3), tPoint[0].add(3, 3, 3));
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bb,
                        e -> e != entity && e.isAlive());
                final LivingEntity[] tt = {null};
                if (targets.size() > 0) {
                	tPoint[0] = targets.get(0).getEyePosition();
                	tt[0] = targets.get(0);
                }
                
                final Vec3[] vecs = new Vec3[totalSlashes];
                final Vec3[] perps = new Vec3[totalSlashes];
                
                for (int i = 0; i < totalSlashes; i++)
                {
                	Vec3 rand = new Vec3(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5).normalize();
                	vecs[i] = rand.subtract(entity.getLookAngle()).scale(Math.random() + 1);
                	
                	Vec3 helper = Vec3.directionFromRotation(
                		    (float)(Math.random() * 360f),
                		    (float)(Math.random() * 360f)
                		).normalize();
                	while (helper.dot(vecs[i]) > 0.99) {
                		helper = Vec3.directionFromRotation(
                    		    (float)(Math.random() * 360f),
                    		    (float)(Math.random() * 360f)
                    		).normalize();
                	}
                	
                	perps[i] = vecs[i].cross(helper).normalize();
                }
                
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
                	if (tt[0] != null)
                		tPoint[0] = tt[0].getEyePosition();
                	
                	Vec3 pos = tPoint[0].subtract(entity.getLookAngle().scale(4));
                	Vec3 desired = pos.subtract(0, entity.getEyeHeight(), 0).subtract(entity.position());
                	Vec3 vel = entity.getDeltaMovement().lerp(desired.scale(0.4), 0.35);
                	vel = new Vec3(vel.x, entity.getDeltaMovement().y, vel.z);
                	MovementHelper.setVelocity(entity, vel);
                	
                    // Teardrop is a cardioid between theta = 3.56 and theta = 5.85
                	/*
                	if (serverLevel != null) {
                		int i = Math.min(totalSlashes - 1, currentTick[0] / slashInterval);
                		double progress = (currentTick[0] % slashInterval) / (double) slashInterval; // between 0 and 1
	                	for (float p = 0; p < progress; p += 0.04f) {
		                	double theta = 3.56 + p * (5.85-3.56);
		                	double r = (0.7 + 1.7 * Math.sin(theta));
		                	double x = r * Math.cos(theta);
		                	double y = r * Math.sin(theta);
		                	
		                	Vec3 dir = vecs[i].scale(y).add(perps[i].scale(x));
		                	double thicc = 1-Math.abs(p-0.5); // 0 - 0.5
		                	// spawn particle at pp
		                	Vector3f color = PINK_PETAL.lerp(WHITE, (float)(thicc*2));
		                	for (float j = 1; j < thicc + 1.05; j += 0.1) {
			                	Vec3 pp = tPoint[0].add(dir.scale(j));
		                		serverLevel.sendParticles(
		                                new DustParticleOptions(color, 1.2f),
		                                pp.x, pp.y, pp.z, 1, 0.05, 0.05, 0.05, 0.01
		                            );
		                	}
	                	}
                	}*/
                	
                	Vec3 rand = new Vec3(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5).normalize();
                	Vec3 dir = rand.subtract(entity.getLookAngle()).scale(Math.random() + 1);
                	FlowerPetalSlashEntity slash = FlowerPetalSlashEntity.create(
                		    level,
                		    tPoint[0],  // position
                		    dir,            // direction
                		    (float)(Math.random()*360),        // roll (degrees)
                		    (float)(Math.random()*1.2+1) // Random scale
                		);
                		level.addFreshEntity(slash);
                		
                	if (currentTick[0] < totalSlashes * slashInterval && currentTick[0] % slashInterval == 0) {
                		playEntityAnimationOnLayer(entity, animations[(int)(animations.length*Math.random())], 10, 3.0f, 4000);
                        level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                        level.playSound(null, entity.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN,
                                SoundSource.PLAYERS, 1.5F, 1.0F);
                        if (serverLevel != null)
                        {
                        	serverLevel.sendParticles(
	                                ParticleTypes.FLASH,
	                                tPoint[0].x, tPoint[0].y, tPoint[0].z, 1, 0.05, 0.05, 0.05, 0.01
	                            );
                        }
                        
                        AABB bbox = new AABB(tPoint[0].subtract(3, 3, 3), tPoint[0].add(3, 3, 3));
                        List<LivingEntity> targetEntities = level.getEntitiesOfClass(LivingEntity.class, bbox,
                            e -> e != entity && e.isAlive());
                        
                            for (LivingEntity target : targetEntities) {
                                if (Damager.hurt(entity, target, damage, true, false, true)) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, true, false));
                            }
                	}
                	
                	currentTick[0]++;
                }, 1, totalDuration);

               

                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("flower_form5_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration + 5);
            }
        );
    }

    /**
     * Sixth Form: Whirling Peach
     * A technique used after or during evasion. The user spins around,
     * moving with their body weight to deliver a powerful attack.
     * Features a quick backstep followed by a spinning slash.
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            24006,
            "Sixth Form: Whirling Peach",
            "Spin around with body weight to deliver an evasive counterattack",
            5, // 5 second cooldown
            (entity, level, formId) -> {
                GuardStateHelper.setGuardState(entity, 10.0, formId);
                setCancelAttackSwing(entity, true);

                Vec3 lookVec = entity.getLookAngle();
                final int totalDuration = 35;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                Vec3 right = new Vec3(-forward.z, 0, forward.x);

                // Phase 1: Quick evasive backstep (first 8 ticks)
                MovementHelper.setVelocity(entity, lookVec.scale(-1.5).add(0, 0.3, 0));
                playEntityAnimation(entity, "backstep");

                // Damage resistance during evasion
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 6, false, false));

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = (int) entity.getPersistentData().getDouble("flower_form6_tick");

                    // Phase 2: Spinning attack (ticks 8-25)
                    if (tick == 8) {
                        // Launch forward spinning with body weight
                        Vec3 currentLook = entity.getLookAngle();
                        MovementHelper.setVelocity(entity, forward.scale(1.5));
                        playEntityAnimation(entity, "sword_rotate");
                    }

                    if (tick >= 8 && tick < 25) {
                        // Spinning damage - 360 degree attack
                        double spinProgress = (tick - 8) / 17.0;
                        double spinAngle = spinProgress * Math.PI * 4; // Two full spins

                        AABB spinBox = entity.getBoundingBox().inflate(3.0);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, spinBox,
                            e -> e != entity && e.isAlive());
                        
                        MovementHelper.setVelocity(entity, forward.scale(1.0).add(right.scale(Math.sin(18 * (((float)tick)/totalDuration)))));

                        if (tick % 4 == 0) {
                        	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                    SoundSource.PLAYERS, 0.9F, 1.4F);
                        	playEntityAnimation(entity, "sword_rotate");
                            float damage = (16.0F);
                            for (LivingEntity target : targets) {
                                Damager.hurt(entity, target, damage);
                                // Spinning knockback - push in direction of spin
                                Vec3 toTarget = target.position().subtract(entity.position()).normalize();
                                MovementHelper.setVelocity(target, toTarget.scale(1.0).add(0, 0.2, 0));
                                MovementHelper.setVelocity(entity, forward.scale(1.0).add(right.scale(Math.sin(6 * (((float)tick)/totalDuration)))));
                            }
                        }

                        // Peach-colored whirling particle trail
                        if (level instanceof ServerLevel serverLevel) {
                            Vec3 pos = entity.position().add(0, 1.0, 0);
                            
                            serverLevel.sendParticles(
                                    new DustParticleOptions(PEACH_PETAL, 0.8f),
                                    pos.x, pos.y, pos.z, 4, 0.2, 0.2, 0.2, 0.01
                                );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(LAVENDAR, 1.1f),
                                    pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.01
                                );
                            serverLevel.sendParticles(
                                    new DustParticleOptions(WHITE, 1.5f),
                                    pos.x, pos.y, pos.z, 4, 0.01, 0.01, 0.01, 0.01
                                );

                            // Spinning peach petals around entity
                            for (int i = 0; i < 3; i++) {
                                double angle = spinAngle + (i * Math.PI * 2 / 3);
                                double radius = 2.0 + Math.sin(spinProgress * Math.PI) * 0.5;
                                double x = pos.x + Math.cos(angle) * radius;
                                double z = pos.z + Math.sin(angle) * radius;
                                double y = pos.y + Math.sin(angle * 2) * 0.4;

                                serverLevel.sendParticles(
                                    new DustParticleOptions(PEACH_PETAL, 1.1f),
                                    x, y, z, 2, 0.1, 0.05, 0.1, 0.01
                                );
                            }

                            // Ground dust from spinning momentum
                            if (tick % 3 == 0) {
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                    pos.x, pos.y - 0.5, pos.z, 1, 0.5, 0.1, 0.5, 0);
                            }
                        }
                    }

                    entity.getPersistentData().putDouble("flower_form6_tick", tick + 1);
                }, 1, totalDuration);

                // Sound
                level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK,
                    SoundSource.PLAYERS, 0.8F, 1.3F);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("flower_form6_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration + 5);
            }
        );
    }

    /**
     * Seventh Form: Bellowing Peach Shockwave (Hashira Exclusive)
     * The user launches forward with a flourish of attacks at such speed and agility that
     * several afterimages are created, which enemies falsely attack, leaving them open
     * to multiple constant arcs of flourishing attacks that immobilize and stun them.
     * Follows up with an upward kick to the abdomen and a stab to the neck, using momentum
     * to throw the opponent backwards before charging forward for a finishing slash.
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            24007,
            "Seventh Form: Bellowing Peach Shockwave",
            "Blinding speed creates afterimages, immobilizing foes before a devastating finish",
            8, // 8 second cooldown
            (entity, level, formId) -> {
                final float damage = (8.0F);
                GuardStateHelper.setGuardState(entity, 14.0, formId);
                setCancelAttackSwing(entity, true);

                final int stage1Flurry = 24;
                final int stage1Pause = 10;
                final int stage2Kick = 8;
                final int stage3Stab = 10;
                final int stage3Pause = 10;
                final int stage4FinalSlash = 12;
                final int totalTicks = stage1Flurry + stage1Pause + stage2Kick + stage3Stab + stage3Pause + stage4FinalSlash;
                final double stage1VelocityPerTick = 15.0 / stage1Flurry;
                final double stage1ZigzagAmplitude = 3;
                final int stage1ZigzagPeriod = 6;

                final int[] tickCounter = {0};
                final int[] stage1AnimIndex = {0};

                final String[] stage1Animations = {
                    "sword_to_left",
                    "sword_to_right",
                    "sword_to_upper",
                    "sword_to_left_reverse",
                    "sword_to_right_reverse"
                };

                final Vec3[] stage1ForwardDir = {entity.getLookAngle().multiply(1, 0, 1)};
                final Vec3[] stage2DashDir = {entity.getLookAngle().multiply(1, 0, 1)};
                final Vec3[] stage3DashDir = {entity.getLookAngle().multiply(1, 0, 1)};
                final Vec3[] stage4DashDir = {entity.getLookAngle().multiply(1, 0, 1)};

                final Vec3 lookVec = entity.getLookAngle();
                final float range = 5.0f;
                final Vec3[] tPoint = {entity.getEyePosition().add(lookVec.scale(range))};
                AABB bb = new AABB(tPoint[0].subtract(3, 3, 3), tPoint[0].add(3, 3, 3));
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bb,
                    e -> e != entity && e.isAlive());
                final LivingEntity[] primaryTarget = {null};
                if (targets.size() > 0) {
                    tPoint[0] = targets.get(0).getEyePosition();
                    primaryTarget[0] = targets.get(0);
                }

                if (primaryTarget[0] != null && primaryTarget[0] != entity) {
                    stage1ForwardDir[0] = primaryTarget[0].getEyePosition().subtract(entity.getEyePosition()).multiply(1, 0, 1);
                }
                if (stage1ForwardDir[0].lengthSqr() < 0.0001) {
                    stage1ForwardDir[0] = new Vec3(
                        Math.sin(Math.toRadians(-entity.getYRot())),
                        0,
                        Math.cos(Math.toRadians(-entity.getYRot()))
                    );
                }
                if (stage1ForwardDir[0].lengthSqr() < 0.0001) {
                    stage1ForwardDir[0] = new Vec3(0, 0, 1);
                }
                stage1ForwardDir[0] = stage1ForwardDir[0].normalize();
                stage2DashDir[0] = stage1ForwardDir[0];
                stage3DashDir[0] = stage1ForwardDir[0];
                stage4DashDir[0] = stage1ForwardDir[0];

                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;

                MovementHelper.setStepHeight(entity, 3);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, totalTicks + 20, 2));
                playEntityAnimation(entity, "speed_attack_sword");

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = tickCounter[0];
                    LivingEntity target = (primaryTarget[0] != null && primaryTarget[0].isAlive()) ? primaryTarget[0] : null;
                    if (target != null) {
                        tPoint[0] = target.getEyePosition();
                    }

                    if (tick < stage1Flurry) {
                        Vec3 forward = stage1ForwardDir[0];
                        Vec3 right = new Vec3(-forward.z, 0, forward.x);

                        double currentSpeed = stage1VelocityPerTick * (0.8 + Math.random() * 0.2);
                        // True left/right oscillation while moving forward.
                        // Previous sign + sine combination could cancel into one-sided drift.
                        double zigzagOffset = stage1ZigzagAmplitude * Math.sin(tick * Math.PI / stage1ZigzagPeriod);

                        double hopY = 0;
                        if (tick % 8 == 0 && entity.onGround()) {
                            hopY = 0.3;
                        }

                        Vec3 forwardMotion = forward.scale(currentSpeed);
                        Vec3 sideMotion = right.scale(zigzagOffset);
                        Vec3 movement = forwardMotion.add(sideMotion);
                        MovementHelper.setVelocity(entity, movement.x, entity.getDeltaMovement().y + hopY, movement.z);

                        if (tick % 5 == 0) {
                            String anim = stage1Animations[stage1AnimIndex[0] % stage1Animations.length];
                            stage1AnimIndex[0]++;
                            playEntityAnimationOnLayer(entity, anim, 10, 3.0f, 4000);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 1.2F);

                            if (!level.isClientSide) {
                                com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity afterImage =
                                    new com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity(
                                        level, entity, 40, entity.position(), entity.getYRot()
                                    );
                                afterImage.startVisibleWithFade();
                                afterImage.setSwinging(true);
                                playEntityAnimationOnLayer(afterImage, anim, 10, 3.0f, 4000);
                                level.addFreshEntity(afterImage);
                            }

                            AABB attackBox = entity.getBoundingBox().expandTowards(forward.scale(4.0)).inflate(2.6);
                            List<LivingEntity> stageTargets = level.getEntitiesOfClass(LivingEntity.class, attackBox,
                                e -> e != entity && e.isAlive()
                                    && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity));

                            for (LivingEntity stageTarget : stageTargets) {
                                if (Damager.hurt(entity, stageTarget, damage * 0.375f, true, false, true)) {
                                    stageTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                                    MovementHelper.addVelocity(stageTarget, forward.x * 0.4, 0.2, forward.z * 0.4);
                                }
                            }

                            /*
                            Vec3 slashOrigin = entity.position()
                                .add(0, entity.getEyeHeight() * 0.7, 0)
                                .add(forward.scale(1.4));
                            Vec3 slashDirection = forward
                                .add(right.scale((Math.random() - 0.5) * 0.6))
                                .add(0, (Math.random() - 0.4) * 0.2, 0)
                                .normalize();
                            FlowerPetalSlashEntity slash = FlowerPetalSlashEntity.create(
                                level,
                                slashOrigin,
                                slashDirection,
                                (float) (Math.random() * 360),
                                (float) (Math.random() * 0.7 + 0.9)
                            );
                            level.addFreshEntity(slash);*/

                            if (serverLevel != null) {
                                double angle = (Math.random() - 0.5) * 10;
                                int arcLength = (int) (100 + Math.random() * 70);
                                Vec3 posOffset = new Vec3(
                                    Math.random() - 0.5,
                                    (Math.random() + 0.5) * 2.0,
                                    Math.random() - 0.5
                                );

                                if (Math.random() > 0.45) {
                                    BonePositionTracker.sendRawHorizontalSlashToClients(
                                        level,
                                        posOffset.add(0, -1, 0),
                                        modelKey,
                                        (float) angle,
                                        Math.random() > 0.5,
                                        arcLength,
                                        100,
                                        0,
                                        0,
                                        (float) angle * 10,
                                        1.5f,
                                        2.1f,
                                        15,
                                        entity.getUUID(),
                                        anim
                                    );
                                } else {
                                    BonePositionTracker.sendRawVerticalSlashToClients(
                                        level,
                                        posOffset.add(0, -1, 0),
                                        modelKey,
                                        (float) angle,
                                        false,
                                        arcLength,
                                        100,
                                        0,
                                        0,
                                        (float) angle * 10,
                                        1.5f,
                                        2.1f,
                                        0,
                                        entity.getUUID(),
                                        anim
                                    );
                                }

                                Vec3 flashPos = entity.position().add(0, entity.getEyeHeight() * 0.7, 0);
                                serverLevel.sendParticles(ParticleTypes.FLASH,
                                    flashPos.x, flashPos.y, flashPos.z,
                                    1, 0.05, 0.05, 0.05, 0.0);
                            }
                        }
                    } else if (tick < stage1Flurry + stage1Pause) {
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.25, 1.0, 0.25));
                    } else if (tick < stage1Flurry + stage1Pause + stage2Kick) {
                        int stageTick = tick - (stage1Flurry + stage1Pause);

                        if (stageTick == 0) {
                            Vec3 dashDir = stage1ForwardDir[0];
                            if (target != null && target != entity) {
                                MovementHelper.lookAt(entity, target.getEyePosition());
                                dashDir = target.getEyePosition().subtract(entity.getEyePosition()).multiply(1, 0, 1);
                            }
                            if (dashDir.lengthSqr() < 0.0001) {
                                dashDir = stage1ForwardDir[0];
                            }
                            if (dashDir.lengthSqr() < 0.0001) {
                                dashDir = new Vec3(0, 0, 1);
                            }
                            stage2DashDir[0] = dashDir.normalize();
                        }
                        Vec3 newVel = stage2DashDir[0].scale(0.85).add(0, 0.08, 0);
                        MovementHelper.setVelocity(entity, new Vec3(newVel.x, entity.getDeltaMovement().y, newVel.z));

                        if (stageTick == 0) {
                            playEntityAnimationOnLayer(entity, "kick_right", 12, 2.0f, 4000);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                                SoundSource.PLAYERS, 1.0F, 1.0F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                                SoundSource.PLAYERS, 0.9F, 1.2F);

                            if (!level.isClientSide) {
                                com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity afterImage =
                                    new com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity(
                                        level, entity, 30, entity.position(), entity.getYRot()
                                    );
                                afterImage.startVisibleWithFade();
                                afterImage.setSwinging(true);
                                playEntityAnimationOnLayer(afterImage, "kick_right", 12, 2.0f, 4000);
                                level.addFreshEntity(afterImage);
                            }

                            AABB kickBox = entity.getBoundingBox().inflate(3.5);
                            List<LivingEntity> kickTargets = level.getEntitiesOfClass(LivingEntity.class, kickBox,
                                e -> e != entity && e.isAlive()
                                    && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity));
                            for (LivingEntity kickTarget : kickTargets) {
                                if (Damager.hurt(entity, kickTarget, damage, true, false, true)) {
                                    Vec3 kbDir = stage2DashDir[0];
                                    if (kbDir.lengthSqr() < 0.0001) {
                                        kbDir = kickTarget.position().subtract(entity.position()).multiply(1, 0, 1);
                                    }
                                    if (kbDir.lengthSqr() < 0.0001) {
                                        kbDir = new Vec3(0, 0, 1);
                                    }
                                    kbDir = kbDir.normalize();
                                    MovementHelper.addVelocity(kickTarget, kbDir.x * 1.1, 0.35, kbDir.z * 1.1);
                                    kickTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                                }
                            }

                            if (serverLevel != null) {
                                Vec3 pos = entity.position().add(0, 1.0, 0);
                                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                                    pos.x, pos.y, pos.z, 6, 0.8, 0.3, 0.8, 0.0);
                                serverLevel.sendParticles(ParticleTypes.FLASH,
                                    pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.0);
                            }
                        }
                    } else if (tick < stage1Flurry + stage1Pause + stage2Kick + stage3Stab) {
                        int stageTick = tick - (stage1Flurry + stage1Pause + stage2Kick);

                        if (stageTick == 0) {
                        	stage2DashDir[0] = entity.getLookAngle();
                            Vec3 dashDir = stage2DashDir[0];
                            if (target != null && target != entity) {
                                MovementHelper.lookAt(entity, target.getEyePosition());
                                dashDir = target.getEyePosition().subtract(entity.getEyePosition()).multiply(1, 0, 1);
                            }
                            if (dashDir.lengthSqr() < 0.0001) {
                                dashDir = stage2DashDir[0];
                            }
                            if (dashDir.lengthSqr() < 0.0001) {
                                dashDir = stage1ForwardDir[0];
                            }
                            if (dashDir.lengthSqr() < 0.0001) {
                                dashDir = new Vec3(0, 0, 1);
                            }
                            stage3DashDir[0] = dashDir.normalize();
                            stage4DashDir[0] = stage3DashDir[0];
                        }

                        Vec3 newVel = stage3DashDir[0].scale(1).add(0, 0.12, 0);
                        MovementHelper.setVelocity(entity, new Vec3(newVel.x, entity.getDeltaMovement().y + 0.1, newVel.z));

                        if (stageTick == 0) {
                            playEntityAnimationOnLayer(entity, "speed_attack_sword", 20, 1.25f, 4000);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                                SoundSource.PLAYERS, 1.2F, 0.9F);

                            if (serverLevel != null) {
                                Vec3 origin = entity.position().add(0, entity.getEyeHeight() * 0.75, 0);
                                for (double d = 0.5; d <= 10.0; d += 0.65) {
                                    Vec3 p = origin.add(stage3DashDir[0].scale(d));
                                    serverLevel.sendParticles(new DustParticleOptions(WHITE, 1.0f),
                                        p.x, p.y, p.z, 2, 0.02, 0.02, 0.02, 0.0);
                                    if (((int) (d * 10)) % 13 == 0) {
                                        serverLevel.sendParticles(ParticleTypes.FLASH,
                                            p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.0);
                                    }
                                }
                            }

                            AABB stabBox = entity.getBoundingBox().expandTowards(stage3DashDir[0].scale(7.0)).inflate(2.5);
                            List<LivingEntity> stabTargets = level.getEntitiesOfClass(LivingEntity.class, stabBox,
                                e -> e != entity && e.isAlive()
                                    && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity));
                            for (LivingEntity stabTarget : stabTargets) {
                                if (Damager.hurt(entity, stabTarget, damage * 1.25f, true, false, true)) {
                                    Vec3 kbDir = stabTarget.position().subtract(entity.position()).multiply(1, 0, 1);
                                    if (kbDir.lengthSqr() < 0.0001) {
                                        kbDir = stage3DashDir[0];
                                    }
                                    kbDir = kbDir.normalize();
                                    MovementHelper.addVelocity(stabTarget, kbDir.x * 1.25, 0.45, kbDir.z * 1.25);
                                }
                            }
                        }
                    } else if (tick < stage1Flurry + stage1Pause + stage2Kick + stage3Stab + stage3Pause) {
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.25, 1.0, 0.25));
                        playEntityAnimation(entity, "sword_to_upper");
                        stage4DashDir[0] = entity.getLookAngle();
                    } else if (tick < totalTicks) {
                        int stageTick = tick - (stage1Flurry + stage1Pause + stage2Kick + stage3Stab + stage3Pause);
                        Vec3 finalDir = stage4DashDir[0];
                        if (finalDir.lengthSqr() < 0.0001) {
                            finalDir = stage3DashDir[0];
                        }
                        if (finalDir.lengthSqr() < 0.0001) {
                            finalDir = stage2DashDir[0];
                        }
                        if (finalDir.lengthSqr() < 0.0001) {
                            finalDir = stage1ForwardDir[0];
                        }
                        if (finalDir.lengthSqr() < 0.0001) {
                            finalDir = new Vec3(0, 0, 1);
                        }
                        finalDir = finalDir.normalize();
                        stage4DashDir[0] = finalDir;

                        
                        Vec3 newVel = finalDir.scale(0.9).add(0, Math.max(entity.getDeltaMovement().y, -0.1), 0);
                        MovementHelper.setVelocity(entity, newVel.x, entity.getDeltaMovement().y, newVel.z);

                        if (stageTick == stage4FinalSlash - 3) {
                            playEntityAnimationOnLayer(entity, "sword_overhead", 10, 3.0f, 4000);
                            AABB attackBox = entity.getBoundingBox().inflate(4);
                            List<LivingEntity> finalTargets = level.getEntitiesOfClass(LivingEntity.class,
                                attackBox, e -> e != entity && e.isAlive()
                                    && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity));

                            for (LivingEntity finalTarget : finalTargets) {
                                if (Damager.hurt(entity, finalTarget, damage * 1.75f, true, false, true)) {
                                    MovementHelper.setVelocity(finalTarget, stage4DashDir[0].scale(0.45).add(0, 0.9f, 0));
                                    finalTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
                                    finalTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                                }
                            }

                            if (serverLevel != null) {
                                Vec3 pos = entity.position();
                                float yawRad = (float) Math.toRadians(entity.getYRot());
                                double pitchRad = 0;
                                Vec3 posOffset = new Vec3(0, 1.5f, 0);
                                Vec3 arcPos = entity.position().add(posOffset);

                                int arcLength = 160;
                                double angle = 0;
                                ParticleHelper.spawnHorizontalArc(serverLevel, arcPos, yawRad, pitchRad,
                                    4, 0.8, arcLength, 10, angle, ParticleTypes.SWEEP_ATTACK, 4);

                                ParticleHelper.spawnHorizontalArc(serverLevel, arcPos, yawRad, pitchRad,
                                    5, 0.1, arcLength, 10, angle,
                                    new DustParticleOptions(PINK_PETAL, (float) (Math.random() * 0.7f + 0.2f)),
                                    3);
                                BonePositionTracker.sendRawHorizontalSlashToClients(
                                    level,
                                    posOffset.add(0, -1, 0),
                                    modelKey,
                                    (float) angle,
                                    false,
                                    arcLength,
                                    100,
                                    0,
                                    0,
                                    (float) angle * 10,
                                    1.5f,
                                    2.1f,
                                    15,
                                    entity.getUUID(),
                                    "sword_to_right"
                                );

                                serverLevel.sendParticles(
                                    ParticleTypes.FLASH,
                                    pos.x, pos.y + 1.0, pos.z,
                                    5, 0.3, 0.3, 0.3, 0
                                );

                                for (int i = 0; i < 8; i++) {
                                    double ringAngle = i * Math.PI * 2 / 8;
                                    double radius = 3.0;
                                    serverLevel.sendParticles(
                                        ParticleTypes.FLASH,
                                        pos.x + Math.cos(ringAngle) * radius,
                                        pos.y + 0.5,
                                        pos.z + Math.sin(ringAngle) * radius,
                                        1, 0, 0, 0, 0
                                    );
                                }

                                for (int i = 0; i < 40; i++) {
                                    double randomAngle = Math.random() * Math.PI * 2;
                                    double dist = Math.random() * 5.0;
                                    double height = Math.random() * 3.0;
                                    serverLevel.sendParticles(
                                        ParticleTypes.END_ROD,
                                        pos.x + Math.cos(randomAngle) * dist,
                                        pos.y + height,
                                        pos.z + Math.sin(randomAngle) * dist,
                                        1, 0, 0.1, 0, 0.05
                                    );
                                }

                                for (int ring = 0; ring < 8; ring++) {
                                    double radius = 1.0 + ring * 1.2;
                                    int particlesInRing = 24 + ring * 4;
                                    for (int i = 0; i < particlesInRing; i++) {
                                        double ringAngle = (i * Math.PI * 2 / particlesInRing) + (ring * 0.2);
                                        double x = pos.x + Math.cos(ringAngle) * radius;
                                        double z = pos.z + Math.sin(ringAngle) * radius;
                                        double y = pos.y + 0.3 + ring * 0.3;

                                        Vector3f color = (ring % 3 == 0)
                                            ? PINK_PETAL
                                            : (ring % 3 == 1) ? MAGENTA_PETAL : PEACH_PETAL;
                                        float size = 2.0f - ring * 0.15f;

                                        serverLevel.sendParticles(
                                            new DustParticleOptions(color, size),
                                            x, y, z, 3, 0.1, 0.2, 0.1, 0.15
                                        );
                                    }
                                }

                                for (int i = 0; i < 80; i++) {
                                    double randomAngle = Math.random() * Math.PI * 2;
                                    double pitch = (Math.random() - 0.5) * Math.PI;
                                    double dist = Math.random() * 4.0;
                                    double x = pos.x + Math.cos(randomAngle) * Math.cos(pitch) * dist;
                                    double y = pos.y + 1.0 + Math.sin(pitch) * dist;
                                    double z = pos.z + Math.sin(randomAngle) * Math.cos(pitch) * dist;

                                    Vector3f color = (i % 4 == 0)
                                        ? PINK_PETAL
                                        : (i % 4 == 1) ? MAGENTA_PETAL : (i % 4 == 2) ? PEACH_PETAL : WHITE;

                                    serverLevel.sendParticles(
                                        new DustParticleOptions(color, 1.5f + (float) (Math.random() * 0.5f)),
                                        x, y, z, 2, 0.2, 0.2, 0.2, 0.2
                                    );
                                }

                                for (int spiral = 0; spiral < 4; spiral++) {
                                    for (int i = 0; i < 20; i++) {
                                        double t = i / 20.0;
                                        double spiralAngle = t * Math.PI * 4 + spiral * Math.PI / 2;
                                        double spiralRadius = t * 5.0;
                                        double x = pos.x + Math.cos(spiralAngle) * spiralRadius;
                                        double z = pos.z + Math.sin(spiralAngle) * spiralRadius;
                                        double y = pos.y + t * 4.0;

                                        serverLevel.sendParticles(
                                            new DustParticleOptions(MAGENTA_PETAL, 1.0f),
                                            x, y, z, 1, 0, 0, 0, 0
                                        );
                                    }
                                }

                                for (int i = 0; i < 40; i++) {
                                    double randomAngle = Math.random() * Math.PI * 2;
                                    double dist = Math.random() * 5.0;
                                    serverLevel.sendParticles(
                                        ParticleTypes.EXPLOSION,
                                        pos.x + Math.cos(randomAngle) * dist,
                                        pos.y + 0.1,
                                        pos.z + Math.sin(randomAngle) * dist,
                                        1, 0, 0, 0, 0
                                    );
                                }

                                serverLevel.sendParticles(
                                    ParticleTypes.TOTEM_OF_UNDYING,
                                    pos.x, pos.y + 1.5, pos.z,
                                    50, 1.5, 1.0, 1.5, 0.5
                                );
                            }

                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 0.9F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                                SoundSource.PLAYERS, 1.0F, 1.1F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                                SoundSource.PLAYERS, 1.5F, 0.8F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE,
                                SoundSource.PLAYERS, 0.8F, 1.5F);
                        }
                    }

                    tickCounter[0]++;
                }, 1, totalTicks);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    MovementHelper.resetStepHeight(entity);
                }, totalTicks + 5);
            }
        );
    }
    /**
     * Eighth Form: Flowing Sakura Petal Onslaught (Hashira Exclusive)
     * The user starts with a flourishing bombardment of slashes while using speed and skill
     * to outrun their opponent, constantly creating flowing arcs of strikes upon the opponent
     * as they jump, leap and dodge around. Their agility and speed blur their motion as they
     * strike with an onslaught of flowing attacks like a tornado of sakura petals.
     */
    public static BreathingForm eighthForm() {
        return new BreathingForm(
            24008,
            "Eighth Form: Flowing Sakura Petal Onslaught",
            "A tornado of sakura petals, overwhelming foes with countless flowing strikes",
            10, // 10 second cooldown
            (entity, level, formId) -> {
                GuardStateHelper.setGuardState(entity, 15.0, formId);
                playEntityAnimation(entity, "ragnaraku1");
                setCancelAttackSwing(entity, true);
                
                final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
						"sword_to_left_reverse", "sword_to_right_reverse" };

                // Enhanced agility and speed
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 3));
                entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 2));

                Vec3 centerPos = entity.position();
                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;

                // Create tornado effect
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = (int) entity.getPersistentData().getDouble("flower_form8_tick");
                    double progress = tick / 60.0;

                    // Circular motion around original position
                    double angle = progress * Math.PI * 8; // Multiple rotations
                    double radius = 3.0;
                    Vec3 targetPos = centerPos.add(
                        Math.cos(angle) * radius,
                        Math.sin(progress * Math.PI * 4) * 2.0, // Up and down
                        Math.sin(angle) * radius
                    );

                    // Move towards target position
                    Vec3 currentPos = entity.position();
                    Vec3 moveVec = targetPos.subtract(currentPos).normalize().scale(0.8);
                    MovementHelper.setVelocity(entity, moveVec);

                    // Tornado damage - hits everything in radius
                    if (tick % 2 == 0) { // Damage every 2 ticks
                    	String slashAnim = animations[(int)(animations.length*Math.random())];
                    	playEntityAnimationOnLayer(entity, slashAnim, 10, 3.0f, 4000);
                    	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.PLAYERS, 1.0F, 1.3F);
                    	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                            SoundSource.PLAYERS, 0.85F, 1.0F);
                    	
	                    AABB tornadoBox = new AABB(centerPos.add(-4, -2, -4), centerPos.add(4, 4, 4));
	                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, tornadoBox,
	                        e -> e != entity && e.isAlive());
	
	                    for (LivingEntity target : targets) {
	                            float damage = (6.0F);
	                            Damager.hurt(entity, target, damage);
	
	                            // Pull towards tornado center
	                            Vec3 pullVec = centerPos.subtract(target.position()).normalize().scale(0.3);
	                            MovementHelper.setVelocity(target, pullVec.add(0, 0.1, 0));
	                    }

                        if (serverLevel != null) {
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

                    // Sakura petal tornado particles (sequential layered vortex, not all-at-once burst)
                    if (serverLevel != null) {
                        final int ringCount = 16;
                        final int ringRevealInterval = 2; // reveal one more layer every 2 ticks
                        final int activeRings = Math.min(ringCount, 1 + tick / ringRevealInterval);
                        final int segmentsPerTick = 3; // sequential samples per layer each tick

                        for (int ring = 0; ring < activeRings; ring++) {
                            double layerSpeed = 0.18 + ring * 0.028;
                            double layerOffset = ring * (Math.PI / 3.0);
                            double ringRadius = 2.0 + (ring * 0.42);
                            double baseHeight = centerPos.y + (ring * 0.48);

                            for (int segment = 0; segment < segmentsPerTick; segment++) {
                                double segmentOffset = segment * (Math.PI * 2.0 / segmentsPerTick);
                                double particleAngle = (tick * layerSpeed) + layerOffset + segmentOffset;
                                double yWave = Math.sin((tick * 0.15) + (ring * 0.55) + segmentOffset) * 0.2;
                                double x = centerPos.x + Math.cos(particleAngle) * ringRadius;
                                double z = centerPos.z + Math.sin(particleAngle) * ringRadius;
                                double y = baseHeight + yWave;

                                serverLevel.sendParticles(
                                    new DustParticleOptions(PINK_PETAL, 1.4f + (ring * 0.03f)),
                                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0
                                );

                                serverLevel.sendParticles(
                                    new DustParticleOptions(MAGENTA_PETAL, 1.1f + (ring * 0.02f)),
                                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0
                                );
                            }
                        }
                    }

                    entity.getPersistentData().putDouble("flower_form8_tick", tick + 1);
                }, 1, 60);

                // Sound effect
                level.playSound(null, entity.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS, 0.6F, 1.8F);
                level.playSound(null, entity.blockPosition(), SoundEvents.ELYTRA_FLYING,
                        SoundSource.PLAYERS, 2.0F, 0.5F);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    if (level instanceof ServerLevel stopSoundLevel) {
                        net.minecraft.resources.ResourceLocation soundId =
                            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.ELYTRA_FLYING);
                        for (ServerPlayer serverPlayer : stopSoundLevel.players()) {
                            if (serverPlayer.distanceToSqr(entity) <= 96.0 * 96.0) {
                                serverPlayer.connection.send(
                                    new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(soundId, SoundSource.PLAYERS)
                                );
                            }
                        }
                    }
                    entity.getPersistentData().remove("flower_form8_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, 100);
            }
        );
    }

    /**
     * Ninth Form: Sudden Bamboo Entrapment (Hashira Exclusive)
     * The user pulls back into a thrusting stance before launching forward with speed and precision,
     * dancing around their opponent while applying piercing strikes all around the opponent's body,
     * maneuvering with speed and agility that creates a blur of fast-paced piercing stabs and strikes,
     * overwhelming the opponent before using the back of the katana to knock them back, seamlessly
     * following up with a decapitating piercing stab to the neck.
     */
    public static BreathingForm ninthForm() {
        return new BreathingForm(
            24009,
            "Ninth Form: Sudden Bamboo Entrapment",
            "Blurring piercing strikes surround and overwhelm, finishing with a precise neck thrust",
            12, // 12 second cooldown
            (entity, level, formId) -> {
                final float damage = (9.0F);
                GuardStateHelper.setGuardState(entity, 16.0, formId);
                setCancelAttackSwing(entity, true);

                final int chargeUp = 10;
                final int circling = 20;
                final int pauseAfterCircling = 10;
                final int hiltLunge = 10;
                final int pauseAfterHilt = 10;
                final int finalLunge = 10;
                final int totalTicks = chargeUp + circling + pauseAfterCircling + hiltLunge + pauseAfterHilt + finalLunge;
                final int[] tickCounter = {0};

                final String[] circlingAnimations = {
                    "sword_to_left",
                    "sword_to_right",
                    "sword_to_upper",
                    "sword_to_left_reverse",
                    "sword_to_right_reverse"
                };
                final int[] slashIndex = {0};

                final Vec3 lookVec = entity.getLookAngle();
                final float range = 5.0f;
                final Vec3[] tPoint = {entity.getEyePosition().add(lookVec.scale(range))};
                final Vec3[] postCircleForward = {entity.getLookAngle().multiply(1, 0, 1)};
                AABB bb = new AABB(tPoint[0].subtract(3, 3, 3), tPoint[0].add(3, 3, 3));
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bb,
                        e -> e != entity && e.isAlive());
                final LivingEntity[] tt = {null};
                if (targets.size() > 0) {
                    tPoint[0] = targets.get(0).getEyePosition();
                    tt[0] = targets.get(0);
                }

                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel) level : null;
                final net.minecraft.resources.ResourceLocation bambooId =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "bamboo");
                final net.minecraft.world.entity.EntityType<?> bambooType =
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(bambooId);

                MovementHelper.setStepHeight(entity, 3);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5));
                playEntityAnimation(entity, "kaishin3");
                level.playSound(null, entity.blockPosition(), SoundEvents.BAMBOO_WOOD_STEP,
                    SoundSource.PLAYERS, 1.0F, 0.75F);

                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = tickCounter[0];
                    LivingEntity target = (tt[0] != null && tt[0].isAlive()) ? tt[0] : null;
                    if (target != null) {
                        tPoint[0] = target.getEyePosition();
                    }

                    if (tick < chargeUp) {
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.35, 0.0, 0.35));
                    } else if (tick < chargeUp + circling) {
                        int circleTick = tick - chargeUp;
                        Vec3 center = target != null
                            ? target.position().add(0, target.getEyeHeight() * 0.6, 0)
                            : tPoint[0].subtract(0, 0.6, 0);

                        double angle = (circleTick / (double) circling) * Math.PI * 4.0; // two circles in 20 ticks
                        double radius = 6.0;
                        Vec3 orbitPos = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);

                        Vec3 desired = orbitPos.subtract(entity.position());
                        Vec3 velocity = desired.scale(0.6);
                        if (velocity.lengthSqr() > 4.0) {
                            velocity = velocity.normalize().scale(2.0);
                        }
                        MovementHelper.setVelocity(entity, velocity.x, Math.max(entity.getDeltaMovement().y, -0.05), velocity.z);
                        MovementHelper.lookAtNoY(entity, center.add(0, 0.8, 0));

                        if (serverLevel != null) {
                            int ringPoints = 20;
                            for (int i = 0; i < ringPoints; i++) {
                                double ringAngle = (i / (double) ringPoints) * Math.PI * 2.0;
                                Vec3 ringPos = center.add(Math.cos(ringAngle) * radius, 0.12, Math.sin(ringAngle) * radius);
                                Vector3f ringColor = (i % 2 == 0) ? PEACH_PETAL : PINK_PETAL;
                                serverLevel.sendParticles(
                                    new DustParticleOptions(ringColor, 0.85f),
                                    ringPos.x, ringPos.y, ringPos.z,
                                    1, 0.01, 0.01, 0.01, 0.0
                                );
                            }

                            serverLevel.sendParticles(
                                new DustParticleOptions(WHITE, 1.0f),
                                orbitPos.x, orbitPos.y + 0.15, orbitPos.z,
                                2, 0.02, 0.02, 0.02, 0.0
                            );
                        }

                        if (circleTick % 5 == 0) {
                            String anim = circlingAnimations[slashIndex[0] % circlingAnimations.length];
                            slashIndex[0]++;
                            playEntityAnimationOnLayer(entity, anim, 10, 3.0f, 4000);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 1.2F);

                            AABB slashBox = new AABB(center.subtract(4.0, 2.0, 4.0), center.add(4.0, 2.0, 4.0));
                            List<LivingEntity> slashTargets = level.getEntitiesOfClass(LivingEntity.class, slashBox,
                                e -> e != entity && e.isAlive());
                            for (LivingEntity slashTarget : slashTargets) {
                                if (Damager.hurt(entity, slashTarget, damage * 0.55555555f, true, false, true)) {
                                    slashTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                                }
                            }

                            if (serverLevel != null) {
                                for (int i = 0; i < 2; i++) {
                                    double spawnAngle = Math.random() * Math.PI * 2;
                                    double spawnRadius = Math.random() * 3.8;
                                    Vec3 bambooVec = center.add(Math.cos(spawnAngle) * spawnRadius, 0, Math.sin(spawnAngle) * spawnRadius);
                                    BlockPos bambooPos = BlockPos.containing(bambooVec.x, center.y, bambooVec.z);

                                    if (bambooType != null) {
                                        net.minecraft.world.entity.Entity bamboo = bambooType.create(level);
                                        if (bamboo != null) {
                                            bamboo.setPos(bambooPos.getX() + 0.5, bambooPos.getY(), bambooPos.getZ() + 0.5);
                                            level.addFreshEntity(bamboo);
                                        }
                                    }

                                    serverLevel.sendParticles(
                                        ParticleTypes.COMPOSTER,
                                        bambooPos.getX() + 0.5, bambooPos.getY() + 1, bambooPos.getZ() + 0.5,
                                        10, 0.3, 0.5, 0.3, 0.1
                                    );
                                    serverLevel.sendParticles(
                                        ParticleTypes.WAX_ON,
                                        bambooPos.getX() + 0.5, bambooPos.getY() + 0.5, bambooPos.getZ() + 0.5,
                                        3, 0.2, 0.2, 0.2, 0.01
                                    );
                                }
                                serverLevel.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z,
                                    1, 0.05, 0.05, 0.05, 0.0);
                            }
                        }
                    } else if (tick < chargeUp + circling + pauseAfterCircling) {
                        if (tick == chargeUp + circling) {
                            // Lock direction at the end of circling and keep it for all later stages.
                            Vec3 lockedForward = entity.getLookAngle().multiply(1, 0, 1);
                            if (lockedForward.lengthSqr() < 0.0001) {
                                float yawAtLock = (float) Math.toRadians(-entity.getYRot());
                                lockedForward = new Vec3(Math.sin(yawAtLock), 0, Math.cos(yawAtLock));
                            }
                            if (lockedForward.lengthSqr() < 0.0001) {
                                lockedForward = new Vec3(0, 0, 1);
                            }
                            postCircleForward[0] = lockedForward.normalize();
                        }
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.22, 1.0, 0.22));
                    } else if (tick < chargeUp + circling + pauseAfterCircling + hiltLunge) {
                        int hiltTick = tick - (chargeUp + circling + pauseAfterCircling);
                        Vec3 forward = postCircleForward[0];
                        if (forward.lengthSqr() < 0.0001) {
                            float yaw = (float) Math.toRadians(-entity.getYRot());
                            forward = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                        }

                        if (hiltTick == 0) {
                            playEntityAnimationOnLayer(entity, "sword_hilt", 30, 1.0f, 4000);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                                SoundSource.PLAYERS, 1.0F, 1.0F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                                SoundSource.PLAYERS, 1.0F, 0.7F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.SHULKER_SHOOT,
                                SoundSource.PLAYERS, 0.8F, 0.8F);

                            if (serverLevel != null) {
                                Vec3 origin = entity.position().add(0, 1.0, 0);
                                for (double d = 1.0; d <= 7.0; d += 1.2) {
                                    Vec3 p = origin.add(forward.scale(d));
                                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z,
                                        1, 0.15, 0.15, 0.15, 0.0);
                                    serverLevel.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z,
                                        1, 0.01, 0.01, 0.01, 0.0);
                                }
                            }

                            AABB hitbox = entity.getBoundingBox().expandTowards(forward.scale(4.0)).inflate(3.5);
                            List<LivingEntity> hiltTargets = level.getEntitiesOfClass(
                                LivingEntity.class,
                                hitbox,
                                e -> e != entity && e.isAlive()
                            );
                            for (LivingEntity hiltTarget : hiltTargets) {
                                Damager.hurt(entity, hiltTarget, damage * 1.11111111f, true, false, true);
                                Vec3 direction = hiltTarget.position().subtract(entity.position()).normalize();
                                MovementHelper.addVelocity(hiltTarget, direction.x * 1.2, 0.5, direction.z * 1.2);
                                hiltTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                            }
                        }

                        MovementHelper.setVelocity(entity, forward.scale(1.9).add(0, 0.12, 0));
                    } else if (tick < chargeUp + circling + pauseAfterCircling + hiltLunge + pauseAfterHilt) {
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.18, 1.0, 0.18));
                        postCircleForward[0] = entity.getLookAngle();
                    } else if (tick < totalTicks) {
                        int finalTick = tick - (chargeUp + circling + pauseAfterCircling + hiltLunge + pauseAfterHilt);
                        Vec3 forward = postCircleForward[0].normalize();
                        if (forward.lengthSqr() < 0.0001) {
                            float yaw = (float) Math.toRadians(-entity.getYRot());
                            forward = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                        }

                        if (finalTick == 0) {
                            playEntityAnimationOnLayer(entity, "speed_attack_sword", 20, 1.25f, 4000);
                            MovementHelper.setVelocity(entity, forward.scale(0.8).add(0, 0.15, 0));
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1.0F, 1.4F);
                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                                SoundSource.PLAYERS, 1.2F, 0.8F);

                            if (serverLevel != null) {
                                Vec3 origin = entity.position().add(0, entity.getEyeHeight() * 0.75, 0);
                                for (double d = 0.5; d <= 11.0; d += 0.6) {
                                    Vec3 p = origin.add(forward.scale(d));
                                    serverLevel.sendParticles(
                                        new DustParticleOptions(WHITE, 1.1f),
                                        p.x, p.y, p.z, 2, 0.02, 0.02, 0.02, 0.0
                                    );
                                    if (((int) (d * 10)) % 12 == 0) {
                                        serverLevel.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z,
                                            1, 0.01, 0.01, 0.01, 0.0);
                                    }
                                }
                            }

                            AABB finalBox = entity.getBoundingBox().expandTowards(forward.scale(9.0)).inflate(2.8);
                            List<LivingEntity> finalTargets = level.getEntitiesOfClass(LivingEntity.class, finalBox,
                                e -> e != entity && e.isAlive());
                            for (LivingEntity finalTarget : finalTargets) {
                                if (Damager.hurt(entity, finalTarget, damage * 1.5555555555f, true, false, true)) {
                                    Vec3 direction = finalTarget.position().subtract(entity.position()).normalize();
                                    MovementHelper.addVelocity(finalTarget, direction.x * 1.6, 0.6, direction.z * 1.6);
                                    finalTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                                }
                            }
                        } else {
                            MovementHelper.setVelocity(entity, forward.scale(2.2).add(0, Math.max(entity.getDeltaMovement().y, 0.0), 0));
                        }
                    }

                    tickCounter[0]++;
                }, 1, totalTicks);

                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    MovementHelper.resetStepHeight(entity);
                }, totalTicks + 5);
            }
        );
    }

    /**
     * Final Form: Equinoctial Vermilion Eye
     * A focusing technique that raises the user's kinetic vision to its maximum
     * as a last resort. When unleashed, the user's sclera turn red and they
     * perceive the world as if it were in slow motion, unleashing a barrage of
     * impossibly fast slashes that create a dome of vermilion flower petals.
     * However, the tremendous strain on the eyes could cause blood vessels to
     * rupture and lead to partial or permanent blindness.
     */
    public static BreathingForm finalForm() {
        return new BreathingForm(
            24010,
            "Final Form: Equinoctial Vermilion Eye",
            "Maximizes kinetic vision to unleash a dome of lethal flower petals at the cost of eyesight",
            15, // 15 second cooldown
            (entity, level, formId) -> {
            	
            	if(entity.hasEffect(ModEffects.VERMILION_EYE.get()))
            		return;
            	
                GuardStateHelper.setGuardState(entity, 20.0, formId);
                playEntityAnimation(entity, "kamusari1");
                //setCancelAttackSwing(entity, true);
                
                final int totalTicks = 20 * 60 * 3; // 3 minutes

                // Vermilion Eye activation - enhanced combat abilities
                //entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                //entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, totalTicks, 2, false, false));
                entity.addEffect(new MobEffectInstance(ModEffects.VERMILION_EYE.get(), totalTicks, 0));

                Vec3 centerPos = entity.position();

                // Create massive petal dome with vermilion eye effects
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    int tick = (int) entity.getPersistentData().getDouble("flower_formF_tick");

                    // Dome expands outward
                    double domeRadius = 3.0 + (tick / 60.0) * 2.0; // Grows from 3 to 5 blocks

                        if (tick == 12) {
                            // Damage all enemies in dome
                            AABB domeBox = new AABB(
                                    centerPos.add(-domeRadius, -2, -domeRadius),
                                    centerPos.add(domeRadius, 4, domeRadius));

                            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, domeBox,
                                    e -> e != entity && e.isAlive());

                            for (LivingEntity target : targets) {

                                    float damage = (17.0F);
                                    Damager.hurt(entity, target, damage);

                                    // Crushing pressure
                                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
                                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                            }
                        }

                    // Vermilion petal dome particles + eye glow
                    if (level instanceof ServerLevel serverLevel) {
                        // Vermilion dome shell
                        for (int lat = 0; lat < 180; lat += 20) {
                            for (int lon = 0; lon < 360; lon += 20) {
                                double latRad = Math.toRadians(lat);
                                double lonRad = Math.toRadians(lon);

                                double x = centerPos.x + domeRadius * Math.sin(latRad) * Math.cos(lonRad);
                                double y = centerPos.y + 1.0 + domeRadius * Math.cos(latRad);
                                double z = centerPos.z + domeRadius * Math.sin(latRad) * Math.sin(lonRad);

                                // Vermilion (red-orange) particles
                                serverLevel.sendParticles(
                                    new DustParticleOptions(new Vector3f(227f/255f, 66f/255f, 52f/255f), 1.5f),
                                    x, y, z, 1, 0, 0, 0, 0
                                );
                            }
                        }

                        // Inner swirl particles
                        double angle = tick * 0.3;
                        for (int i = 0; i < 10; i++) {
                            double spiralAngle = angle + (i * Math.PI * 2 / 10);
                            double spiralRadius = domeRadius * 0.6;
                            double x = centerPos.x + Math.cos(spiralAngle) * spiralRadius;
                            double z = centerPos.z + Math.sin(spiralAngle) * spiralRadius;

                            serverLevel.sendParticles(
                                new DustParticleOptions(PINK_PETAL, 1.0f),
                                x, centerPos.y + 1.5, z, 2, 0.2, 0.2, 0.2, 0.05
                            );
                        }

                        // Vermilion eye glow near user's head
                        Vec3 headPos = entity.position().add(0, entity.getEyeHeight(), 0);
                        for (int i = 0; i < 2; i++) {
                            double offsetX = (Math.random() - 0.5) * 0.3;
                            double offsetY = (Math.random() - 0.5) * 0.2;
                            double offsetZ = (Math.random() - 0.5) * 0.3;

                            serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(200f/255f, 30f/255f, 30f/255f), 0.6f),
                                headPos.x + offsetX, headPos.y + offsetY, headPos.z + offsetZ,
                                1, 0, 0, 0, 0
                            );
                        }

                        // Intensifying strain - red drip particles in second half
                        if (tick > 30 && tick % 10 == 0) {
                            serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(180f/255f, 10f/255f, 10f/255f), 0.4f),
                                headPos.x, headPos.y - 0.1, headPos.z,
                                2, 0.15, 0.05, 0.15, 0.01
                            );
                        }
                    }

                    entity.getPersistentData().putDouble("flower_formF_tick", tick + 1);
                }, 1, 60);

                // Sound effects
                level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 0.8F, 1.5F);

                // Cleanup + eye strain penalty
                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("flower_formF_tick");
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                }, 100);
                
                // Cleanup + eye strain penalty
                AbilityScheduler.scheduleOnce(entity, () -> {
                    entity.getPersistentData().remove("flower_formF_tick");
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalTicks+1);
            }
        );
    }

    /**
     * Creates the Flower Breathing technique for a generic (base) sword.
     * Includes forms 1-6 only. No Final Form.
     */
    public static BreathingTechnique createGenericFlowerBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());   // 1301 - Flowing Tiger Lily Petals
        forms.add(secondForm());  // 1302 - Honorable Shadow Plum
        forms.add(thirdForm());   // 1303 - Scattering Rose-Peach Thorns
        forms.add(fourthForm());  // 1304 - Crimson Hanagoromo
        forms.add(fifthForm());   // 1305 - Peonies of Futility
        forms.add(sixthForm());   // 1306 - Whirling Peach

        // Pink palette for Flower Breathing
        return new BreathingTechnique("Flower Breathing", forms, "§d", "§d");
    }

    /**
     * Creates the Flower Breathing technique for Kanawo's sword.
     * Includes forms 1-6 plus the Final Form: Equinoctial Vermilion Eye.
     */
    public static BreathingTechnique createKanawoFlowerBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());   // 1301 - Flowing Tiger Lily Petals
        forms.add(secondForm());  // 1302 - Honorable Shadow Plum
        forms.add(thirdForm());   // 1303 - Scattering Rose-Peach Thorns
        forms.add(fourthForm());  // 1304 - Crimson Hanagoromo
        forms.add(fifthForm());   // 1305 - Peonies of Futility
        forms.add(sixthForm());   // 1306 - Whirling Peach
        forms.add(finalForm());   // 1310 - Equinoctial Vermilion Eye

        // Pink palette for Flower Breathing
        return new BreathingTechnique("Flower Breathing", forms, "§d", "§d");
    }

    /**
     * Creates the complete Flower Breathing technique for Hashira-level sword (Kanae).
     * Includes all forms (1-9) plus Final Form.
     */
    public static BreathingTechnique createHashiraFlowerBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());    // 1301 - Flowing Tiger Lily Petals
        forms.add(secondForm());   // 1302 - Honorable Shadow Plum
        forms.add(thirdForm());    // 1303 - Scattering Rose-Peach Thorns
        forms.add(fourthForm());   // 1304 - Crimson Hanagoromo
        forms.add(fifthForm());    // 1305 - Peonies of Futility
        forms.add(sixthForm());    // 1306 - Whirling Peach
        forms.add(seventhForm());  // 1307 - Bellowing Peach Shockwave
        forms.add(eighthForm());   // 1308 - Flowing Sakura Petal Onslaught
        forms.add(ninthForm());    // 1309 - Sudden Bamboo Entrapment
        forms.add(finalForm());    // 1310 - Equinoctial Vermilion Eye

        // Pink palette for Flower Breathing
        return new BreathingTechnique("Flower Breathing", forms, "§d", "§d");
    }
}
