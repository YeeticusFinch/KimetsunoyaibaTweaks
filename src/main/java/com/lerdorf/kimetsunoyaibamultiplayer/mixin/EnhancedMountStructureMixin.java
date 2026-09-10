package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limits each base Natagumo structure to one deterministic candidate per enhanced region. */
@Mixin(net.minecraft.world.level.chunk.ChunkGenerator.class)
public class EnhancedMountStructureMixin {
    private static final ResourceKey<Structure> RUI = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui"));
    private static final ResourceKey<Structure> RUI_BROTHER = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui_brother"));

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void kimetsu$limitNatagumoStructures(
            StructureSet.StructureSelectionEntry entry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return;
        }

        ResourceKey<Structure> key = entry.structure().unwrapKey().orElse(null);
        if (!RUI.equals(key) && !RUI_BROTHER.equals(key)) {
            return;
        }

        int salt = RUI.equals(key) ? 414556433 : 672031365;
        RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(
                100, 50, RandomSpreadType.LINEAR, salt);
        if (!isPreferredCandidate(placement, seed, chunkPos,
                EnhancedMountBiomeConfig.natagumoNoiseScale)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isPreferredCandidate(RandomSpreadStructurePlacement placement, long seed,
                                                ChunkPos candidate, double scale) {
        int regionSize = Math.max(1, (int) Math.ceil(scale / 16.0));
        int regionX = Math.floorDiv(candidate.x, regionSize);
        int regionZ = Math.floorDiv(candidate.z, regionSize);
        int startX = regionX * regionSize;
        int startZ = regionZ * regionSize;
        int endX = startX + regionSize - 1;
        int endZ = startZ + regionSize - 1;

        ChunkPos preferred = null;
        int firstGridX = Math.floorDiv(startX, placement.spacing()) - 1;
        int lastGridX = Math.floorDiv(endX, placement.spacing()) + 1;
        int firstGridZ = Math.floorDiv(startZ, placement.spacing()) - 1;
        int lastGridZ = Math.floorDiv(endZ, placement.spacing()) + 1;
        for (int gridX = firstGridX; gridX <= lastGridX; gridX++) {
            for (int gridZ = firstGridZ; gridZ <= lastGridZ; gridZ++) {
                ChunkPos possible = placement.getPotentialStructureChunk(seed, gridX, gridZ);
                if (possible.x < startX || possible.x > endX || possible.z < startZ || possible.z > endZ) {
                    continue;
                }
                if (!EnhancedMountBiomeSource.isRegionSelected(seed, possible.getMiddleBlockX(),
                        possible.getMiddleBlockZ(), scale, EnhancedMountBiomeConfig.natagumoMountainChance,
                        0x4E415441)) {
                    continue;
                }
                if (preferred == null || possible.toLong() < preferred.toLong()) {
                    preferred = possible;
                }
            }
        }
        return candidate.equals(preferred);
    }
}
