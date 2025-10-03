package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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

    // Client-side method to play animation locally
    public static void playTestAnimationLocally() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            AbstractClientPlayer clientPlayer = mc.player;

            // Try to get the animation from registry
            KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(TEST_ANIMATION);
            if (animation == null) {
                // Try alternate resource locations
                ResourceLocation altLocation = ResourceLocation.fromNamespaceAndPath("playeranimator", "sword_to_left");
                animation = PlayerAnimationRegistry.getAnimation(altLocation);
            }

            if (animation != null) {
                AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(clientPlayer);
                if (animationStack != null) {
                    KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);
                    ModifierLayer modifierLayer = new ModifierLayer<>();
                    modifierLayer.setAnimation(animPlayer);
                    animationStack.addAnimLayer(2000, modifierLayer); // High priority

                    if (Config.logDebug) {
                        Log.info("Playing test animation locally for player: {}", clientPlayer.getName().getString());
                    }

                    // Show on-screen message
                    if (Config.onScreenDebug) {
                        clientPlayer.displayClientMessage(
                            Component.literal("§a[Test] Playing sword_to_left animation"),
                            true
                        );
                    }
                }
            } else {
                if (Config.logDebug) {
                    Log.warn("Could not find test animation 'sword_to_left' in registry");
                }

                // Show error message
                clientPlayer.displayClientMessage(
                    Component.literal("§c[Test] Animation 'sword_to_left' not found in registry"),
                    true
                );
            }
        } catch (Exception e) {
            Log.error("Failed to play test animation locally", e);
        }
    }
}