package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.BaseNamedDemonEyesLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.client.renderer.KanawoRenderer")
public abstract class BaseKanawoRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void kimetsunoyaibamultiplayer$addDemonizedEyesLayer(CallbackInfo ci) {
        GeoEntityRenderer renderer = (GeoEntityRenderer) (Object) this;
        renderer.addRenderLayer(new BaseNamedDemonEyesLayer(renderer,
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/entity/demon_eyes_kanawo.png")));
    }
}
