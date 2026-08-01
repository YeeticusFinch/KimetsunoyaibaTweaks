package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.FearClientHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FearGameRendererMixin {
    @Inject(
        method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void kimetsunoyaibamultiplayer$renderFearWorldOverlay(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        FearClientHandler.renderWorldFearOverlay(poseStack, partialTick);
    }
}
