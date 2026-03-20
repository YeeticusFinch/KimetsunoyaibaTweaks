package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

import java.util.UUID;

public class DemonVillagerEntity extends AbstractDemonEntity {
    private static final UUID COMBAT_SPRINT_UUID = UUID.fromString("22c5beba-2d3e-41df-b436-ab9ad7d2cd01");
    private static final AttributeModifier COMBAT_SPRINT_MODIFIER =
        new AttributeModifier(COMBAT_SPRINT_UUID, "Demon villager combat sprint", 0.65D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private boolean useLeftPunchNext = true;
    private int meleeAnimationTicks = 0;
    private int combatSprintTicks = 0;
    private int combatSprintCooldownTicks = 60;

    public DemonVillagerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 30.0D)
            .add(Attributes.ATTACK_DAMAGE, 5.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DemonVillagerMeleeGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            target -> target != null && target.isAlive() && !Damager.isDemon(target)));
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
            && combatSprintCooldownTicks <= 0 && this.random.nextFloat() < 0.0075F) {
            combatSprintTicks = 20 * (4 + this.random.nextInt(3));
            combatSprintCooldownTicks = 20 * (8 + this.random.nextInt(7));
            shouldSprint = true;
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
        return 0.0F;
    }

    @Override
    protected String resolveSprintAnimation() {
        return "sprint_noob";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return "sprint".equals(animation) || "sprint_noob".equals(animation);
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
        }

        if (result && meleeAnimationTicks <= 0) {
            this.playGeckoAnimation(useLeftPunchNext ? "punch_left" : "punch_right", 10);
            useLeftPunchNext = !useLeftPunchNext;
            meleeAnimationTicks = 10;
        }
        return result;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    private static class DemonVillagerMeleeGoal extends MeleeAttackGoal {
        private final DemonVillagerEntity demon;

        private DemonVillagerMeleeGoal(DemonVillagerEntity demon, double speedModifier) {
            super(demon, speedModifier, false);
            this.demon = demon;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return 4.0D + target.getBbWidth();
        }

        @Override
        public void tick() {
            super.tick();
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
