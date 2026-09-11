package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoDensityFunction;
import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoTerrainContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wraps vanilla terrain density sampling instead of rewriting finished chunk blocks. */
@Mixin(NoiseChunk.class)
public abstract class NatagumoDensityMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;finalDensity()Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction kimetsu$wrapFinalDensity(NoiseRouter router) {
        DensityFunction delegate = router.finalDensity();
        NatagumoTerrainContext.Context context = NatagumoTerrainContext.get();
        return context == null
                ? delegate
                : new NatagumoDensityFunction(delegate, context.source(), context.sampler());
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;initialDensityWithoutJaggedness()Lnet/minecraft/world/level/levelgen/DensityFunction;"))
    private DensityFunction kimetsu$wrapInitialDensity(NoiseRouter router) {
        DensityFunction delegate = router.initialDensityWithoutJaggedness();
        NatagumoTerrainContext.Context context = NatagumoTerrainContext.get();
        return context == null
                ? delegate
                : new NatagumoDensityFunction(delegate, context.source(), context.sampler());
    }
}
