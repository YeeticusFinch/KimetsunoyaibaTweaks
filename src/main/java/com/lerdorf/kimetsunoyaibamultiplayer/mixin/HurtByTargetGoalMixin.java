package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HurtByTargetGoal.class)
public abstract class HurtByTargetGoalMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void knymp$skipInvalidRevengeTarget(CallbackInfo ci) {
        if (!hasValidRevengeTarget()) {
            ci.cancel();
        }
    }

    @Inject(method = "m_26047_", at = @At("HEAD"), cancellable = true, remap = false)
    private void knymp$skipNullSameTypeRevenge(CallbackInfo ci) {
        if (!hasValidRevengeTarget()) {
            ci.cancel();
        }
    }

    @Inject(method = "alertOther", at = @At("HEAD"), cancellable = true, require = 0)
    private void knymp$skipNullAlertTarget(Mob alertedMob, LivingEntity target, CallbackInfo ci) {
        if (alertedMob == null || target == null || !target.isAlive()) {
            ci.cancel();
        }
    }

    private boolean hasValidRevengeTarget() {
        Mob owner = ((TargetGoalAccessor) (Object) this).knymp$getMob();
        LivingEntity target = owner == null ? null : owner.getLastHurtByMob();
        return target != null && target.isAlive();
    }
}
