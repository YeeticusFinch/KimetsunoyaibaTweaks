package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes base-mod (and any other addon) targeting goals created with
 * checkSight=false acquiring targets through walls.
 *
 * KimetsunoYaiba v3 constructs nearly all of its demon/slayer
 * NearestAttackableTargetGoals with (false, false) for (checkSight, mustReach),
 * which lets demons and slayers aggro entities many layers of stone away
 * (e.g. players far underground in caves).
 */
@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin {

    /**
     * After every findTarget() scan we drop any newly acquired target the mob
     * cannot actually see, unless the target is direct retaliation
     * (the last entity that hurt this mob).
     */
    @Inject(method = "findTarget", at = @At("TAIL"), require = 0)
    private void knymp$requireLineOfSightAfterFind(CallbackInfo ci) {
        if (!com.lerdorf.kimetsunoyaibamultiplayer.Config.requireLineOfSightAggro) {
            return;
        }

        Goal self = (Goal) (Object) this;
        Mob owner = knymp$getOwningMob(self);
        if (owner == null || owner.level().isClientSide()) {
            return;
        }

        LivingEntity target = owner.getTarget();
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }

        // Direct retaliation is always allowed regardless of sight.
        if (owner.getLastHurtByMob() == target) {
            return;
        }

        if (!owner.getSensing().hasLineOfSight(target)) {
            owner.setTarget(null);
        }
    }

    /**
     * TargetGoal.mob is a protected package-private field on the superclass.
     * Read it reflectively once per call - cheap relative to target scans,
     * which run at most every 10 ticks per mob.
     */
    private static Mob knymp$getOwningMob(Goal goal) {
        try {
            Object mobField = TARGET_MOB_FIELD.get(goal);
            return mobField instanceof Mob mob ? mob : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final java.lang.reflect.Field TARGET_MOB_FIELD = findTargetMobField();

    private static java.lang.reflect.Field findTargetMobField() {
        Class<?> clazz = Goal.class;
        while (clazz != null) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.getType() == Mob.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
