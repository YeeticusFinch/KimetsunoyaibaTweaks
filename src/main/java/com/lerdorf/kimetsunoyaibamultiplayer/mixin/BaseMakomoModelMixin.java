package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.mcreator.kimetsunoyaiba.entity.MakomoEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.entity.model.MakomoModel")
public abstract class BaseMakomoModelMixin {
    private static final ResourceLocation DEMONIZED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_makomo.png");

    @Inject(
        method = "getTextureResource(Lnet/mcreator/kimetsunoyaiba/entity/MakomoEntity;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void kimetsunoyaibamultiplayer$useDemonizedMakomoTexture(
            MakomoEntity entity,
            CallbackInfoReturnable<ResourceLocation> cir) {
        if (entity != null && entity.getPersistentData().getBoolean("oni")) {
            cir.setReturnValue(DEMONIZED_TEXTURE);
        }
    }
}
