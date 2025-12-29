package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.WhipDamageHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveSwordSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.LoveTornadoSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
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

import java.util.ArrayList;
import java.util.HashMap;
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
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                	}

                	
                	if (serverLevel != null && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
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
                		if (currentTick[0] < 31 && serverLevel != null) {
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
                	if (currentTick[0] > 50 && currentTick[0] < 80 && currentTick[0] % 2 == 0) {
                		if (Config.logDebug)
                			System.out.println("Damaging " + targets.size() + " targets on tick " + currentTick[0] + " ");
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

				final float damage = DamageCalculator.calculateScaledDamage(entity, 10.0F);
            	 // Set guard state (formId auto-injected as 22002)
                GuardStateHelper.setGuardState(entity, 8.0, formId);

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

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                // Spawn love sword slashes visual effect
                Vec3 spawnPos = entity.position().add(0, entity.getEyeHeight(), 0); // Eye level
                Vec3 lookVec = entity.getLookAngle();

                final ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;

                
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
                
                double ogY = entity.position().y; // TODO: Measure the delta Y and add that to the Y position of the love sword slashes
                
                MovementHelper.setVelocity(entity, new Vec3(0, 0.75f, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 5, 10, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, totalDuration+10, 10, false, false));
                
                HashMap<Integer, List<LivingEntity>> targets = new HashMap<Integer, List<LivingEntity>>();
                ArrayList<LivingEntity> hitTargets = new ArrayList<LivingEntity>();
                
                //LoveSwordSlashesEntity[] swordSlashes = {null};
                
                int range = 30;
                
                AbilityScheduler.scheduleRepeating(entity, () -> {

                	if (currentTick[0] % 4 == 0) {
                		// Use yaw rotation to get forward direction (works even when looking up/down)
                		float yaw = (float) Math.toRadians(-entity.getYRot());
                		vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                		// Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
                		vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
                		
                		// Raycast to find collision point (block or entity)
                		Vec3 eyePos = entity.getEyePosition();
                		Vec3 lookAngle = entity.getLookAngle();
                		vectors[2] = eyePos.add(lookAngle.scale(range)); // Default to max range
                		
                		MovementHelper.lookAtTarget(entity);

                		for (int i = 1; i < range; i++) {
                			Vec3 vec = eyePos.add(lookAngle.scale(i));

                			// Check block collision
                			BlockPos blockPos = new BlockPos((int)Math.floor(vec.x), (int)Math.floor(vec.y), (int)Math.floor(vec.z));
                			if (!level.getBlockState(blockPos).isAir()) {
                				vectors[2] = vec;
                				int r = 15;
                    			// Check entity collision for the giant cloud at the bottom
                    			AABB rayBox = new AABB(vec.subtract(r, r, r), vec.add(r, r, r));
                    			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                    				LivingEntity.class, rayBox,
                    				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                    			);
                    			targets.put(i, entitiesInRay);
                    			for (LivingEntity le : entitiesInRay) {
                    				float x = (float)(le.position().x + (Math.random()-0.5)*le.getEyeHeight());
    	                			float y = (float)(le.position().y + (Math.random()-0.5)*le.getEyeHeight());
    	                			float z = (float)(le.position().z + (Math.random()-0.5)*le.getEyeHeight());
    	                			if (serverLevel != null) {
	    		                		serverLevel.sendParticles(
	    		        						ModParticles.LOVE_IMPACT.get(),
	    		        						x, y, z,
	    		        						3, 0.3, 0.3, 0.3, 0
	    		        					);
    	                			}
    		                		
    		        				level.playSound(null, x, y, z,
    		                				   SoundEvents.PLAYER_ATTACK_STRONG,
    		                				    SoundSource.PLAYERS, 1.0f, 1f);
    		        				float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
                    				Damager.hurt(entity, le, damage);
                    			}
                				break; // Stop at first block hit
                			}
                			
                			int r = 5;
                			// Check entity collision
                			AABB rayBox = new AABB(vec.subtract(r, r, r), vec.add(r, r, r));
                			List<LivingEntity> entitiesInRay = level.getEntitiesOfClass(
                				LivingEntity.class, rayBox,
                				e -> e != entity && e.isAlive() && isTargetable(entity, e)
                			);
                			targets.put(i, entitiesInRay);
                			/*
                			if (!entitiesInRay.isEmpty()) {
                				vectors[2] = vec;
                				break; // Stop at first entity hit
                			}
                		*/
                			
                		}
                	}

                	
                	if (serverLevel != null && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
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
                	
                	if (currentTick[0] > 15 && currentTick[0] < 35) {
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, 0), entity.getDeltaMovement().z);
                		if (serverLevel != null) {
                			for (int i = 0; i < 5; i++) {
	                			// Spawn a cloud of sword slashes at the target point
	                			float x = (float)(vectors[2].x + (Math.random()-0.5)*15);
	                			float y = (float)(vectors[2].y + (Math.random()-0.5)*15);
	                			float z = (float)(vectors[2].z + (Math.random()-0.5)*15);
		                		serverLevel.sendParticles(
		        						ModParticles.LOVE_SLASH.get(),
		        						x, y, z,
		        						3, 0.3, 0.3, 0.3, 0
		        					);
		                		
		                		
		                		
		        				level.playSound(null, x, y, z,
		                				    ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("kimetsunoyaiba", "sword_sweep")),
		                				    SoundSource.PLAYERS, 1.0f, 1.5f);
		        				
		        				x = (float)(vectors[2].x + (Math.random()-0.5)*15);
	                			y = (float)(vectors[2].y + (Math.random()-0.5)*15);
	                			z = (float)(vectors[2].z + (Math.random()-0.5)*15);
		                		serverLevel.sendParticles(
		        						ParticleTypes.CAMPFIRE_COSY_SMOKE,
		        						x, y, z,
		        						3, 0.3, 0.3, 0.3, 0
		        					);
		                		
		                		x = (float)(vectors[2].x + (Math.random()-0.5)*15);
	                			y = (float)(vectors[2].y + (Math.random()-0.5)*15);
	                			z = (float)(vectors[2].z + (Math.random()-0.5)*15);
		                		serverLevel.sendParticles(
		        						ParticleTypes.EXPLOSION,
		        						x, y, z,
		        						3, 0.3, 0.3, 0.3, 0
		        					);
                			}
                		}
                	}
                	if (currentTick[0] >= 30) {
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, 0), entity.getDeltaMovement().z);
                		// Launch the sword slashes forward
                		if (spawnedLoveSlashes[0] == false) {
                			spawnedLoveSlashes[0] = true;

                			// Calculate direction vector from eye position to target
                			Vec3 direction = vectors[2].subtract(entity.getEyePosition()).normalize();

                			// Calculate yaw from direction (Minecraft uses -Z as north, rotation is counterclockwise from south)
                			// atan2(x, z) gives angle from north axis
                            float yawSlashRot = (float) Math.toDegrees(Math.atan2(direction.x, -direction.z));

                            // Calculate pitch from normalized direction (negative because Minecraft pitch is inverted)
                            float pitchSlashRot = (float) -Math.toDegrees(Math.asin(direction.y));

                            // Spawn the love sword slashes entity
                            LoveSwordSlashesSpawner.spawnLoveSwordSlashes(
                                level, entity.getEyePosition(), yawSlashRot, pitchSlashRot, "love_third_form", 40
                            );
                		}
                		if (serverLevel != null) {
                			// Hit them in reverse order, starting from furthest and then coming to the closest
                			int totalHits = totalDuration - 30;
                			int currentHit = currentTick[0] - 30;
                			int windowSize = targets.size() / totalHits;
                			for (int i = targets.size()-1-currentHit * windowSize; i >= targets.size()-1-(currentHit+1) * windowSize; i--)
                    			for (LivingEntity le : targets.get(i)) {
                    				//if (!hitTargets.contains(le)) {
                    				//	hitTargets.add(le);
	                    				float x = (float)(le.position().x + (Math.random()-0.5)*le.getEyeHeight());
	    	                			float y = (float)(le.position().y + (Math.random()-0.5)*le.getEyeHeight());
	    	                			float z = (float)(le.position().z + (Math.random()-0.5)*le.getEyeHeight());
	    		                		serverLevel.sendParticles(
	    		        						ModParticles.LOVE_IMPACT.get(),
	    		        						x, y, z,
	    		        						3, 0.3, 0.3, 0.3, 0
	    		        					);
	    		                		
	    		                		
	    		        				level.playSound(null, x, y, z,
	    		                				   SoundEvents.PLAYER_ATTACK_STRONG,
	    		                				    SoundSource.PLAYERS, 1.0f, 1f);
	    		        				float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
	                    				Damager.hurt(entity, le, damage);
                    				//}
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
            }
        );
    }
    
    /**
     * Fourth Form: The Heartbreak of Unrequited Love
     */
    public static BreathingForm fourthForm() {
    	return new BreathingForm(
    			22004, // Love Fourth Form ID
    			"Fourth Form: The Heartbreak of Unrequited Love",
    			"The slayer catches an oncoming attack with their whip-blade, tossing it aside leaving the target open for a counter, or a retreat",
    			6,
    			(entity, level, formId) -> {
    				// TODO: Implement fourth form
    				ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
                	float [][][] particlePoints = ParticlePositions.fourth_form.get("point_a");
                	
                	float damage = DamageCalculator.calculateScaledDamage(entity, 6.0F);

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
                            		            new DustParticleOptions(
                            		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
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
                    	
                    	if (serverLevel != null && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
                    		for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                    			float x = particlePoints[currentTick[0]][i][0];
                    			float y = particlePoints[currentTick[0]][i][1];
                    			float z = particlePoints[currentTick[0]][i][2];

                    			Vec3 pos = entity.position().add(vecs[1].scale(z).add(vecs[2].scale(x)).add(vecs[3].scale(y)));

                    			// Spawn pink dust particle at pos
                    			serverLevel.sendParticles(
                    		            new DustParticleOptions(
                    		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                    		                1.8f                           // scale
                    		            ),
                    		            pos.x, pos.y, pos.z,
                    		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                    		        );
                    			
                    			// Spawn white dust particle at pos
                    			serverLevel.sendParticles(
                    					new DustParticleOptions(
                        		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                        		                1.5f                           // scale
                        		            ),
                    		            pos.x, pos.y, pos.z,
                    		            1, 0, 0, 0, 0 // count, velocity
                    		        );
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

				float damage = DamageCalculator.calculateScaledDamage(entity, 13.0F);
            	 // Set guard state (formId auto-injected as 22005)
                GuardStateHelper.setGuardState(entity, damage, formId);

                // Play player animation
                playEntityAnimation(entity, "love_fifth_form");
                
                // Trigger whip animation (client-side)
                triggerWhipAnimation(entity, "love_fifth_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

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
                	
                	if (serverLevel != null && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
                		for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                			float x = particlePoints[currentTick[0]][i][0];
                			float y = particlePoints[currentTick[0]][i][1];
                			float z = particlePoints[currentTick[0]][i][2];

                			Vec3 pos = entity.position().add(vecs[1].scale(z).add(vecs[2].scale(x)).add(vecs[3].scale(y)));

                			// Spawn pink dust particle at pos
                			serverLevel.sendParticles(
                		            new DustParticleOptions(
                		                new Vector3f(1.0f, 0.4f, 0.7f), // PINK
                		                1.8f                           // scale
                		            ),
                		            pos.x, pos.y, pos.z,
                		            2, 0.05, 0.05, 0.05, 0 // count, velocity
                		        );
                			
                			// Spawn white dust particle at pos
                			serverLevel.sendParticles(
                					new DustParticleOptions(
                    		                new Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                    		                1.5f                           // scale
                    		            ),
                		            pos.x, pos.y, pos.z,
                		            1, 0, 0, 0, 0 // count, velocity
                		        );
                		}
                	}
                	
                	if (currentTick[0] < 14) {
                		// Jump upwards and backwards (do the backflip)
                		MovementHelper.lookAtTarget(entity);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().normalize().scale(-0.65).add(0, 1.5f, 0));
                	}
                	else if (currentTick[0] < 25) {
                		// Hover and aim
                		MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(1, 0, 1));
                		MovementHelper.lookAtTarget(entity);
                	}
                	else if (currentTick[0] == 26) {
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
            				Damager.hurt(entity, le, damage*0.7f);
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
                				Damager.hurt(entity, le, damage*0.7f, true);
                			}
                		}
                	}
                	else if (currentTick[0] < 55) {
                		// Big explosion
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
                				Damager.hurt(entity, le, damage * 0.7f);
                			}
                		}
                		
                		if (serverLevel != null) {
                			level.playSound(null, vecs[4].x, vecs[4].y, vecs[4].z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                			for (int i = 0; i < 25; i++) {
	                			// Spawn an explosion that gets bigger and bigger (from tick 41 to 54)
                				float explosionRadius = 2+(currentTick[0] - 41) * 1.4f; // Scale from 0 to ~10 blocks
                				Vec3 pos = vecs[4].add((new Vec3(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5)).normalize().scale(explosionRadius));
		                		
                				if (Math.random() > 0.8) {
                					level.playSound(null, vecs[4].x, vecs[4].y, vecs[4].z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
                				}
                				
                				if (Math.random() < 0.3) {
                					serverLevel.sendParticles(
                        		            new DustParticleOptions(
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
                	else if (currentTick[0] < 75) {
                		// Post explosion rings
                		
                		int r = Math.min(currentTick[0]-55, 15);
                		
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
            }
        );
    }

	/**
     * Sixth Form: Cat-Legged Winds of Love (PLACEHOLDER)
     * TODO: Implement
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            22006, // Love Sixth Form ID
            "Sixth Form: Cat-Legged Winds of Love",
            "Spinning tornado whip attack",
            5,
            (entity, level, formId) -> {
                // TODO: Implement sixth form
            	float damage = DamageCalculator.calculateScaledDamage(entity, 6.0F);
            	ServerLevel serverLevel = (level instanceof ServerLevel ? (ServerLevel)level : null);
            	float [][][] particlePoints = ParticlePositions.sixth_form.get("point_a");
            	 // Set guard state (formId auto-injected as 22006)
                GuardStateHelper.setGuardState(entity, damage*4, formId); // extra protection

                // Play player animation
                playEntityAnimation(entity, "love_sixth_form");
                
                // Trigger whip animation (client-side)
                //triggerWhipAnimation(entity, "love_sixth_form", 1.0);

                // Prevent normal attack swing
                setCancelAttackSwing(entity, true);

                final int totalDuration = 90; // 4.5 seconds
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
                		triggerWhipAnimation(entity, "sword_to_right", 1.0);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().scale(0.9f));
                	}
                	else if (currentTick[0] == 10) {
                		triggerWhipAnimation(entity, "sword_to_left", 1.0);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().scale(0.9f));
                	}
                	else if (currentTick[0] == 20) {
                		triggerWhipAnimation(entity, "love_sixth_form", 1.0);
                		MovementHelper.setVelocity(entity, entity.getLookAngle().scale(0.5f).add(0, 1, 0));
                		entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
                	}
                	
                	if (currentTick[0] == 2 || currentTick[0] == 12 || currentTick[0] == 32) {
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
                					Damager.hurt(entity, target, damage, true);
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
                	
                	if (currentTick[0] > 30 && currentTick[0] % 2 == 0) {
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
                            Vec3 knockbackDir = target.position().subtract(entity.getEyePosition()).normalize();
                            target.setDeltaMovement(target.getDeltaMovement().add(knockbackDir.scale(1.0)));
                        }
                	}
                	
                	if (currentTick[0] >= 20) { // Particles from particle positions (starting at tick 20
                    	if (serverLevel != null && particlePoints != null && currentTick[0] < particlePoints.length && particlePoints[currentTick[0]-20].length > 0) {
                    		for (int i = 0; i < particlePoints[currentTick[0]-20].length; i++) {
                    			float x = particlePoints[currentTick[0]-20][i][0];
                    			float y = particlePoints[currentTick[0]-20][i][1];
                    			float z = particlePoints[currentTick[0]-20][i][2];

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
