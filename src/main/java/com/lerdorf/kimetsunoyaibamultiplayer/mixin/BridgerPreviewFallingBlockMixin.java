package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BridgerPreviewManager;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class BridgerPreviewFallingBlockMixin {
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void kimetsu$ignoreBridgerPreviewPick(CallbackInfoReturnable<Boolean> cir) {
        if (BridgerPreviewManager.isPreviewEntity((FallingBlockEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
