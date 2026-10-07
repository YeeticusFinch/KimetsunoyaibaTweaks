package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.mcreator.kimetsunoyaiba.entity.KanawoEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.entity.model.KanawoModel")
public abstract class BaseKanawoModelMixin {
    private static final ResourceLocation DEMONIZED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "textures/entity/oni_kanawo.png");

    @Inject(
        method = "getTextureResource(Lnet/mcreator/kimetsunoyaiba/entity/KanawoEntity;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void kimetsunoyaibamultiplayer$useDemonizedKanawoTexture(
            KanawoEntity entity,
            CallbackInfoReturnable<ResourceLocation> cir) {
        if (entity != null && entity.getPersistentData().getBoolean("oni")) {
            cir.setReturnValue(DEMONIZED_TEXTURE);
        }
    }
}
