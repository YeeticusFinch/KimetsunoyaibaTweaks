package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.client.particle.DustParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Particle.class)
public abstract class DustParticleMixin {
    @Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void kimetsu$makeDustFullbright(float partialTick, CallbackInfoReturnable<Integer> cir) {
        kimetsu$setDustFullbright(cir);
    }

    @Inject(method = "m_6355_", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kimetsu$makeDustFullbrightSrg(float partialTick, CallbackInfoReturnable<Integer> cir) {
        kimetsu$setDustFullbright(cir);
    }

    private void kimetsu$setDustFullbright(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof DustParticle) {
            cir.setReturnValue(0xF000F0);
        }
    }
}
