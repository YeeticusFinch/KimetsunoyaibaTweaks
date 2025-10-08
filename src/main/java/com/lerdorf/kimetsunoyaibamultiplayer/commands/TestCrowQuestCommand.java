package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Test command to demonstrate crow quest markers
 * Usage: /testcrowquest <x> <y> <z> [duration]
 */
public class TestCrowQuestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testcrowquest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("location", Vec3Argument.vec3())
                        .executes(context -> {
                            return executeCrowQuest(
                                    context.getSource(),
                                    Vec3Argument.getVec3(context, "location"),
                                    1200 // Default 60 seconds
                            );
                        })
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 72000))
                                .executes(context -> {
                                    return executeCrowQuest(
                                            context.getSource(),
                                            Vec3Argument.getVec3(context, "location"),
                                            IntegerArgumentType.getInteger(context, "duration")
                                    );
                                })
                        )
                )
        );

        // Add a command to clear quest markers
        dispatcher.register(Commands.literal("clearcrowquest")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    CrowQuestMarkerHandler.clearQuestMarker(player.getUUID());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Cleared crow quest marker"),
                            true
                    );
                    return 1;
                })
        );
    }

    private static int executeCrowQuest(CommandSourceStack source, Vec3 location, int durationTicks) {
        try {
            Player player = source.getPlayerOrException();

            // NOTE: Quest markers are client-side only. This command just demonstrates the format.
            // Actual quest markers are set via chat messages detected by CrowQuestMarkerHandlerClient.
            // Send a simulated crow quest message that will be detected by the client handler
            String locationName = "Test Location";
            int x = (int) location.x;
            int z = (int) location.z;

            int durationSeconds = durationTicks / 20;
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "%s is at %d ~ %d (Quest markers are client-side, this is a test message)",
                            locationName, x, z
                    )),
                    true
            );

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to create crow quest message: " + e.getMessage()));
            return 0;
        }
    }
}