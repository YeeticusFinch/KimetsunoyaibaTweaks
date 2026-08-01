package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.VindicatorsBane;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class DemonVindicatorEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = VindicatorsBane.ART_ID;
    private static final UUID COMBAT_SPRINT_UUID = UUID.fromString("9448de55-29de-4140-bf1a-91bb5a3207e3");
    private static final AttributeModifier COMBAT_SPRINT_MODIFIER =
        new AttributeModifier(COMBAT_SPRINT_UUID, "Demon vindicator combat sprint", 0.8D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private int meleeAnimationTicks = 0;
    private int combatSprintTicks = 0;
    private int combatSprintCooldownTicks = 40;

    public DemonVindicatorEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 42.0D)
            .add(Attributes.ATTACK_DAMAGE, 9.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.31D)
            .add(Attributes.ARMOR, 3.0D);
    }

    public static void registerBloodDemonArt() {
        VindicatorsBane.register();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DemonVindicatorMeleeGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.95D));
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
            tickCombatSprint();
        }
        if (meleeAnimationTicks > 0) {
            meleeAnimationTicks--;
        }
    }

    private void tickCombatSprint() {
        if (combatSprintCooldownTicks > 0) {
            combatSprintCooldownTicks--;
        }

        boolean shouldSprint = false;
        LivingEntity target = this.getTarget();
        if (combatSprintTicks > 0) {
            combatSprintTicks--;
            shouldSprint = target != null && target.isAlive();
        } else if (target != null && target.isAlive() && this.distanceToSqr(target) <= 196.0D
            && combatSprintCooldownTicks <= 0 && this.random.nextFloat() < 0.012F) {
            combatSprintTicks = 20 * (4 + this.random.nextInt(4));
            combatSprintCooldownTicks = 20 * (6 + this.random.nextInt(5));
            shouldSprint = true;
        }

        if (shouldSprint && this.getDeltaMovement().horizontalDistanceSqr() < 0.0009D && this.getNavigation().isDone()) {
            shouldSprint = false;
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

    @Override
    protected float getBloodDemonArtUseChance() {
        return 0.045F;
    }

    @Override
    protected String resolveSprintAnimation() {
        return "sprint_vindicator";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return "sprint".equals(animation) || "sprint_vindicator".equals(animation);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingTarget)) {
            return false;
        }

        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean result = Damager.hurt(this, livingTarget, damage);
        if (result) {
            this.doEnchantDamageEffects(this, livingTarget);
            BloodDemonArtM1AttackHandler.performNichirinLikeSlashAttack(this, livingTarget.getUUID());
            this.meleeAnimationTicks = 10;
            BleedingHandler.applyOrRefreshBleeding(livingTarget, 20 * 8, 1);
            disableShieldIfBlocking(livingTarget);
        }
        return result;
    }

    private void disableShieldIfBlocking(LivingEntity target) {
        if (!(target instanceof Player player) || !player.isBlocking()) {
            return;
        }

        player.getCooldowns().addCooldown(Items.SHIELD, 100);
        player.stopUsingItem();
        player.level().broadcastEntityEvent(player, (byte)30);
        this.playSound(SoundEvents.SHIELD_BREAK, 1.0F, 0.9F + (this.getRandom().nextFloat() * 0.2F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
        if (this.random.nextFloat() < 0.20F) {
            this.spawnAtLocation(new ItemStack(ModItems.VINDICATOR_DEMON_ART.get()));
        }
    }

    private static class DemonVindicatorMeleeGoal extends MeleeAttackGoal {
        private final DemonVindicatorEntity demon;

        private DemonVindicatorMeleeGoal(DemonVindicatorEntity demon, double speedModifier) {
            super(demon, speedModifier, false);
            this.demon = demon;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return 5.5D + target.getBbWidth();
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = this.mob.getTarget();
            if (target != null && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                this.mob.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            super.stop();
            if (demon.combatSprintTicks <= 0) {
                demon.setSprinting(false);
            }
        }
    }
}
