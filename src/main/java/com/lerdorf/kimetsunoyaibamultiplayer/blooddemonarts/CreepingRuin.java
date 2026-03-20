package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonCreeperEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class CreepingRuin {
    public static final String ART_ID = "creeping_ruin";
    public static final int FORM_THUNDERBRAND = 3100;
    public static final int FORM_TEMPEST_ASCENSION = 3101;
    public static final int FORM_DETONATION = 3102;

    private CreepingRuin() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }

        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Creeping Ruin", createTechnique());
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Creeping Ruin",
            List.of(
                createThunderbrandForm(),
                createTempestAscensionForm(),
                createDetonationForm()
            ),
            0x1f5d2d
        );
    }

    public static BloodDemonArtForm createThunderbrandForm() {
        return new BloodDemonArtForm(
            FORM_THUNDERBRAND,
            "Thunderbrand",
            "Strike yourself with lightning to become charged.",
            10,
            CreepingRuin::executeThunderbrand
        );
    }

    public static BloodDemonArtForm createTempestAscensionForm() {
        return new BloodDemonArtForm(
            FORM_TEMPEST_ASCENSION,
            "Tempest Ascension",
            "Leap and call down a ring of lightning.",
            8,
            CreepingRuin::executeTempestAscension
        );
    }

    public static BloodDemonArtForm createDetonationForm() {
        return new BloodDemonArtForm(
            FORM_DETONATION,
            "Detonation",
            "Explode, hide, and regenerate.",
            14,
            CreepingRuin::executeDetonation
        );
    }

    public static void executeThunderbrand(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, "ragnaraku1", 12);
        } else if (entity instanceof DemonCreeperEntity creeper) {
            creeper.playGeckoAnimation("reel", 12);
            creeper.startChargedState(20 * 30);
            creeper.applyBloodDemonArtCooldown(20 * 10);
        }

        net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(entity.position());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int age = 0;

            @Override
            public void run() {
                double radius = 1.0D + age;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(0.5D), entity.getZ(), 8, radius * 0.08D, 0.05D, radius * 0.08D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY(0.5D), entity.getZ(), 2, radius * 0.05D, 0.05D, radius * 0.05D, 0.0D);

                AABB area = entity.getBoundingBox().inflate(radius + 0.75D);
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, living -> living != entity && living.isAlive())) {
                    double distance = target.distanceTo(entity);
                    if (distance > radius || distance < Math.max(0.0D, radius - 1.25D)) {
                        continue;
                    }

                    float damage = entity instanceof DemonCreeperEntity creeper && creeper.isChargedState() ? 9.0F : 6.0F;
                    Damager.hurt(entity, target, damage);

                    Vec3 knockback = target.position().subtract(entity.position()).normalize().scale(1.0D);
                    target.push(knockback.x, 0.35D, knockback.z);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                age++;
            }
        }, 1, 10);
    }

    public static void executeTempestAscension(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, "sword_rotate", 12);
        } else if (entity instanceof DemonCreeperEntity creeper) {
            creeper.playGeckoAnimation("spin", 12);
        }

        Vec3 look = entity.getLookAngle().normalize();
        entity.setDeltaMovement(entity.getDeltaMovement().add(look.x * 0.8D, 0.9D, look.z * 0.8D));
        entity.hurtMarked = true;

        AbilityScheduler.scheduleOnce(entity, () -> spawnLightningRing(serverLevel, entity, 10.0D, 20), 10);
        if (entity instanceof DemonCreeperEntity creeper && creeper.isChargedState()) {
            AbilityScheduler.scheduleOnce(entity, () -> spawnLightningRing(serverLevel, entity, 15.0D, 10), 15);
        }
    }

    public static void executeDetonation(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        if (!(entity instanceof DemonCreeperEntity creeper)) {
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
            triggerDetonationRecovery(entity);
            return;
        }

        LivingEntity target = creeper.getTarget();
        if (target == null || !target.isAlive() || creeper.distanceToSqr(target) > 9.0D || creeper.isPrimingDetonation()) {
            creeper.applyBloodDemonArtCooldown(20);
            return;
        }

        creeper.beginDetonationPrimeSequence();
    }

    private static void triggerDetonationRecovery(LivingEntity entity) {
        int delay = 40 + entity.getRandom().nextInt(121);
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive()) {
                return;
            }

            if (entity instanceof DemonCreeperEntity creeper) {
                creeper.beginDetonationRecoverySequence();
                return;
            }

            float healAmount = Math.min(entity.getMaxHealth() * 0.5F, entity.getMaxHealth() - entity.getHealth());
            if (healAmount > 0.0F) {
                entity.heal(healAmount);
            }
        }, delay);
    }

    private static void spawnLightningRing(ServerLevel level, LivingEntity entity, double radius, int degreeStep) {
        java.util.Set<java.util.UUID> struckEntities = new java.util.HashSet<>();
        for (int degrees = 0; degrees < 360; degrees += degreeStep) {
            double radians = Math.toRadians(degrees);
            double x = entity.getX() + (Math.cos(radians) * radius);
            double z = entity.getZ() + (Math.sin(radians) * radius);

            net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(x, entity.getY(), z);
                lightning.setVisualOnly(false);
                level.addFreshEntity(lightning);
            }
        }

        AABB strikeArea = entity.getBoundingBox().inflate(radius + 2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, strikeArea,
            living -> living != entity && living.isAlive())) {
            double distance = target.distanceTo(entity);
            if (distance > radius || !struckEntities.add(target.getUUID())) {
                continue;
            }

            net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(target.position());
                lightning.setVisualOnly(false);
                level.addFreshEntity(lightning);
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 2));
        }
    }
}
