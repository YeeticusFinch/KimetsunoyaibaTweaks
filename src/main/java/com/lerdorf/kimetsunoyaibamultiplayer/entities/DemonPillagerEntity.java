package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.UUID;

public class DemonPillagerEntity extends AbstractDemonEntity {
    private static final UUID SWORD_SPRINT_UUID = UUID.fromString("99fcb6de-2c5a-4d0a-bfcf-cd4d8f96d95e");
    private static final AttributeModifier SWORD_SPRINT_MODIFIER =
        new AttributeModifier(SWORD_SPRINT_UUID, "Demon pillager sword sprint", 0.55D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final ItemStack CROSSBOW_STACK = new ItemStack(Items.CROSSBOW);
    private static final ItemStack IRON_SWORD_STACK = new ItemStack(Items.IRON_SWORD);
    private static final String[] SWORD_ATTACK_ANIMATIONS = {"sword_to_left", "sword_to_right", "sword_overhead"};
    private static final int CROSSBOW_LOAD_TICKS = 10;
    private static final int CROSSBOW_AIM_TICKS = 10;
    private static final int CROSSBOW_SHOOT_TICKS = 10;

    private int meleeAnimationTicks = 0;
    private int crossbowLoadingTicks = 0;
    private int aimTicks = 0;
    private int rangedCooldownTicks = 25;
    private int swordSprintTicks = 0;
    private int swordSprintCooldownTicks = 40;
    private int swordAnimationIndex = 0;

    public DemonPillagerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, CROSSBOW_STACK.copy());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 36.0D)
            .add(Attributes.ATTACK_DAMAGE, 5.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DemonPillagerMeleeGoal(this, 1.15D));
        this.goalSelector.addGoal(2, new DemonPillagerRangedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            this::canTargetNonDemonVictim));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            updateWeaponMode();
            tickRangedAttack();
            tickSwordSprint();
        }

        if (meleeAnimationTicks > 0) {
            meleeAnimationTicks--;
        }
    }

    private void updateWeaponMode() {
        ItemStack desired = isSwordMode() ? IRON_SWORD_STACK : CROSSBOW_STACK;
        // Do not replace a charged crossbow with the uncharged template stack.
        if (this.getMainHandItem().getItem() != desired.getItem()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, desired.copy());
        }
    }

    private void tickRangedAttack() {
        if (rangedCooldownTicks > 0) {
            rangedCooldownTicks--;
        }

        LivingEntity target = this.getTarget();
        if (isSwordMode()) {
            if (crossbowLoadingTicks > 0) {
                crossbowLoadingTicks = 0;
                if ("crossbow_load".equals(getCurrentAnimation())) {
                    playGeckoAnimation("idle", 0);
                }
            }
            aimTicks = 0;
            return;
        }

        if (crossbowLoadingTicks > 0) {
            if (target == null || !target.isAlive()) {
                crossbowLoadingTicks = 0;
                playGeckoAnimation("idle", 0);
                return;
            }

            holdPositionForCrossbowLoading(target);
            crossbowLoadingTicks--;
            if (crossbowLoadingTicks == 0) {
                loadCrossbow();
                aimTicks = CROSSBOW_AIM_TICKS;
            }
            return;
        }

        if (aimTicks <= 0) {
            return;
        }

        if (target == null || !target.isAlive() || isSwordMode()) {
            aimTicks = 0;
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        aimTicks--;
        if (aimTicks <= 0) {
            fireCrossbowAt(target);
        }
    }

    private void fireCrossbowAt(LivingEntity target) {
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        ItemStack crossbow = this.getMainHandItem();
        if (!(crossbow.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(crossbow)) {
            return;
        }

        Arrow arrow = new Arrow(this.level(), this);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.3333333333333333D) - arrow.getY() + horizontal * 0.2D;
        arrow.shoot(dx, dy, dz, 1.8F, 8.0F);
        arrow.setBaseDamage(4.0D);
        arrow.setCritArrow(false);
        arrow.pickup = Arrow.Pickup.DISALLOWED;
        this.level().addFreshEntity(arrow);
        crossbow.getOrCreateTag().remove("ChargedProjectiles");
        CrossbowItem.setCharged(crossbow, false);
        this.playGeckoAnimation("crossbow_shoot", CROSSBOW_SHOOT_TICKS);
        this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.95F + (this.getRandom().nextFloat() * 0.1F));
        this.rangedCooldownTicks = 35;
    }

    private void holdPositionForCrossbowLoading(LivingEntity target) {
        this.getNavigation().stop();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        this.setSprinting(false);
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
    }

    private void loadCrossbow() {
        ItemStack crossbow = this.getMainHandItem();
        if (!(crossbow.getItem() instanceof CrossbowItem)) {
            return;
        }

        ListTag projectiles = new ListTag();
        projectiles.add(new ItemStack(Items.ARROW).save(new CompoundTag()));
        crossbow.getOrCreateTag().put("ChargedProjectiles", projectiles);
        CrossbowItem.setCharged(crossbow, true);
    }

    private void tickSwordSprint() {
        if (!isSwordMode() || aimTicks > 0) {
            swordSprintTicks = 0;
            setSwordSprinting(false);
            return;
        }

        if (swordSprintCooldownTicks > 0) {
            swordSprintCooldownTicks--;
        }

        boolean shouldSprint = false;
        LivingEntity target = this.getTarget();
        if (swordSprintTicks > 0) {
            swordSprintTicks--;
            shouldSprint = target != null && target.isAlive();
        } else if (target != null && target.isAlive() && this.distanceToSqr(target) > 4.0D
            && swordSprintCooldownTicks <= 0 && this.random.nextFloat() < 0.01F) {
            swordSprintTicks = 20 * (3 + this.random.nextInt(3));
            swordSprintCooldownTicks = 20 * (6 + this.random.nextInt(5));
            shouldSprint = true;
        }

        if (shouldSprint && this.getDeltaMovement().horizontalDistanceSqr() < 0.0009D && this.getNavigation().isDone()) {
            shouldSprint = false;
        }

        setSwordSprinting(shouldSprint);
    }

    private void setSwordSprinting(boolean sprinting) {
        this.setSprinting(sprinting);
        AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        boolean hasModifier = movement.getModifier(SWORD_SPRINT_UUID) != null;
        if (sprinting && !hasModifier) {
            movement.addTransientModifier(SWORD_SPRINT_MODIFIER);
        } else if (!sprinting && hasModifier) {
            movement.removeModifier(SWORD_SPRINT_UUID);
        }
    }

    public boolean isSwordMode() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.distanceToSqr(target) <= 25.0D;
    }

    public boolean isAimingShot() {
        return aimTicks > 0;
    }

    public boolean isCrossbowLoading() {
        return crossbowLoadingTicks > 0;
    }

    public boolean isCrossbowLoaded() {
        return this.getMainHandItem().getItem() instanceof CrossbowItem
            && CrossbowItem.isCharged(this.getMainHandItem());
    }

    public int getRangedCooldownTicks() {
        return rangedCooldownTicks;
    }

    public void startAimingShot() {
        if (crossbowLoadingTicks > 0 || aimTicks > 0 || isSwordMode() || isCrossbowLoaded()) {
            return;
        }

        crossbowLoadingTicks = CROSSBOW_LOAD_TICKS;
        holdPositionForCrossbowLoading(this.getTarget());
        playGeckoAnimation("crossbow_load", CROSSBOW_LOAD_TICKS);
        this.playSound(SoundEvents.CROSSBOW_LOADING_START, 0.8F, 0.9F + (this.getRandom().nextFloat() * 0.1F));
    }

    @Override
    protected boolean hasAnimationOverlay() {
        return true;
    }

    @Override
    protected String getAnimationOverlay() {
        return isCrossbowLoaded() ? "crossbow_aim" : null;
    }

    @Override
    protected float getBloodDemonArtUseChance() {
        return 0.0F;
    }

    @Override
    protected boolean isUsingLockedAnimation() {
        return super.isUsingLockedAnimation() || crossbowLoadingTicks > 0;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!isSwordMode() || !(entity instanceof LivingEntity livingTarget)) {
            return false;
        }

        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean result = Damager.hurt(this, livingTarget, damage);
        if (result) {
            this.doEnchantDamageEffects(this, livingTarget);
        }

        if (result && meleeAnimationTicks <= 0) {
            this.playGeckoAnimation(SWORD_ATTACK_ANIMATIONS[swordAnimationIndex], 12);
            swordAnimationIndex = (swordAnimationIndex + 1) % SWORD_ATTACK_ANIMATIONS.length;
            meleeAnimationTicks = 12;
        }

        return result;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    private static class DemonPillagerMeleeGoal extends MeleeAttackGoal {
        private final DemonPillagerEntity demon;

        private DemonPillagerMeleeGoal(DemonPillagerEntity demon, double speedModifier) {
            super(demon, speedModifier, false);
            this.demon = demon;
        }

        @Override
        public boolean canUse() {
            return demon.isSwordMode() && !demon.isAimingShot() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return demon.isSwordMode() && !demon.isAimingShot() && super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            if (demon.swordSprintTicks <= 0) {
                demon.setSwordSprinting(false);
            }
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = this.mob.getTarget();
            if (target != null && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                this.mob.getNavigation().stop();
            }
        }
    }

    private static class DemonPillagerRangedGoal extends Goal {
        private final DemonPillagerEntity demon;
        private final double speedModifier;

        private DemonPillagerRangedGoal(DemonPillagerEntity demon, double speedModifier) {
            this.demon = demon;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = demon.getTarget();
            return target != null && target.isAlive() && !demon.isSwordMode();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = demon.getTarget();
            return target != null && target.isAlive() && !demon.isSwordMode();
        }

        @Override
        public void stop() {
            demon.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = demon.getTarget();
            if (target == null) {
                return;
            }

            demon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (demon.isCrossbowLoading()) {
                demon.getNavigation().stop();
                return;
            }

            double distanceSq = demon.distanceToSqr(target);
            if (distanceSq > 64.0D) {
                demon.getNavigation().moveTo(target, speedModifier);
            } else {
                demon.getNavigation().stop();
            }

            if (demon.getSensing().hasLineOfSight(target) && demon.getRangedCooldownTicks() <= 0) {
                demon.startAimingShot();
            }
        }
    }
}
