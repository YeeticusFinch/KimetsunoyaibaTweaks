package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept command-based particle spawning from the base KimetsunoYaiba mod.
 * The base mod uses Commands.performCommand() to execute "particle dust ..." commands
 * when players swing nichirin swords.
 */
@Mixin(Commands.class)
public class CommandsMixin {

    /**
     * Intercepts Commands.performCommand() to cancel particle commands from swing procedures.
     * Method signature: performCommand(CommandSourceStack source, String command)
     * Obfuscated: m_230957_(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I
     */
    @Inject(
        method = "m_230957_(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void cancelBaseModParticleCommands(
            CommandSourceStack source,
            String command,
            CallbackInfoReturnable<Integer> cir) {

        // Check if this is a particle command
        if (command == null || !command.trim().startsWith("particle")) {
            return;
        }

        Log.debug("CommandsMixin particle command - disableBaseModSwordSwingParticles={} - command={}",
            Config.disableBaseModSwordSwingParticles, command);

        // Only process if config option is enabled
        if (!Config.disableBaseModSwordSwingParticles) {
            return;
        }

        Log.debug("Particle command detected, checking stack trace...");

        // Check stack trace to see if called from base mod's swing procedures
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();

            // Check if called from base mod's sword swing procedures
            if (className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingZeroProcedure") ||
                className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingKaminariProcedure") ||
                className.equals("net.mcreator.kimetsunoyaiba.procedures.SwingItemProcedure") ||
                // Match any Swing procedure that doesn't contain "Breathes"
                (className.startsWith("net.mcreator.kimetsunoyaiba.procedures.Swing") &&
                 !className.contains("Breathes"))) {

                Log.debug("Canceling base mod particle command from: {} - Command: {}", className, command);

                // Cancel the command execution by returning 0 (command failed/no effect)
                cir.setReturnValue(0);
                return;
            }
        }
    }
}
