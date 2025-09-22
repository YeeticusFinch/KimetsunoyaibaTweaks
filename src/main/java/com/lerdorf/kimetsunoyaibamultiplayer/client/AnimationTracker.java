package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

public class AnimationTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, AnimationState> activeAnimations = new HashMap<>();
    private static int tickCounter = 0;

    private static class AnimationState {
        ResourceLocation animationId;
        int lastTick;
        boolean isActive;

        AnimationState(ResourceLocation animationId, int lastTick) {
            this.animationId = animationId;
            this.lastTick = lastTick;
            this.isActive = true;
        }
    }

    public static void tick() {
        tickCounter++;

        if (Config.logDebug && tickCounter % 40 == 0) { // Log every 2 seconds to show we're running
            LOGGER.info("AnimationTracker is running, tick: {}", tickCounter);
        }

        if (tickCounter % 2 != 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (Config.logDebug && tickCounter % 100 == 0) { // Log less frequently when no player
                LOGGER.debug("No player or level available, player: {}, level: {}", mc.player, mc.level);
            }
            return;
        }

        checkPlayerAnimation(mc.player);
    }

    private static void checkPlayerAnimation(AbstractClientPlayer player) {
        try {
            if (Config.logDebug) {
                LOGGER.debug("Checking animation for player: {}", player.getName().getString());
            }
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack == null) {
                if (Config.logDebug) {
                    LOGGER.debug("Animation stack is null for player {}", player.getName().getString());
                }
                checkForStoppedAnimation(player.getUUID());
                return;
            }

            Field layersField = AnimationStack.class.getDeclaredField("layers");
            layersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Pair<Integer, IAnimation>> layers = (List<Pair<Integer, IAnimation>>) layersField.get(animationStack);

            if (Config.logDebug) {
                LOGGER.debug("Found {} animation layers for player {}", layers.size(), player.getName().getString());
            }

            boolean foundActiveAnimation = false;

            for (int i = 0; i < layers.size(); i++) {
                Pair<Integer, IAnimation> pair = layers.get(i);
                IAnimation anim = pair.getRight();
                if (Config.logDebug) {
                    LOGGER.debug("Layer {}: animation={}, active={}, type={}",
                        i, anim, (anim != null ? anim.isActive() : "null"),
                        (anim != null ? anim.getClass().getSimpleName() : "null"));
                }

                if (anim != null && anim.isActive()) {
                    if (Config.logDebug) {
                        LOGGER.info("Found active animation on layer {}: type={}", i, anim.getClass().getSimpleName());
                    }

                    // Check if it's a ModifierLayer wrapping another animation
                    if (anim instanceof ModifierLayer) {
                        ModifierLayer<?> modLayer = (ModifierLayer<?>) anim;
                        IAnimation innerAnim = modLayer.getAnimation();
                        if (Config.logDebug) {
                            LOGGER.info("ModifierLayer contains: {}", innerAnim != null ? innerAnim.getClass().getSimpleName() : "null");
                        }

                        if (innerAnim instanceof KeyframeAnimationPlayer) {
                            KeyframeAnimationPlayer animPlayer = (KeyframeAnimationPlayer) innerAnim;
                            KeyframeAnimation data = animPlayer.getData();
                            if (data != null) {
                                foundActiveAnimation = true;
                                if (Config.logDebug) {
                                    LOGGER.info("Processing wrapped keyframe animation for player {}", player.getName().getString());
                                }
                                processActiveAnimation(player, animPlayer, data);
                                break;
                            }
                        } else if (innerAnim != null) {
                            // Try to extract KeyframeAnimationPlayer from nested structure
                            KeyframeAnimationPlayer animPlayer = extractKeyframePlayer(innerAnim);
                            if (animPlayer != null) {
                                KeyframeAnimation data = animPlayer.getData();
                                if (data != null) {
                                    foundActiveAnimation = true;
                                    if (Config.logDebug) {
                                        LOGGER.info("Processing deeply nested keyframe animation for player {}", player.getName().getString());
                                    }
                                    processActiveAnimation(player, animPlayer, data);
                                    break;
                                }
                            }
                        }
                    } else if (anim instanceof KeyframeAnimationPlayer) {
                        KeyframeAnimationPlayer animPlayer = (KeyframeAnimationPlayer) anim;
                        KeyframeAnimation data = animPlayer.getData();

                        if (data != null) {
                            foundActiveAnimation = true;
                            if (Config.logDebug) {
                                LOGGER.info("Processing direct keyframe animation for player {}", player.getName().getString());
                            }
                            processActiveAnimation(player, animPlayer, data);
                            break;
                        } else {
                            LOGGER.warn("KeyframeAnimationPlayer has null data for player {}", player.getName().getString());
                        }
                    } else {
                        if (Config.logDebug) {
                            LOGGER.info("Found active non-keyframe animation: {}", anim.getClass().getSimpleName());
                        }
                    }
                }
            }

            if (!foundActiveAnimation) {
                checkForStoppedAnimation(player.getUUID());
            }

        } catch (Exception e) {
            LOGGER.error("Error checking player animation for player {}", player.getName().getString(), e);
        }
    }

    private static void processActiveAnimation(AbstractClientPlayer player, KeyframeAnimationPlayer animPlayer, KeyframeAnimation data) {
        UUID playerUUID = player.getUUID();

        ResourceLocation animationId = null;
        UUID animUuid = data.getUuid();
        if (animUuid != null) {
            // Check if UUID is being corrupted
            if (Config.logDebug) {
                LOGGER.info("Animation UUID: {}", animUuid);
            }
            animationId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animUuid.toString());
        }

        if (animationId == null) {
            try {
                Field extraDataField = KeyframeAnimation.class.getDeclaredField("extraData");
                extraDataField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> extraData = (Map<String, Object>) extraDataField.get(data);
                if (extraData != null) {
                    Object name = extraData.get("name");
                    if (name instanceof String && !((String) name).isEmpty()) {
                        animationId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", (String) name);
                    }
                }
            } catch (Exception ex) {
                LOGGER.debug("Could not get extra data from animation: {}", ex.getMessage());
            }
        }

        if (animationId == null) {
            animationId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "unknown_" + System.currentTimeMillis());
        }

        int currentTick = animPlayer.getTick();
        int length = data.getLength();
        boolean isLooping = data.isInfinite();

        AnimationState currentState = activeAnimations.get(playerUUID);

        if (currentState == null || !currentState.animationId.equals(animationId) ||
            Math.abs(currentState.lastTick - currentTick) > 3) {

            if (Config.logDebug) {
                LOGGER.info("Sending animation sync: player={}, animation={}, tick={}, length={}, looping={}",
                    player.getName().getString(), animationId, currentTick, length, isLooping);
            }

            // Send debug chat message to local player
            if (Config.onScreenDebug) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.literal("§a[AnimSync] Detected animation: " + animationId.getPath()),
                        true
                    );
                }
            }

            AnimationSyncPacket packet = new AnimationSyncPacket(
                playerUUID, animationId, currentTick, length, isLooping, false, data
            );
            ModNetworking.sendToServer(packet);

            activeAnimations.put(playerUUID, new AnimationState(animationId, currentTick));
        } else {
            currentState.lastTick = currentTick;
        }
    }

    private static void checkForStoppedAnimation(UUID playerUUID) {
        AnimationState state = activeAnimations.get(playerUUID);
        if (state != null && state.isActive) {
            if (Config.logDebug) {
                LOGGER.info("Animation stopped for player {}", playerUUID);
            }

            // Send debug chat message to local player
            if (Config.onScreenDebug) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getUUID().equals(playerUUID)) {
                    mc.player.displayClientMessage(
                        Component.literal("§c[AnimSync] Animation stopped"),
                        true
                    );
                }
            }

            AnimationSyncPacket packet = AnimationSyncPacket.createStopPacket(playerUUID);
            ModNetworking.sendToServer(packet);

            state.isActive = false;
            activeAnimations.remove(playerUUID);
        }
    }

    private static KeyframeAnimationPlayer extractKeyframePlayer(IAnimation anim) {
        try {
            // Use reflection to find KeyframeAnimationPlayer in nested structures
            Field[] fields = anim.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(anim);
                if (value instanceof KeyframeAnimationPlayer) {
                    return (KeyframeAnimationPlayer) value;
                } else if (value instanceof IAnimation) {
                    // Recursive check
                    KeyframeAnimationPlayer nested = extractKeyframePlayer((IAnimation) value);
                    if (nested != null) return nested;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract KeyframeAnimationPlayer via reflection: {}", e.getMessage());
        }
        return null;
    }

    public static void clearTrackedAnimations() {
        activeAnimations.clear();
    }
}