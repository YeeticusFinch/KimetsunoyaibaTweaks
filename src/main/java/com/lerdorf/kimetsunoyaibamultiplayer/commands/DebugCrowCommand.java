package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.client.CrowAnimationHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Debug command to inspect crow entity structure
 * Usage: /debugcrow
 */
public class DebugCrowCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugcrow")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    Vec3 pos = context.getSource().getPosition();
                    List<Entity> nearbyEntities = context.getSource().getLevel().getEntities(
                            null,
                            new AABB(pos.x - 10, pos.y - 10, pos.z - 10,
                                    pos.x + 10, pos.y + 10, pos.z + 10)
                    );

                    boolean foundCrow = false;
                    for (Entity entity : nearbyEntities) {
                        if (entity.getType().toString().contains("kasugai_crow")) {
                            foundCrow = true;
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Found crow: " + entity.getName().getString()),
                                    true
                            );
                            CrowAnimationHandler.debugCrowEntity(entity);
                            break;
                        }
                    }

                    if (!foundCrow) {
                        context.getSource().sendFailure(
                                Component.literal("No kasugai_crow found within 10 blocks. Check server console for debug output.")
                        );
                    } else {
                        context.getSource().sendSuccess(
                                () -> Component.literal("Check server console for detailed crow entity debug information"),
                                true
                        );
                    }

                    return 1;
                })
        );
    }
}