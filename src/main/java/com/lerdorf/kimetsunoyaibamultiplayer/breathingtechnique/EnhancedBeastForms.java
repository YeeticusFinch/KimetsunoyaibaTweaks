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
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BeastSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.WhiteSlashesEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.WhiteSlashesSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BeastSlashesEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
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

    private static void stopEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            if (player.level().isClientSide) {
                return;
            }
            ResourceLocation animationLocation;
            if (animationName != null && animationName.contains(":")) {
                String[] parts = animationName.split(":", 2);
                animationLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                animationLocation = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
            }
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket(
                    player.getUUID(), animationLocation, 0, 0, false, true
                )
            );
            return;
        }

        if (!entity.level().isClientSide) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket(entity.getId(), "__stop__")
            );
        } else {
            com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper.stopAnimationOnMob(entity);
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

    private static boolean hurtWithBeastBleeding(LivingEntity attacker, LivingEntity target, float damage, boolean dualWield) {
        boolean hurt = Damager.hurt(attacker, target, damage);
        if (hurt) {
            BleedingHandler.applyBeastBleeding(target, dualWield);
        }
        return hurt;
    }

    private static boolean hurtWithBeastBleeding(LivingEntity attacker, LivingEntity target, float damage, boolean resetInvulnerability, boolean dualWield) {
        boolean hurt = Damager.hurt(attacker, target, damage, resetInvulnerability, false, true);
        if (hurt) {
            BleedingHandler.applyBeastBleeding(target, dualWield);
        }
        return hurt;
    }
    
	public static BreathingForm firstForm() {
        return new BreathingForm(
        	25001,
            "First Fang: Pierce",
            "The user stabs the target's neck with both blades.",
            2, // 2 second cooldown
            (entity, level, formId) -> {
            	boolean twoSwords = hasTwoSwords(entity);
                float damage = (twoSwords ? 16.0F : 12.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 15.0F : 11.0F, formId);
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
                		if (hurtWithBeastBleeding(entity, target, damage, twoSwords) && twoSwords) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(0.1));
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
                    
                    stopEntityAnimation(entity, "breath_beast1");

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
                float damage = (twoSwords ? 24.0F : 12.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 16.0F : 8F, formId);
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
                		
						//renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, false, entityId, animationName, entity);
                		
                		if (!twoSwords) {
                			BonePositionTracker.sendRawHorizontalSlashToClients(
                				level,
                				new Vec3(0, 0.5, 0),
                				modelKey,
                				-2.0f,
                				true,
                				arcLength,
                				Math.max(20, (int)(arcLength / 1.2f)),
                				0,
                				0,
                				-20,
                				1.0f,
                				1.1f,
                				-15,
                				entity.getUUID(),
                				"sword_to_left"
                			);
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
                		if (hurtWithBeastBleeding(entity, target, damage, twoSwords)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 0.2 : 0.1));
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
                    stopEntityAnimation(entity, "breath_beast2");
                    
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
                float damage = (twoSwords ? 21.0F : 10.5F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 16.0F : 8.0F, formId);
                if (twoSwords)
                	playEntityAnimation(entity, "beast2");
                else
                	playEntityAnimation(entity, "sword_to_right");
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
                		
						//renderHorizontalSlashModel(level, entityPos, yawRad, entityHeight, progress, modelKey, false, false, entityId, animationName, entity);
                		
                		if (!twoSwords) {
                			BonePositionTracker.sendRawHorizontalSlashToClients(
                				level,
                				new Vec3(0, 0.5, 0),
                				modelKey,
                				2.0f,
                				false,
                				arcLength,
                				Math.max(20, (int)(arcLength / 1.2f)),
                				0,
                				0,
                				20,
                				1.0f,
                				1.1f,
                				15,
                				entity.getUUID(),
                				"sword_to_right"
                			);
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
                		if (hurtWithBeastBleeding(entity, target, damage, twoSwords)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 2 : 1));
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
                    stopEntityAnimation(entity, "kimetsunoyaibamultiplayer:beast2");
                    
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
            	
                float damage = (twoSwords ? 30.0F : 15f);
                GuardStateHelper.setGuardState(entity, twoSwords ? 20.0F : 10.0F, formId);
                
                //setCancelAttackSwing(entity, true);
                setCancelAttackSwing(entity, false); // we want the sword slaashes to happen
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;
                
                final int formDuration = 35;
                
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
                		//playEntityAnimation(entity, "beast4");
                            // The 4 slashes (two on each side of different radiuses

                        // mainhand sword to right
                        //renderHorizontalSlashModel(level, entity.position(), yawRad, entity.getEyeHeight(), progress, modelKey, true, false, entityId, "sword_to_right", entity);
                        
                        // offhand sword to left
                        //renderHorizontalSlashModel(level, entity.position(), yawRad, entity.getEyeHeight(), progress, modelKey, false, true, entityId, "left_sword_to_left", entity);

                		List<LivingEntity> targets = new ArrayList<LivingEntity>();
    	                
    	                Vec3 tpos = entity.getEyePosition().add(forward.scale(5));
                    	AABB hitBox = new AABB(tpos.add(0, -1, 0), tpos.add(0, 1, 0)).inflate(5);
                    	targets.addAll(level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive()));
                    	for (LivingEntity target : targets) {
                    		if (hurtWithBeastBleeding(entity, target, damage, twoSwords)) MovementHelper.addVelocity(target, forward.multiply(1, forward.y < 0 ? 0 : 1, 1).add(0, 0.1, 0).scale(twoSwords ? 2 : 1));
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
                                boolean directionFlag = i % 2 == 0;
        						int arcLength = (int) (100 + Math.random() * 40);
        						
        						boolean biggerRad = twoSwords && (i >= 2);

        						BonePositionTracker.sendRawHorizontalSlashToClients(
        								level, // level
        								new Vec3(0, heightRand-1, 0),
        								modelKey, // model key
        								directionFlag ? -2 : 2, // hor
        								directionFlag, // reverse
        								arcLength, // arc range
        								(int)(arcLength*1.4f), // duration
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
                	
                	currentTick[0]++;
                	
                	
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
                float damage = (4.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 11.0F : 5.5F, formId);
                entity.setNoGravity(true);
                
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

                    int[] currentTick = { 0 };
                    final int[] index = { 0 };
                
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    // Hold the user in place vertically during the form (hover, no falling).
                    Vec3 vel = entity.getDeltaMovement();
                    MovementHelper.setVelocity(entity, vel.x, Math.max(0, vel.y), vel.z);
                    //entity.setDeltaMovement(vel.x, 0.0, vel.z);
                    entity.fallDistance = 0.0F;

                    //int tick = (int) entity.getPersistentData().getDouble("flower_form2_tick");
                    double progress = currentTick[0] / (double) totalDuration;
                    double rotAngle = progress * Math.PI * 6; // 3 full rotations

                    // Deflect/knockback enemies that get close
                    AABB deflectBox = entity.getBoundingBox().inflate(radius);
                    List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(LivingEntity.class, deflectBox,
                        e -> e != entity && e.isAlive());
                    
                    List<Projectile> nearbyProjectiles = level.getEntitiesOfClass(Projectile.class, deflectBox);

                    if ((twoSwords ? (currentTick[0] % 3) : (currentTick[0] % 6)) == 0)
                    {
                    	playEntityAnimationOnLayer(entity, animations[(int)(index[0]%animations.length)], 10, 2.0f, 4000);
                        index[0]++;
                        level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 0.8F, 1.3F);
                    	
                    	for (LivingEntity target : nearbyTargets) {
                            // Push enemies away with rotating slashes
                            Vec3 knockbackDir = target.position().subtract(centerPos).normalize();
                            MovementHelper.setVelocity(target, knockbackDir.scale(0.8).add(0, 0.2, 0));

                            // Deal moderate damage on contact
                                //float damage = (6.0F);
                                hurtWithBeastBleeding(entity, target, damage, true, twoSwords);
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
                    entity.setNoGravity(false);
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
                float damage = (twoSwords ? 12.0F : 6.0F);
                GuardStateHelper.setGuardState(entity, twoSwords ? 10.0F : 5.0F, formId);
                
                setCancelAttackSwing(entity, true);
                playEntityAnimation(entity, "kimetsunoyaibamultiplayer:beast6");
                
                final int formDuration = 40; // 2.0s total
                final int whiteSlashesDuration = 24; // 1.2s
                final int beastSlashesDuration = 15; // 0.75s
                final int fullHitTick = 30; // 1.5s

                final WhiteSlashesEntity[] whiteSlashesRef = new WhiteSlashesEntity[1];
                if (!level.isClientSide) {
                    whiteSlashesRef[0] = WhiteSlashesSpawner.spawnWhiteSlashes(
                        level,
                        entity.getEyePosition().subtract(0, 0.3f, 0),
                        entity.getYRot(),
                        -entity.getXRot()*0.5f,
                        twoSwords ? "white_slashes_saw" : "white_slashes_saw_1",
                        whiteSlashesDuration
                    );
                }

                final int[] tick = {0};
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    if (!entity.isAlive()) {
                        return;
                    }

                    Vec3 look = entity.getLookAngle().normalize();
                    Vec3 centerPos = entity.getEyePosition().add(look.scale(2.2));

                    if (!level.isClientSide && tick[0] < whiteSlashesDuration && whiteSlashesRef[0] != null && whiteSlashesRef[0].isAlive()) {
                        whiteSlashesRef[0].setPos(centerPos.x, centerPos.y, centerPos.z);
                        whiteSlashesRef[0].setYRot(entity.getYRot());
                        whiteSlashesRef[0].setXRot(-entity.getXRot());
                        whiteSlashesRef[0].yRotO = entity.getYRot();
                        whiteSlashesRef[0].xRotO = -entity.getXRot();
                    }

                    if (!level.isClientSide) {
                        Vec3 forwardNow = new Vec3(look.x, 0.0, look.z).normalize();
                        if (forwardNow.lengthSqr() < 1.0E-6) {
                            forwardNow = new Vec3(0, 0, 1);
                        }

                        AABB hitBox = new AABB(centerPos.add(-3, -2, -3), centerPos.add(3, 2, 3)).inflate(2.0);
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive());

                        for (LivingEntity target : targets) {
                            Vec3 toTarget = target.position().subtract(entity.position()).normalize();
                            if (toTarget.dot(forwardNow) < 0.15D) {
                                continue;
                            }
                            if (target instanceof WhiteSlashesEntity || target instanceof BeastSlashesEntity) {
                                continue;
                            }
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 4, true, false, true));

                            if (tick[0] % 5 == 0 && tick[0] < fullHitTick) {
                                Damager.hurt(entity, target, damage * 0.333334f, true, false, true);
                            }
                        }

                        if (tick[0] == fullHitTick) {
                            for (LivingEntity target : targets) {
                                Vec3 toTarget = target.position().subtract(entity.position()).normalize();
                                if (toTarget.dot(forwardNow) < 0.15D) {
                                    continue;
                                }
                                Damager.hurt(entity, target, damage, true);
                            }
                        }

                        if (tick[0] == whiteSlashesDuration) {
                            if (whiteSlashesRef[0] != null && whiteSlashesRef[0].isAlive()) {
                                whiteSlashesRef[0].discard();
                            }
                        }
                        if (tick[0] == whiteSlashesDuration + 10) {
                            
                            BeastSlashesSpawner.spawnBeastSlashes(
                                level,
                                centerPos.subtract(0, 0.3f, 0),
                                entity.getYRot(),
                                -entity.getXRot()*0.5f,
                                twoSwords ? "base" : "base_1",
                                beastSlashesDuration
                            );
                        }
                    }

                    tick[0]++;
                }, 1, formDuration);
                
                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    stopEntityAnimation(entity, "kimetsunoyaibamultiplayer:beast6");
                    if (!level.isClientSide && whiteSlashesRef[0] != null && whiteSlashesRef[0].isAlive()) {
                        whiteSlashesRef[0].discard();
                    }
                    
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
                float damage = 14F;
                GuardStateHelper.setGuardState(entity, 14F, formId);

                setCancelAttackSwing(entity, true);
                ServerLevel serverLevel = level instanceof ServerLevel ? (ServerLevel)level : null;

                final int formDuration = 100; // 5 seconds
                final double rushSpeed = twoSwords ? 1.9 : 1.35;
                final float originalStepHeight = entity.maxUpStep();
                entity.setMaxUpStep(2.0F);

                playEntityAnimation(entity, "kimetsunoyaiba:sprint2");
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, formDuration + 6,
                        twoSwords ? 2 : 1, false, false, true));

                if (serverLevel != null) {
                    Vec3 p = entity.position().subtract(entity.getLookAngle().multiply(1.2, 0, 1.2));
                    serverLevel.sendParticles(
                                    ParticleTypes.EXPLOSION_EMITTER,
                                    p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01
                                );

                    AABB blastBox = new AABB(p.add(-3.5, -1.5, -3.5), p.add(3.5, 2.5, 3.5));
                    List<LivingEntity> blastTargets = level.getEntitiesOfClass(
                        LivingEntity.class, blastBox, e -> e != entity && e.isAlive()
                    );

                    for (LivingEntity target : blastTargets) {
                        Vec3 away = target.position().subtract(entity.position());
                        Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);
                        if (horizontalAway.lengthSqr() < 1.0E-6) {
                            horizontalAway = new Vec3(0.0, 0.0, 1.0);
                        } else {
                            horizontalAway = horizontalAway.normalize();
                        }

                        MovementHelper.setVelocity(target, horizontalAway.scale(twoSwords ? 1.35 : 1.05).add(0.45, 0.35, 0.0));
                        if (hurtWithBeastBleeding(entity, target, damage * 0.8f, true, twoSwords)) {
                            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                                SoundSource.PLAYERS, 0.85F, 1.15F);
                        }
                    }
                }

                final int[] tick = {0};
                AbilityScheduler.scheduleRepeating(entity, () -> {
                    if (!entity.isAlive()) {
                        return;
                    }

                    Vec3 look = entity.getLookAngle().normalize();
                    Vec3 forward = new Vec3(look.x, 0.0, look.z);
                    if (forward.lengthSqr() < 1.0E-6) {
                        forward = new Vec3(0, 0, 1);
                    } else {
                        forward = forward.normalize();
                    }

                    MovementHelper.setVelocity(entity, forward.scale(rushSpeed).add(0, entity.getDeltaMovement().y, 0));
                        entity.fallDistance = 0.0F;

                    if (tick[0] % 20 == 0) {
                        playEntityAnimationOnLayer(entity, "kimetsunoyaiba:sprint2", 25, 1.0f, 4000);
                    }

                    /*
                    if (tick[0] % hitInterval == 0) {
                        playEntityAnimationOnLayer(entity, "beast2", 8, 1.6f, 4000);

                        Vec3 center = entity.getEyePosition().add(forward.scale(1.6));
                        AABB hitBox = new AABB(center.add(-2.2, -1.3, -2.2), center.add(2.2, 1.3, 2.2));
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive());

                        for (LivingEntity target : targets) {
                            Vec3 toTarget = target.position().subtract(entity.position()).normalize();
                            if (toTarget.dot(forward) < 0.1D) {
                                continue;
                            }
                            if (hurtWithBeastBleeding(entity, target, damage * 0.5f, true, twoSwords)) {
                                MovementHelper.setVelocity(target, forward.scale(1.0).add(0, 0.18, 0));
                            }
                        }

                        if (serverLevel != null) {
                            BonePositionTracker.sendRawHorizontalSlashToClients(
                                level,
                                new Vec3(0, 0.25, 0),
                                modelKey,
                                2.0f,
                                false,
                                120,
                                80,
                                0,
                                0,
                                25,
                                1.3f,
                                1.3f,
                                15,
                                entity.getUUID(),
                                "beast2"
                            );

                            if (twoSwords) {
                                BonePositionTracker.sendRawHorizontalSlashToClients(
                                    level,
                                    new Vec3(0, 0.25, 0),
                                    modelKey,
                                    -2.0f,
                                    true,
                                    120,
                                    80,
                                    0,
                                    0,
                                    -25,
                                    1.5f,
                                    1.4f,
                                    -15,
                                    entity.getUUID(),
                                    "breath_beast2"
                                );
                            }
                        }

                        level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.PLAYERS, 0.85F, 1.3F);
                    }
                    */
                    tick[0]++;
                }, 1, formDuration);

                // ===== CLEANUP =====
                AbilityScheduler.scheduleOnce(entity, () -> {

                    // Clear guard state (only touches Damage, guard, attack - not skill/breathes/cnt1)
                    GuardStateHelper.clearGuardState(entity);
                    setCancelAttackSwing(entity, false);
                    entity.setMaxUpStep(originalStepHeight);
                        MovementHelper.setVelocity(entity, entity.getDeltaMovement().multiply(0.2, 1.0, 0.2));
                    stopEntityAnimation(entity, "kimetsunoyaiba:sprint2");
                    stopEntityAnimation(entity, "kimetsunoyaiba:sprint");
                    stopEntityAnimation(entity, "kimetsunoyaibamultiplayer:sprint_noob");

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
                float damage = (5.0F);
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
                float damage = (5.0F);
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
                float damage = (5.0F);
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

        return new BreathingTechnique("Beast Breathing", forms, "§b", "§b");
    }
}
