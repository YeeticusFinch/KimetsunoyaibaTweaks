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
        String animationName = null;

        // Try multiple approaches to extract the animation name

        // 1. Try to get name from extraData first (most reliable)
        try {
            Field extraDataField = KeyframeAnimation.class.getDeclaredField("extraData");
            extraDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> extraData = (Map<String, Object>) extraDataField.get(data);
            if (extraData != null) {
                Object name = extraData.get("name");
                if (name instanceof String && !((String) name).isEmpty()) {
                    animationName = (String) name;
                    if (Config.logDebug) {
                        LOGGER.info("Found animation name from extraData: {}", animationName);
                    }
                }
            }
        } catch (Exception ex) {
            if (Config.logDebug) {
                LOGGER.debug("Could not get extraData: {}", ex.getMessage());
            }
        }

        // 2. Try reflection to find any field containing animation name/identifier
        if (animationName == null) {
            animationName = extractAnimationNameViaReflection(data);
        }

        // 3. Try to match UUID against known animation names (fallback)
        if (animationName == null) {
            UUID animUuid = data.getUuid();
            if (animUuid != null) {
                if (Config.logDebug) {
                    LOGGER.info("Animation UUID: {}", animUuid);
                }
                animationName = mapUuidToAnimationName(animUuid);
            }
        }

        // 4. Final fallback
        if (animationName == null) {
            animationName = "unknown_" + System.currentTimeMillis();
            if (Config.logDebug) {
                LOGGER.warn("Could not determine animation name, using fallback: {}", animationName);
            }
        }

        // Create ResourceLocation with the detected/fallback name
        animationId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);

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

    private static String extractAnimationNameViaReflection(KeyframeAnimation data) {
        try {
            // Try to find fields that might contain the animation name
            Field[] fields = KeyframeAnimation.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(data);

                if (value instanceof String) {
                    String strValue = (String) value;
                    // Check if this looks like an animation name from our list
                    if (isKnownAnimationName(strValue)) {
                        if (Config.logDebug) {
                            LOGGER.info("Found animation name via reflection in field '{}': {}", field.getName(), strValue);
                        }
                        return strValue;
                    }
                } else if (value instanceof ResourceLocation) {
                    ResourceLocation resLoc = (ResourceLocation) value;
                    String path = resLoc.getPath();
                    if (isKnownAnimationName(path)) {
                        if (Config.logDebug) {
                            LOGGER.info("Found animation name via ResourceLocation in field '{}': {}", field.getName(), path);
                        }
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            if (Config.logDebug) {
                LOGGER.debug("Failed to extract animation name via reflection: {}", e.getMessage());
            }
        }
        return null;
    }

    private static boolean isKnownAnimationName(String name) {
        // List of known kimetsunoyaiba animation names
        String[] knownNames = {
            "idle", "idle_senior", "walk", "walk_senior", "swim", "sprint", "sprint_senior", "death",
            "punch_right", "punch_left", "kick_right", "kick_left", "kick_rotate1", "kick_rotate2",
            "kick_rotate3", "kick_rotate4", "kick_rotate5", "kick_flying", "punch_right_jinbe",
            "sword_to_right", "sword_to_left", "sword_rotate", "punch_overhead", "sword_overhead",
            "backstep", "guard", "right_arm_front", "right_arm_front2", "right_arm_up", "both_arm_up",
            "both_arm_ground", "both_arm_front", "right_leg_front", "yasakani_no_magatama", "negative",
            "cancel", "fall1", "invisibility", "enkai", "amane_dachi", "punch_gomu_pistol1",
            "punch_gomu_pistol2", "punch_gomu_gatling1", "punch_gomu_gatling2", "gear_2", "energy_charge",
            "speed_attack1", "speed_attack_punch", "speed_attack_sword", "hakoku_right1", "hakoku_right2",
            "hakoku_left1", "hakoku_left2", "headbutt_1", "headbutt_2", "ul_zugan", "rin",
            "raimei_hakke1", "raimei_hakke2", "ragnaraku1", "ragnaraku2", "ragnaraku3", "ragnaraku4",
            "iai1", "rokuogan", "fly_front", "fly_back", "fly_front2", "fly_back2", "fly_front3",
            "yamiyami_blackhole", "clap", "kaishin1", "kaishin2", "kaishin3", "kaishin4",
            "kamusari1", "kamusari2", "kamusari3", "togen_totsuka1", "togen_totsuka2", "breath1",
            "breath2", "rashin", "kick_akaza1", "kick_akaza2", "sword_to_right_reverse",
            "sword_to_left_reverse", "breath_sun2_1", "breath_sun2_2", "breath_sound5", "breath_sound5_p",
            "breath_beast1", "breath_beast2", "sword_to_upper"
        };

        for (String known : knownNames) {
            if (known.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String mapUuidToAnimationName(UUID uuid) {
        // This is a fallback - in practice, we'd need to build a mapping of UUIDs to names
        // For now, we'll just use this for logging and return null
        if (Config.logDebug) {
            LOGGER.info("No UUID-to-name mapping available for UUID: {}", uuid);
        }
        return null;
    }

    public static void clearTrackedAnimations() {
        activeAnimations.clear();
    }
}