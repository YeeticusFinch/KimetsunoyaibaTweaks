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
    @Inject(method = "alertOther", at = @At("HEAD"), cancellable = true, require = 0)
    private void knymp$skipNullAlertTarget(Mob alertedMob, LivingEntity target, CallbackInfo ci) {
        if (alertedMob == null || target == null || !target.isAlive()) {
            ci.cancel();
        }
    }

    @Inject(method = "m_5766_", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void knymp$skipNullAlertTargetSrg(Mob alertedMob, LivingEntity target, CallbackInfo ci) {
        knymp$skipNullAlertTarget(alertedMob, target, ci);
    }
}
