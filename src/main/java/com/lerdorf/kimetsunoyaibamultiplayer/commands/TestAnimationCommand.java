package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
// import dev.kosmx.playerAnim.api.layered.AnimationStack; // REMOVED: Client-only, unused in server command
// import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer; // REMOVED: Client-only, unused in server command
// import dev.kosmx.playerAnim.api.layered.ModifierLayer; // REMOVED: Client-only, unused in server command
// import dev.kosmx.playerAnim.core.data.KeyframeAnimation; // REMOVED: Client-only, unused in server command
// import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess; // REMOVED: Client-only, unused in server command
// import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry; // REMOVED: Client-only, unused in server command
// import net.minecraft.client.Minecraft; // REMOVED: Client-only, causes server crash
// import net.minecraft.client.player.AbstractClientPlayer; // REMOVED: Client-only, causes server crash
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class TestAnimationCommand {
    private static final ResourceLocation TEST_ANIMATION = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sword_to_left");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("testanim")
            .requires(source -> source.hasPermission(0)) // Anyone can use this command
            .executes(context -> {
                CommandSourceStack source = context.getSource();

                if (source.isPlayer()) {
                    ServerPlayer player = source.getPlayer();
                    if (player != null) {
                        return executeTestAnimation(player);
                    }
                }

                source.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            });

        dispatcher.register(command);
    }

    private static int executeTestAnimation(ServerPlayer player) {
        try {
            UUID playerUUID = player.getUUID();

            player.sendSystemMessage(Component.literal("Playing test animation 'sword_to_left' on all players (including you)..."));

            if (Config.logDebug) {
                Log.info("Test animation command executed by player: {}", player.getName().getString());
            }

            // Create the animation sync packet for sword_to_left animation
            AnimationSyncPacket packet = new AnimationSyncPacket(
                playerUUID,
                TEST_ANIMATION,
                0,  // Start at tick 0
                30, // Animation length (approximate for sword_to_left)
                false, // Not looping
                false, // Not stopping
                null // No animation data (will use registry lookup)
            );

            // Send to all clients including the sender (this will show animation on everyone including the command user)
            ModNetworking.sendToAllClients(packet);

            player.sendSystemMessage(Component.literal("Test animation packet sent to all players (including yourself)!"));

            return 1;
        } catch (Exception e) {
            Log.error("Failed to execute test animation command", e);
            player.sendSystemMessage(Component.literal("Failed to play test animation: " + e.getMessage()));
            return 0;
        }
    }

    // NOTE: playTestAnimationLocally() method removed - had client-only imports
    // If needed, this functionality can be added to ClientCommandHandler.java
}