package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;

import org.joml.Vector3f;

/**
 * Example variation registrations for testing the breathing form variations system.
 *
 * This class demonstrates how to register variations for breathing forms.
 * Copy this pattern to create your own variations.
 */
public class WaterVariations {

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
     * Register all water breathing variations.
     * Call this from the mod's initialization (FMLCommonSetupEvent).
     */
    public static void register() {
        Log.info("Registering Water Breathing variations...");
        Log.debug("DEBUG: WaterVariations.register() called - registering variations for form ID 102");

        // Register Water Breathing variations for base mod swords
        registerWaterBreathingTestVariations();

        Log.info("Finished registering Water Breathing variations");
        Log.debug("DEBUG: WaterVariations.register() completed");
    }

    /**
     * TEST: Water Breathing variations for base mod sword testing
     *
     * These variations work with ALL base mod Water Breathing swords.
     * Form ID 102 = Water Breathing Second Form (base mod uses breathes value as form ID)
     */
    private static void registerWaterBreathingTestVariations() {
        // Water Breathing Second Form: Lateral Water Wheel
        // Form ID 102 = Water Second Form from base mod
        BreathingFormVariation lateralWheel = new BreathingFormVariation(
            "Second Form, Improved: Lateral Water Wheel",
            "Spinning water wheel attack with wider range",
            3,  // Cooldown
            (entity, level, formId) -> {
                // formId is automatically 102 (Water Second Form)
            	ServerLevel serverLevel = level.isClientSide() ? null : (ServerLevel) level;
            	float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
            	GuardStateHelper.setGuardState(entity, damage, formId);
            	int tickDuration = 15;
            	
            	playEntityAnimation(entity, "kimetsunoyaibamultiplayer:side_flip");
            	
            	MovementHelper.setVelocity(entity, entity.getLookAngle().add(0, 0.5, 0).normalize().scale(0.9));
            	
            	if (serverLevel != null) {
            		AbilityScheduler.scheduleOnce(entity, () -> {
	            		// Spawn a horizontal circle of water particles centered on the player, 2 block radius
	            		int c = 0;
	            		for (float i = 0; i < 2*Math.PI; i+= 0.1) {
	            			Vec3 rad = new Vec3(Math.cos(i), 0, Math.sin(i));
	            			for (float j = 2; j < 2.5; j+=0.2f) {
	            				Vec3 pos = entity.getEyePosition().add(rad.scale(j));
	                			serverLevel.sendParticles(
	                					(net.minecraft.core.particles.SimpleParticleType)ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation("kimetsunoyaiba", "water")), //kimetsunoyaiba:particle_blue_smoke
	            						pos.x, pos.y, pos.z,
	            						2, 0.02, 0.02, 0.02, 0.02
	            					);
	            			}
	            			
	            			if (c % 8 == 0) {
	            				for (float j = 2; j < 3.5; j+=0.2f) {
	                				Vec3 pos = entity.getEyePosition().add(rad.scale(j));
	                    			serverLevel.sendParticles(
	                    					ParticleTypes.CLOUD, // white spikes
	                						pos.x, pos.y, pos.z,
	                						1, 0.02, 0.02, 0.02, 0.02
	                					);
	                			}
	            			}
	            			
	            			c++;
	            		}
            		}, 2);
            	}
            	
            	AABB hitBox = entity.getBoundingBox().inflate(5.0);
                List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);

                for (Entity target : targets) {
                	if (target instanceof LivingEntity livingTarget) {
                        Damager.hurt(entity, livingTarget, damage);

                        // Brief confusion
                        //livingTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                	}
                	// Knockback away from center
                    Vec3 knockbackDir = target.position().subtract(entity.position()).normalize();
                    target.setDeltaMovement(target.getDeltaMovement().add(knockbackDir.scale(0.5)));
                }
            	
            	
            	// Reset NBT tags after form
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);

                    // Re-enable normal attack swings and particles
                    setCancelAttackSwing(entity, false);
                }, tickDuration);
                
                // Play an attack sound
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
						1.0F, 1.0F);
            },
            Collections.emptySet()  // Empty set = applies to all Water Breathing swords
        );
        VariationRegistry.register(102, lateralWheel); // 102 = Water Second Form
        Log.debug("DEBUG: Registered Lateral Water Wheel variation for form ID 102");

        // Water Breathing Second Form: Rolling Water Wheel
        BreathingFormVariation rollingWheel = new BreathingFormVariation(
            "Second Form: Rolling Water Wheel",
            "Water wheel that rolls like a wheel",
            5,  // Longer cooldown
            (entity, level, formId) -> {
            	// formId is automatically 102 (Water Second Form)
            	ServerLevel serverLevel = level.isClientSide() ? null : (ServerLevel) level;
            	float damage = DamageCalculator.calculateScaledDamage(entity, 8.0F);
            	GuardStateHelper.setGuardState(entity, damage, formId);
            	int tickDuration = 60;
            	final float originalStepHeight = 0.6f;
				MovementHelper.setStepHeight(entity, 3);
            	
				int[] tickCounter = {0};
				
            	
            	AbilityScheduler.scheduleRepeating(entity, () -> {
            		Vec3 vel = entity.getLookAngle();
            		vel = new Vec3(vel.x, entity.getDeltaMovement().y, vel.z);
            		MovementHelper.setVelocity(entity, vel);
            		
            		if (tickCounter[0] % 10 == 0) {
                    	playEntityAnimation(entity, "kimetsunoyaibamultiplayer:front_flip");
            		}
            		
            		if (tickCounter[0] % 2 == 0) {
            			if (serverLevel != null) {
                    		// Spawn a vertical circle of water particles centered on the player, 2 block radius
                    		int c = 0;
                    		for (float i = 0; i < 2*Math.PI; i+= 0.1) {
                    			Vec3 rad = vel.normalize().scale(Math.cos(i)).add(new Vec3(0, Math.sin(i), 0));
                    			for (float j = 2; j < 2.5; j+=0.2f) {
                    				Vec3 pos = entity.getEyePosition().add(rad.scale(j));
                        			serverLevel.sendParticles(
                        					(net.minecraft.core.particles.SimpleParticleType)ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation("kimetsunoyaiba", "water")), //kimetsunoyaiba:particle_blue_smoke
                    						pos.x, pos.y, pos.z,
                    						2, 0.02, 0.02, 0.02, 0.02
                    					);
                    			}
                    			
                    			if ((c + tickCounter[0]) % 8 == 0) {
                    				for (float j = 2; j < 3.5; j+=0.15f) {
                        				Vec3 pos = entity.getEyePosition().add(rad.scale(j));
                            			serverLevel.sendParticles(
                            					ParticleTypes.CLOUD, // white spikes
                        						pos.x, pos.y, pos.z,
                        						1, 0.02, 0.02, 0.02, 0.02
                        					);
                        			}
                    			}
                    			
                    			c++;
                    		}
                    	}
            			
            			AABB hitBox = entity.getBoundingBox().inflate(5.0);
                        List<Entity> targets = entity.level().getEntities(entity, hitBox, e -> e != entity);

                        for (Entity target : targets) {
                        	if (target instanceof LivingEntity livingTarget) {
                                Damager.hurt(entity, livingTarget, damage);
                                
                                // Play an attack sound
                                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                						1.0F, 1.0F);

                                // Brief confusion
                                //livingTarget.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        	}
                        	// Knockback away from center
                            Vec3 knockbackDir = target.position().subtract(entity.position()).normalize();
                            target.setDeltaMovement(target.getDeltaMovement().add(knockbackDir.scale(0.5)));
                        }
            		}
            		
            		tickCounter[0]++;
            		
            	}, 1, tickDuration);
            	
            	
            	
            	
            	
            	
            	// Reset NBT tags after form
                AbilityScheduler.scheduleOnce(entity, () -> {
                	MovementHelper.setStepHeight(entity, originalStepHeight);
                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);

                    // Re-enable normal attack swings and particles
                    setCancelAttackSwing(entity, false);
                }, tickDuration);
                
                
            },
            Collections.emptySet()
        );
        VariationRegistry.register(102, rollingWheel); // 102 = Water Second Form
        Log.debug("DEBUG: Registered Rolling Water Wheel variation for form ID 102");

        Log.info("Registered 2 variations for Water Breathing Second Form (ID 102)");
        Log.debug("DEBUG: Finished registering 2 variations for form ID 102");

        // Verify registration
        int count = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.getVariationCount(102, null);
        Log.debug("DEBUG: VariationRegistry now shows " + count + " variations for form ID 102");
    }
}
