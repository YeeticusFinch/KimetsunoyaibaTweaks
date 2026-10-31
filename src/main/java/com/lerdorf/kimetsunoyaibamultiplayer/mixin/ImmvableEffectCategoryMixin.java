package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the base mod's immovable effect as harmful in the vanilla effect UI. */
@Mixin(MobEffect.class)
public abstract class ImmvableEffectCategoryMixin {
    private static final String IMMVABLE_EFFECT_CLASS =
        "net.mcreator.kimetsunoyaiba.potion.ImmvableMobEffect";

    @Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
    private void kimetsunoyaibamultiplayer$markImmvableAsHarmful(
            CallbackInfoReturnable<MobEffectCategory> cir) {
        if (IMMVABLE_EFFECT_CLASS.equals(this.getClass().getName())) {
            cir.setReturnValue(MobEffectCategory.HARMFUL);
        }
    }
}
