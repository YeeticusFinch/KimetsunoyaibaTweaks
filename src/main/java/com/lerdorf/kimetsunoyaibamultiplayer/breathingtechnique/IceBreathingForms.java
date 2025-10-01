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
 * Implementation of all Ice Breathing forms (6 forms + 7th for Hanazawa)
 */
public class IceBreathingForms {

    /**
     * First Form: Paralyzing Icicle
     * Speed stab with slowness/mining fatigue
     */
    public static BreathingForm firstForm() {
        return new BreathingForm(
            "First Form: Paralyzing Icicle",
            "Stab forward with incredible speed",
            100, // 5 second cooldown
            (player, level) -> {
                // TODO: Play animation: kimetsunoyaiba:speed_attack_sword
                // TODO: Launch player forward

                // Apply effects to targets in front
                Vec3 lookVec = player.getLookAngle();
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

                // Play sound
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        );
    }

    /**
     * Second Form: Winter Wrath
     * Circle target with rotational slashes
     */
    public static BreathingForm secondForm() {
        return new BreathingForm(
            "Second Form: Winter Wrath",
            "Circle and deliver rotational slashes",
            120, // 6 second cooldown
            (player, level) -> {
                // TODO: Implement circling movement and animations
                // TODO: Play animations: ragnaraku1, sword_to_right, sword_to_left

                // Damage entities in a circle around the player
                AABB area = player.getBoundingBox().inflate(5.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 6.0F);
                }

                // Spawn circle particles
                spawnCircleParticles(level, player.position().add(0, 1, 0), 5.0, ParticleTypes.SNOWFLAKE, 50);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        );
    }

    /**
     * Third Form: Merciful Hail Fall
     * Leap up and deliver multiple downward slashes
     */
    public static BreathingForm thirdForm() {
        return new BreathingForm(
            "Third Form: Merciful Hail Fall",
            "Leap and deliver powerful downward slashes",
            140, // 7 second cooldown
            (player, level) -> {
                // TODO: Make player leap upward and hover
                // TODO: Play animations: ragnaraku2, ragnaraku3 alternating

                player.setDeltaMovement(player.getDeltaMovement().add(0, 1.0, 0));

                // Damage entities below
                AABB area = player.getBoundingBox().inflate(3.0, 0.5, 3.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 10.0F);
                }

                // Spawn falling particles
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 6;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 6;
                    level.addParticle(ParticleTypes.SNOWFLAKE,
                        player.getX() + offsetX, player.getY() + 3, player.getZ() + offsetZ,
                        0, -0.5, 0);
                }

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        );
    }

    /**
     * Fourth Form: Silent Avalanche
     * Teleport forward and deliver powerful slash
     */
    public static BreathingForm fourthForm() {
        return new BreathingForm(
            "Fourth Form: Silent Avalanche",
            "Dash forward with incredible speed",
            100, // 5 second cooldown
            (player, level) -> {
                // TODO: Play animations: kamusari3, sword_to_left

                // Teleport player forward
                Vec3 lookVec = player.getLookAngle();
                Vec3 newPos = player.position().add(lookVec.scale(10.0));
                player.teleportTo(newPos.x, newPos.y, newPos.z);

                // Damage nearby entities
                AABB area = player.getBoundingBox().inflate(3.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 12.0F);
                }

                // Spawn particles
                spawnCircleParticles(level, player.position().add(0, 1, 0), 3.0, ParticleTypes.CLOUD, 30);

                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        );
    }

    /**
     * Fifth Form: Cold Blue Assault
     * Fast ground dash with constant attacks
     */
    public static BreathingForm fifthForm() {
        return new BreathingForm(
            "Fifth Form: Cold Blue Assault",
            "Swift dash with continuous slashes",
            100, // 5 second cooldown
            (player, level) -> {
                // TODO: Implement continuous forward movement
                // TODO: Play animations: kamusari3, sword_rotate

                // Apply speed boost
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));

                // Damage entities in path
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position();
                Vec3 endPos = startPos.add(lookVec.scale(8.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(1.5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 5.0F);
                }

                // Spawn particles along path
                spawnParticleLine(level, startPos.add(0, 1, 0), endPos.add(0, 1, 0), ParticleTypes.SNOWFLAKE, 40);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        );
    }

    /**
     * Sixth Form: Snowflake Cycle
     * Jump and spin, then deliver horizontal slash
     */
    public static BreathingForm sixthForm() {
        return new BreathingForm(
            "Sixth Form: Snowflake Cycle",
            "Spin and deliver a devastating slash",
            120, // 6 second cooldown
            (player, level) -> {
                // TODO: Play animations: ragnaraku1, sword_rotate

                // Launch player upward and forward
                Vec3 lookVec = player.getLookAngle();
                player.setDeltaMovement(lookVec.scale(0.5).add(0, 0.8, 0));

                // Damage entities around
                AABB area = player.getBoundingBox().inflate(4.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 11.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)); // Nausea
                }

                // Spawn spinning particles
                spawnCircleParticles(level, player.position().add(0, 1, 0), 4.0, ParticleTypes.SNOWFLAKE, 40);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        );
    }

    /**
     * Seventh Form: Icicle Claws (Hanazawa's sword only)
     * Blind enemies, then rapid multi-directional slashes
     */
    public static BreathingForm seventhForm() {
        return new BreathingForm(
            "Seventh Form: Icicle Claws",
            "Blind and strike from all directions",
            160, // 8 second cooldown
            (player, level) -> {
                // TODO: Play animations: speed_attack_sword, then alternate between slashing animations

                // First, blind enemies in front
                Vec3 lookVec = player.getLookAngle();
                Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);
                Vec3 endPos = startPos.add(lookVec.scale(5.0));

                AABB hitBox = new AABB(startPos, endPos).inflate(2.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    // Apply blindness
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0)); // 15 seconds

                    // Multiple rapid hits
                    target.hurt(level.damageSources().playerAttack(player), 4.0F);
                    target.hurt(level.damageSources().playerAttack(player), 4.0F);
                    target.hurt(level.damageSources().playerAttack(player), 4.0F);
                    target.hurt(level.damageSources().playerAttack(player), 4.0F);
                }

                // Spawn particles
                spawnCircleParticles(level, player.position().add(0, 1, 0), 5.0, ParticleTypes.SNOWFLAKE, 60);

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
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
     * Create the complete Ice Breathing technique with 6 forms
     */
    public static BreathingTechnique createIceBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        return new BreathingTechnique("Ice Breathing", forms);
    }

    /**
     * Create Ice Breathing with 7th form for Hanazawa's sword
     */
    public static BreathingTechnique createIceBreathingWithSeventh() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm());
        forms.add(secondForm());
        forms.add(thirdForm());
        forms.add(fourthForm());
        forms.add(fifthForm());
        forms.add(sixthForm());
        forms.add(seventhForm());
        return new BreathingTechnique("Ice Breathing", forms);
    }
}
