package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class ClientCommandHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEST_ANIMATION = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sword_to_left");

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("testanimc")
            .executes(context -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    return executeClientTestAnimation(mc.player);
                }
                return 0;
            });

        dispatcher.register(command);
        LOGGER.info("Registered client-side test animation command: /testanimc");
    }

    private static int executeClientTestAnimation(AbstractClientPlayer player) {
        try {
            player.displayClientMessage(Component.literal("§a[Client Test] Attempting to play sword_to_left animation..."), false);

            // First, try to find the animation in the registry
            KeyframeAnimation animation = findTestAnimation();

            if (animation != null) {
                // Play the animation locally on the client player
                playAnimationOnPlayer(player, animation);

                // Also send to server for other players to see
                AnimationSyncPacket packet = new AnimationSyncPacket(
                    player.getUUID(),
                    TEST_ANIMATION,
                    0,
                    30,
                    false,
                    false,
                    animation
                );
                ModNetworking.sendToServer(packet);

                player.displayClientMessage(Component.literal("§a[Client Test] Animation started successfully!"), false);
                return 1;
            } else {
                // Fallback: just send the packet without animation data
                player.displayClientMessage(Component.literal("§e[Client Test] Animation not found in registry, sending sync packet anyway..."), false);

                AnimationSyncPacket packet = new AnimationSyncPacket(
                    player.getUUID(),
                    TEST_ANIMATION,
                    0,
                    30,
                    false,
                    false,
                    null
                );
                ModNetworking.sendToServer(packet);

                return 1;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute client test animation", e);
            player.displayClientMessage(Component.literal("§c[Client Test] Error: " + e.getMessage()), false);
            return 0;
        }
    }

    private static KeyframeAnimation findTestAnimation() {
        // Try multiple resource locations
        ResourceLocation[] possibleLocations = {
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sword_to_left"),
            ResourceLocation.fromNamespaceAndPath("playeranimator", "sword_to_left"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "sword_to_left"),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/sword_to_left")
        };

        for (ResourceLocation loc : possibleLocations) {
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(loc);
            if (anim != null) {
                if (Config.logDebug) {
                    LOGGER.info("Found test animation at: {}", loc);
                }
                return anim;
            }
        }

        if (Config.logDebug) {
            LOGGER.warn("Could not find test animation 'sword_to_left' in registry");
        }
        return null;
    }

    private static void playAnimationOnPlayer(AbstractClientPlayer player, KeyframeAnimation animation) {
        try {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                // Create animation player
                KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(animation);

                // Create modifier layer and set animation
                ModifierLayer<IAnimation> modifierLayer = new ModifierLayer<>();
                modifierLayer.setAnimation(animPlayer);

                // Add to animation stack with high priority
                animationStack.addAnimLayer(3000, modifierLayer);

                if (Config.logDebug) {
                    LOGGER.info("Applied test animation to local player");
                }

                // Show on-screen debug message
                if (Config.onScreenDebug) {
                    player.displayClientMessage(
                        Component.literal("§a[AnimSync] Playing test animation locally"),
                        true
                    );
                }

                // Auto-remove after animation completes (approx 2 seconds for sword_to_left)
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            modifierLayer.setAnimation(null);
                            if (Config.logDebug) {
                                LOGGER.info("Test animation completed");
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }, 2000);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to play animation on player", e);
        }
    }
}