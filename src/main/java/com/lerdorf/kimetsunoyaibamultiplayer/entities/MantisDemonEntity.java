package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MantisSpinSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
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
    private static final int LEAP_ANIMATION_TICKS = 13;
    private static final int LAND_ANIMATION_TICKS = 5;
    private static final int LEAP_DELAY_AFTER_LANDING_TICKS = 5;
    private static final int CLOSE_COMBAT_RANGE = 10;
    private static final int CLOSE_COMBAT_MIN_TICKS = 100;
    private static final int CLOSE_COMBAT_MAX_TICKS = 200;
    private static final int JUMP_PHASE_MIN_TICKS = 100;
    private static final int JUMP_PHASE_MAX_TICKS = 200;
    private static final float BACKSTEP_CHANCE = 0.30F;
    private static final int BACKSTEP_ANIMATION_TICKS = 10;
    private static final int BACKSTEP_MIN_COOLDOWN_TICKS = 30;
    private static final int BACKSTEP_BASE_COOLDOWN_TICKS = 60;
    private static final int BACKSTEP_COOLDOWN_VARIANCE = 15;
    private static final double MELEE_ATTACK_RANGE = 3.0D;
    private static final double MELEE_ATTACK_RANGE_SQUARED = MELEE_ATTACK_RANGE * MELEE_ATTACK_RANGE;
    private static final double MELEE_AOE_BOX_SIZE = 5.0D;
    private static final int MELEE_ANIMATION_TICKS = 10;
    private static final int SPIN_ATTACK_TICKS = 15;
    private static final double SPIN_ATTACK_RADIUS = 5.0D;
    private static final double SPIN_PARTICLE_RADIUS = 3.5D;
    private static final double SPIN_KNOCKBACK = 1.25D;
    private static final double WEAK_ATTACK_DEFENSE = 3.0D;
    private static final String SWING_INDEX_TAG = "KnYMantisSwingIndex";
    private static final String[] SWING_ANIMATIONS = {
        "mantis_swing_0",
        "mantis_swing_1",
        "mantis_swing_2",
        "mantis_swing_3"
    };
    private static final double LANDING_RADIUS = 4.0D;
    private static final double LANDING_RADIUS_SQUARED = LANDING_RADIUS * LANDING_RADIUS;

    private int leapDelayTicks;
    private int lastLandingTick = -1;
    private int spinAttackTicks;
    private int closeCombatTicks;
    private int jumpPhaseTicks;
    private int nextBackstepTick;

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
        this.goalSelector.addGoal(1, new MantisMeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    private static final class MantisMeleeAttackGoal extends MeleeAttackGoal {
        private MantisMeleeAttackGoal(MantisDemonEntity mantis, double speedModifier,
            boolean followingTargetEvenIfNotSeen) {
            super(mantis, speedModifier, followingTargetEvenIfNotSeen);
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return MELEE_ATTACK_RANGE_SQUARED;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            tickCombatPhase();
            tickSpinAttack();
            tickCombatLeap();
        }
    }

    private void tickCombatPhase() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !canTargetNonDemonVictim(target)
            || this.distanceToSqr(target) > CLOSE_COMBAT_RANGE * CLOSE_COMBAT_RANGE) {
            this.closeCombatTicks = 0;
            this.jumpPhaseTicks = 0;
            return;
        }

        if (this.closeCombatTicks > 0) {
            this.closeCombatTicks--;
            if (this.closeCombatTicks == 0) {
                this.jumpPhaseTicks = randomTicks(JUMP_PHASE_MIN_TICKS, JUMP_PHASE_MAX_TICKS);
            }
            return;
        }

        if (this.jumpPhaseTicks > 0) {
            this.jumpPhaseTicks--;
            return;
        }

        this.closeCombatTicks = randomTicks(CLOSE_COMBAT_MIN_TICKS, CLOSE_COMBAT_MAX_TICKS);
    }

    private int randomTicks(int minimum, int maximum) {
        return minimum + this.getRandom().nextInt(maximum - minimum + 1);
    }

    private void tickSpinAttack() {
        if (this.spinAttackTicks <= 0) {
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double angle = (this.tickCount * 0.35D) + (i * (Math.PI * 2.0D / 8.0D));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    this.getX() + Math.cos(angle) * SPIN_PARTICLE_RADIUS,
                    this.getY(0.55D),
                    this.getZ() + Math.sin(angle) * SPIN_PARTICLE_RADIUS,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
            }
        }

        this.spinAttackTicks--;
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

        if (this.closeCombatTicks > 0) {
            return;
        }

        // At close range, the melee goal chooses between swings, spin, and a leap.
        if (this.distanceToSqr(target) <= MELEE_ATTACK_RANGE_SQUARED) {
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

    private boolean performMeleeSwing() {
        if (this.getAnimationTicks() > 0 || this.spinAttackTicks > 0) {
            return false;
        }

        int index = Math.floorMod(this.getPersistentData().getInt(SWING_INDEX_TAG), SWING_ANIMATIONS.length);
        String animation = SWING_ANIMATIONS[index];
        this.getPersistentData().putInt(SWING_INDEX_TAG, (index + 1) % SWING_ANIMATIONS.length);

        this.getNavigation().stop();
        this.playGeckoAnimation(animation, MELEE_ANIMATION_TICKS);
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.HOSTILE, 1.0F, 0.95F + this.getRandom().nextFloat() * 0.1F);
        sendClawSlash(animation);
        setWeakAttackState(MELEE_ANIMATION_TICKS);
        damageMeleeTargets();
        return true;
    }

    private boolean performSpinAttack() {
        if (this.getAnimationTicks() > 0 || this.spinAttackTicks > 0) {
            return false;
        }

        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.playGeckoAnimation("mantis_spin", SPIN_ATTACK_TICKS);
        this.spinAttackTicks = SPIN_ATTACK_TICKS;
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.HOSTILE, 1.25F, 0.8F + this.getRandom().nextFloat() * 0.1F);

        if (this.level() instanceof ServerLevel serverLevel) {
            ModNetworking.sendToNearby(
                new MantisSpinSlashPacket(this.getUUID()),
                serverLevel,
                this.getX(), this.getY(), this.getZ(),
                com.lerdorf.kimetsunoyaibamultiplayer.Config.mobSlashBroadcastRange
            );
        }
        setWeakAttackState(SPIN_ATTACK_TICKS);
        damageSpinTargets();
        return true;
    }

    private void sendClawSlash(String animation) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ModNetworking.sendToNearby(
            new MobSwordSlashPacket(this.getUUID(), animation, 0, "claw"),
            serverLevel,
            this.getX(), this.getY(), this.getZ(),
            com.lerdorf.kimetsunoyaibamultiplayer.Config.mobSlashBroadcastRange
        );
    }

    private void setWeakAttackState(int durationTicks) {
        GuardStateHelper.setWeakAttackState(this, WEAK_ATTACK_DEFENSE);
        AbilityScheduler.scheduleOnce(this, () -> GuardStateHelper.clearGuardState(this), durationTicks);
    }

    private float getMantisAttackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void damageMeleeTargets() {
        Vec3 eyePos = this.position().add(0.0D, this.getEyeHeight(), 0.0D);
        Vec3 lookVec = this.getLookAngle().normalize();
        Vec3 frontPos = eyePos.add(lookVec.scale(MELEE_AOE_BOX_SIZE / 1.5D));
        AABB attackBox = new AABB(
            frontPos.add(-MELEE_AOE_BOX_SIZE / 2.0D, -MELEE_AOE_BOX_SIZE / 2.0D, -MELEE_AOE_BOX_SIZE / 2.0D),
            frontPos.add(MELEE_AOE_BOX_SIZE / 2.0D, MELEE_AOE_BOX_SIZE / 2.0D, MELEE_AOE_BOX_SIZE / 2.0D)
        );

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            target -> target != this && target.isAlive() && canTargetNonDemonVictim(target))) {
            if (Damager.hurt(this, target, getMantisAttackDamage())) {
                target.knockback(0.2D, this.getX() - target.getX(), this.getZ() - target.getZ());
            }
        }
    }

    private void damageSpinTargets() {
        AABB attackBox = this.getBoundingBox().inflate(SPIN_ATTACK_RADIUS, 1.5D, SPIN_ATTACK_RADIUS);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            candidate -> candidate != this && candidate.isAlive()
                && this.distanceToSqr(candidate) <= SPIN_ATTACK_RADIUS * SPIN_ATTACK_RADIUS
                && canTargetNonDemonVictim(candidate))) {
            if (Damager.hurt(this, target, getMantisAttackDamage())) {
                target.knockback(SPIN_KNOCKBACK, this.getX() - target.getX(), this.getZ() - target.getZ());
            }
        }
    }

    private void tryBackstep(LivingEntity attacker) {
        if (attacker == null || this.tickCount < this.nextBackstepTick || this.getAnimationTicks() > 0
            || !this.onGround() || this.getRandom().nextFloat() >= BACKSTEP_CHANCE) {
            return;
        }

        Vec3 away = this.position().subtract(attacker.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-4D) {
            away = this.getLookAngle().scale(-1.0D);
            away = new Vec3(away.x, 0.0D, away.z);
        }
        if (away.lengthSqr() < 1.0E-4D) {
            return;
        }

        away = away.normalize();
        this.setDeltaMovement(away.x, 0.45D, away.z);
        this.hurtMarked = true;
        this.playGeckoAnimation("backstep", BACKSTEP_ANIMATION_TICKS);

        int jitter = this.getRandom().nextInt(BACKSTEP_COOLDOWN_VARIANCE * 2 + 1)
            - BACKSTEP_COOLDOWN_VARIANCE;
        this.nextBackstepTick = this.tickCount
            + Math.max(BACKSTEP_MIN_COOLDOWN_TICKS, BACKSTEP_BASE_COOLDOWN_TICKS + jitter);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        boolean hardLanding = fallDistance > 2.0F && !this.isInWaterOrBubble() && !this.isNoGravity();
        if (hardLanding && !this.level().isClientSide && this.lastLandingTick != this.tickCount) {
            triggerLandingImpact();
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide && this.isAlive()
            && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            tryBackstep(attacker);
        }
        return damaged;
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
            if (Damager.hurt(this, target, getMantisAttackDamage())) {
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
        if (!(entity instanceof LivingEntity target) || !canTargetNonDemonVictim(target)
            || this.distanceToSqr(target) > MELEE_ATTACK_RANGE_SQUARED) {
            return false;
        }

        return switch (this.getRandom().nextInt(4)) {
            case 0, 1 -> performMeleeSwing();
            case 2 -> performSpinAttack();
            default -> {
                launchAt(target);
                yield true;
            }
        };
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
