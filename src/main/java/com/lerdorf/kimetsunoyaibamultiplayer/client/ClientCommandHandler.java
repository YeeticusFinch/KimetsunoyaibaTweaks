package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                // Default: use sword_to_left
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    return executeClientTestAnimation(mc.player, "sword_to_left");
                }
                return 0;
            })
            .then(Commands.argument("animation", StringArgumentType.string())
                .executes(context -> {
                    // Use specified animation
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        String animationName = StringArgumentType.getString(context, "animation");
                        return executeClientTestAnimation(mc.player, animationName);
                    }
                    return 0;
                }));

        dispatcher.register(command);
        LOGGER.info("Registered client-side test animation command: /testanimc [animation]");
    }

    private static int executeClientTestAnimation(AbstractClientPlayer player, String animationName) {
        try {
            player.displayClientMessage(Component.literal("§a[Client Test] Attempting to play '" + animationName + "' animation..."), false);

            // Create ResourceLocation from animation name
            ResourceLocation animationLocation = parseAnimationName(animationName);

            // First, try to find the animation in the registry
            KeyframeAnimation animation = findAnimation(animationLocation);

            if (animation != null) {
                // Play the animation locally on the client player
                playAnimationOnPlayer(player, animation);

                // Also send to server for other players to see
                AnimationSyncPacket packet = new AnimationSyncPacket(
                    player.getUUID(),
                    animationLocation,
                    0,
                    30,
                    false,
                    false,
                    animation
                );
                ModNetworking.sendToServer(packet);

                player.displayClientMessage(Component.literal("§a[Client Test] Animation '" + animationName + "' started successfully!"), false);
                return 1;
            } else {
                // Fallback: just send the packet without animation data
                player.displayClientMessage(Component.literal("§e[Client Test] Animation '" + animationName + "' not found in registry, sending sync packet anyway..."), false);

                AnimationSyncPacket packet = new AnimationSyncPacket(
                    player.getUUID(),
                    animationLocation,
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

    private static ResourceLocation parseAnimationName(String animationName) {
        // If already contains namespace, parse it
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        // Otherwise, default to kimetsunoyaiba namespace
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    private static KeyframeAnimation findAnimation(ResourceLocation animationLocation) {
        // Try the exact location first
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animationLocation);
        if (anim != null) {
            if (Config.logDebug) {
                LOGGER.info("Found animation at: {}", animationLocation);
            }
            return anim;
        }

        // Try alternative namespaces
        String path = animationLocation.getPath();
        ResourceLocation[] alternativeLocations = {
            ResourceLocation.fromNamespaceAndPath("playeranimator", path),
            ResourceLocation.fromNamespaceAndPath("minecraft", path),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/" + path)
        };

        for (ResourceLocation loc : alternativeLocations) {
            anim = PlayerAnimationRegistry.getAnimation(loc);
            if (anim != null) {
                if (Config.logDebug) {
                    LOGGER.info("Found animation at alternative location: {}", loc);
                }
                return anim;
            }
        }

        if (Config.logDebug) {
            LOGGER.warn("Could not find animation '{}' in registry", animationLocation);
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