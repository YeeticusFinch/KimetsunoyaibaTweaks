package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.BaseInosukeDemonEyesLayer;
import net.mcreator.kimetsunoyaiba.entity.InosukeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.client.renderer.InosukeRenderer")
public abstract class BaseInosukeRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void kimetsunoyaibamultiplayer$addDemonizedEyesLayer(CallbackInfo ci) {
        GeoEntityRenderer<InosukeEntity> renderer = (GeoEntityRenderer<InosukeEntity>) (Object) this;
        renderer.addRenderLayer(new BaseInosukeDemonEyesLayer(renderer));
    }
}
