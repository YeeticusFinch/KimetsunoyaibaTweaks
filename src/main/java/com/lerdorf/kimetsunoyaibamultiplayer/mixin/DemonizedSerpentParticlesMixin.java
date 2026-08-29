package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the white serpent-breathing particles spawned by the base KimetsunoYaiba mod
 * with black ones when the user (player or entity) is a demon.
 *
 * The base mod spawns all Serpent Breathing form particles by executing literal commands like
 *   particle dust 1 1 1 2 ~ ~ ~ 0.25 0.25 0.25 0 20 force
 * from BreathesHebi1-4Procedure, BreathesEnentyodaProcedure and SwingHebi1Procedure
 * (reached both from Iguro's sword and from this mod's snake sword).
 *
 * When the config option is enabled and the commanding entity is a demon, we rewrite
 * "dust 1 1 1" to "dust 0 0 0" (black) keeping identical size and positioning, and run
 * the rewritten command instead of the original.
 */
@Mixin(Commands.class)
public class DemonizedSerpentParticlesMixin {
    private static final Pattern WHITE_DUST_PARTICLE_COMMAND = Pattern.compile(
        "^/?(particle\\s+)(?:minecraft:)?dust\\s+1(?:\\.0+)?\\s+1(?:\\.0+)?\\s+1(?:\\.0+)?\\b"
    );

    @Inject(
        method = "performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void kimetsu$demonizeSerpentParticles(
            CommandSourceStack source,
            String command,
            CallbackInfoReturnable<Integer> cir) {

        if (!Config.demonizedBreathingStyles || command == null) {
            return;
        }

        String trimmed = command.trim();
        Matcher matcher = WHITE_DUST_PARTICLE_COMMAND.matcher(trimmed);
        if (!matcher.find()) {
            return;
        }

        // The base mod builds its CommandSourceStack with the breathing entity attached
        Entity entity = source.getEntity();
        if (!(entity instanceof LivingEntity living) || !Damager.isDemon(living)) {
            return;
        }

        // Only touch serpent breathing form/swing particles.
        if (!isHoldingSerpentBreathingSword(living) && !isFromSerpentProcedure()) {
            return;
        }

        String blackCommand = matcher.replaceFirst("$1dust 0 0 0");
        Log.debug("Demonized serpent breathing: replacing particle command '{}' with '{}'",
            trimmed, blackCommand);

        // Run the recolored command instead of the original.
        // This re-enters performCommand, but the rewritten command no longer matches
        // "particle dust 1 1 1" so it executes normally without recursing here.
        try {
            cir.setReturnValue(source.getServer().getCommands().getDispatcher().execute(blackCommand, source));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            Log.debug("Demonized serpent particle command failed: {}", e.getMessage());
            cir.setReturnValue(0);
        }
    }

    private static boolean isHoldingSerpentBreathingSword(LivingEntity living) {
        ItemStack mainHand = living.getMainHandItem();
        if (SwordParticleMapping.isSerpentBreathingSword(mainHand)) {
            return true;
        }
        return SwordParticleMapping.isSerpentBreathingSword(living.getOffhandItem());
    }

    private static boolean isFromSerpentProcedure() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("net.mcreator.kimetsunoyaiba.procedures.")
                    && (className.contains("BreathesHebi")
                        || className.contains("BreathesEnentyoda")
                        || className.contains("SwingHebi"))) {
                return true;
            }
        }
        return false;
    }
}
