package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "net.minecraft.world.inventory.GrindstoneMenu$2",
    "net.minecraft.world.inventory.GrindstoneMenu$3"
})
public abstract class GrindstoneAlchemyInputMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void kimetsu$allowAlchemyGrindstoneInputs(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(Items.BONE) || stack.is(Items.CALCITE)) {
            cir.setReturnValue(true);
        }
    }
}
