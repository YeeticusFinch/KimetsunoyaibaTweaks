package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.AbilityParticleRenderThrottle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelParticleThrottleMixin {
    @Inject(
        method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void throttleRegularParticle(ParticleOptions particle, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (!AbilityParticleRenderThrottle.shouldRender(particle)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void throttleOverrideLimiterParticle(ParticleOptions particle, boolean overrideLimiter,
            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (!AbilityParticleRenderThrottle.shouldRender(particle)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void throttleAlwaysVisibleParticle(ParticleOptions particle, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (!AbilityParticleRenderThrottle.shouldRender(particle)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void throttleAlwaysVisibleOverrideLimiterParticle(ParticleOptions particle, boolean overrideLimiter,
            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (!AbilityParticleRenderThrottle.shouldRender(particle)) {
            ci.cancel();
        }
    }
}
