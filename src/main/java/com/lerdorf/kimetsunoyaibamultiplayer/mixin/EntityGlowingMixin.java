package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.client.VermilionEyeEntityRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SpatialAwarenessEntityRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make entities glow when the player has the Vermilion Eye effect.
 * This enables seeing entities through walls with color-coded outlines.
 *
 * Color coding:
 * - Red (0xFF0000): Hostile/aggressive entities, hostile players
 * - Blue (0x0088FF): Non-hostile players
 * - Yellow (0xFFFF00): Neutral mobs (monsters not currently aggro)
 * - Green (0x00FF00): Passive animals
 */
@Mixin(Entity.class)
public class EntityGlowingMixin {

    /**
     * Inject into isCurrentlyGlowing() - MCP name (used in dev environment)
     */
    @Inject(
        method = "isCurrentlyGlowing",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        handleGlowingCheck(cir);
    }

    /**
     * Inject into isCurrentlyGlowing() - SRG name (used in production)
     */
    @Inject(
        method = "m_142038_",
        at = @At("RETURN"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void onIsCurrentlyGlowingSrg(CallbackInfoReturnable<Boolean> cir) {
        handleGlowingCheck(cir);
    }

    /**
     * Common handler for glowing check
     */
    private void handleGlowingCheck(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        // Hard-stop stale Vermilion glow on effect removal, even if the vanilla glowing tag is still true.
        if (!VermilionEyeEntityRenderer.isActive() && VermilionEyeEntityRenderer.wasRecentlyHighlighted(self)) {
            boolean hasVanillaGlowingEffect = self instanceof LivingEntity living && living.hasEffect(MobEffects.GLOWING);
            if (!hasVanillaGlowingEffect) {
                cir.setReturnValue(false);
                return;
            }
        }

        // If already glowing (e.g., from Glowing effect), don't interfere
        if (cir.getReturnValue()) {
            return;
        }

        if (SpatialAwarenessEntityRenderer.shouldEntityGlow(self)) {
            cir.setReturnValue(true);
            return;
        }

        if (VermilionEyeEntityRenderer.shouldEntityGlow(self)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Inject into getTeamColor() - MCP name (used in dev environment)
     */
    @Inject(
        method = "getTeamColor",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        handleTeamColorCheck(cir);
    }

    /**
     * Inject into getTeamColor() - SRG name (used in production)
     */
    @Inject(
        method = "m_20250_",
        at = @At("RETURN"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void onGetTeamColorSrg(CallbackInfoReturnable<Integer> cir) {
        handleTeamColorCheck(cir);
    }

    /**
     * Common handler for team color override
     */
    private void handleTeamColorCheck(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (SpatialAwarenessEntityRenderer.shouldEntityGlow(self)) {
            cir.setReturnValue(SpatialAwarenessEntityRenderer.getGlowColor());
            return;
        }
        if (VermilionEyeEntityRenderer.shouldEntityGlow(self)) {
            // Override with our threat-based color
            int color = VermilionEyeEntityRenderer.getGlowColor(self);
            cir.setReturnValue(color);
        }
    }
}
