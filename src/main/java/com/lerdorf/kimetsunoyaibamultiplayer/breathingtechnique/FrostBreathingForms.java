package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.FancyMath;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;

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
            5, // 5 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "speed_attack_sword");

                // Launch entity forward slightly
                Vec3 lookVec = entity.getLookAngle();
                entity.setDeltaMovement(lookVec.scale(0.5));

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
     * Third Form: Hoarfrost Drift
     * Swerving forward movement with spinning blade, multiple attacks
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            "Third Form: Hoarfrost Drift",
            "Move in a wave with spinning blade",
            6, // 6 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_rotate");

                // Apply speed boost
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2));

                final Vec3 initialLook = entity.getLookAngle();
                final int totalTicks = 80; // 4 seconds
                final int attackInterval = 8; // Attack every 0.4 seconds

                for (int tick = 0; tick < totalTicks; tick++) {
                    final int currentTick = tick;

                    AbilityScheduler.scheduleOnce(entity, () -> {
                        // Swerve left and right in a wave pattern
                        double waveOffset = Math.sin((currentTick / (double) totalTicks) * Math.PI * 4) * 0.3;
                        Vec3 rightVec = initialLook.cross(new Vec3(0, 1, 0)).normalize();
                        Vec3 movement = initialLook.scale(0.6).add(rightVec.scale(waveOffset));

                        entity.setDeltaMovement(movement.add(0, entity.getDeltaMovement().y, 0));

                        // Multiple attacks during dash
                        if (currentTick % attackInterval == 0) {
                            // Always play attack animation
                            playEntityAnimation(entity, "sword_rotate");

                            // AOE damage in path
                            AABB hitBox = entity.getBoundingBox().inflate(2.5);
                            List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                                e -> e != entity && e.isAlive());

                            for (LivingEntity target : targets) {
                                float damage = DamageCalculator.calculateScaledDamage(entity, 6.0F);
                                Damager.hurt(entity, target, damage);
                            }

                            if (level instanceof ServerLevel serverLevel) {
                                spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 2.0, ParticleTypes.SNOWFLAKE, 12);
                                spawnCircleParticles(serverLevel, entity.position().add(0, 1, 0), 2.0, ParticleTypes.CLOUD, 6);
                            }

                            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 0.8F, 1.0F);
                        }
                    }, tick);
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Fourth Form: Freezing Cold
     * Vertical slash sending cold air blast 20 blocks forward
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            "Fourth Form: Freezing Cold",
            "Send a blast of freezing air",
            7, // 7 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_overhead");

                // Send blast forward
                Vec3 lookVec = entity.getLookAngle();
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(20.0));

                // Create wave of cold air
                AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                for (LivingEntity target : targets) {
                    float damage = DamageCalculator.calculateScaledDamage(entity, 9.0F);
                    Damager.hurt(entity, target, damage);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2)); // 20 seconds slowness
                    target.setTicksFrozen(target.getTicksFrozen() + 400); // Freeze visual effect
                }

                // Spawn particles
                if (level instanceof ServerLevel serverLevel) {
                    spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 60);
                    spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.CLOUD, 40);

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

                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
                level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, entity.blockPosition(), SoundEvents.SHULKER_SHOOT,
                    SoundSource.PLAYERS, 0.8F, 0.8F);
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
            8, // 8 second cooldown
            (entity, level) -> {
                // Apply speed and invisibility
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 3)); // 6 seconds
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0)); // 6 seconds

                // Schedule automatic attack at end if player doesn't attack
                final boolean[] hasAttacked = {false};

                // TODO: Detect when player attacks to trigger early
                // For now, just schedule the 3 jabs after 6 seconds
                AbilityScheduler.scheduleOnce(entity, () -> {
                    if (!hasAttacked[0]) {
                        executeThreeJabs(entity, level);
                    }
                }, 120);

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
        playEntityAnimation(entity, "speed_attack_sword");

        Vec3 lookVec = entity.getLookAngle();
        Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
        Vec3 endPos = startPos.add(lookVec.scale(3.0));

        AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
        List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, hitBox,
            e -> e != entity && e.isAlive());

        // 3 jabs
        for (LivingEntity target : targets) {
            float damage = DamageCalculator.calculateScaledDamage(entity, 5.0F);
            Damager.hurt(entity, target, damage);
            Damager.hurt(entity, target, damage);
            Damager.hurt(entity, target, damage);
        }

        if (level instanceof ServerLevel serverLevel) {
            spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 20);
        }

        level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, entity.blockPosition(), SoundEvents.SNOW_BREAK,
            SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Sixth Form: Polar Mark
     * Throw sword as projectile (simplified - uses raycast for now)
     * TODO: Implement actual item_display entity projectile
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Polar Mark",
            "Throw your sword forward",
            9, // 9 second cooldown
            (entity, level) -> {
                playEntityAnimation(entity, "sword_overhead");

                // TODO: Create item_display entity that flies forward as projectile
                // For now, damage in a line
                Vec3 lookVec = entity.getLookAngle();
                Vec3 startPos = entity.position().add(0, entity.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(15.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != entity && e.isAlive());

                // Hit first target only
                if (!targets.isEmpty()) {
                    float damage = DamageCalculator.calculateScaledDamage(entity, 13.0F);
                    targets.get(0).hurt(DamageCalculator.getDamageSource(entity), damage);
                }

                // Spawn particles along the path
                if (level instanceof ServerLevel serverLevel) {
                    spawnParticleLine(serverLevel, startPos, endPos, ParticleTypes.SNOWFLAKE, 50);
                }

                level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

                // Sound for sword returning
                AbilityScheduler.scheduleOnce(entity, () -> {
                    level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_RETURN,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                }, 20);
            }
        );
    }

    /**
     * Seventh Form: Golden Senses (Komorebi's sword only)
     * Temporarily switch sword to golden model and enhance stats
     * TODO: Implement model switching
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            "Seventh Form: Golden Senses",
            "Sword glows golden, empowering you",
            40, // 40 second cooldown
            (entity, level) -> {
                // TODO: Play kaishin3 animation
                // TODO: Change sword model to nichirinsword_golden
                playEntityAnimation(entity, "sword_overhead"); // Placeholder

                // Get current effect levels and add 1
                int hasteLevel = entity.hasEffect(MobEffects.DIG_SPEED) ?
                    entity.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 1 : 0;
                int speedLevel = entity.hasEffect(MobEffects.MOVEMENT_SPEED) ?
                    entity.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
                int strengthLevel = entity.hasEffect(MobEffects.DAMAGE_BOOST) ?
                    entity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1 : 0;

                // Apply enhanced effects for 20 seconds
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, hasteLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, speedLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, strengthLevel));
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));

                // Spawn golden particles
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 50; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 3;
                        double offsetY = level.random.nextDouble() * 2;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 3;
                        // TODO: Use yellow dust particle
                        serverLevel.sendParticles(ParticleTypes.CLOUD,
                            entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                            1, 0, 0.1, 0, 0.05);
                    }

                    // Continue spawning golden particles during the effect
                    for (int tick = 0; tick < 400; tick += 10) {
                        AbilityScheduler.scheduleOnce(entity, () -> {
                            for (int i = 0; i < 3; i++) {
                                double offsetX = (level.random.nextDouble() - 0.5) * 2;
                                double offsetY = level.random.nextDouble() * 2;
                                double offsetZ = (level.random.nextDouble() - 0.5) * 2;
                                serverLevel.sendParticles(ParticleTypes.CLOUD,
                                    entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                                    1, 0, 0.05, 0, 0.02);
                            }
                        }, tick);
                    }
                }

                // TODO: Play kimetsunoyaiba:awakening sound
                level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);

                // TODO: After 20 seconds, change sword model back
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
