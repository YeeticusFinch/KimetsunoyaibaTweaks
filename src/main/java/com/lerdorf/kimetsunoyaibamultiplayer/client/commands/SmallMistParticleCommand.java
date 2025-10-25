package com.lerdorf.kimetsunoyaibamultiplayer.client.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.SmallMistParticleHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

@OnlyIn(Dist.CLIENT)
public class SmallMistParticleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("smallmist")
            .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                .executes(context -> {
                    int count = IntegerArgumentType.getInteger(context, "count");
                    spawnParticles(context.getSource(), count);
                    return 1;
                })
            )
            .executes(context -> {
                spawnParticles(context.getSource(), 10); // Default to 10 particles
                return 1;
            })
        );
    }

    private static void spawnParticles(CommandSourceStack source, int count) {
        if (source.getEntity() != null) {
            double x = source.getEntity().getX();
            double y = source.getEntity().getY() + 1.0; // Spawn above player
            double z = source.getEntity().getZ();
            
            SmallMistParticleHandler.spawnSmallMistParticles(x, y, z, count, 1.0);
            
            source.sendSuccess(() -> Component.literal("Spawned " + count + " small mist particles!")
                .withStyle(ChatFormatting.GREEN), true);
        }
    }
}