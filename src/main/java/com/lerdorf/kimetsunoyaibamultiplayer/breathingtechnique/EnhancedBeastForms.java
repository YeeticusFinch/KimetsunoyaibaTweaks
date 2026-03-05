package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import java.util.ArrayList;
import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

import org.joml.Vector3f;

/**
 * Enhanced Beast Breathing forms.
 * Placeholder implementation with 10 forms for future custom effects.
 * Form IDs in the 25000s range (25001-25010).
 */
public class EnhancedBeastForms {
    private static final UUID DUAL_WIELD_ATTACK_SPEED_MODIFIER_UUID =
            UUID.fromString("8df74f20-6d63-4e62-bef7-c6fd4cff3651");

    private static final Vector3f WHITE = new Vector3f(255f/255f, 255f/255f, 255f/255f);
    
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

    public static String modelKey = "beast";
	
    public static boolean hasTwoSwords(LivingEntity entity) {
    	ItemStack mainHand = entity.getMainHandItem();
    	ItemStack offHand = entity.getOffhandItem();
    	return isBeastSword(mainHand) && isBeastSword(offHand);
    }

    /**
     * Keep attack speed in sync with beast dual-wield state.
     * Adds +100% total attack speed only while both hands hold beast/inosuke swords.
     */
    public static void syncDualWieldAttackSpeed(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        attackSpeed.removeModifier(DUAL_WIELD_ATTACK_SPEED_MODIFIER_UUID);
        if (!hasTwoSwords(entity)) {
            return;
        }

        attackSpeed.addTransientModifier(new AttributeModifier(
                DUAL_WIELD_ATTACK_SPEED_MODIFIER_UUID,
                "Beast dual-wield attack speed",
                1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    private static boolean isBeastSword(ItemStack stack) {
    	if (stack == null || stack.isEmpty()) {
    		return false;
    	}

    	ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
    	if (id == null) {
    		return false;
    	}

    	String key = id.toString();
    	return key.equals("kimetsunoyaiba:nichirinsword_inosuke")
    			|| key.equals("kimetsunoyaibamultiplayer:nichirinsword_inosuke")
    			|| key.equals("kimetsunoyaibamultiplayer:nichirinsword_beast");
    }
    
	public static BreathingForm firstForm() {
        return new BreathingForm(
        	25001,
            "First Fang: Pierce",
            "The user stabs the target's neck with both blades.",
            2, // 2 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, twoSwords ? 12.0F : 6.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 12.0F : 6.0F, formId);
                playEntityAnimation(entity, "breath_beast1");
                setCancelAttackSwing(entity, true);
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 10;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                if (twoSwords) {
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
	                    SoundSource.PLAYERS, 1.0F, 1.2F);
				level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
	                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }
                
                // One shot ability
                AbilityScheduler.scheduleOnce(entity, () -> {

                	if (serverLevel != null) {
                		ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().add(right), entity.getEyePosition().subtract(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                		if (twoSwords)
                			ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().subtract(right), entity.getEyePosition().add(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                	}
                	
                	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					
					List<LivingEntity> targets = new ArrayList<LivingEntity>();
	                
	                Vec3 pos = entity.getEyePosition().add(forward.scale(3));
                	AABB hitBox = new AABB(pos.add(0, -1, 0), pos.add(0, 1, 0)).inflate(3);
                	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive()));
                	for (LivingEntity target : targets) {
                		if (Damager.hurt(entity, target, damage) && twoSwords) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(0.1));
                	}

                }, 2);
                
                if (twoSwords) {
                AbilityScheduler.scheduleOnce(entity, () -> {
                	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }, 6);
                }
                
             // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm secondForm() {
        return new BreathingForm(
        	25002,
            "Second Fang: Slice",
            "The user unleashes a double slash with two blades in an X-shaped cut.",
            2, // 2 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, twoSwords ? 12.0F : 6.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 11.0F : 5.5F, formId);
                if (twoSwords)
                	playEntityAnimation(entity, "breath_beast2");
                else
                	playEntityAnimation(entity, "sword_to_left");
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 10;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                if (twoSwords) {
	                level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }
                
                // One shot ability
                AbilityScheduler.scheduleOnce(entity, () -> {

                	if (serverLevel != null) {
                		//ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().add(right), entity.getEyePosition().subtract(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                		//if (twoSwords)
                		//	ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().subtract(right), entity.getEyePosition().add(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                	
                		yawRad[0] = (float)Math.toRadians(entity.getYRot());
						double pitchRad = 0;
						Vec3 posOffset = new Vec3(0, 1.5f, 0);
						Vec3 pos = entity.position().add(posOffset);

						int arcLength = 160;
						float angle = 5;
                		
						//renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, false, entityId, animationName, entity);
                		
                		if (twoSwords) {
                			
                			angle *= -1;
                			
                		}
                		
                	}
                	
                	level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					
					List<LivingEntity> targets = new ArrayList<LivingEntity>();
	                
	                Vec3 pos = entity.getEyePosition().add(forward.scale(3));
                	AABB hitBox = new AABB(pos.add(0, -1, 0), pos.add(0, 1, 0)).inflate(3);
                	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive()));
                	for (LivingEntity target : targets) {
                		if (Damager.hurt(entity, target, damage)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 0.2 : 0.1));
                	}

                }, 2);
                
                if (twoSwords) {
                AbilityScheduler.scheduleOnce(entity, () -> {
                	level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }, 6);
                }
                
             // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm thirdForm() {
        return new BreathingForm(
        	25003,
            "Third Fang: Devour",
            "The user releases two simultaneous horizontal slashes towards the target's throat to decapitate them.",
            2, // 2 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, twoSwords ? 10.0F : 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                if (twoSwords)
                	playEntityAnimation(entity, "beast2");
                else
                	playEntityAnimation(entity, "sword_to_left");
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 10;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                if (twoSwords) {
	                level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }
                
                // One shot ability
                AbilityScheduler.scheduleOnce(entity, () -> {

                	if (serverLevel != null) {
                		//ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().add(right), entity.getEyePosition().subtract(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                		//if (twoSwords)
                		//	ParticleHelper.spawnParticleLine(serverLevel, entity.getEyePosition().subtract(right), entity.getEyePosition().add(right.scale(0.5)).add(forward.scale(3)), ParticleTypes.SWEEP_ATTACK, 20);
                	
                		yawRad[0] = (float)Math.toRadians(entity.getYRot());
						double pitchRad = 0;
						Vec3 posOffset = new Vec3(0, 1.5f, 0);
						Vec3 pos = entity.position().add(posOffset);

						int arcLength = 160;
						float angle = 5;
                		
						//renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, false, entityId, animationName, entity);
                		
                		if (twoSwords) {
                			
                			angle *= -1;
                			
                		}
                		
                	}
                	
                	level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					
					List<LivingEntity> targets = new ArrayList<LivingEntity>();
	                
	                Vec3 pos = entity.getEyePosition().add(forward.scale(3));
                	AABB hitBox = new AABB(pos.add(0, -1, 0), pos.add(0, 1, 0)).inflate(3);
                	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive()));
                	for (LivingEntity target : targets) {
                		if (Damager.hurt(entity, target, damage)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 2 : 1));
                	}

                }, 2);
                
                if (twoSwords) {
                AbilityScheduler.scheduleOnce(entity, () -> {
                	level.playSound(null, entity.blockPosition(), SWORD_SWEEP_SOUND,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
		                    SoundSource.PLAYERS, 1.0F, 1.2F);
                }, 6);
                }
                
             // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm fourthForm() {
        return new BreathingForm(
        	25004,
            "Fourth Fang: Slice 'n' Dice",
            "The user delivers multiple diagonal double slashes with both swords.",
            3, // 3 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
            	
            	playEntityAnimation(entity, "beast4");
            	
                float damage = DamageCalculator.calculateScaledDamage(entity, twoSwords ? 11.0F : 5.5f);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                int[] currentTick = {0};
                
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
                	
                	if (currentTick[0] <= 10) {
                		// Accelerate forward
                		MovementHelper.setVelocity(entity, entity.getLookAngle());
                	}
                	
                	if (currentTick[0] == 15) {
                		// The 4 slashes (two on each side of different radiuses
                		
                		List<LivingEntity> targets = new ArrayList<LivingEntity>();
    	                
    	                Vec3 tpos = entity.getEyePosition().add(forward.scale(5));
                    	AABB hitBox = new AABB(tpos.add(0, -1, 0), tpos.add(0, 1, 0)).inflate(5);
                    	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive()));
                    	for (LivingEntity target : targets) {
                    		if (Damager.hurt(entity, target, damage)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 2 : 1));
                    	}
                		
                		for (int i = 0; i < (twoSwords ? 4 : 2); i++)
                		{
                			// Spawn particles and slash models (server-side only)
        					if (serverLevel != null) {

        						double yaw_rad = Math.toRadians(entity.getYRot()+20);
        						float pitchDeg = (float)((Math.random()-0.5) * 20);
        						double pitchRad = Math.toRadians(pitchDeg);
        						
        						float heightRand = (float)( (Math.random() + 0.3) * 2 + pitchRad/Math.PI);
        						
        						Vec3 pos = entity.position().add(Math.random() - 0.5, heightRand,
        								Math.random() - 0.5);
        						
        						// Send raw slash render request to all clients
        						float slashAngle = (float)pitchRad;
        						boolean directionFlag = twoSwords ? (i == 0) : false;
        						int arcLength = (int) (100 + Math.random() * 40);
        						
        						boolean biggerRad = twoSwords && i >= 2;

        						BonePositionTracker.sendRawHorizontalSlashToClients(
        								level, // level
        								new Vec3(0, heightRand-1, 0),
        								modelKey, // model key
        								directionFlag ? -2 : 2, // hor
        								directionFlag, // reverse
        								arcLength, // arc range
        								(int)(arcLength/1.2f), // duration
        								0, // yaw offset
        								(float)pitchDeg, // pitch offset
        								directionFlag ? -20 : 20, // roll offset 
        								biggerRad ? 1.9f : 1f, // radius scalar
        								biggerRad ? 1.6f : 1.1f, // size scalar
        								directionFlag ? -15 : 15, // angle offset
        								entity.getUUID(), // entity id
        								directionFlag ? "sword_to_left" : "sword_to_right"); // animation name

        						double angle = directionFlag ? -20 : 20;
        						{
        							for (int j = 0; j < 2; j++) {
        								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yaw_rad, pitchRad,
        										(biggerRad ? 4 : 2)+j*0.3f, 0.2, arcLength, Math.random()*10+10, angle, ParticleTypes.ENCHANTED_HIT,
        										1);

        								ParticleHelper.spawnHorizontalArc(serverLevel, pos, yaw_rad, pitchRad,
        										(biggerRad ? 4 : 2)+j*0.3f, 0.2, arcLength, Math.random()*15+15, angle, new DustParticleOptions(new Vector3f(255f / 255f, 255f / 255f, 255f / 255f),
        												(float) (Math.random()*0.9f + 0.2f)), 1
        											);
        							}
        						}
        					}

        					// if (currentTick % (attackInterval * 3) == 0) {
        					level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
        							SoundSource.PLAYERS, 0.7F, 1.4F);
                		}
                	}
                	
                	
                }, 1, formDuration);
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm fifthForm() {
        return new BreathingForm(
        	25005,
            "Fifth Fang: Crazy Cutting",
            "The user slices everything in all directions while in mid-air.",
            3, // 3 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.5F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 11.0F : 5.5F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, true);
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                Vec3 centerPos = entity.position();
                final int totalDuration = 35; // 1.5 seconds
                
                final Vec3[] circleVec = new Vec3[9];
                final int[] circleCounter = {0};
                final int maxCircleCount = 3;
                final float radius = 3.75f;
                
                final String[] animations = { "sword_to_left", "sword_to_right", "left_sword_to_left", "left_sword_to_right",
						"sword_to_left_reverse", "sword_to_right_reverse", "left_sword_to_left_reverse", "left_sword_to_right_reverse" };

                int[] currentTick = {0};
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    //int tick = (int) entity.getPersistentData().getDouble("flower_form2_tick");
                    double progress = currentTick[0] / (double) totalDuration;
                    double rotAngle = progress * Math.PI * 6; // 3 full rotations

                    // Deflect/knockback enemies that get close
                    AABB deflectBox = entity.getBoundingBox().inflate(radius);
                    List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, deflectBox,
                        e -> e != entity && e.isAlive());
                    
                    List<Projectile> nearbyProjectiles = level.getEntitiesOfClass(Projectile.class, deflectBox);

                    if ((twoSwords ? (currentTick[0] % 5) : (currentTick[0] % 10)) == 0)
                    {
                    	playEntityAnimationOnLayer(entity, animations[(int)(Math.random()*animations.length)], 10, 2.0f, 4000);
                    	level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 0.8F, 1.3F);
                    	
                    	for (LivingEntity target : nearbyTargets) {
                            // Push enemies away with rotating slashes
                            Vec3 knockbackDir = target.position().subtract(centerPos).normalize();
                            MovementHelper.setVelocity(target, knockbackDir.scale(0.8).add(0, 0.2, 0));

                            // Deal moderate damage on contact
                                //float damage = DamageCalculator.calculateScaledDamage(entity, 6.0F);
                                Damager.hurt(entity, target, damage, true);
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

                    // Rotating particles
                    if (serverLevel != null) {
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
                                        ParticleTypes.SWEEP_ATTACK,
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
                                        ParticleTypes.CRIT,
                                        p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01
                                    );
                        	}
                        	
                        circleCounter[0]++;
                        if (circleCounter[0] > maxCircleCount) circleCounter[0] = 0;
                        
                        double pitchRad = Math.toRadians((Math.random()-0.3) * 5);
						Vec3 posOffset = new Vec3(Math.random() - 0.5, (Math.random() + 0.5) * 2,
								Math.random() - 0.5);
						Vec3 pos = entity.position().add(posOffset);

						int arcLength = (int) (100 + Math.random() * 70);
						double angle = (Math.random() - 0.5) * 10;
						boolean particle = false;
						float yawOffset = (float)(Math.PI * 2 * Math.random());
						
							BonePositionTracker.sendRawHorizontalSlashToClients(
									level, // level
									posOffset.add(0, -1, 0),
									modelKey, // model key
									(float)angle, // vert
									Math.random() > 0.5, // reverse
									arcLength, // arc range
									100, // duration
									yawOffset, // yaw offset
									0, // pitch offset
									(float)angle*10, // roll offset 
									1.5f, // radius scalar
									2.1f, // size scalar
									15, // angle offset
									entity.getUUID(), // entity id
									Math.random() > 0.5 ? "sword_to_left" : "sword_to_right"); // animation name


							BonePositionTracker.sendRawVerticalSlashToClients(
									level, // level
									posOffset.add(0, -1, 0) ,
									modelKey, // model key
									(float)angle, // angle
									false, // reverse
									arcLength, // arc range
									100, // duration
									yawOffset, // yaw offset
									0, // pitch offset
									(float)angle*10, // roll offset 
									1.5f, // radius scalar
									2.1f, // size scalar
									0, // angle offset
									entity.getUUID(), // entity id
									"sword_overhead"); // animation name
                        
                    }

                    //entity.getPersistentData().putDouble("flower_form2_tick", tick + 1);
                    currentTick[0]++;
                }, 1, totalDuration);
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm sixthForm() {
        return new BreathingForm(
        	25006,
            "Sixth Fang: Palisade Bite",
            "The user releases simultaneous slashes with two swords from both directions in a saw-like movement.",
            6, // 6 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm seventhForm() {
        return new BreathingForm(
        	25007,
            "Seventh Form: Spatial Awareness",
            "The user utilizes their sense of touch to identify the position of enemies and their weaknesses by feeling small disturbances in the air. When using this technique, they crouch down with both arms extended and palms opened, sharpening their sensitivity to even the faintest shifts.",
            8, // 8 second cooldown
            (entity, level, formId) -> {
            	entity.addEffect(new MobEffectInstance(ModEffects.SPATIAL_AWARENESS.get(), 20*8, 0));
            }
                
        );
    }
	
	public static BreathingForm eigthForm() {
        return new BreathingForm(
        	25008,
            "Eighth Form: Explosive Rush",
            "The user charges towards their opponent at blinding speeds, ignoring all incoming attacks.",
            5, // 5 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm ninthForm() {
        return new BreathingForm(
        	25009,
            "Ninth Fang: Extending Bendy Slash",
            "The user dislocates the joints of their arm to increase the range of their attack, and then unleashing a fast swinging forward strike.",
            5, // 5 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm tenthForm() {
        return new BreathingForm(
        	25010,
            "Tenth Fang: Whirling Fangs",
            "The user rapidly spins their swords in a circular motion, deflecting enemy attacks such as projectiles.",
            6, // 6 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
	public static BreathingForm eleventhForm() {
        return new BreathingForm(
        	25010,
            "Eleventh Fang: Sudden Throwing Strike",
            "The user throws both of their blades in a spinning motion at an enemy.",
            6, // 6 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                int formDuration = 20;
                
                final float[] yawRad = {(float) Math.toRadians(-entity.getYRot())};
                Vec3 forward = new Vec3(Math.sin(yawRad[0]), 0, Math.cos(yawRad[0])).normalize();
                Vec3 right = new Vec3(-forward.z, 0, forward.x);
                
                
                
                
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    //setCancelAttackSwing(entity, false);
                    
                }, formDuration + 1);
            }
                
        );
    }
	
    public static BreathingTechnique createBeastBreathing() {
        List<BreathingForm> forms = new ArrayList<>();

        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        forms.add(seventhForm());
        forms.add(eigthForm());
        forms.add(ninthForm());
        forms.add(tenthForm());

        return new BreathingTechnique("Beast Breathing", forms, "§3", "§3");
    }
}
