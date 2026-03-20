package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * GeckoLib-backed poodle companion built on top of vanilla wolf behavior.
 */
public class PrincessEntity extends Wolf implements GeoEntity {
    private static final double WALK_SPEED = 0.3D;
    private static final double SPRINT_SPEED = 0.6D;
    private static final double OWNER_FAR_DISTANCE = 12.0D;
    private static final double OWNER_SLEEP_DISTANCE = 2.75D;
    private static final double OWNER_RELAX_DISTANCE = 5.0D;
    private static final int OWNER_STILL_TICKS_FOR_LAY = 20 * 20;
    private static final int ROLL_DURATION_TICKS = 16;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean awaitingEggOwner;
    private boolean restoreSitAfterSleep;
    private boolean relaxing;
    private boolean wasInWater;
    private int rollTicks;
    private int rollDirection;
    private int shakeAnimationTicks;
    private int ownerStillTicks;
    private BlockPos lastOwnerBlockPos;

    public PrincessEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PrincessSleepGoal(this));
        this.goalSelector.addGoal(3, new PrincessOrderedSitGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new PrincessFollowOwnerGoal(this));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    private void syncPrincessStats() {
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);

        if (maxHealth != null) {
            maxHealth.setBaseValue(100.0D);
            this.setHealth(100.0F);
        }

        if (attackDamage != null) {
            attackDamage.setBaseValue(8.0D);
        }

        if (movementSpeed != null) {
            movementSpeed.setBaseValue(WALK_SPEED);
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.setPersistenceRequired();
        this.syncPrincessStats();
        this.setHealth(this.getMaxHealth());
        this.awaitingEggOwner = reason == MobSpawnType.SPAWN_EGG && !this.isTame();
        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.awaitingEggOwner) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0D);
            if (nearestPlayer != null) {
                this.tame(nearestPlayer);
                this.syncPrincessStats();
                this.setHealth(this.getMaxHealth());
                this.setOrderedToSit(false);
                this.awaitingEggOwner = false;
            }
        }

        this.syncPrincessStats();
        this.handleSleepingOwner();
        this.updateRelaxingState();
        this.updateSprintSpeedState();
        this.updateWaterShakeState();
    }

    private void handleSleepingOwner() {
        LivingEntity owner = this.getOwner();

        if (!(owner instanceof Player player) || !player.isSleeping()) {
            if (this.restoreSitAfterSleep && !this.isOrderedToSit()) {
                this.setOrderedToSit(true);
            }

            this.restoreSitAfterSleep = false;
            return;
        }

        if (this.isOrderedToSit()) {
            this.restoreSitAfterSleep = true;
            this.setOrderedToSit(false);
        }

        this.setTarget(null);

        double distance = this.distanceTo(player);
        if (distance > OWNER_SLEEP_DISTANCE) {
            this.getNavigation().moveTo(player, this.shouldSprintMovement() ? 1.2D : 1.0D);
        } else {
            this.getNavigation().stop();
        }
    }

    private void updateRelaxingState() {
        LivingEntity owner = this.getOwner();

        if (this.shouldSleepNearOwner()) {
            this.relaxing = false;
            this.rollTicks = 0;
            this.rollDirection = 0;
            return;
        }

        if (!this.isTame() || owner == null || this.isOrderedToSit() || this.isInterested() || this.getTarget() != null) {
            this.relaxing = false;
            this.rollTicks = 0;
            this.rollDirection = 0;
            this.ownerStillTicks = 0;
            this.lastOwnerBlockPos = owner != null ? owner.blockPosition() : null;
            return;
        }

        BlockPos ownerPos = owner.blockPosition();
        if (ownerPos.equals(this.lastOwnerBlockPos)) {
            this.ownerStillTicks++;
        } else {
            this.ownerStillTicks = 0;
            this.lastOwnerBlockPos = ownerPos;
        }

        if (this.ownerStillTicks < OWNER_STILL_TICKS_FOR_LAY || this.distanceTo(owner) > OWNER_RELAX_DISTANCE) {
            this.relaxing = false;
            this.rollTicks = 0;
            this.rollDirection = 0;
            return;
        }

        if (this.getNavigation().isDone() && this.getDeltaMovement().horizontalDistanceSqr() < 0.0025D) {
            this.relaxing = true;
            this.getNavigation().stop();

            if (this.rollTicks > 0) {
                this.rollTicks--;
                if (this.rollTicks <= 0) {
                    this.rollDirection = 0;
                }
            } else if (this.random.nextInt(140) == 0) {
                this.rollDirection = this.random.nextBoolean() ? 1 : -1;
                this.rollTicks = ROLL_DURATION_TICKS;
            }
        } else {
            this.relaxing = false;
            this.rollTicks = 0;
            this.rollDirection = 0;
        }
    }

    private void updateSprintSpeedState() {
        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        double targetSpeed = this.shouldSprintMovement() ? SPRINT_SPEED : WALK_SPEED;
        if (Math.abs(movementSpeed.getBaseValue() - targetSpeed) > 0.0001D) {
            movementSpeed.setBaseValue(targetSpeed);
        }
    }

    private void updateWaterShakeState() {
        boolean inWater = this.isInWaterOrBubble();

        if (this.wasInWater && !inWater && this.shakeAnimationTicks <= 0) {
            this.shakeAnimationTicks = 20;
            this.playSound(SoundEvents.WOLF_SHAKE, 0.8F, this.getVoicePitch());
        }

        this.wasInWater = inWater;

        if (this.shakeAnimationTicks > 0) {
            this.shakeAnimationTicks--;
            if (this.shakeAnimationTicks == 10) {
                this.playSound(SoundEvents.WOLF_SHAKE, 0.8F, this.getVoicePitch());
            }
        }
    }

    public boolean shouldSprintMovement() {
        LivingEntity owner = this.getOwner();
        boolean farFromOwner = this.isTame() && owner != null && this.distanceTo(owner) > OWNER_FAR_DISTANCE;
        return this.getTarget() != null || farFromOwner;
    }

    public boolean shouldSleepNearOwner() {
        LivingEntity owner = this.getOwner();
        return owner instanceof Player player
            && player.isSleeping()
            && this.distanceTo(player) <= OWNER_SLEEP_DISTANCE
            && this.getNavigation().isDone();
    }

    public boolean isRelaxing() {
        return this.relaxing;
    }

    public boolean isRollingLeft() {
        return this.rollTicks > 0 && this.rollDirection < 0;
    }

    public boolean isRollingRight() {
        return this.rollTicks > 0 && this.rollDirection > 0;
    }

    public boolean shouldUseClosedEyesTexture() {
        return this.shouldSleepNearOwner();
    }

    public boolean isPrincessShaking() {
        return this.shakeAnimationTicks > 0;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BEEF)
            || stack.is(Items.CHICKEN)
            || stack.is(Items.PORKCHOP)
            || stack.is(Items.RABBIT)
            || stack.is(Items.MUTTON)
            || stack.is(Items.SALMON)
            || stack.is(Items.COD)
            || stack.is(Items.COOKED_BEEF)
            || stack.is(Items.COOKED_CHICKEN)
            || stack.is(Items.COOKED_PORKCHOP)
            || stack.is(Items.COOKED_RABBIT)
            || stack.is(Items.COOKED_MUTTON)
            || stack.is(Items.COOKED_SALMON)
            || stack.is(Items.COOKED_COD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);

        if (!this.level().isClientSide && result.consumesAction()) {
            this.relaxing = false;
            this.rollTicks = 0;
            this.rollDirection = 0;
        }

        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide || !this.isPrincessShaking()) {
            return;
        }

        if (this.tickCount % 2 == 0) {
            Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
            for (int i = 0; i < 4; i++) {
                double vx = (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 0.6D;
                double vy = this.random.nextDouble() * 0.08D;
                double vz = (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 0.6D;
                ((ServerLevel)this.level()).sendParticles(ParticleTypes.SPLASH,
                    center.x + vx, center.y, center.z + vz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAngry()) {
            return SoundEvents.WOLF_GROWL;
        }

        if (this.isTame() && this.getHealth() < this.getMaxHealth() * 0.35F) {
            return SoundEvents.WOLF_WHINE;
        }

        return this.random.nextInt(3) == 0 ? SoundEvents.WOLF_PANT : SoundEvents.WOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, this.getVoicePitch());
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.18F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 2, state -> {
            if (this.isPrincessShaking()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("shake"));
            }

            if (this.shouldSleepNearOwner()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("sleep"));
            }

            if (this.isInSittingPose()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("sit"));
            }

            if (this.isRollingLeft()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("roll_left"));
            }

            if (this.isRollingRight()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("roll_right"));
            }

            if (this.isRelaxing()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("lay"));
            }

            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(this.shouldSprintMovement() ? "sprint" : "walk"));
            }

            if (this.isInterested()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("wag"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public PrincessEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        PrincessEntity puppy = ModEntities.PRINCESS.get().create(serverLevel);
        if (puppy != null && ageableMob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            puppy.setOwnerUUID(ownable.getOwnerUUID());
            puppy.setTame(true);
            puppy.syncPrincessStats();
        }
        return puppy;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("AwaitingEggOwner", this.awaitingEggOwner);
        tag.putBoolean("RestoreSitAfterSleep", this.restoreSitAfterSleep);
        tag.putBoolean("WasInWater", this.wasInWater);
        tag.putInt("OwnerStillTicks", this.ownerStillTicks);
        tag.putBoolean("Relaxing", this.relaxing);
        tag.putInt("RollTicks", this.rollTicks);
        tag.putInt("RollDirection", this.rollDirection);
        tag.putInt("ShakeAnimationTicks", this.shakeAnimationTicks);

        if (this.lastOwnerBlockPos != null) {
            tag.putInt("LastOwnerX", this.lastOwnerBlockPos.getX());
            tag.putInt("LastOwnerY", this.lastOwnerBlockPos.getY());
            tag.putInt("LastOwnerZ", this.lastOwnerBlockPos.getZ());
            tag.putBoolean("HasLastOwnerPos", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.awaitingEggOwner = tag.getBoolean("AwaitingEggOwner");
        this.restoreSitAfterSleep = tag.getBoolean("RestoreSitAfterSleep");
        this.wasInWater = tag.getBoolean("WasInWater");
        this.ownerStillTicks = tag.getInt("OwnerStillTicks");
        this.relaxing = tag.getBoolean("Relaxing");
        this.rollTicks = tag.getInt("RollTicks");
        this.rollDirection = tag.getInt("RollDirection");
        this.shakeAnimationTicks = tag.getInt("ShakeAnimationTicks");

        if (tag.getBoolean("HasLastOwnerPos")) {
            this.lastOwnerBlockPos = new BlockPos(tag.getInt("LastOwnerX"), tag.getInt("LastOwnerY"), tag.getInt("LastOwnerZ"));
        } else {
            this.lastOwnerBlockPos = null;
        }
    }

    private static class PrincessFollowOwnerGoal extends Goal {
        private final PrincessEntity princess;

        private PrincessFollowOwnerGoal(PrincessEntity princess) {
            this.princess = princess;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.princess.getOwner();
            return owner != null
                && !owner.isSpectator()
                && !this.princess.isOrderedToSit()
                && !this.princess.shouldSleepNearOwner()
                && !owner.isSleeping()
                && this.princess.distanceTo(owner) > 3.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = this.princess.getOwner();
            return owner != null
                && !this.princess.getNavigation().isDone()
                && !this.princess.isOrderedToSit()
                && !owner.isSleeping()
                && this.princess.distanceTo(owner) > 2.5D;
        }

        @Override
        public void tick() {
            LivingEntity owner = this.princess.getOwner();
            if (owner == null) {
                return;
            }

            double speed = this.princess.shouldSprintMovement() ? 1.2D : 1.0D;
            this.princess.getLookControl().setLookAt(owner, 10.0F, this.princess.getMaxHeadXRot());
            this.princess.getNavigation().moveTo(owner, speed);
        }

        @Override
        public void stop() {
            this.princess.getNavigation().stop();
        }
    }

    private static class PrincessOrderedSitGoal extends Goal {
        private final PrincessEntity princess;

        private PrincessOrderedSitGoal(PrincessEntity princess) {
            this.princess = princess;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.princess.isOrderedToSit() && !this.princess.shouldSleepNearOwner();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.princess.getNavigation().stop();
            this.princess.setTarget(null);
        }

        @Override
        public void tick() {
            this.princess.getNavigation().stop();
        }
    }

    private static class PrincessSleepGoal extends Goal {
        private final PrincessEntity princess;

        private PrincessSleepGoal(PrincessEntity princess) {
            this.princess = princess;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.princess.getOwner();
            return owner instanceof Player player && player.isSleeping();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            LivingEntity owner = this.princess.getOwner();
            if (owner == null) {
                return;
            }

            if (this.princess.distanceTo(owner) > OWNER_SLEEP_DISTANCE) {
                double speed = this.princess.shouldSprintMovement() ? 1.2D : 1.0D;
                this.princess.getNavigation().moveTo(owner, speed);
                this.princess.getLookControl().setLookAt(owner, 10.0F, this.princess.getMaxHeadXRot());
            } else {
                this.princess.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            this.princess.getNavigation().stop();
        }
    }
}
