package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Fourth Form's target pitch to the whole player model, not only the camera/head. */
@Mixin(PlayerRenderer.class)
public abstract class LovePlayerRendererMixin {
    @Inject(
        method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At("TAIL")
    )
    private void kimetsunoyaibamultiplayer$rotateFourthFormBody(AbstractClientPlayer player,
                                                                  PoseStack poseStack, float ageInTicks,
                                                                  float rotationYaw, float partialTick,
                                                                  CallbackInfo callbackInfo) {
        if (!AnimationSyncHandler.isAnimationActive(player.getUUID(), "love_fourth_form_short")) {
            return;
        }

        float pitch = Mth.rotLerp(partialTick, player.xRotO, player.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
