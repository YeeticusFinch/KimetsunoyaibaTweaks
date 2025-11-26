package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.WhipDamageHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

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
     */
    private static void triggerWhipAnimation(LivingEntity entity, String animationName, double extension) {
        if (!(entity instanceof Player player)) {
            return; // Whip physics only work for players
        }

        // Send whip animation packet to all clients
        if (!entity.level().isClientSide) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket packet =
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.WhipAnimationPacket(
                    player.getUUID(),
                    animationName,
                    extension,
                    0 // Start at tick 0
                );
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);
        } else {
            // If called on client (shouldn't happen for forms), apply directly
            // Use DistExecutor to avoid loading client class on server
            final Player finalPlayer = player;
            final String finalAnimName = animationName;
            final double finalExtension = extension;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics.playAnimation(
                    finalPlayer, finalAnimName, finalExtension, 10
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
            "First Form: Shivers of First Love",
            "Multi-directional rapid slashes with the whip",
            5, // 5 second cooldown
            (entity, level) -> {
                // Set guard state
                GuardStateHelper.setGuardState(entity, 8.0, 22001); // ID 21001 for Love Breathing
                
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
                final float baseDamage = 12.0f;

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
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}

                	
                	if (level instanceof ServerLevel serverLevel && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
                		for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                			float x = particlePoints[currentTick[0]][i][0];
                			float y = particlePoints[currentTick[0]][i][1];
                			float z = particlePoints[currentTick[0]][i][2];

                			Vec3 pos = entity.position().add(vectors[0].scale(z).add(vectors[1].scale(x)).add(0, y, 0));

                			// Spawn pink dust particle at pos
                			serverLevel.sendParticles(
                		            new DustParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.2f                           // scale
                		            ),
                		            pos.x, pos.y, pos.z,
                		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                		        );
                			
                			// Spawn white dust particle at pos
                			serverLevel.sendParticles(
                					new DustParticleOptions(
                    		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                    		                1f                           // scale
                    		            ),
                		            pos.x, pos.y, pos.z,
                		            1, 0, 0, 0, 0 // count, velocity
                		        );
                		}
                	}

                	if (currentTick[0] < 26) { // Preparing for the pounce (1.31 seconds)
                		if (currentTick[0] % 5 == 0 && currentTick[0] < 10) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
                				    SoundSource.PLAYERS, 1.0f, 1.5f);
                		}
                	}
                	else if (currentTick[0] < 52) { // Sprinting fast (2.6 seconds)
                		if (currentTick[0] < 27) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 2.0f);
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
                		}
                		if (currentTick[0] < 31 && level instanceof ServerLevel serverLevel) {
                			serverLevel.sendParticles(
            						ModParticles.LOVE_IMPACT.get(),
            						entity.getX(), entity.getY(), entity.getZ(),
            						3, 0.3, 0.3, 0.3, 0
            					);
                		}
                		if (currentTick[0] % 2 == 0) {
                			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                				    SoundEvents.HORSE_STEP, SoundSource.PLAYERS, 1.0f, 2f);
                		}
                		// Make the player zoom forward
                		Vec3 forward = vectors[0].scale(1.4); // Speed boost
                		MovementHelper.setVelocity(entity, new Vec3(forward.x, entity.getDeltaMovement().y, forward.z));
                		//entity.setDeltaMovement(forward.x, entity.getDeltaMovement().y, forward.z);

                		// Collect targets within 6 blocks that are targetable
                		if (level instanceof ServerLevel serverLevel) {
                			AABB searchBox = entity.getBoundingBox().inflate(6.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				if (!targets.contains(target)) {
                					targets.add(target);
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
                	else if (currentTick[0] < 55) { // Floating and spinning (2.75 seconds)
                		// Give the player slow falling
                		if (currentTick[0] == 52) {
                			entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
                		}
                	}
                	else if (currentTick[0] > 55 && currentTick[0] % 2 == 1 && currentTick[0] < 80) { // Sword swing - damage all targets
                		
                		if (currentTick[0] % 3 == 0) {
                			level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        			SoundSource.PLAYERS, 1.5F, 1.3F);
                		}
                		
                		// Damage all collected targets
                		for (LivingEntity target : targets) {
                			if (target.isAlive() && Math.random() < 0.3) {
                				Damager.hurt(entity, target, baseDamage);

                				// Spawn love_impact particles at random offsets
                				if (level instanceof ServerLevel serverLevel) {
                					double targetHeight = target.getBbHeight();
                					for (int i = 0; i < 3; i++) {
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
                						
                						level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "punch1")),
                            				    SoundSource.PLAYERS, 1.0f, 1.5f);
                						
                						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            				    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
                						
                						level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            				    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 2f);
                					}
                				}
                				
                				// Play impact sound
                        		level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        			SoundSource.PLAYERS, 1.5F, 0.8F);
                        		level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
                    				    SoundSource.PLAYERS, 1.0f, 1f);
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
            }
        );
    }

    /**
     * Second Form: Love Pangs (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            "Second Form: Love Pangs",
            "Rapid extension strikes",
            3,
            (entity, level) -> {
                // TODO: Implement second form
            	 // Set guard state
                GuardStateHelper.setGuardState(entity, 8.0, 22002); // ID 21002 for Love Breathing
                
                float baseDamage = 10;

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

                	
                	if (level instanceof ServerLevel serverLevel && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
                		for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                			float x = particlePoints[currentTick[0]][i][0];
                			float y = particlePoints[currentTick[0]][i][1];
                			float z = particlePoints[currentTick[0]][i][2];

                			Vec3 pos = entity.position().add(vectors[0].scale(z).add(vectors[1].scale(x)).add(0, y, 0));

                			// Spawn pink dust particle at pos
                			serverLevel.sendParticles(
                		            new DustParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.2f                           // scale
                		            ),
                		            pos.x, pos.y, pos.z,
                		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                		        );
                			
                			// Spawn white dust particle at pos
                			serverLevel.sendParticles(
                					new DustParticleOptions(
                    		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                    		                1f                           // scale
                    		            ),
                		            pos.x, pos.y, pos.z,
                		            1, 0, 0, 0, 0 // count, velocity
                		        );
                		}
                	}
                	
                	if (currentTick[0] < 12) {
                		// Jump up (0.6 seconds)
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.2).add(0, 0.5f, 0));
                	}
                	else if (currentTick[0] < 40) {
                		// Launch forward and attack (2.9 seconds)
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(0.8));
                		
                		if (level instanceof ServerLevel serverLevel && currentTick[0] % 2 == 0) {
                			AABB searchBox = entity.getBoundingBox().inflate(6.0);
                			List<LivingEntity> nearby = level.getEntitiesOfClass(
                				LivingEntity.class, searchBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);

                			for (LivingEntity target : nearby) {
                				if (Math.random() < 0.3) {
                					Damager.hurt(entity, target, baseDamage);
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
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
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
            "Third Form: Catlove Shower",
            "Overhead rain of whip slashes",
            4,
            (entity, level) -> {
                // TODO: Implement third form
            	 // Set guard state
                GuardStateHelper.setGuardState(entity, 8.0, 22003); // ID 21003 for Love Breathing

                // Play player animation
                playEntityAnimation(entity, "love_third_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_third_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 40; // 2 seconds
                final int[] currentTick = {0};
                final int interval = 1;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
            	
            	
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2); // 3.2 seconds
            }
        );
    }

    /**
     * Fifth Form: Swaying Love, Wildclaw (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            "Fifth Form: Swaying Love, Wildclaw",
            "Wide sweeping arc attack",
            4,
            (entity, level) -> {
                // TODO: Implement fifth form
            	 // Set guard state
                GuardStateHelper.setGuardState(entity, 8.0, 22005); // ID 21001 for Love Breathing

                // Play player animation
                playEntityAnimation(entity, "love_fifth_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_fifth_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 96; // 5.8 seconds
                final int[] currentTick = {0};
                final int interval = 1;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
            	
            	
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2); // 3.2 seconds
            }
        );
    }

    /**
     * Sixth Form: Cat-Legged Winds of Love (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Cat-Legged Winds of Love",
            "Spinning tornado whip attack",
            5,
            (entity, level) -> {
                // TODO: Implement sixth form
            	 // Set guard state
                GuardStateHelper.setGuardState(entity, 8.0, 22006); // ID 21001 for Love Breathing

                // Play player animation
                playEntityAnimation(entity, "love_sixth_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_sixth_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 40; // 2 seconds
                final int[] currentTick = {0};
                final int interval = 1;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
            	
            	
            	
	            	currentTick[0] += interval;
	            }, interval, totalDuration);
                
               
                // Schedule cleanup
                AbilityScheduler.scheduleOnce(entity, () -> {
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                }, totalDuration+2);
            }
        );
    }

    /**
     * Create the complete Love Breathing technique.
     */
    public static BreathingTechnique createLoveBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fifthForm());
        forms.add(sixthForm());

        // Pink colors for Love Breathing (§d = light purple/pink, §5 = dark purple)
        return new BreathingTechnique("Love Breathing", forms, "§d", "§5");
    }

    /**
     * Generic Love Breathing (same as main technique).
     */
    public static BreathingTechnique createGenericLoveBreathing() {
        return createLoveBreathing();
    }
}
