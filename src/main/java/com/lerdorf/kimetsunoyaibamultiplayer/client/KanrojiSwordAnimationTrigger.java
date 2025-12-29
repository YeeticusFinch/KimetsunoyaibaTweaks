package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * Client-side helper for triggering GeckoLib animations on the Kanroji sword item.
 * This is called when player/entity animations are triggered via AnimationHelper.
 */

// Reference Video: https://youtube.com/shorts/eW8BzeoE0WI?si=osgxdeRSuooACeVV
public class KanrojiSwordAnimationTrigger {

    /**
     * Trigger a GeckoLib animation on the Kanroji sword if the entity is holding it.
     * This maps player/entity animation names to corresponding sword animations.
     *
     * @param entity The entity holding the sword
     * @param animationName The animation name being played on the entity
     */
    public static void triggerAnimation(LivingEntity entity, String animationName) {
        Log.debug("[KanrojiSwordAnimationTrigger] triggerAnimation() called for entity: {}, animation: {}",
                  entity.getName().getString(), animationName);

        ItemStack mainHandItem = entity.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(mainHandItem.getItem() instanceof NichirinSwordKanrojiAnimated sword)) {
            return;
        }

        Log.debug("[KanrojiSwordAnimationTrigger] Entity IS holding Kanroji sword!");

        triggerAnimationOnItemStack(entity, mainHandItem, animationName);
    }

    /**
     * Trigger a GeckoLib animation on a specific Kanroji sword ItemStack.
     * Now uses per-entity animation tracking instead of per-ItemStack to avoid
     * all swords sharing the same animation state.
     *
     * @param entity The entity associated with the sword
     * @param itemStack The ItemStack containing the Kanroji sword
     * @param animationName The animation name to play
     */
    public static void triggerAnimationOnItemStack(LivingEntity entity, ItemStack itemStack, String animationName) {
        if (!(itemStack.getItem() instanceof NichirinSwordKanrojiAnimated sword)) {
            return;
        }

        // Extract just the animation name (remove namespace if present)
        String cleanAnimName = animationName;
        if (animationName.contains(":")) {
            cleanAnimName = animationName.substring(animationName.indexOf(":") + 1);
        }
        if (cleanAnimName.contains("/")) {
            cleanAnimName = cleanAnimName.substring(cleanAnimName.lastIndexOf("/") + 1);
        }

        // Map entity animation names to sword animation names
        String swordAnimName = mapToSwordAnimation(cleanAnimName);

        Log.debug("[KanrojiSwordAnimationTrigger] Entity {} animation: {} -> Clean: {} -> Sword anim: {}",
                  entity.getName().getString(), animationName, cleanAnimName, swordAnimName);

        if (swordAnimName != null) {
            // Store animation in per-entity tracker
            // The animation controller reads from this tracker during rendering
            KanrojiSwordEntityAnimationTracker.setAnimation(entity.getUUID(), swordAnimName);

            Log.debug("[KanrojiSwordAnimationTrigger] Set animation {} for entity {}",
                      swordAnimName, entity.getName().getString());

            // Notify the animation handler so it doesn't override with movement animations
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                KanrojiSwordAnimationHandler.notifyActionAnimation(entity.getUUID(), swordAnimName);
            }
        } else {
            Log.debug("[KanrojiSwordAnimationTrigger] No sword animation mapping for entity anim: {} (clean: {})",
                      animationName, cleanAnimName);
        }
    }

    /**
     * Resolve the GeckoLib per-ItemStack instance ID in a way that is compatible with
     * different GeckoLib versions and both client/server sides.
     * Prefers calling GeoItem.getId(ItemStack) on client; falls back to
     * GeoItem.getOrAssignId(ItemStack, Level/ServerLevel) via reflection; and finally
     * a stable hash if no API match is found.
     */
    private static long resolveGeoItemInstanceId(ItemStack stack, LivingEntity entity) {
        try {
            // Preferred on client: GeoItem.getId(ItemStack)
            java.lang.reflect.Method m = GeoItem.class.getMethod("getId", ItemStack.class);
            Object result = m.invoke(null, stack);
            if (result instanceof Number) {
                Log.debug("[KanrojiSwordAnimationTrigger] Resolved GeoItem instanceId via GeoItem.getId(ItemStack)");
                return ((Number) result).longValue();
            }
        } catch (Throwable ignored) {}

        // Try getOrAssignId with whatever Level-type the version expects
        try {
            for (java.lang.reflect.Method m : GeoItem.class.getMethods()) {
                if (!m.getName().equals("getOrAssignId")) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 && ItemStack.class.isAssignableFrom(params[0])) {
                    // Second param can be Level or ServerLevel depending on GeckoLib version
                    if (params[1].isInstance(entity.level())) {
                        Object result = m.invoke(null, stack, entity.level());
                        if (result instanceof Number) {
                            Log.debug("[KanrojiSwordAnimationTrigger] Resolved GeoItem instanceId via GeoItem.getOrAssignId(ItemStack, {})", params[1].getSimpleName());
                            return ((Number) result).longValue();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Last resort: stable-ish fallback id to keep renderer and trigger consistent enough
        Log.debug("[KanrojiSwordAnimationTrigger] Falling back to hash-based instanceId resolution");
        return (long) System.identityHashCode(stack) ^ (long) entity.getId();
    }

    /**
     * Map entity/player animation names to corresponding sword animation names.
     * Returns null if there's no corresponding sword animation.
     */
    private static String mapToSwordAnimation(String entityAnimationName) {
        // Direct matches (when animation names are the same)
        switch (entityAnimationName) {
            case "sword_to_left":
            case "sword_to_right":
            case "speed_attack_sword":
            case "sword_overhead":
            case "kanroji_sword_overhead":
            case "sword_rotate":
            case "love_first_form":
            case "love_second_form":
            case "love_third_form":
            case "love_fourth_form":
            case "love_fifth_form":
            case "love_sixth_form":
                return entityAnimationName;

            // Walk/sprint animations
            case "walk":
            case "walking":
                return "walk";

            case "sprint":
            case "sprinting":
                return "sprint";

            // Sheath animation (when sword is on hip/back)
            case "sheath":
            case "sheathed":
                return "sheath";

            // Random transition animations
            case "random":
            case "random2":
                return entityAnimationName;

            // Idle fallback
            case "idle":
            case "standing":
                return "idle";

            // Default: try to use the animation name as-is if it exists
            default:
                // If the animation name contains any of our known animations, use it
                if (entityAnimationName.contains("sword_to_left")) return "sword_to_left";
                if (entityAnimationName.contains("sword_to_right")) return "sword_to_right";
                if (entityAnimationName.contains("speed_attack")) return "speed_attack_sword";
                if (entityAnimationName.contains("overhead")) return "sword_overhead";
                if (entityAnimationName.contains("rotate")) return "sword_rotate";
                if (entityAnimationName.contains("walk")) return "walk";
                if (entityAnimationName.contains("sprint")) return "sprint";
                if (entityAnimationName.contains("sheath")) return "sheath";

                return null; // No corresponding sword animation
        }
    }
}
