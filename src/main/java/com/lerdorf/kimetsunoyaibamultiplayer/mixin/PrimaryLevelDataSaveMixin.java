package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;
import java.util.Map;

/** Keeps runtime-only biome wrappers out of the level.dat worldgen codec. */
@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataSaveMixin {
    @Redirect(
            method = "setTagData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldGenSettings;encode(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/world/level/levelgen/WorldOptions;Lnet/minecraft/core/RegistryAccess;)Lcom/mojang/serialization/DataResult;"
            )
    )
    private <T> DataResult<T> kimetsu$encodeSerializableWorldgenSettings(
            DynamicOps<T> ops, WorldOptions options, RegistryAccess registryAccess) {
        Registry<LevelStem> levelStems = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        Map<ChunkGenerator, EnhancedMountBiomeSource> replacedSources = new IdentityHashMap<>();

        for (LevelStem levelStem : levelStems) {
            ChunkGenerator generator = levelStem.generator();
            if (generator.getBiomeSource() instanceof EnhancedMountBiomeSource enhancedSource) {
                replacedSources.put(generator, enhancedSource);
                generator.biomeSource = enhancedSource.serializationDelegate();
            }
        }

        try {
            return WorldGenSettings.encode(ops, options, registryAccess);
        } finally {
            for (Map.Entry<ChunkGenerator, EnhancedMountBiomeSource> entry : replacedSources.entrySet()) {
                entry.getKey().biomeSource = entry.getValue();
            }
        }
    }
}
