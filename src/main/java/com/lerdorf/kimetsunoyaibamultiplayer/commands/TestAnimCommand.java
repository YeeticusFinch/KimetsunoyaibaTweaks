package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class TestAnimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testanim")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("animation", StringArgumentType.string())
                .executes(TestAnimCommand::testSpecificAnimation))
            .executes(TestAnimCommand::testDefaultAnimation));
    }

    private static int testDefaultAnimation(CommandContext<CommandSourceStack> context) {
        return testAnimation(context, "sword_to_right");
    }

    private static int testSpecificAnimation(CommandContext<CommandSourceStack> context) {
        String animationName = StringArgumentType.getString(context, "animation");
        return testAnimation(context, animationName);
    }

    private static int testAnimation(CommandContext<CommandSourceStack> context, String animationName) {
        System.out.println("TestAnimCommand executed with animation: " + animationName);

        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        System.out.println("Player holding: " + mainHand.getItem());

        if (mainHand.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou need to hold a sword to test particles"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§aTesting animation: §b" + animationName));
        player.sendSystemMessage(Component.literal("§7This will spawn particles following the animation arc"));

        // Calculate animation duration based on actual config parameters
        double totalSteps = com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleArcDegrees /
                           com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleAngleIncrement;
        double totalTicks = totalSteps / com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleStepsPerTick;
        int animationTicks = Math.max(1, (int)Math.ceil(totalTicks));

        player.sendSystemMessage(Component.literal("§7Animation will complete in " + animationTicks + " ticks"));

        // Simulate the fast animation progression
        for (int tick = 0; tick < animationTicks; tick++) {
            final int currentTick = tick;
            player.getServer().execute(() -> {
                // Force spawn particles for each tick of the animation
                SwordParticleHandler.spawnSwordParticles(player, mainHand, animationName, currentTick);
            });
        }

        player.sendSystemMessage(Component.literal("§aAnimation test completed!"));
        return 1;
    }
}