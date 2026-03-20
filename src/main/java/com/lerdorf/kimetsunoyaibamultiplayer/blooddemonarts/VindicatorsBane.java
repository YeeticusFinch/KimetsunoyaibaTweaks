package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVindicatorEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class VindicatorsBane {
    public static final String ART_ID = "vindicators_bane";
    public static final int FORM_EXECUTIONERS_CLEAVE = 3200;
    public static final int FORM_SPLITTER_STRIKE = 3201;
    public static final int FORM_BLOODLUST_RUSH = 3202;

    private VindicatorsBane() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }

        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Vindicator's Bane", createTechnique());
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Vindicator's Bane",
            List.of(
                new BloodDemonArtForm(FORM_EXECUTIONERS_CLEAVE, "Executioner's Cleave", "Spin and cleave everything around you.", 8, VindicatorsBane::executeExecutionersCleave),
                new BloodDemonArtForm(FORM_SPLITTER_STRIKE, "Splitter Strike", "Leap up, crash down, and tear targets open.", 10, VindicatorsBane::executeSplitterStrike),
                new BloodDemonArtForm(FORM_BLOODLUST_RUSH, "Bloodlust Rush", "Rush forward in a flurry of front flips.", 12, VindicatorsBane::executeBloodlustRush)
            ),
            0x8f8f8f
        );
    }

    private static void executeExecutionersCleave(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        playAnimation(entity, "sword_rotate", 12);
        entity.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F + (entity.getRandom().nextFloat() * 0.2F));

        ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0.0D, 1.0D, 0.0D), 5.0D, ParticleTypes.SWEEP_ATTACK, 36);

        float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
            entity.getBoundingBox().inflate(5.0D, 1.5D, 5.0D),
            living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 25.0D)) {
            Damager.hurt(entity, target, damage);
        }
    }

    private static void executeSplitterStrike(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 launch = getTargetDirection(entity).scale(0.55D);
        playAnimation(entity, "sword_to_upper", 10);
        entity.setDeltaMovement(entity.getDeltaMovement().add(launch.x, 0.85D, launch.z));
        entity.hurtMarked = true;

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive()) {
                return;
            }

            Vec3 dive = getTargetDirection(entity).scale(0.7D);
            entity.setDeltaMovement(dive.x, -1.0D, dive.z);
            entity.hurtMarked = true;
            playAnimation(entity, "sword_overhead", 12);
            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!entity.isAlive()) {
                    return;
                }

                entity.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.9F + (entity.getRandom().nextFloat() * 0.2F));
                entity.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 0.95F + (entity.getRandom().nextFloat() * 0.15F));
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(0.1D), entity.getZ(), 12, 0.45D, 0.15D, 0.45D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY(0.2D), entity.getZ(), 20, 0.8D, 0.3D, 0.8D, 0.03D);

                float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35F;
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(3.0D, 1.5D, 3.0D),
                    living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 9.0D)) {
                    if (Damager.hurt(entity, target, damage)) {
                        BleedingHandler.applyOrRefreshBleeding(target, 20 * 10, 1);
                    }
                }
            }, 10);
        }, 10);
    }

    private static void executeBloodlustRush(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!entity.isAlive()) {
                    return;
                }

                Vec3 dash = getLookDirection(entity).scale(1.0D);
                entity.setDeltaMovement(dash.x, 0.12D, dash.z);
                entity.hurtMarked = true;

                if (tick % 10 == 0) {
                    playAnimation(entity, "kimetsunoyaibamultiplayer:front_flip", 10);
                    double yaw = Math.atan2(dash.z, dash.x) - (Math.PI / 2.0D);
                    double pitch = Math.atan2(dash.y, Math.sqrt((dash.x * dash.x) + (dash.z * dash.z)));
                    Vec3 center = entity.position().add(0.0D, 1.0D, 0.0D);
                    ParticleHelper.spawnVerticalArc(serverLevel, center, yaw, pitch,
                        1.6D, 0.1D, 360, 10.0D, 0.0D, ParticleTypes.SWEEP_ATTACK, 24);
                    ParticleHelper.spawnVerticalArc(serverLevel, center, yaw, pitch,
                        1.8D, 0.1D, 360, 10.0D, 0.0D, ModParticles.BLOOD.get(), 24);

                    float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(3.0D, 1.2D, 3.0D),
                        living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 9.0D)) {
                        Damager.hurt(entity, target, damage);
                    }
                }

                tick++;
            }
        }, 1, 40);
    }

    private static void playAnimation(LivingEntity entity, String animation, int duration) {
        String resolvedAnimation = animation;
        int namespaceSplit = animation.indexOf(':');
        if (namespaceSplit >= 0) {
            resolvedAnimation = animation.substring(namespaceSplit + 1);
        }

        if (entity instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(resolvedAnimation, duration);
        } else if (entity instanceof net.minecraft.world.entity.player.Player player) {
            KnYAPI.playAnimation(player, animation, duration);
        }
    }

    private static Vec3 getTargetDirection(LivingEntity entity) {
        LivingEntity target = entity.getLastHurtMob();
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
            target = mob.getTarget();
        }

        if (target != null && target.isAlive()) {
            Vec3 towardTarget = target.position().subtract(entity.position());
            if (towardTarget.horizontalDistanceSqr() > 1.0E-4D) {
                return towardTarget.normalize();
            }
        }

        Vec3 look = entity.getLookAngle();
        return look.horizontalDistanceSqr() > 1.0E-4D ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 getLookDirection(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        return look.lengthSqr() > 1.0E-4D ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }
}
