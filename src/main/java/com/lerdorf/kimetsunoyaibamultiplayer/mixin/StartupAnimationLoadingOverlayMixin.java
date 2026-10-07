package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.StartupAnimationRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeLoadingOverlay.class)
public abstract class StartupAnimationLoadingOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void kimetsunoyaibamultiplayer$renderStartupAnimation(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        StartupAnimationRenderer.render(guiGraphics);
    }
}
