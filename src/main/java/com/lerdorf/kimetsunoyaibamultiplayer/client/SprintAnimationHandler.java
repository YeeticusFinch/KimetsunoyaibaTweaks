package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BaseModBreathingExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles sprint animation for players holding nichirin swords
 */
public class SprintAnimationHandler {
    private static final int SPRINT_LAYER_PRIORITY = 200;
    private static final long TRAINING_SWORD_REFRESH_TICKS = 26L;
    private static final long NICHIRIN_REFRESH_TICKS = 4L;
    private static final Map<UUID, ModifierLayer<IAnimation>> activeSprintLayers = new HashMap<>();
    private static final Map<UUID, Long> lastSprintApplyTick = new HashMap<>();
    private static KeyframeAnimation cachedSprintAnimation = null;
    private static KeyframeAnimation cachedSprintNoobAnimation = null;
    private static int cachedSprintDurationMs = 2000; // Default 2 seconds, will be updated from actual animation
    private static int cachedSprintNoobDurationMs = 2000;

    /**
     * Called every client tick to check if player is sprinting with a nichirin sword
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Check if sprint animation is enabled in config
        if (!com.lerdorf.kimetsunoyaibamultiplayer.Config.enableNichirinSprintAnimation) {
            // Remove any active sprint animations if config is disabled
            if (activeSprintLayers.containsKey(mc.player.getUUID())) {
                removeSprintAnimation(mc.player);
                lastSprintApplyTick.remove(mc.player.getUUID());
            }
            return;
        }

        checkPlayerSprint(mc.player);
    }

    private static void checkPlayerSprint(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        boolean isSprinting = player.isSprinting();

        // Check if player is holding a nichirin sword (custom or base mod)
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isTrainingSword = TrainingSwordHelper.isTrainingSword(mainHandItem);
        boolean holdingNichirinSword = mainHandItem.getItem() instanceof BreathingSwordItem ||
                                      (SwordRegistry.getSword(mainHandItem.getItem()) != null) ||
                                      BaseModBreathingExecutor.isBaseModNichirinSword(mainHandItem.getItem());

        // Only process if holding a nichirin sword
        if (!holdingNichirinSword || !isSprinting) {
            // Remove sprint animation if not holding sword or not sprinting
            if (activeSprintLayers.containsKey(playerUUID)) {
                removeSprintAnimation(player);
            }
            lastSprintApplyTick.remove(playerUUID);
            return;
        }

        // Player is sprinting with a nichirin sword - manage the looping animation
        long currentTick = player.level().getGameTime();
        long refreshIntervalTicks = isTrainingSword ? TRAINING_SWORD_REFRESH_TICKS : NICHIRIN_REFRESH_TICKS;
        Long lastApply = lastSprintApplyTick.get(playerUUID);

        if (lastApply == null) {
            // Start the sprint animation
            applySprintAnimation(player, isTrainingSword);
            lastSprintApplyTick.put(playerUUID, currentTick);
        } else {
            if ((currentTick - lastApply) >= refreshIntervalTicks) {
                // Force refresh the sprint animation to prevent walk from overriding
                applySprintAnimation(player, isTrainingSword);
                lastSprintApplyTick.put(playerUUID, currentTick);
            }
            // Otherwise animation is still playing - do nothing
        }
    }

    private static void applySprintAnimation(AbstractClientPlayer player, boolean useNoobSprint) {
        // Remove existing sprint layer if any
        removeSprintAnimation(player);

        AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack == null) {
            return;
        }

        KeyframeAnimation sprintAnim = useNoobSprint ? getSprintNoobAnimation() : getSprintAnimation();
        if (sprintAnim == null) {
            return;
        }

        // Create a new layer for sprint animation
        ModifierLayer<IAnimation> sprintLayer = new ModifierLayer<>();
        // Play once - we'll manually loop by re-triggering in tick()
        KeyframeAnimationPlayer sprintPlayer = new KeyframeAnimationPlayer(sprintAnim);
        sprintLayer.setAnimation(sprintPlayer);

        // Add the layer to the stack at the specified priority
        animationStack.addAnimLayer(SPRINT_LAYER_PRIORITY, sprintLayer);

        // Track the active layer
        activeSprintLayers.put(player.getUUID(), sprintLayer);
    }

    private static KeyframeAnimation getSprintAnimation() {
        if (cachedSprintAnimation != null) {
            return cachedSprintAnimation;
        }

        cachedSprintAnimation = PlayerAnimationRegistry.getAnimation(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint2"));
        if (cachedSprintAnimation == null) {
            cachedSprintAnimation = PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint"));
        }

        if (cachedSprintAnimation == null) {
            System.err.println("[SprintAnimationHandler] Could not find sprint animation (tried sprint2 and sprint)");
            return null;
        }

        int lengthTicks = cachedSprintAnimation.getLength();
        cachedSprintDurationMs = lengthTicks * 50;
        com.lerdorf.kimetsunoyaibamultiplayer.Log.info(
            "[SprintAnimationHandler] Sprint animation duration: {} ticks = {} ms",
            lengthTicks, cachedSprintDurationMs);
        return cachedSprintAnimation;
    }

    private static KeyframeAnimation getSprintNoobAnimation() {
        if (cachedSprintNoobAnimation != null) {
            return cachedSprintNoobAnimation;
        }

        // Prefer this mod's noob sprint, then compatibility fallbacks.
        cachedSprintNoobAnimation = PlayerAnimationRegistry.getAnimation(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "sprint_noob"));
        if (cachedSprintNoobAnimation == null) {
            cachedSprintNoobAnimation = PlayerAnimationRegistry.getAnimation(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint_noob"));
        }
        if (cachedSprintNoobAnimation == null) {
            cachedSprintNoobAnimation = PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sprint_noob2"));
        }
        if (cachedSprintNoobAnimation == null) {
            cachedSprintNoobAnimation = getSprintAnimation();
        }

        if (cachedSprintNoobAnimation == null) {
            System.err.println("[SprintAnimationHandler] Could not find sprint_noob animation; no fallback available");
            return null;
        }

        int lengthTicks = cachedSprintNoobAnimation.getLength();
        cachedSprintNoobDurationMs = lengthTicks * 50;
        com.lerdorf.kimetsunoyaibamultiplayer.Log.info(
            "[SprintAnimationHandler] Sprint noob animation duration: {} ticks = {} ms",
            lengthTicks, cachedSprintNoobDurationMs);
        return cachedSprintNoobAnimation;
    }

    private static void removeSprintAnimation(AbstractClientPlayer player) {
        UUID playerUUID = player.getUUID();
        ModifierLayer<IAnimation> sprintLayer = activeSprintLayers.remove(playerUUID);

        if (sprintLayer != null) {
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack != null) {
                // Remove by setting animation to null
                sprintLayer.setAnimation(null);
            }
        }
    }

    /**
     * Clean up when a player disconnects
     */
    public static void cleanup(UUID playerUUID) {
        activeSprintLayers.remove(playerUUID);
        lastSprintApplyTick.remove(playerUUID);
    }
}
