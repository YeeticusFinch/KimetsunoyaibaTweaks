package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoStructureCandidates;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
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
@Mixin(ChunkGenerator.class)
public class EnhancedMountStructureMixin {
    private static final RandomSpreadStructurePlacement RUI_PLACEMENT =
            new RandomSpreadStructurePlacement(8, 4, RandomSpreadType.LINEAR, 182736451);
    private static final RandomSpreadStructurePlacement BROTHER_PLACEMENT =
            new RandomSpreadStructurePlacement(8, 4, RandomSpreadType.LINEAR, 918273645);
    private static final ResourceKey<Structure> RUI = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui"));
    private static final ResourceKey<Structure> RUI_BROTHER = ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "house_rui_brother"));

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void kimetsu$limitNatagumoStructures(StructureSet.StructureSelectionEntry entry,
            StructureManager structureManager, RegistryAccess registryAccess, RandomState randomState,
            StructureTemplateManager structureTemplateManager, long seed, ChunkAccess chunk,
            ChunkPos chunkPos, SectionPos sectionPos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled
                || !(((ChunkGenerator) (Object) this).getBiomeSource() instanceof EnhancedMountBiomeSource)) return;
        ResourceKey<Structure> key = entry.structure().unwrapKey().orElse(null);
        if (!RUI.equals(key) && !RUI_BROTHER.equals(key)) return;
        boolean rui = RUI.equals(key);
        if (!NatagumoStructureCandidates.isPreferred(rui ? RUI_PLACEMENT : BROTHER_PLACEMENT,
                seed, chunkPos, rui ? "Boss Ring" : "Boss Minions Ring - Brother Region")) {
            cir.setReturnValue(false);
        }
    }
}
