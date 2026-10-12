package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.RuiBloodDemonArt;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Rui, Lower Five, using the shared biped animation set. */
public class RuiEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = RuiBloodDemonArt.ART_ID;

    private static final double MIN_RANGED_ATTACK_DISTANCE = 10.0D;
    private static final double MAX_RANGED_ATTACK_DISTANCE = 40.0D;
    private static final double BACKSTEP_HORIZONTAL_SPEED = 1.75D;
    private static final double BACKSTEP_VERTICAL_SPEED = 0.85D;
    private int threadAttackCooldownTicks;
    private int nextBackstepTick;

    public RuiEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 4,
            true, false, false));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1,
            true, false, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 120.0D)
            .add(Attributes.ATTACK_DAMAGE, 10.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.22D)
            .add(Attributes.ARMOR, 10.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 1.5D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    public static void registerBloodDemonArt() {
        RuiBloodDemonArt.register();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new RuiRangedCombatGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)
            || !canTargetNonDemonVictim(livingTarget)
            || RuiBloodDemonArt.isDecapitationActive(this)
            || distanceToSqr(livingTarget) > MAX_RANGED_ATTACK_DISTANCE * MAX_RANGED_ATTACK_DISTANCE
            || !hasLineOfSight(livingTarget)
            || threadAttackCooldownTicks > 0
            || isUsingLockedAnimation()) {
            return false;
        }

        boolean basicAttack = this.random.nextFloat() < 0.66F;
        boolean performed = basicAttack
            ? RuiBloodDemonArt.performBasicAttack(this, livingTarget)
            : RuiBloodDemonArt.performSilkStrike(this, livingTarget);
        if (performed) {
            threadAttackCooldownTicks = 12;
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        setSprinting(false);
        if (!level().isClientSide) {
            RuiBloodDemonArt.tickDecapitation(this);
            if (getHealth() <= getMaxHealth() * 0.10F
                && getExternalBloodDemonArtCooldownTicks() <= 0
                && !RuiBloodDemonArt.isDecapitationActive(this)
                && RuiBloodDemonArt.tryStartDecapitateSelf(this)) {
                setExternalBloodDemonArtCooldownTicks(600);
            }
            if (threadAttackCooldownTicks > 0) {
                threadAttackCooldownTicks--;
            }
            if (getDeltaMovement().horizontalDistanceSqr() < 0.0001D && !isUsingLockedAnimation()) {
                setSprinting(false);
            }
        }
    }

    @Override
    protected boolean canUseBloodDemonArtAgainst(LivingEntity target) {
        return !RuiBloodDemonArt.isDecapitationActive(this)
            && target != null
            && distanceToSqr(target) <= MAX_RANGED_ATTACK_DISTANCE * MAX_RANGED_ATTACK_DISTANCE
            && hasLineOfSight(target);
    }

    @Override
    protected String resolveSprintAnimation() {
        return resolveWalkAnimation();
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return false;
    }

    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        setYRot(yaw);
        yRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private final class RuiRangedCombatGoal extends Goal {
        private RuiRangedCombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && canTargetNonDemonVictim(target)
                && distanceToSqr(target) <= MAX_RANGED_ATTACK_DISTANCE * MAX_RANGED_ATTACK_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && canTargetNonDemonVictim(target)
                && distanceToSqr(target) <= (MAX_RANGED_ATTACK_DISTANCE + 8.0D)
                    * (MAX_RANGED_ATTACK_DISTANCE + 8.0D);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            faceTarget(target);
            double distanceSqr = distanceToSqr(target);
            if (distanceSqr < MIN_RANGED_ATTACK_DISTANCE * MIN_RANGED_ATTACK_DISTANCE) {
                if (hasLineOfSight(target) && threadAttackCooldownTicks <= 0) {
                    doHurtTarget(target);
                }
                if (tickCount >= nextBackstepTick && random.nextFloat() < 0.12F) {
                    performBackstep(target);
                } else {
                    moveAwayFrom(target);
                }
                return;
            }

            if (distanceSqr > MAX_RANGED_ATTACK_DISTANCE * MAX_RANGED_ATTACK_DISTANCE) {
                getNavigation().moveTo(target, 0.7D);
                return;
            }

            getNavigation().stop();
            if (hasLineOfSight(target) && threadAttackCooldownTicks <= 0) {
                doHurtTarget(target);
            }
        }

        private void moveAwayFrom(LivingEntity target) {
            Vec3 away = position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-6D) {
                away = getLookAngle().scale(-1.0D);
                away = new Vec3(away.x, 0.0D, away.z);
            }
            if (away.lengthSqr() > 1.0E-6D) {
                away = away.normalize();
                Vec3 destination = position().add(away.scale(8.0D));
                getNavigation().moveTo(destination.x, destination.y, destination.z, 0.7D);
            }
        }

        private void performBackstep(LivingEntity target) {
            Vec3 away = position().subtract(target.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-6D) {
                away = getLookAngle().scale(-1.0D);
                away = new Vec3(away.x, 0.0D, away.z);
            }
            away = away.normalize();
            getNavigation().stop();
            setDeltaMovement(away.x * BACKSTEP_HORIZONTAL_SPEED, BACKSTEP_VERTICAL_SPEED,
                away.z * BACKSTEP_HORIZONTAL_SPEED);
            hasImpulse = true;
            hurtMarked = true;
            playGeckoAnimation("backstep", 8);
            nextBackstepTick = tickCount + 50 + random.nextInt(31);
        }
    }

    @Override
    protected com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry.RegisteredBloodDemonArt getBloodDemonArt() {
        return com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry.getArt(BLOOD_DEMON_ART_ID);
    }

    @Override
    protected double getBloodDemonArtRange() {
        return MAX_RANGED_ATTACK_DISTANCE;
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return 0.08F;
    }
}
