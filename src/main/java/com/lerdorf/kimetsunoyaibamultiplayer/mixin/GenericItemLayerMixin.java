package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.GeoBone;

@Mixin(targets = "net.mcreator.kimetsunoyaiba.GenericItemLayer")
public abstract class GenericItemLayerMixin {
    private static final String MAIN_HAND_BONE = "itemMainHand";
    private static final String MAIN_HAND_BONE_2 = "itemMainHand2";
    private static final String MAIN_HAND_BONE_3 = "itemMainHand3";

    @Inject(
        method = "getStackForBone(Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void hideNichirinSwordWhenSheathed(GeoBone bone, LivingEntity entity,
                                               CallbackInfoReturnable<ItemStack> cir) {
        if (!SwordDisplayConfig.enabled) {
            return;
        }

        String boneName = bone.getName();
        if (!MAIN_HAND_BONE.equals(boneName) &&
            !MAIN_HAND_BONE_2.equals(boneName) &&
            !MAIN_HAND_BONE_3.equals(boneName)) {
            return;
        }

        ItemStack stack = cir.getReturnValue();
        if (!SwordParticleMapping.isKimetsunoyaibaSword(stack) || SwordParticleMapping.isSheathExempt(stack)) {
            return;
        }

        EntityCombatStateTracker.updateCombatState(entity);
        if (!EntityCombatStateTracker.isInCombat(entity)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
