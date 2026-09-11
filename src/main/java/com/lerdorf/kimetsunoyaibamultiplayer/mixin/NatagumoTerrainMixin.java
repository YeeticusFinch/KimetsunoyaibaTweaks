package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoTerrainContext;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the active Natagumo source available while NoiseChunk builds vanilla density functions. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NatagumoTerrainMixin {
    @Inject(method = "createNoiseChunk", at = @At("HEAD"))
    private void kimetsu$startNatagumoDensity(
            net.minecraft.world.level.chunk.ChunkAccess chunk,
            net.minecraft.world.level.StructureManager structureManager,
            net.minecraft.world.level.levelgen.blending.Blender blender,
            RandomState randomState,
            CallbackInfoReturnable<?> cir) {
        if (!(((NoiseBasedChunkGenerator) (Object) this).getBiomeSource()
                instanceof EnhancedMountBiomeSource enhancedSource)) {
            return;
        }
        NatagumoTerrainContext.set(enhancedSource, randomState.sampler());
    }

    @Inject(method = "createNoiseChunk", at = @At("RETURN"))
    private void kimetsu$finishNatagumoDensity(CallbackInfoReturnable<?> cir) {
        NatagumoTerrainContext.clear();
    }
}
