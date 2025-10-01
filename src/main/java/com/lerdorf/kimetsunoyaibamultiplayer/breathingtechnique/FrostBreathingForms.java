package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.core.particles.ParticleTypes;
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

/**
 * Implementation of all Frost Breathing forms (6 forms + 7th for Hiori)
 */
public class FrostBreathingForms {

    /**
     * First Form: Lavish Tundra
     * Fast horizontal dash with left-right swings
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
            "First Form: Lavish Tundra",
            "Dash forward with flowing horizontal strikes",
            100, // 5 second cooldown
            (player, level) -> {
                // TODO: Play animations: sword_to_left, sword_to_right
                // TODO: Make player sprint forward

                // Apply speed and dash forward
                Vec3 lookVec = player.getLookAngle();
                player.setDeltaMovement(lookVec.scale(1.5).add(0, 0.1, 0));

                // Damage entities in path
                Vec3 startPos = player.position();
                Vec3 endPos = startPos.add(lookVec.scale(6.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 7.0F);
                }

                // Spawn particles
                spawnParticleLine(level, startPos.add(0, 1, 0), endPos.add(0, 1, 0), ParticleTypes.SNOWFLAKE, 30);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
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
            100, // 5 second cooldown
            (player, level) -> {
                // TODO: Play animation: speed_attack_sword (stab)

                // Launch player forward slightly
                Vec3 lookVec = player.getLookAngle();
                player.setDeltaMovement(lookVec.scale(0.5));

                // Apply effects to targets in front
                Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(3.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 8.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4)); // 8 seconds, extreme slowness
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 4)); // 8 seconds, mining fatigue
                }

                // Spawn particles
                spawnParticleLine(level, startPos, endPos, ParticleTypes.SNOWFLAKE, 20);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Third Form: Hoarfrost Drift
     * Swerving forward movement with spinning blade
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            "Third Form: Hoarfrost Drift",
            "Move in a wave with spinning blade",
            120, // 6 second cooldown
            (player, level) -> {
                // TODO: Play animation: sword_rotate
                // TODO: Implement wave motion

                // Apply speed boost
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));

                // Damage entities in path
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position();
                Vec3 endPos = startPos.add(lookVec.scale(8.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 6.0F);
                }

                // Spawn particles along path
                spawnParticleLine(level, startPos.add(0, 1, 0), endPos.add(0, 1, 0), ParticleTypes.SNOWFLAKE, 40);
                spawnParticleLine(level, startPos.add(0, 1, 0), endPos.add(0, 1, 0), ParticleTypes.CLOUD, 20);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Fourth Form: Freezing Cold
     * Vertical slash sending cold air blast
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            "Fourth Form: Freezing Cold",
            "Send a blast of freezing air",
            140, // 7 second cooldown
            (player, level) -> {
                // TODO: Play animation: sword_overhead

                // Send blast forward
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(20.0));

                // Create multiple segments of the blast
                AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 9.0F);
                    // TODO: Apply custom frozen effect
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2)); // 20 seconds slowness
                    target.setTicksFrozen(target.getTicksFrozen() + 200); // Freeze visual effect
                }

                // Spawn particles
                spawnParticleLine(level, startPos, endPos, ParticleTypes.SNOWFLAKE, 60);
                spawnParticleLine(level, startPos, endPos, ParticleTypes.CLOUD, 40);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        );
    }

    /**
     * Fifth Form: Numbing Arctic Dance
     * Speed + invisibility, then 3 jabs
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            "Fifth Form: Numbing Arctic Dance",
            "Flicker in and out, then strike",
            160, // 8 second cooldown
            (player, level) -> {
                // TODO: Play animation: speed_attack_sword (stab) x3
                // TODO: Implement automatic attack at end of 6 seconds

                // Apply speed and invisibility
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 3)); // 6 seconds
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0)); // 6 seconds

                // Damage entities in front (3 jabs)
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(3.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 5.0F);
                    target.hurt(level.damageSources().playerAttack(player), 5.0F);
                    target.hurt(level.damageSources().playerAttack(player), 5.0F);
                }

                // Spawn particles
                spawnCircleParticles(level, player.position().add(0, 1, 0), 3.0, ParticleTypes.SNOWFLAKE, 30);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        );
    }

    /**
     * Sixth Form: Polar Mark
     * Throw sword as projectile
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Polar Mark",
            "Throw your sword forward",
            180, // 9 second cooldown
            (player, level) -> {
                // TODO: Play animation: sword_overhead
                // TODO: Create item_display entity that flies forward as projectile
                // TODO: Return sword to hand on hit

                // For now, damage in a line
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(15.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 13.0F);
                    break; // Only hit first target
                }

                // Spawn particles
                spawnParticleLine(level, startPos, endPos, ParticleTypes.SNOWFLAKE, 50);

                level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        );
    }

    /**
     * Seventh Form: Golden Senses (Hiori's sword only)
     * Temporarily switch sword to golden model and enhance stats
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            "Seventh Form: Golden Senses",
            "Sword glows golden, empowering you",
            800, // 40 second cooldown
            (player, level) -> {
                // TODO: Play animation: kaishin3
                // TODO: Temporarily change sword model to nichirinsword_golden
                // TODO: Change back after 20 seconds

                // Get current effect levels and add 1
                int hasteLevel = player.hasEffect(MobEffects.DIG_SPEED) ?
                    player.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 1 : 0;
                int speedLevel = player.hasEffect(MobEffects.MOVEMENT_SPEED) ?
                    player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
                int strengthLevel = player.hasEffect(MobEffects.DAMAGE_BOOST) ?
                    player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1 : 0;

                // Apply enhanced effects for 20 seconds
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, hasteLevel));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, speedLevel));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, strengthLevel));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));

                // Spawn golden particles
                for (int i = 0; i < 50; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 3;
                    double offsetY = level.random.nextDouble() * 2;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 3;
                    // TODO: Use yellow dust particle
                    level.addParticle(ParticleTypes.CLOUD,
                        player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ,
                        0, 0.1, 0);
                }

                // TODO: Play custom awakening sound: kimetsunoyaiba:awakening
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        );
    }

    // Helper methods for particle effects
    private static void spawnParticleLine(Level level, Vec3 start, Vec3 end, net.minecraft.core.particles.ParticleOptions particle, int count) {
        Vec3 direction = end.subtract(start);
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            Vec3 pos = start.add(direction.scale(t));
            level.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    private static void spawnCircleParticles(Level level, Vec3 center, double radius, net.minecraft.core.particles.ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.addParticle(particle, x, center.y, z, 0, 0, 0);
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
     * Create Frost Breathing with 7th form for Hiori's sword
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
