package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.client.McaDemonEyesLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "forge.net.mca.client.render.ZombieVillagerEntityMCARenderer")
public abstract class McaZombieVillagerRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void kimetsunoyaibamultiplayer$addDemonEyesLayer(CallbackInfo ci) {
        MobRenderer renderer = (MobRenderer) (Object) this;
        renderer.addLayer(new McaDemonEyesLayer(renderer));
    }
}
