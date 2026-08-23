package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.mcreator.kimetsunoyaiba.entity.InosukeEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.entity.model.InosukeModel")
public abstract class BaseInosukeModelMixin {
    private static final ResourceLocation DEMONIZED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_inosuke.png");

    @Inject(
        method = "getTextureResource(Lnet/mcreator/kimetsunoyaiba/entity/InosukeEntity;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void kimetsunoyaibamultiplayer$useDemonizedInosukeTexture(
            InosukeEntity entity,
            CallbackInfoReturnable<ResourceLocation> cir) {
        if (entity != null && entity.getPersistentData().getBoolean("oni")) {
            cir.setReturnValue(DEMONIZED_TEXTURE);
        }
    }
}
