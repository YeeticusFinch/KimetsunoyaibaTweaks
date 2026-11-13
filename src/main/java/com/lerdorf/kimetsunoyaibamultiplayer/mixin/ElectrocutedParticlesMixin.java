package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces Electrocuted's vanilla effect bubbles with the base mod's thunder particles. */
@Mixin(LivingEntity.class)
public abstract class ElectrocutedParticlesMixin {
    @Redirect(
        method = "tickEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        ),
        require = 0
    )
    private void kimetsunoyaibamultiplayer$replaceElectrocutedParticles(
        Level level, ParticleOptions original, double x, double y, double z,
        double xSpeed, double ySpeed, double zSpeed) {
        LivingEntity entity = (LivingEntity) (Object) this;
        MobEffectInstance effect = entity.getEffect(ModEffects.ELECTROCUTED.get());
        if (effect != null && original.getType() == ParticleTypes.ENTITY_EFFECT) {
            ParticleOptions replacement = thunderParticle(effect.getAmplifier());
            if (replacement != null) {
                level.addParticle(replacement, x, y, z, xSpeed, ySpeed, zSpeed);
                return;
            }
        }
        level.addParticle(original, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    private static ParticleOptions thunderParticle(int amplifier) {
        String path = amplifier <= 0
            ? "particle_thunder"
            : amplifier <= 3 ? "particle_thunder_2" : "particle_thunder_3";
        ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", path));
        return particleType instanceof SimpleParticleType simpleParticle ? simpleParticle : null;
    }
}
