package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for playing draw/sheath animations when swords are drawn from or returned to sheaths.
 *
 * Features:
 * - Plays correct draw animation based on hand (mainhand/offhand) and sheath position
 * - Plays sheath animations in reverse (negative speed)
 * - Supports both players and entities
 * - Triggers special animations on Kanroji swords
 * - Cooldown system to prevent animation spam
 */
public class DrawSheathAnimationHelper {

    // Animation layer priority (higher than breathing forms at 3000)
    private static final int DRAW_SHEATH_LAYER = 4000;

    // Animation duration in ticks (0.25 seconds = 5 ticks to match animation files)
    private static final int DRAW_ANIM_TICKS = 5;

    // Minimum time between animations (prevents spam from rapid slot switching)
    private static final long ANIMATION_COOLDOWN_MS = 500;

    // Track last animation time per entity to enforce cooldown
    private static final Map<UUID, Long> lastAnimationTime = new ConcurrentHashMap<>();

    /**
     * Play draw animation for a player when they draw a sword from their sheath.
     *
     * @param player The player drawing the sword
     * @param slot The hotbar slot the sword is being drawn from
     * @param sheathPosition The position of the sheath (HIP or BACK)
     * @param isLeft Whether this is the left display slot
     * @param sword The sword ItemStack being drawn
     */
    public static void playDrawAnimation(Player player, int slot,
                                        SwordDisplayConfig.SwordDisplayPosition sheathPosition,
                                        boolean isLeft, ItemStack sword) {
        if (player == null || sheathPosition == null) {
            return;
        }

        // Check if animations are enabled in config
        if (!SwordDisplayConfig.drawSheathAnimations) {
            return;
        }

        // Check cooldown
        if (!canPlayAnimation(player)) {
            return;
        }

        // Determine which hand is drawing the sword
        boolean isMainHand = isMainHand(player, slot);

        // Get the appropriate animation name
        String animationName = getDrawAnimationName(isMainHand, sheathPosition, isLeft);

        if (animationName != null) {
            Log.debug("Playing draw animation for player {}: {} (slot={}, mainHand={}, position={}, left={})",
                player.getName().getString(), animationName, slot, isMainHand, sheathPosition, isLeft);

            // Play the animation with normal speed
            AnimationHelper.playAnimationOnLayer(
                player,
                animationName,
                DRAW_ANIM_TICKS,
                1.0f,  // Normal speed
                DRAW_SHEATH_LAYER
            );

            // Trigger Kanroji sword special animation if applicable
            triggerKanrojiDrawAnimation(player, sword);

            // Update cooldown
            lastAnimationTime.put(player.getUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Play sheath animation for a player when they sheath their sword.
     * This plays the draw animation in reverse (negative speed).
     *
     * @param player The player sheathing the sword
     * @param slot The hotbar slot the sword WAS in (previous slot before switch)
     * @param currentSlot The current hotbar slot AFTER the switch
     * @param sheathPosition The position of the sheath (HIP or BACK)
     * @param isLeft Whether this is the left display slot
     */
    public static void playSheathAnimation(Player player, int slot, int currentSlot,
                                          SwordDisplayConfig.SwordDisplayPosition sheathPosition,
                                          boolean isLeft) {
        if (player == null || sheathPosition == null) {
            return;
        }

        // Check if animations are enabled in config
        if (!SwordDisplayConfig.drawSheathAnimations) {
            return;
        }

        // Check cooldown
        if (!canPlayAnimation(player)) {
            return;
        }

        // Determine which hand sheathed the sword
        // The previous slot WAS the mainhand if it equals the current selected slot before the switch
        // But since we switched away FROM it, we need to check if it WAS mainhand
        // We can determine this by checking if the slot is NOT offhand (40)
        boolean isMainHand = (slot != 40);

        // Get the same animation name as draw, but we'll play it in reverse
        String animationName = getDrawAnimationName(isMainHand, sheathPosition, isLeft);

        if (animationName != null) {
            Log.debug("Playing sheath animation for player {}: {} REVERSED (previousSlot={}, currentSlot={}, mainHand={}, position={}, left={})",
                player.getName().getString(), animationName, slot, currentSlot, isMainHand, sheathPosition, isLeft);

            // Play the animation with NEGATIVE speed (reverse)
            AnimationHelper.playAnimationOnLayer(
                player,
                animationName,
                DRAW_ANIM_TICKS,
                -1.0f,  // REVERSE SPEED
                DRAW_SHEATH_LAYER
            );

            // Update cooldown
            lastAnimationTime.put(player.getUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Play draw animation for an entity when they enter combat.
     * Simpler than player version - uses default mainhand assumption.
     *
     * @param entity The entity drawing their sword
     * @param sheathPosition The position of the sheath
     * @param sword The sword ItemStack being drawn
     */
    public static void playDrawAnimation(LivingEntity entity,
                                        SwordDisplayConfig.SwordDisplayPosition sheathPosition,
                                        ItemStack sword) {
        if (entity == null || sheathPosition == null) {
            return;
        }

        // Check if animations are enabled in config
        if (!SwordDisplayConfig.drawSheathAnimations) {
            return;
        }

        // Check cooldown
        if (!canPlayAnimation(entity)) {
            return;
        }

        // Entities always use mainhand, assume left position for default
        String animationName = getDrawAnimationName(true, sheathPosition, true);

        if (animationName != null) {
            Log.debug("Playing draw animation for entity {}: {} (position={})",
                entity.getType().getDescriptionId(), animationName, sheathPosition);

            // Play the animation with normal speed
            AnimationHelper.playAnimationOnLayer(
                entity,
                animationName,
                DRAW_ANIM_TICKS,
                1.0f,  // Normal speed
                DRAW_SHEATH_LAYER
            );

            // Trigger Kanroji sword special animation if applicable
            triggerKanrojiDrawAnimation(entity, sword);

            // Update cooldown
            lastAnimationTime.put(entity.getUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Play sheath animation for an entity when they exit combat.
     * This plays the draw animation in reverse.
     *
     * @param entity The entity sheathing their sword
     * @param sheathPosition The position of the sheath
     */
    public static void playSheathAnimation(LivingEntity entity,
                                          SwordDisplayConfig.SwordDisplayPosition sheathPosition) {
        if (entity == null || sheathPosition == null) {
            return;
        }

        // Check if animations are enabled in config
        if (!SwordDisplayConfig.drawSheathAnimations) {
            return;
        }

        // Check cooldown
        if (!canPlayAnimation(entity)) {
            return;
        }

        // Entities always use mainhand, assume left position for default
        String animationName = getDrawAnimationName(true, sheathPosition, true);

        if (animationName != null) {
            Log.debug("Playing sheath animation for entity {}: {} REVERSED (position={})",
                entity.getType().getDescriptionId(), animationName, sheathPosition);

            // Play the animation with NEGATIVE speed (reverse)
            AnimationHelper.playAnimationOnLayer(
                entity,
                animationName,
                DRAW_ANIM_TICKS,
                -1.0f,  // REVERSE SPEED
                DRAW_SHEATH_LAYER
            );

            // Update cooldown
            lastAnimationTime.put(entity.getUUID(), System.currentTimeMillis());
        }
    }

    /**
     * Determine the correct animation name based on hand and sheath position.
     *
     * Animation mappings:
     * - Mainhand + Left Hip → draw_hip_left_mainhand
     * - Offhand + Left Hip → draw_hip_left_offhand
     * - Mainhand + Right Hip → draw_hip_right_mainhand
     * - Offhand + Right Hip → draw_hip_right_offhand
     * - Mainhand + Back (left or right) → draw_back_mainhand
     * - Offhand + Back (left or right) → draw_back_offhand
     *
     * @param isMainHand Whether the sword is in the main hand
     * @param position The sheath position (HIP or BACK)
     * @param isLeft Whether the sheath is on the left side
     * @return The animation name, or null if invalid combination
     */
    private static String getDrawAnimationName(boolean isMainHand,
                                               SwordDisplayConfig.SwordDisplayPosition position,
                                               boolean isLeft) {
        if (position == null) {
            return null;
        }

        String hand = isMainHand ? "mainhand" : "offhand";

        switch (position) {
            case HIP:
                // Hip positions: distinguish left vs right
                String side = isLeft ? "left" : "right";
                return "draw_hip_" + side + "_" + hand;

            case BACK:
                // Back positions: both left and right use same animation
                return "draw_back_" + hand;

            default:
                return null;
        }
    }

    /**
     * Check if a slot is the main hand slot.
     *
     * @param player The player
     * @param slot The hotbar slot number
     * @return True if this slot is currently selected (main hand)
     */
    private static boolean isMainHand(Player player, int slot) {
        // Slot 40 is the offhand
        if (slot == 40) {
            return false;
        }

        // Main hand is the currently selected hotbar slot
        return slot == player.getInventory().selected;
    }

    /**
     * Check if enough time has passed since the last animation for this entity.
     *
     * @param entity The entity to check
     * @return True if animation can be played, false if still in cooldown
     */
    private static boolean canPlayAnimation(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        Long lastTime = lastAnimationTime.get(entity.getUUID());
        if (lastTime == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastAnim = currentTime - lastTime;

        return timeSinceLastAnim >= ANIMATION_COOLDOWN_MS;
    }

    /**
     * Trigger special Kanroji sword animation when drawing.
     * Plays "random" or "random2" animation on the sword item itself.
     *
     * @param entity The entity drawing the sword
     * @param sword The sword ItemStack
     */
    private static void triggerKanrojiDrawAnimation(LivingEntity entity, ItemStack sword) {
        if (sword == null || sword.isEmpty()) {
            return;
        }

        // Check if this is a Kanroji sword
        if (!(sword.getItem() instanceof NichirinSwordKanrojiAnimated)) {
            return;
        }

        // Only trigger on client side
        if (!entity.level().isClientSide) {
            return;
        }

        // Randomly choose between "random" and "random2" animations
        String kanrojiAnim = entity.level().random.nextBoolean() ? "random" : "random2";

        Log.debug("Triggering Kanroji sword animation: {} for entity {}",
            kanrojiAnim, entity.getName().getString());

        // Trigger the animation on the sword item
        KanrojiSwordAnimationTrigger.triggerAnimationOnItemStack(entity, sword, kanrojiAnim);
    }

    /**
     * Clear all tracked animation cooldowns.
     * Should be called when disconnecting from server.
     */
    public static void clearAll() {
        lastAnimationTime.clear();
    }
}
