package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/** Large spider-family demon with heavy melee attacks and leap attacks. */
public class RuiFatherEntity extends AbstractDemonEntity {
    private static final EntityDataAccessor<Boolean> MOLTED =
        SynchedEntityData.defineId(RuiFatherEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ATTACK_COOLDOWN_TICKS = 16;
    private static final int NORMAL_ATTACK_TICKS = 12;
    private static final int BITE_ATTACK_TICKS = 16;
    private static final int BITE_IMPACT_DELAY_TICKS = 5;
    private static final int SLAM_ANIMATION_TICKS = 20;
    private static final int SLAM_IMPACT_DELAY_TICKS = 5;
    private static final int SHOCKWAVE_TICKS = 8;
    private static final double MELEE_RANGE = 5.0D;
    private static final double NORMAL_SLAM_RADIUS = 6.5D;
    private static final double JUMP_SLAM_RADIUS = 9.0D;
    private static final double FRONT_FLIP_MIN_DISTANCE = 10.0D;
    private static final double FRONT_FLIP_MAX_DISTANCE = 32.0D;
    private static final String BITE_TARGET_TAG = "RuiFatherBiteTarget";

    private int attackCooldownTicks;
    private int pendingBiteTicks;
    private int pendingSlamTicks;
    private int shockwaveTicks;
    private double shockwaveRadius;
    private double shockwaveMaxRadius;
    private UUID biteTargetId;
    private boolean jumpSlamActive;
    private int jumpSlamAgeTicks;
    private int nextFrontFlipTick;
    private int nextJumpSlamTick;

    public RuiFatherEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        applyCombatEffects(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 240.0D)
            .add(Attributes.ATTACK_DAMAGE, 14.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.34D)
            .add(Attributes.ARMOR, 12.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 3.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOLTED, false);
    }

    public boolean isMolted() {
        return this.entityData.get(MOLTED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new RuiFatherCombatGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    @Override
    public void tick() {
        boolean wasOnGround = onGround();
        super.tick();

        if (level().isClientSide) {
            return;
        }

        boolean shouldBeMolted = getHealth() < getMaxHealth() * 0.5F;
        if (shouldBeMolted != isMolted()) {
            entityData.set(MOLTED, shouldBeMolted);
            applyCombatEffects(shouldBeMolted);
        }

        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        tickPendingAttacks();
        tickShockwave();

        if (jumpSlamActive) {
            jumpSlamAgeTicks++;
            if (jumpSlamAgeTicks > 2 && !wasOnGround && onGround()) {
                jumpSlamActive = false;
                getNavigation().stop();
                playGeckoAnimation("giant_slam", SLAM_ANIMATION_TICKS);
                executeSlam(true);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)
            || !canTargetNonDemonVictim(livingTarget)
            || distanceToSqr(livingTarget) > (MELEE_RANGE + livingTarget.getBbWidth())
                * (MELEE_RANGE + livingTarget.getBbWidth())
            || isUsingLockedAnimation() || attackCooldownTicks > 0) {
            return false;
        }

        faceTarget(livingTarget);
        float choice = random.nextFloat();
        if (choice < 0.18F) {
            startBite(livingTarget);
        } else if (choice < 0.30F) {
            startSlam(false);
        } else if (choice < 0.65F) {
            performPunch(livingTarget, "punch_right2");
        } else {
            performPunch(livingTarget, "punch_left2");
        }
        return true;
    }

    private void performPunch(LivingEntity target, String animation) {
        getNavigation().stop();
        playGeckoAnimation(animation, NORMAL_ATTACK_TICKS);
        attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
        playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.65F + random.nextFloat() * 0.1F);
        Damager.hurt(this, target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private void startBite(LivingEntity target) {
        getNavigation().stop();
        faceTarget(target);
        playGeckoAnimation("bite3", BITE_ATTACK_TICKS);
        attackCooldownTicks = ATTACK_COOLDOWN_TICKS + 4;
        pendingBiteTicks = BITE_IMPACT_DELAY_TICKS;
        biteTargetId = target.getUUID();
        getPersistentData().putUUID(BITE_TARGET_TAG, target.getUUID());
        playSound(SoundEvents.HUSK_AMBIENT, 1.4F, 0.65F);
    }

    private void startSlam(boolean enhanced) {
        getNavigation().stop();
        playGeckoAnimation("giant_slam", SLAM_ANIMATION_TICKS);
        attackCooldownTicks = enhanced ? 30 : 24;
        pendingSlamTicks = enhanced ? 0 : SLAM_IMPACT_DELAY_TICKS;
        if (enhanced) {
            executeSlam(true);
        }
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.7F, 0.55F);
    }

    private void startJumpSlam(LivingEntity target) {
        Vec3 direction = horizontalDirectionTo(target);
        getNavigation().stop();
        faceTarget(target);
        setDeltaMovement(direction.x * 1.45D, 1.65D, direction.z * 1.45D);
        hasImpulse = true;
        hurtMarked = true;
        jumpSlamActive = true;
        jumpSlamAgeTicks = 0;
        attackCooldownTicks = 50;
        playGeckoAnimation("both_arm_up", 40);
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.5F);
        nextJumpSlamTick = tickCount + 100 + random.nextInt(101);
    }

    private void startFrontFlip(LivingEntity target) {
        Vec3 direction = horizontalDirectionTo(target);
        getNavigation().stop();
        faceTarget(target);
        setDeltaMovement(direction.x * 1.8D, 0.9D, direction.z * 1.8D);
        hasImpulse = true;
        hurtMarked = true;
        playGeckoAnimation("front_flip", 15);
        nextFrontFlipTick = tickCount + 70 + random.nextInt(71);
    }

    private void tickPendingAttacks() {
        if (pendingBiteTicks > 0 && --pendingBiteTicks == 0) {
            LivingEntity target = resolveBiteTarget();
            biteTargetId = null;
            getPersistentData().remove(BITE_TARGET_TAG);
            if (target != null && target.isAlive()
                && distanceToSqr(target) <= 30.0D && hasLineOfSight(target)) {
                if (Damager.hurt(this, target,
                    (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35D))) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 4));
                }
            }
        }

        if (pendingSlamTicks > 0 && --pendingSlamTicks == 0) {
            executeSlam(false);
        }
    }

    private LivingEntity resolveBiteTarget() {
        if (biteTargetId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(biteTargetId);
        return entity instanceof LivingEntity living && canTargetNonDemonVictim(living) ? living : null;
    }

    private void executeSlam(boolean enhanced) {
        double radius = enhanced ? JUMP_SLAM_RADIUS : NORMAL_SLAM_RADIUS;
        float damage = enhanced ? 42.0F : 26.0F;
        shockwaveTicks = SHOCKWAVE_TICKS;
        shockwaveRadius = 0.75D;
        shockwaveMaxRadius = radius;

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(0.1D), getZ(),
                enhanced ? 2 : 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
            SoundSource.HOSTILE, enhanced ? 2.3F : 1.7F, 0.65F);

        AABB impactArea = getBoundingBox().inflate(radius, 2.5D, radius);
        for (Entity entity : level().getEntities(this, impactArea,
            candidate -> candidate != this && distanceToSqr(candidate) <= radius * radius + 4.0D)) {
            Vec3 away = entity.position().subtract(position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize();
            double knockback = enhanced ? 1.45D : 1.05D;
            entity.push(away.x * knockback, enhanced ? 1.5D : 1.1D, away.z * knockback);
            entity.hurtMarked = true;
            if (entity instanceof LivingEntity living && canTargetNonDemonVictim(living)) {
                Damager.hurt(this, living, damage);
            }
        }
    }

    private void tickShockwave() {
        if (shockwaveTicks <= 0) {
            return;
        }

        double progress = 1.0D - (shockwaveTicks / (double) SHOCKWAVE_TICKS);
        shockwaveRadius = Math.max(shockwaveRadius, shockwaveMaxRadius * progress);
        if (level() instanceof ServerLevel serverLevel) {
            int points = 24;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    getX() + Math.cos(angle) * shockwaveRadius,
                    getY(0.12D),
                    getZ() + Math.sin(angle) * shockwaveRadius,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }
        shockwaveTicks--;
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        Vec3 direction = target.position().subtract(position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = getLookAngle();
            direction = new Vec3(direction.x, 0.0D, direction.z);
        }
        return direction.lengthSqr() < 1.0E-4D
            ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void faceTarget(LivingEntity target) {
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 direction = horizontalDirectionTo(target);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        setYRot(yaw);
        yRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
    }

    private void applyCombatEffects(boolean molted) {
        removeEffect(MobEffects.DAMAGE_BOOST);
        removeEffect(MobEffects.DAMAGE_RESISTANCE);
        removeEffect(MobEffects.MOVEMENT_SPEED);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE,
            molted ? 5 : 3, true, false, false));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE,
            molted ? 2 : 0, true, false, false));
        if (molted) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE,
                1, true, false, false));
        }
    }

    @Override
    protected String resolveWalkAnimation() {
        return "giant_walk2";
    }

    @Override
    protected String resolveSprintAnimation() {
        return "giant_walk2";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 2.2F, 0.55F + random.nextFloat() * 0.1F);
    }

    @Override
    public void playAmbientSound() {
        playSound(SoundEvents.SPIDER_AMBIENT, getSoundVolume(), 0.75F);
        playSound(SoundEvents.ENDERMAN_AMBIENT, getSoundVolume(), 0.75F);
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        playSound(SoundEvents.SPIDER_HURT, getSoundVolume(), 0.75F);
        playSound(SoundEvents.ENDERMAN_HURT, getSoundVolume(), 0.75F);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && isDeadOrDying()) {
            playSound(SoundEvents.SPIDER_DEATH, getSoundVolume(), 0.75F);
            playSound(SoundEvents.ENDERMAN_DEATH, getSoundVolume(), 0.75F);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        // His leap slam is an attack, not self-inflicted fall damage.
        return false;
    }

    private final class RuiFatherCombatGoal extends Goal {
        private RuiFatherCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && canTargetNonDemonVictim(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && canTargetNonDemonVictim(target);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            faceTarget(target);
            double distanceSq = distanceToSqr(target);
            if (jumpSlamActive) {
                getNavigation().stop();
                return;
            }

            if (getAnimationTicks() > 0) {
                getNavigation().stop();
                return;
            }

            if (distanceSq >= FRONT_FLIP_MIN_DISTANCE * FRONT_FLIP_MIN_DISTANCE
                && distanceSq <= FRONT_FLIP_MAX_DISTANCE * FRONT_FLIP_MAX_DISTANCE
                && tickCount >= nextFrontFlipTick && random.nextFloat() < 0.025F) {
                startFrontFlip(target);
                return;
            }

            if (distanceSq >= 6.0D * 6.0D && distanceSq <= 16.0D * 16.0D
                && onGround() && tickCount >= nextJumpSlamTick && random.nextFloat() < 0.012F) {
                startJumpSlam(target);
                return;
            }

            if (distanceSq > MELEE_RANGE * MELEE_RANGE) {
                getNavigation().moveTo(target, 1.15D);
            } else if (hasLineOfSight(target) && attackCooldownTicks <= 0) {
                doHurtTarget(target);
            }
        }
    }
}
