package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the base mod's Fire Resistance sunlight immunity in
 * NikkoyakeProcedure when fire_resistance_gives_sunlight_immunity is false.
 *
 * The base mod's sunlight burn procedure returns early (no damage) when the
 * entity has the Fire Resistance effect. When the config option is disabled,
 * this mixin makes that check report "no Fire Resistance" so demons burn in
 * sunlight regardless of the potion effect.
 */
@Mixin(targets = "net.mcreator.kimetsunoyaiba.procedures.NikkoyakeProcedure")
public abstract class BaseFireResistanceSunlightImmunityMixin {
    @Redirect(
        method = "execute",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_21023_(Lnet/minecraft/world/effect/MobEffect;)Z",
            remap = false
        ),
        require = 0
    )
    private static boolean kimetsunoyaibamultiplayer$suppressFireResistanceSunlightImmunity(
            LivingEntity entity,
            MobEffect effect) {
        if (MobEffects.FIRE_RESISTANCE.equals(effect)
            && !isFireResistanceSunlightImmunityEnabled()) {
            return false;
        }
        return entity.hasEffect(effect);
    }

    private static boolean isFireResistanceSunlightImmunityEnabled() {
        return CustomProgressionConfig.fireResistanceGivesSunlightImmunity == null
            || CustomProgressionConfig.fireResistanceGivesSunlightImmunity.get();
    }
}