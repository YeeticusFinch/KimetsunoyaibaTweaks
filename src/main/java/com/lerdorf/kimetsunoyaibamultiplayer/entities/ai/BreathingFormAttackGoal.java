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
    private final int minCooldownTicks = 40; // 2 seconds minimum between forms (non-Muichiro)
    private final double formUseChance = 0.15; // 15% chance per second to use form (non-Muichiro)

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

        // Muichiro should cast as soon as cooldown finishes
        if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity) {
            return true;
        }

        // Random chance to use form (checked every tick)
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

        // Pick a breathing form, weighted to favor smaller cooldowns
        BreathingTechnique technique = this.entity.getBreathingTechnique();
        BreathingForm form = pickWeightedFormConstrained(technique, target);

        if (form != null) {
            // Face the target immediately before executing the form
            faceTarget(target);

            // Execute the breathing form
            form.getEffect().execute(this.entity, this.entity.level());

            // Set cooldown
            int cooldownTicks;
            if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity) {
                // 30% faster: wait only 60% of original cooldown
                cooldownTicks = Math.max(1, (int)Math.round(form.getCooldownSeconds() * 12.0));
            } else {
                cooldownTicks = Math.max(form.getCooldownSeconds() * 20, minCooldownTicks);
            }
            this.entity.setBreathingFormCooldown(cooldownTicks);
        }
    }

    private BreathingForm pickWeightedFormConstrained(BreathingTechnique technique, LivingEntity target) {
        java.util.List<BreathingForm> forms = technique.getForms();
        if (forms == null || forms.isEmpty()) return null;

        double dSq = this.entity.distanceToSqr(target);
        java.util.List<BreathingForm> allowed = new java.util.ArrayList<>();
        for (BreathingForm f : forms) {
            if (isFormAllowedForDistance(f, dSq)) {
                allowed.add(f);
            }
        }
        if (allowed.isEmpty()) {
            return null;
        }

        double total = 0.0;
        double[] weights = new double[allowed.size()];
        for (int i = 0; i < allowed.size(); i++) {
            int cd = Math.max(1, allowed.get(i).getCooldownSeconds());
            double w = 1.0 / cd; // smaller cooldown -> higher weight
            weights[i] = w;
            total += w;
        }

        double r = this.entity.getRandom().nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < allowed.size(); i++) {
            cumulative += weights[i];
            if (r <= cumulative) {
                return allowed.get(i);
            }
        }
        return allowed.get(allowed.size() - 1);
    }

    private boolean isFormAllowedForDistance(BreathingForm form, double dSq) {
        String n = form.getName();
        if (n == null) return true;
        String name = n.toLowerCase();
        // firstForm & secondForm: within 5 blocks
        if (name.startsWith("first form") || name.startsWith("second form")) {
            return dSq <= 25.0;
        }
        // thirdForm: within 10 blocks
        if (name.startsWith("third form")) {
            return dSq <= 100.0;
        }
        // fourthForm: more than 10 blocks
        if (name.startsWith("fourth form")) {
            return dSq > 100.0;
        }
        // fifthForm: >10 and <50
        if (name.startsWith("fifth form")) {
            return dSq > 100.0 && dSq < 2500.0;
        }
        // sixthForm: >5 and <50
        if (name.startsWith("sixth form")) {
            return dSq > 25.0 && dSq < 2500.0;
        }
        // seventhForm: within 20 blocks
        if (name.startsWith("seventh form")) {
            return dSq <= 400.0;
        }
        // default allow
        return true;
    }

    private void faceTarget(LivingEntity target) {
        // Set look control for immediate facing
        this.entity.getLookControl().setLookAt(target, 180.0F, 180.0F);
        // Also adjust body/head rotation to face target
        double dx = target.getX() - this.entity.getX();
        double dz = target.getZ() - this.entity.getZ();
        float yaw = (float)(Math.atan2(dz, dx) * (180D/Math.PI)) - 90.0F;
        this.entity.setYRot(yaw);
        this.entity.setYHeadRot(yaw);
    }
}
