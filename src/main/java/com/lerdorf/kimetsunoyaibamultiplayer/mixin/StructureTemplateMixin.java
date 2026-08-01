package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeStructureMaterializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {
    @Inject(
        method = "processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false,
        require = 1
    )
    private static void kimetsu$expandBridgersBeforePlacement(ServerLevelAccessor level, BlockPos origin, BlockPos pivot,
                                                              StructurePlaceSettings settings,
                                                              List<StructureTemplate.StructureBlockInfo> originalInfos,
                                                              StructureTemplate template,
                                                              CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> cir) {
        cir.setReturnValue(BridgeStructureMaterializer.expandProcessedBridgers(level, settings, cir.getReturnValue()));
    }
}
