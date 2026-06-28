package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.CreepingRuin;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

import java.util.UUID;

public class DemonCreeperEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = CreepingRuin.ART_ID;
    private static final UUID CHARGED_ATTACK_UUID = UUID.fromString("a1b65ec2-bf38-42e2-8016-7e9e78f67001");
    private static final UUID COMBAT_SPRINT_UUID = UUID.fromString("4eb84b57-2c4f-4bea-8db8-e0d633f6c9a7");
    private static final AttributeModifier CHARGED_ATTACK_MODIFIER =
        new AttributeModifier(CHARGED_ATTACK_UUID, "Charged demon creeper bonus", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier COMBAT_SPRINT_MODIFIER =
        new AttributeModifier(COMBAT_SPRINT_UUID, "Demon creeper combat sprint", 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final EntityDataAccessor<Boolean> CHARGED =
        SynchedEntityData.defineId(DemonCreeperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CHARGED_TICKS =
        SynchedEntityData.defineId(DemonCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIDDEN_TICKS =
        SynchedEntityData.defineId(DemonCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REGENERATING_TICKS =
        SynchedEntityData.defineId(DemonCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DETONATION_PRIME_TICKS =
        SynchedEntityData.defineId(DemonCreeperEntity.class, EntityDataSerializers.INT);

    private int meleeAnimationTicks = 0;
    private int combatSprintTicks = 0;
    private int combatSprintCooldownTicks = 80;
    private int headSpinCooldownTicks = 0;

    public DemonCreeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static void registerBloodDemonArt() {
        CreepingRuin.register();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGED, false);
        this.entityData.define(CHARGED_TICKS, 0);
        this.entityData.define(HIDDEN_TICKS, 0);
        this.entityData.define(REGENERATING_TICKS, 0);
        this.entityData.define(DETONATION_PRIME_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DemonCreeperMeleeGoal(this, 1.1D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            this::canTargetNonDemonVictim));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            tickChargedState();
            tickDetonationPrime();
            tickHideAndRegeneration();
            tickCombatSprint();
        }

        if (meleeAnimationTicks > 0) {
            meleeAnimationTicks--;
        }
        if (headSpinCooldownTicks > 0) {
            headSpinCooldownTicks--;
        }
    }

    private void tickCombatSprint() {
        if (combatSprintCooldownTicks > 0) {
            combatSprintCooldownTicks--;
        }

        boolean shouldSprint = false;
        if (!isHiddenState() && !isRegenerating()) {
            LivingEntity target = this.getTarget();
            if (combatSprintTicks > 0) {
                combatSprintTicks--;
                shouldSprint = target != null && target.isAlive();
            } else if (target != null && target.isAlive() && this.distanceToSqr(target) <= 256.0D
                && combatSprintCooldownTicks <= 0 && this.random.nextFloat() < 0.0085F) {
                combatSprintTicks = 20 * 10;
                combatSprintCooldownTicks = 20 * (14 + this.random.nextInt(10));
                shouldSprint = true;
            }
        } else {
            combatSprintTicks = 0;
        }

        this.setSprinting(shouldSprint);

        AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        boolean hasModifier = movement.getModifier(COMBAT_SPRINT_UUID) != null;
        if (shouldSprint && !hasModifier) {
            movement.addTransientModifier(COMBAT_SPRINT_MODIFIER);
        } else if (!shouldSprint && hasModifier) {
            movement.removeModifier(COMBAT_SPRINT_UUID);
        }
    }

    private void tickChargedState() {
        int chargedTicks = this.entityData.get(CHARGED_TICKS);
        if (chargedTicks > 0) {
            chargedTicks--;
            this.entityData.set(CHARGED_TICKS, chargedTicks);
            if (!this.entityData.get(CHARGED)) {
                this.entityData.set(CHARGED, true);
            }
        } else if (this.entityData.get(CHARGED)) {
            this.entityData.set(CHARGED, false);
        }

        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }

        boolean hasModifier = attack.getModifier(CHARGED_ATTACK_UUID) != null;
        if (isChargedState() && !hasModifier) {
            attack.addTransientModifier(CHARGED_ATTACK_MODIFIER);
        } else if (!isChargedState() && hasModifier) {
            attack.removeModifier(CHARGED_ATTACK_UUID);
        }
    }

    private void tickDetonationPrime() {
        int primeTicks = this.entityData.get(DETONATION_PRIME_TICKS);
        if (primeTicks <= 0) {
            return;
        }

        LivingEntity target = this.getTarget();
        this.getNavigation().stop();
        this.setSprinting(false);
        this.setDeltaMovement(Vec3.ZERO);

        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 16.0D) {
            cancelDetonationPrime();
            return;
        }

        int elapsed = 20 - primeTicks;
        if (elapsed % 3 == 0) {
            float progress = Math.min(1.0F, elapsed / 18.0F);
            float pitch = Mth.lerp(progress, 0.7F, 1.6F);
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, pitch);
        }

        primeTicks--;
        this.entityData.set(DETONATION_PRIME_TICKS, primeTicks);
        if (primeTicks <= 0) {
            triggerDetonationExplosion();
        }
    }

    private void tickHideAndRegeneration() {
        int hiddenTicks = this.entityData.get(HIDDEN_TICKS);
        if (hiddenTicks > 0) {
            this.entityData.set(HIDDEN_TICKS, hiddenTicks - 1);
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35D));
            if (getAnimationTicks() <= 1 || !"hide".equals(getCurrentAnimation())) {
                playGeckoAnimation("hide", 12);
            }
            if (hiddenTicks - 1 <= 0) {
                this.entityData.set(REGENERATING_TICKS, 20);
                playGeckoAnimation("regenerate", 20);
            }
            return;
        }

        int regeneratingTicks = this.entityData.get(REGENERATING_TICKS);
        if (regeneratingTicks > 0) {
            this.entityData.set(REGENERATING_TICKS, regeneratingTicks - 1);
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().scale(0.2D));
            this.setSprinting(false);
            float healPerTick = this.getMaxHealth() * 0.025F;
            float missingHealth = this.getMaxHealth() - this.getHealth();
            if (missingHealth > 0.0F) {
                this.heal(Math.min(healPerTick, missingHealth));
            }
        }
    }

    @Override
    protected double getBloodDemonArtRange() {
        return 14.0D;
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return isChargedState() ? 0.06F : 0.04F;
    }

    @Override
    protected void tickBloodDemonArt() {
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = getBloodDemonArt();
        LivingEntity target = getTarget();
        if (art == null || target == null || !target.isAlive() || getBloodDemonArtCooldownTicks() > 0 || isUsingLockedAnimation()) {
            return;
        }

        double distanceSq = this.distanceToSqr(target);
        double maxRange = getBloodDemonArtRange();
        if (distanceSq > maxRange * maxRange) {
            return;
        }
        if (this.random.nextFloat() > (distanceSq <= 9.0D ? 0.11F : getBloodDemonArtUseChance())) {
            return;
        }

        BloodDemonArtForm form;
        if (distanceSq <= 9.0D && !isPrimingDetonation() && this.random.nextFloat() < 0.6F) {
            form = art.getTechnique().getForms().stream()
                .filter(candidate -> candidate.getFormId() == CreepingRuin.FORM_DETONATION)
                .findFirst()
                .orElse(null);
        } else {
            int index = this.random.nextInt(Math.max(1, art.getTechnique().getFormCount() - 1));
            if (index >= art.getTechnique().getFormCount()) {
                index = 0;
            }
            form = art.getTechnique().getForm(index);
        }

        if (form == null) {
            return;
        }

        form.execute(this, level());
        if (getBloodDemonArtCooldownTicks() <= 0) {
            setBloodDemonArtCooldownTicks(Math.max(20, form.getCooldownSeconds() * 20));
        }
    }

    @Override
    protected double getSprintEnterSpeed(boolean currentlySprintingAnim) {
        return Double.MAX_VALUE;
    }

    @Override
    protected boolean isUsingLockedAnimation() {
        return super.isUsingLockedAnimation() || isHiddenState() || isRegenerating() || isPrimingDetonation();
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (isHiddenState() || isRegenerating() || isPrimingDetonation()) {
            return false;
        }

        if (!(entity instanceof LivingEntity livingTarget)) {
            return false;
        }

        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean result = Damager.hurt(this, livingTarget, damage);
        if (result) {
            this.doEnchantDamageEffects(this, livingTarget);
        }

        if (result && meleeAnimationTicks <= 0) {
            playGeckoAnimation("attack", 10);
            meleeAnimationTicks = 10;
            tryTriggerHeadSpin();
        }
        return result;
    }

    private void tryTriggerHeadSpin() {
        if (headSpinCooldownTicks > 0 || this.random.nextFloat() >= 0.22F) {
            return;
        }

        headSpinCooldownTicks = 20 * 6;
        playGeckoAnimation("head_spin", 14);
        meleeAnimationTicks = Math.max(meleeAnimationTicks, 14);
        AbilityScheduler.scheduleOnce(this, this::performHeadSpinDamage, 5);
    }

    private void performHeadSpinDamage() {
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.isAlive() || isHiddenState() || isRegenerating()) {
            return;
        }

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB area = this.getBoundingBox().inflate(3.0D, 1.0D, 3.0D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
            target -> target != this && target.isAlive() && !Damager.isDemon(target) && this.distanceToSqr(target) <= 9.0D)) {
            Damager.hurt(this, target, damage);
            Vec3 knockback = target.position().subtract(this.position());
            if (knockback.lengthSqr() > 1.0E-4D) {
                knockback = knockback.normalize().scale(0.8D);
                target.push(knockback.x, 0.25D, knockback.z);
            }
        }

        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(0.55D), getZ(), 18, 1.1D, 0.15D, 1.1D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(0.55D), getZ(), 12, 0.9D, 0.2D, 0.9D, 0.02D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isHiddenState()) {
            return false;
        }
        if ((source.getEntity() == this || source.getDirectEntity() == this) && source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt lightningBolt) {
        super.thunderHit(level, lightningBolt);
        startChargedState(20 * 30);
    }

    public void startChargedState(int ticks) {
        this.entityData.set(CHARGED, true);
        this.entityData.set(CHARGED_TICKS, Math.max(this.entityData.get(CHARGED_TICKS), ticks));
    }

    public boolean isChargedState() {
        return this.entityData.get(CHARGED);
    }

    public boolean isHiddenState() {
        return this.entityData.get(HIDDEN_TICKS) > 0;
    }

    public boolean isRegenerating() {
        return this.entityData.get(REGENERATING_TICKS) > 0;
    }

    public boolean isPrimingDetonation() {
        return this.entityData.get(DETONATION_PRIME_TICKS) > 0;
    }

    public boolean isDetonationFlickerWhite() {
        int primeTicks = this.entityData.get(DETONATION_PRIME_TICKS);
        if (primeTicks <= 0) {
            return false;
        }
        int elapsed = 20 - primeTicks;
        return ((elapsed / 5) & 1) == 0;
    }

    public int getChargedFrame() {
        return this.tickCount & 15;
    }

    private void beginDetonationRecovery() {
        int hideDuration = 40 + this.random.nextInt(121);
        this.entityData.set(HIDDEN_TICKS, hideDuration);
        this.entityData.set(REGENERATING_TICKS, 0);
        this.setBloodDemonArtCooldownTicks(20 + hideDuration + 20);
    }

    private void beginDetonationPrime() {
        this.entityData.set(DETONATION_PRIME_TICKS, 20);
        this.getNavigation().stop();
        this.setSprinting(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void cancelDetonationPrime() {
        this.entityData.set(DETONATION_PRIME_TICKS, 0);
    }

    private void triggerDetonationExplosion() {
        if (this.level().isClientSide) {
            return;
        }

        this.entityData.set(DETONATION_PRIME_TICKS, 0);
        playGeckoAnimation("explode", 10);
        triggerExplosion(isChargedState() ? 6.0F : 4.0F);
        AbilityScheduler.scheduleOnce(this, this::beginDetonationRecovery, 10);
    }

    private void triggerExplosion(float power) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(0.5D), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, Level.ExplosionInteraction.MOB);
    }


    public void beginDetonationPrimeSequence() {
        beginDetonationPrime();
    }

    public void beginDetonationRecoverySequence() {
        beginDetonationRecovery();
    }

    public void applyBloodDemonArtCooldown(int cooldownTicks) {
        setBloodDemonArtCooldownTicks(cooldownTicks);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.29D)
            .add(Attributes.ARMOR, 5.0D);
    }

    private static class DemonCreeperMeleeGoal extends MeleeAttackGoal {
        private final DemonCreeperEntity creeper;

        private DemonCreeperMeleeGoal(DemonCreeperEntity creeper, double speedModifier) {
            super(creeper, speedModifier, false);
            this.creeper = creeper;
        }

        @Override
        public boolean canUse() {
            return !creeper.isHiddenState() && !creeper.isRegenerating() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !creeper.isHiddenState() && !creeper.isRegenerating() && super.canContinueToUse();
        }

        @Override
        public void tick() {
            if (creeper.isHiddenState() || creeper.isRegenerating()) {
                creeper.getNavigation().stop();
                return;
            }
            super.tick();
        }

        @Override
        public void stop() {
            super.stop();
            if (creeper.combatSprintTicks <= 0) {
                creeper.setSprinting(false);
            }
        }
    }
}
