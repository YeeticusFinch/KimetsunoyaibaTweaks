package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.EnhancedFormCycleHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes mouse movement to the form wheel without rotating the camera when configured. */
@Mixin(MouseHandler.class)
public abstract class EnhancedFormCycleMouseMixin {
    @Shadow(remap = false) private double f_91516_;
    @Shadow(remap = false) private double f_91517_;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void kimetsu$lockCameraForFormWheel(CallbackInfo ci) {
        if (!EnhancedFormCycleHandler.isCameraMovementLocked()) {
            return;
        }

        double deltaX = f_91516_;
        double deltaY = f_91517_;
        f_91516_ = 0.0D;
        f_91517_ = 0.0D;
        EnhancedFormCycleHandler.handleLockedMouseMovement(deltaX, deltaY);
        ci.cancel();
    }
}
