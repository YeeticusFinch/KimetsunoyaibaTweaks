package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Mixin to hide nichirin swords from entity hands when not in combat.
 * This affects ALL GeckoLib entities, including those from the base mod and other mods.
 */
@Mixin(value = BlockAndItemGeoLayer.class, remap = false)
public class BlockAndItemGeoLayerMixin {

    static {
        Log.info("BlockAndItemGeoLayerMixin loaded successfully!");
    }

    /**
     * Intercepts the getStackForBone method to hide nichirin swords when entity is not in combat.
     * This runs AFTER the original method, allowing us to modify its return value.
     */
    @Inject(
        method = "getStackForBone",
        at = @At("RETURN"),
        cancellable = true,
        remap = false,
        require = 0  // Don't crash if method not found (base mod might not use this)
    )
    private void hideNichirinSwordsWhenNotInCombat(
            GeoBone bone,
            GeoAnimatable animatable,
            CallbackInfoReturnable<ItemStack> cir) {

        // This Mixin only works if base mod uses GeckoLib's BlockAndItemGeoLayer
        // Base mod entities don't implement GeoAnimatable, so this might not fire
        if (animatable instanceof LivingEntity entity) {
            Log.info("BlockAndItemGeoLayerMixin called for bone: {} entity: {}",
                bone.getName(), entity.getType().getDescriptionId());
        }

        // Only process main hand bones
        if (!bone.getName().equals("itemMainHand") &&
            !bone.getName().equals("itemMainHand2") &&
            !bone.getName().equals("itemMainHand3")) {
            return;
        }

        // Only process if animatable is a LivingEntity
        if (!(animatable instanceof LivingEntity entity)) {
            return;
        }

        // Get the item that would be rendered
        ItemStack stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) {
            Log.debug("  Stack is null or empty");
            return;
        }

        // Debug logging
        Log.debug("BlockAndItemGeoLayerMixin: Processing entity {} with item {}",
            entity.getType().getDescriptionId(), stack.getDescriptionId());

        // If it's a nichirin sword and entity is not in combat, hide it
        boolean isKnYSword = SwordParticleMapping.isKimetsunoyaibaSword(stack);
        boolean inCombat = EntityCombatStateTracker.isInCombat(entity);

        Log.debug("  isKnYSword={}, inCombat={}", isKnYSword, inCombat);

        if (isKnYSword && !inCombat) {
            Log.debug("  Hiding sword from hand!");
            // Return empty stack to hide the sword from hand
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
