package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BaseBackstepHudOverlay;
import net.mcreator.kimetsunoyaiba.client.screens.OverlayCooldownTimeOverlay;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OverlayCooldownTimeOverlay.class, remap = false)
public abstract class BaseBackstepHudOverlayMixin {
    @Inject(method = "eventHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private static void kimetsunoyaibamultiplayer$replaceBackstepHud(RenderGuiEvent.Pre event, CallbackInfo ci) {
        if (!BaseBackstepHudOverlay.shouldReplaceBaseOverlay()) {
            return;
        }

        ci.cancel();
        BaseBackstepHudOverlay.render(event);
    }
}
