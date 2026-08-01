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
//import com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
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
public class MistVariations {

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
        Log.info("Registering Mist Breathing variations from Tweaks...");

        registerSeventhFormVariation();

        Log.info("Finished registering Mist Breathing variations from Tweaks");
    }

    /**
     * Seventh Form: Obscuring Clouds, Undulation
     * Enhanced Mist Seventh Form (20007)
     */
    private static void registerSeventhFormVariation() {
        BreathingFormVariation variation = new BreathingFormVariation(
            "Seventh Form: Obscuring Clouds, Undulation",
            "An offensive variant of Obscuring Clouds including an offensive bombardement",
            22,
            (entity, level, formId) -> {
            	 if (Config.logDebug) {
                     Log.debug("Enhanced Mist 7th Form: Starting for {}", entity.getName().getString());
                 }

                 GuardStateHelper.setGuardState(entity, 10, formId);

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
                 Log.debug("[DEBUG 7TH FORM] Starting clone spawn phase...");
                 Log.debug("[DEBUG 7TH FORM] Level type: " + level.getClass().getSimpleName());
                 Log.debug("[DEBUG 7TH FORM] Is ServerLevel: " + (level instanceof ServerLevel));

                 if (level instanceof ServerLevel serverLevel) {
                     int numClones = 6 + level.random.nextInt(5); // INCREASED: 6-10 clones
                     Log.debug("[DEBUG 7TH FORM] Spawning " + numClones + " clones");
                     Log.debug("[DEBUG 7TH FORM] Center position: " + centerPos);
                     Log.debug("[DEBUG 7TH FORM] Player: " + entity.getName().getString());

                     for (int i = 0; i < numClones; i++) {
                         // Spawn clones at random positions around the user
                         double angle = Math.toRadians(level.random.nextInt(360));
                         double distance = 5 + level.random.nextDouble() * 10; // 5-15 blocks away

                         double spawnX = entity.getX() + Math.cos(angle) * distance;
                         double spawnZ = entity.getZ() + Math.sin(angle) * distance;
                         double spawnY = entity.getY();

                         Log.debug("[DEBUG 7TH FORM] Clone #" + i + " at: " + spawnX + ", " + spawnY + ", " + spawnZ);

                         try {
                             // Create clone with center position for circular motion
                             com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity clone =
                                 new com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity(
                                     serverLevel, entity, formDuration, centerPos);

                             clone.setPos(spawnX, spawnY, spawnZ);
                             boolean success = serverLevel.addFreshEntity(clone);
                             Log.debug("[DEBUG 7TH FORM] Clone #" + i + " added: " + success + " (ID: " + clone.getId() + ")");
                         } catch (Exception e) {
                             System.err.println("[ERROR 7TH FORM] Failed to spawn clone #" + i);
                             e.printStackTrace();
                         }
                     }
                     Log.debug("[DEBUG 7TH FORM] Finished spawning " + numClones + " clones");
                 } else {
                     Log.debug("[DEBUG 7TH FORM] NOT spawning clones - level is client-side");
                 }

                 // ===== PHASE 3: TELEPORT ON ATTACK MECHANIC =====
                 // Set a flag to enable teleport on next attack
                 //entity.getPersistentData().putBoolean("mist_7th_teleport_active", true);
                 // Store the end time using tickCount (works on both client and server)
                 //entity.getPersistentData().putInt("mist_7th_teleport_duration", formDuration);
                 // Add initial cooldown to prevent immediate teleport on activation (1 second = 20 ticks)
                 //entity.getPersistentData().putInt("mist_7th_teleport_cooldown", entity.tickCount + 10);

                 // Speed boost while in mist
                 //entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, formDuration, 2));
                 //entity.addEffect(new MobEffectInstance(MobEffects.JUMP, formDuration, 1));

                 // Apply fading invisibility effect to make player blend with clones
                 // This creates a pulsing invisibility effect (fade in/out) similar to clones
                 final int[] fadeTickCounter = {0};
                 final boolean[] currentlyInvisible = {false};
                 final int[] invisibleDuration = {60 + level.random.nextInt(40)}; // Start invisible 3-5 seconds

                 final int attackInterval = 5; 
                 
                 final String[] animations = { "sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper",
 						"sword_to_left_reverse", "sword_to_right_reverse" };
                 
                 final String modelKey = SwordSlashModelRegistry.getModelKeyForSword(entity.getMainHandItem());
                 
                 double damage = (5F);
                 
                 AbilityScheduler.scheduleRepeating(entity, () -> {
                     fadeTickCounter[0]++;

                     if (fadeTickCounter[0] < 15) { // Launch upwards
                    	 if (fadeTickCounter[0] == 0) MovementHelper.setVelocity(entity, entity.getLookAngle().scale(-0.3f).add(0, 0.7f, 0));
                     }
                     else {
                    	 if (fadeTickCounter[0] % attackInterval == 1) {
     						MovementHelper.lookAtTarget(entity);
     						// Spawn mist clouds during the charge
     	                    if (level instanceof ServerLevel serverLevel) {
     	                        serverLevel.sendParticles(ModParticles.MIST_PARTICLE.get(),
     	                            entity.getX(), entity.getY() + 1, entity.getZ(),
     	                            6, 1.0, 1.0, 1.0, 0.05);
     	                    }
     					}

     					// Attack every attackInterval ticks
     					if (fadeTickCounter[0] % attackInterval == 0) {
     						int animIndex = (fadeTickCounter[0] / attackInterval) % 4;

     						if (Config.logDebug) {
     							Log.debug("Seventh Form: Playing attack animation '{}' on layer 4000",
     									animations[animIndex]);
     						}
     						
     						MovementHelper.lookAtTarget(entity);

     						// Use layer 4000 with 3x speed for ultra-fast attacks
     						playEntityAnimationOnLayer(entity, animations[animIndex], 10, 3.0f, 4000);

     						Vec3 attackerPos = entity.position().add(0, entity.getEyeHeight(), 0);
     						// Vec3 lookVec = entity.getLookAngle().normalize();

     						AABB attackBox = entity.getBoundingBox().inflate(5);

     						// AABB attackBox = entity.getBoundingBox().inflate(4.5);
     						List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class,
     								attackBox, e -> e != entity && e.isAlive());

     						for (LivingEntity target : targets) {
     							Damager.hurt(entity, target, (float)damage, false);
     						}

     						// Spawn particles
     						if (level instanceof ServerLevel serverLevel) {

     							double yawRad = Math.toRadians(entity.getYRot() + (Math.random() - 0.5) * 30);
     							double pitchRad = Math.toRadians((Math.random()-0.3) * 5);
     							Vec3 posOffset = new Vec3(Math.random() - 0.5, (Math.random() + 0.5) * 2,
     									Math.random() - 0.5);
     							Vec3 pos = entity.position().add(posOffset);

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

     						// Sound every 3rd attack to avoid spam
     						// if (currentTick % (attackInterval * 3) == 0) {
     						level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
     								SoundSource.PLAYERS, 0.7F, 1.4F);
     						level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F,
     								1.9F);
     						// }
     					}
                     }
                     
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

                 // Apply darkness to all living entities within 20 blocks of the cloud origin
                 // every second during the form. Excludes the caster.
                 if (level instanceof ServerLevel) {
                     final Vec3 cloudOrigin = entity.position();
                     final int tickInterval = 20; // every 1 second
                     AbilityScheduler.scheduleRepeating(entity, () -> {
                         AABB area = new AABB(cloudOrigin, cloudOrigin).inflate(20.0);
                         List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
                             LivingEntity.class,
                             area,
                             e -> e != entity && e.isAlive()
                         );
                         for (LivingEntity le : nearby) {
                             // 3 seconds of darkness, reapplied every second while inside the cloud
                         	if (!(le instanceof Player p && (p.isCreative() || p.isSpectator())))
                         		le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                         }
                     }, tickInterval, formDuration);
                 }

                 // ===== CLEANUP =====
                 AbilityScheduler.scheduleOnce(entity, () -> {
                     //entity.getPersistentData().remove("mist_7th_teleport_active");
                     //entity.getPersistentData().remove("mist_7th_teleport_duration");

                     // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                     GuardStateHelper.clearGuardState(entity);

                     if (Config.logDebug) {
                         Log.debug("Enhanced Mist 7th Form: Cleanup complete for {}", entity.getName().getString());
                     }
                 }, formDuration + 1);

                 if (Config.logDebug) {
                     Log.debug("Enhanced Mist 7th Form: All phases scheduled for {}", entity.getName().getString());
                 }
            },
            Collections.emptySet()  // Applies to all Love Breathing swords
        );

        VariationRegistry.register(20007, variation);
        Log.debug("DEBUG: Registered '" + variation.getName() + "' for form ID 20007");
    }
    

}
