package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limits each base Natagumo structure to one deterministic candidate per region. */
@Mixin(net.minecraft.world.level.chunk.ChunkGenerator.class)
public class EnhancedMountStructureMixin {
    private static final String BOSS_RING = "Boss Ring";
    private static final String BROTHER_REGION = "Boss Minions Ring - Brother Region";
    private static final int CUSTOM_STRUCTURE_SPACING = 8;
    private static final int CUSTOM_STRUCTURE_SEPARATION = 4;
    private static final int RUI_STRUCTURE_SALT = 182736451;
    private static final int RUI_BROTHER_STRUCTURE_SALT = 918273645;
    private static final ResourceKey<Structure> RUI = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui"));
    private static final ResourceKey<Structure> RUI_BROTHER = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui_brother"));

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

        int salt = RUI.equals(key) ? RUI_STRUCTURE_SALT : RUI_BROTHER_STRUCTURE_SALT;
        RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(
                CUSTOM_STRUCTURE_SPACING, CUSTOM_STRUCTURE_SEPARATION, RandomSpreadType.LINEAR, salt);
        String targetRegion = RUI.equals(key) ? BOSS_RING : BROTHER_REGION;
        if (!isPreferredCandidate(placement, seed, chunkPos, targetRegion)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isPreferredCandidate(RandomSpreadStructurePlacement placement, long seed,
                                                ChunkPos candidate, String targetRegion) {
        EnhancedMountBiomeSource.NatagumoRegion region = EnhancedMountBiomeSource.getNatagumoRegion(
                seed, candidate.getMiddleBlockX(), candidate.getMiddleBlockZ());
        if (region == null || region.strength() < EnhancedMountBiomeConfig.natagumoBiomeThreshold
                || !targetRegion.equals(EnhancedMountBiomeSource.natagumoRegionName(
                region, candidate.getMiddleBlockX(), candidate.getMiddleBlockZ()))) {
            return false;
        }

        int radiusChunks = Math.max(1, (int) Math.ceil(region.radius() / 16.0) + 2);
        int centerChunkX = Math.floorDiv(region.centerX(), 16);
        int centerChunkZ = Math.floorDiv(region.centerZ(), 16);
        ChunkPos preferred = null;
        long preferredDistance = Long.MAX_VALUE;
        int firstGridX = Math.floorDiv(centerChunkX - radiusChunks, placement.spacing()) - 1;
        int lastGridX = Math.floorDiv(centerChunkX + radiusChunks, placement.spacing()) + 1;
        int firstGridZ = Math.floorDiv(centerChunkZ - radiusChunks, placement.spacing()) - 1;
        int lastGridZ = Math.floorDiv(centerChunkZ + radiusChunks, placement.spacing()) + 1;

        for (int gridX = firstGridX; gridX <= lastGridX; gridX++) {
            for (int gridZ = firstGridZ; gridZ <= lastGridZ; gridZ++) {
                ChunkPos possible = placement.getPotentialStructureChunk(seed, gridX, gridZ);
                if (Math.abs(possible.x - centerChunkX) > radiusChunks
                        || Math.abs(possible.z - centerChunkZ) > radiusChunks) {
                    continue;
                }
                EnhancedMountBiomeSource.NatagumoRegion possibleRegion = EnhancedMountBiomeSource.getNatagumoRegion(
                        seed, possible.getMiddleBlockX(), possible.getMiddleBlockZ());
                if (possibleRegion == null || possibleRegion.ring() != region.ring()
                        || possibleRegion.centerX() != region.centerX()
                        || possibleRegion.centerZ() != region.centerZ()
                        || possibleRegion.strength() < EnhancedMountBiomeConfig.natagumoBiomeThreshold
                        || !targetRegion.equals(EnhancedMountBiomeSource.natagumoRegionName(
                        possibleRegion, possible.getMiddleBlockX(), possible.getMiddleBlockZ()))) {
                    continue;
                }

                long dx = possible.getMiddleBlockX() - region.centerX();
                long dz = possible.getMiddleBlockZ() - region.centerZ();
                long distance = dx * dx + dz * dz;
                if (distance < preferredDistance) {
                    preferred = possible;
                    preferredDistance = distance;
                }
            }
        }
        return candidate.equals(preferred);
    }
}
