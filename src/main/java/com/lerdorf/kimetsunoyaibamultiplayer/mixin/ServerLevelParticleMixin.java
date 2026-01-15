package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept and optionally cancel sword swing particles from the base KimetsunoYaiba mod.
 * This only affects particles spawned by the base mod's SwingZeroProcedure and similar swing procedures.
 */
@Mixin(ServerLevel.class)
public class ServerLevelParticleMixin {

    /**
     * Intercepts ServerLevel.sendParticles() to cancel sword swing particles from the base mod.
     * The method signature is:
     * sendParticles(ParticleOptions particle, double x, double y, double z, int count, double xOffset, double yOffset, double zOffset, double speed)
     *
     * In obfuscated form this is m_8767_
     */
    @Inject(
        method = "m_8767_(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0  // Don't crash if method signature changes
    )
    private void cancelBaseModSwordSwingParticles(
            ParticleOptions particle,
            double x, double y, double z,
            int count,
            double xOffset, double yOffset, double zOffset,
            double speed,
            CallbackInfoReturnable<Integer> cir) {

        // Debug: Log every particle spawn attempt
        Log.debug("ServerLevelParticleMixin called - Config disabled: {}", Config.disableBaseModSwordSwingParticles);

        // Only process if config option is enabled
        if (!Config.disableBaseModSwordSwingParticles) {
            return;
        }

        // Check stack trace to see if this is called from base mod's swing procedures
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        Log.debug("Checking stack trace for swing procedures...");

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();

            // Check if called from base mod's sword swing procedures
            // These are the procedures that spawn particles on left-click sword swings
            if (className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingZeroProcedure") ||
                className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingKaminariProcedure") ||
                className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingItemProcedure") ||
                // Add other swing-related procedures if needed
                (className.startsWith("net.mcreator.kimetsunoyaiba.procedures.Swing") &&
                 !className.contains("Breathes"))) {  // Don't cancel breathing form particles

                Log.debug("Canceling base mod sword swing particle from: {}", className);

                // Cancel the particle spawn by returning 0 (no particles spawned)
                cir.setReturnValue(0);
                return;
            }
        }
    }
}
