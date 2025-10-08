package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal that makes breathing slayer entities use their breathing forms in combat
 */
public class BreathingFormAttackGoal extends Goal {
    private final BreathingSlayerEntity entity;
    private final double attackRange = 24.0D;
    private final int minCooldownTicks = 40; // 2 seconds minimum between forms
    private final double formUseChance = 0.15; // 15% chance per second to use form

    public BreathingFormAttackGoal(BreathingSlayerEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Check if target is in range
        double distanceSq = this.entity.distanceToSqr(target);
        if (distanceSq > attackRange * attackRange) {
            return false;
        }

        // Check if breathing form is on cooldown
        if (this.entity.isBreathingFormOnCooldown()) {
            return false;
        }

        // Random chance to use form (checked every tick, so ~20% chance per second at 15% per tick)
        return this.entity.getRandom().nextDouble() < (formUseChance / 20.0);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // One-shot goal
    }

    @Override
    public void start() {
        LivingEntity target = this.entity.getTarget();
        if (target == null) return;

        // Get current breathing form
        BreathingTechnique technique = this.entity.getBreathingTechnique();
        int formIndex = this.entity.getCurrentFormIndex();
        BreathingForm form = technique.getForm(formIndex);

        if (form != null) {
            // Execute the breathing form
            form.getEffect().execute(this.entity, this.entity.level());

            // Set cooldown
            int cooldownTicks = Math.max(form.getCooldownSeconds() * 20, minCooldownTicks);
            this.entity.setBreathingFormCooldown(cooldownTicks);

            // Cycle to next form for variety
            this.entity.cycleForm();
        }
    }
}
