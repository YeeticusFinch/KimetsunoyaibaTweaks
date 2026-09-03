package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A leaping demon that attacks by repeatedly crashing down around its target. */
public class MantisDemonEntity extends AbstractDemonEntity {
    private static final double MAX_LEAP_VERTICAL_VELOCITY = 2.0D;
    private static final double MIN_LEAP_VERTICAL_VELOCITY = MAX_LEAP_VERTICAL_VELOCITY / 3.0D;
    private static final double LEAP_HORIZONTAL_VELOCITY = 2.0D;
    private static final int LEAP_ANIMATION_TICKS = 8;
    private static final int LAND_ANIMATION_TICKS = 5;
    private static final int LEAP_DELAY_AFTER_LANDING_TICKS = 5;
    private static final double LANDING_RADIUS = 4.0D;
    private static final double LANDING_RADIUS_SQUARED = LANDING_RADIUS * LANDING_RADIUS;
    private static final float LANDING_DAMAGE = 8.0F;

    private int leapDelayTicks;
    private int lastLandingTick = -1;

    public MantisDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 90.0D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.30D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            tickCombatLeap();
        }
    }

    private void tickCombatLeap() {
        if (this.lastLandingTick == this.tickCount) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !canTargetNonDemonVictim(target)) {
            this.leapDelayTicks = 0;
            return;
        }

        if (this.leapDelayTicks > 0) {
            this.leapDelayTicks--;
            if (this.leapDelayTicks > 0) {
                return;
            }
        }

        if (this.onGround() && !this.isInWaterOrBubble() && !this.isNoGravity()
            && this.getAnimationTicks() <= 0) {
            launchAt(target);
        }
    }

    private void launchAt(LivingEntity target) {
        Vec3 horizontal = target.position().subtract(this.position());
        horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = this.getLookAngle();
            horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
        }
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }

        horizontal = horizontal.normalize().scale(LEAP_HORIZONTAL_VELOCITY);
        double verticalVelocity = MIN_LEAP_VERTICAL_VELOCITY
            + this.getRandom().nextDouble() * (MAX_LEAP_VERTICAL_VELOCITY - MIN_LEAP_VERTICAL_VELOCITY);
        this.getNavigation().stop();
        this.setSprinting(false);
        this.setDeltaMovement(horizontal.x, verticalVelocity, horizontal.z);
        this.hurtMarked = true;
        this.playGeckoAnimation("mantis_jump", LEAP_ANIMATION_TICKS);
        this.level().playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
            SoundSource.HOSTILE, 1.0F, 0.9F + this.getRandom().nextFloat() * 0.2F);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        boolean hardLanding = fallDistance > 2.0F && !this.isInWaterOrBubble() && !this.isNoGravity();
        if (hardLanding && !this.level().isClientSide && this.lastLandingTick != this.tickCount) {
            triggerLandingImpact();
        }
        return false;
    }

    private void triggerLandingImpact() {
        this.lastLandingTick = this.tickCount;
        this.leapDelayTicks = LEAP_DELAY_AFTER_LANDING_TICKS;
        this.playGeckoAnimation("mantis_land", LAND_ANIMATION_TICKS);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY(0.1D), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY(0.1D), this.getZ(), 8, 0.8D, 0.15D, 0.8D, 0.04D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
            SoundSource.HOSTILE, 1.25F, 0.9F + this.getRandom().nextFloat() * 0.2F);

        AABB impactArea = this.getBoundingBox().inflate(LANDING_RADIUS, 1.5D, LANDING_RADIUS);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, impactArea,
            target -> target != this && target.isAlive() && !Damager.isDemon(target)
                && this.distanceToSqr(target) <= LANDING_RADIUS_SQUARED + target.getBbWidth())) {
            if (Damager.hurt(this, target, LANDING_DAMAGE)) {
                Vec3 knockback = target.position().subtract(this.position());
                if (knockback.lengthSqr() > 1.0E-4D) {
                    knockback = knockback.normalize().scale(0.9D);
                    target.push(knockback.x, 0.45D, knockback.z);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity target) || Damager.isDemon(target)) {
            return false;
        }
        return Damager.hurt(this, target, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    protected String resolveIdleAnimation() {
        return this.onGround() ? "mantis_idle" : "mantis_fall";
    }

    @Override
    protected String resolveWalkAnimation() {
        return this.onGround() ? "mantis_walk" : "mantis_fall";
    }

    @Override
    protected String resolveSprintAnimation() {
        return "mantis_walk";
    }

    @Override
    protected boolean isBaseMovementAnimation(String animation) {
        return "mantis_idle".equals(animation) || "mantis_walk".equals(animation)
            || "mantis_fall".equals(animation);
    }
}
