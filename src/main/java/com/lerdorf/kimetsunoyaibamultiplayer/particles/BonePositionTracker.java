package com.lerdorf.kimetsunoyaibamultiplayer.particles;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class BonePositionTracker {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Calculates the world position of the sword tip for a given entity based on animation
     * @param entity The entity to calculate sword tip position for
     * @param animationName The name of the animation being performed
     * @return The world position of the sword tip, or null if it cannot be calculated
     */
    @Nullable
    public static Vec3 getSwordTipPosition(LivingEntity entity, String animationName) {
        return getSwordTipPosition(entity, animationName, -1);
    }

    /**
     * Calculates the world position of the sword tip for a given entity based on animation with tick info
     * @param entity The entity to calculate sword tip position for
     * @param animationName The name of the animation being performed
     * @param animationTick The current animation tick (-1 if unknown)
     * @return The world position of the sword tip, or null if it cannot be calculated
     */
    @Nullable
    public static Vec3 getSwordTipPosition(LivingEntity entity, String animationName, int animationTick) {
        if (entity == null) {
            return null;
        }

        // Use animation progress to determine position along swing arc
        float animationProgress = getAnimationProgress(entity, animationTick);

        // Use hardcoded paths based on animation name for better accuracy
        Vec3 swordTipOffset = getSwordTipOffsetForAnimation(entity, animationName, animationProgress);
        if (swordTipOffset == null) {
            return null;
        }

        // Get entity position and add the offset
        Vec3 entityPos = entity.position();
        return entityPos.add(swordTipOffset);
    }

    /**
     * Gets the current animation progress (0.0 to 1.0) for varying particle positions
     * @param entity The entity performing the animation
     * @param animationTick The current animation tick (-1 if unknown)
     * @return A value between 0.0 and 1.0 representing animation progress
     */
    private static float getAnimationProgress(LivingEntity entity, int animationTick) {
        if (animationTick >= 0) {
            // Use actual animation tick for more accurate progress
            // Make sword swings much faster - complete in 4 ticks (almost instant)
            return Math.min(1.0f, animationTick / 4.0f);
        } else {
            // Fallback: Use world time for a very fast animation cycle
            long worldTime = entity.level().getGameTime();
            // Create a 4-tick animation cycle for very fast sword swings
            return (worldTime % 4) / 4.0f;
        }
    }

    /**
     * Gets the sword tip offset based on the specific animation being performed
     * @param entity The entity performing the animation
     * @param animationName The name of the animation
     * @param progress The animation progress (0.0 to 1.0)
     * @return The offset from entity position to sword tip
     */
    @Nullable
    private static Vec3 getSwordTipOffsetForAnimation(LivingEntity entity, String animationName, float progress) {
        if (animationName == null) {
            return null;
        }

        float yaw = entity.getYRot();
        double entityHeight = entity.getBbHeight();

        // Convert yaw to radians for calculations
        double yawRad = Math.toRadians(yaw);

        // Define sword tip positions based on animation using circular arcs
        double tipX, tipY, tipZ;

        switch (animationName) {
            case "sword_to_right":
                // Circular arc from left to right, slightly upward
                // Arc starts at player's left side, sweeps to right side with upward motion
                double rightArcAngle = Math.PI * progress; // 0 to π (180°)
                double rightRadius = 1.6; // Arc radius (moved further from player)

                // Center the arc further in front of player at chest height
                double rightCenterX = 1.6 * Math.cos(yawRad);
                double rightCenterY = entityHeight * 0.75;
                double rightCenterZ = 1.6 * Math.sin(yawRad);

                // Calculate position on arc (starts from left, goes right)
                // FIXED: Reversed the direction to match sword motion
                double rightLocalX = rightRadius * Math.cos(rightArcAngle); // Start left (-), end right (+)
                double rightLocalZ = rightRadius * Math.sin(rightArcAngle) * 0.3; // Less forward motion
                double rightLocalY = 0.3 * Math.sin(rightArcAngle); // Slight upward arc

                // Rotate to match player's facing direction
                tipX = rightCenterX + (rightLocalX * Math.cos(yawRad) - rightLocalZ * Math.sin(yawRad));
                tipY = rightCenterY + rightLocalY;
                tipZ = rightCenterZ + (rightLocalX * Math.sin(yawRad) + rightLocalZ * Math.cos(yawRad));
                break;

            case "sword_to_left":
                // Circular arc from right to left, slightly downward
                // Arc starts at player's right side, sweeps to left side with downward motion
                double leftArcAngle = Math.PI * progress; // 0 to π (180°)
                double leftRadius = 1.6; // Arc radius (moved further from player)

                // Center the arc further in front of player at chest height
                double leftCenterX = 1.6 * Math.cos(yawRad);
                double leftCenterY = entityHeight * 0.75;
                double leftCenterZ = 1.6 * Math.sin(yawRad);

                // Calculate position on arc (starts from right, goes left)
                // FIXED: Reversed the direction to match sword motion
                double leftLocalX = -leftRadius * Math.cos(leftArcAngle); // Start right (+), end left (-)
                double leftLocalZ = leftRadius * Math.sin(leftArcAngle) * 0.3; // Less forward motion
                double leftLocalY = -0.2 * Math.sin(leftArcAngle); // Slight downward arc

                // Rotate to match player's facing direction
                tipX = leftCenterX + (leftLocalX * Math.cos(yawRad) - leftLocalZ * Math.sin(yawRad));
                tipY = leftCenterY + leftLocalY;
                tipZ = leftCenterZ + (leftLocalX * Math.sin(yawRad) + leftLocalZ * Math.cos(yawRad));
                break;

            case "sword_rotate":
                // Spinning attack - full 360° rotation
                double rotateAngle = yawRad + (progress * 2.0 * Math.PI);
                double rotateRadius = 1.5;

                tipX = rotateRadius * Math.cos(rotateAngle);
                tipY = entityHeight * 0.8 + 0.2;
                tipZ = rotateRadius * Math.sin(rotateAngle);
                break;

            case "sword_overhead":
                // Overhead attack - arcs from above and forward
                double overheadProgress = progress * Math.PI; // 0 to π
                double cosYaw = Math.cos(yawRad);
                double sinYaw = Math.sin(yawRad);

                double overheadCenterX = 0.2 * cosYaw;
                double overheadCenterY = entityHeight * 1.0;
                double overheadCenterZ = 0.2 * sinYaw;

                tipX = overheadCenterX + (1.0 * cosYaw + 0.5 * Math.sin(overheadProgress) * cosYaw);
                tipY = overheadCenterY + 0.3 - 0.4 * Math.cos(overheadProgress);
                tipZ = overheadCenterZ + (1.0 * sinYaw + 0.5 * Math.sin(overheadProgress) * sinYaw);
                break;

            default:
                // Generic sword animation - simple forward arc
                if (animationName.contains("sword") || animationName.contains("breath")) {
                    double genericAngle = Math.PI * 0.4 * (progress - 0.5); // -36° to +36°
                    double genericCenterX = 0.3 * Math.cos(yawRad);
                    double genericCenterY = entityHeight * 0.8;
                    double genericCenterZ = 0.3 * Math.sin(yawRad);

                    tipX = genericCenterX + (1.0 * Math.cos(yawRad + genericAngle));
                    tipY = genericCenterY + 0.1 + 0.1 * Math.sin(progress * Math.PI);
                    tipZ = genericCenterZ + (1.0 * Math.sin(yawRad + genericAngle));
                } else {
                    return null; // Not a sword animation
                }
                break;
        }

        return new Vec3(tipX, tipY, tipZ);
    }

    /**
     * Gets the itemMainHand bone position from the animation system
     * @param entity The entity to get bone position for
     * @return The bone position offset, or null if not available
     */
    @Nullable
    private static Vec3 getItemMainHandBonePosition(LivingEntity entity) {
        try {
            if (!(entity instanceof AbstractClientPlayer)) {
                return null;
            }

            AbstractClientPlayer player = (AbstractClientPlayer) entity;
            AnimationStack animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (animationStack == null) {
                return null;
            }

            // Look for active animations that might provide bone transformation data
            Field layersField = AnimationStack.class.getDeclaredField("layers");
            layersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Pair<Integer, IAnimation>> layers = (List<Pair<Integer, IAnimation>>) layersField.get(animationStack);

            for (Pair<Integer, IAnimation> pair : layers) {
                IAnimation anim = pair.getRight();
                if (anim != null && anim.isActive()) {
                    Vec3 bonePos = extractBonePositionFromAnimation(anim);
                    if (bonePos != null) {
                        return bonePos;
                    }
                }
            }

        } catch (Exception e) {
            if (Config.logDebug) {
                LOGGER.debug("Failed to get bone position from animation system: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Attempts to extract bone position data from an active animation
     * @param animation The animation to extract bone data from
     * @return The bone position, or null if not available
     */
    @Nullable
    private static Vec3 extractBonePositionFromAnimation(IAnimation animation) {
        // Try to get bone data from KeyframeAnimationPlayer
        KeyframeAnimationPlayer animPlayer = null;

        if (animation instanceof KeyframeAnimationPlayer) {
            animPlayer = (KeyframeAnimationPlayer) animation;
        } else if (animation instanceof ModifierLayer) {
            ModifierLayer<?> modLayer = (ModifierLayer<?>) animation;
            IAnimation innerAnim = modLayer.getAnimation();
            if (innerAnim instanceof KeyframeAnimationPlayer) {
                animPlayer = (KeyframeAnimationPlayer) innerAnim;
            }
        }

        if (animPlayer != null) {
            return extractBonePositionFromKeyframes(animPlayer);
        }

        return null;
    }

    /**
     * Extracts bone position from keyframe animation data
     * @param animPlayer The keyframe animation player
     * @return The bone position, or null if not available
     */
    @Nullable
    private static Vec3 extractBonePositionFromKeyframes(KeyframeAnimationPlayer animPlayer) {
        try {
            KeyframeAnimation data = animPlayer.getData();
            if (data == null) {
                return null;
            }

            // For now, we'll use a simplified approach since accessing bone-specific
            // transform data requires deeper integration with the animation system
            // In the future, this could be enhanced to read actual bone transforms

            if (Config.logDebug) {
                LOGGER.debug("Found keyframe animation, using estimated bone position");
            }

            // Return a basic offset that represents the hand position
            // This will be enhanced when we have access to actual bone transform data
            return new Vec3(0.3, -0.2, 0.4); // Approximate hand position relative to entity center

        } catch (Exception e) {
            if (Config.logDebug) {
                LOGGER.debug("Failed to extract bone position from keyframes: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Estimates the main hand position based on entity orientation when bone data is not available
     * @param entity The entity to estimate hand position for
     * @return Estimated hand position offset
     */
    private static Vec3 estimateMainHandPosition(LivingEntity entity) {
        // Basic hand position estimation based on entity type and orientation
        double handX = 0.3; // Slightly to the right (main hand)
        double handY = entity.getBbHeight() * 0.7; // About chest height
        double handZ = 0.2; // Slightly forward

        // Adjust based on entity rotation
        float yawRad = (float) Math.toRadians(entity.getYRot());

        double rotatedX = handX * Math.cos(yawRad) - handZ * Math.sin(yawRad);
        double rotatedZ = handX * Math.sin(yawRad) + handZ * Math.cos(yawRad);

        return new Vec3(rotatedX, handY, rotatedZ);
    }

    /**
     * Calculates the sword tip position based on hand position and entity orientation
     * @param entity The entity holding the sword
     * @param handPosition The position of the hand relative to entity center
     * @return The sword tip position offset
     */
    private static Vec3 calculateSwordTipOffset(LivingEntity entity, Vec3 handPosition) {
        // Estimate sword length (about 1 block)
        double swordLength = 1.0;

        // Get entity's looking direction for sword orientation
        Vec3 lookDirection = entity.getLookAngle();

        // Calculate sword tip position (extend from hand in looking direction)
        Vec3 swordDirection = lookDirection.multiply(swordLength, swordLength * 0.3, swordLength);

        return handPosition.add(swordDirection);
    }

    /**
     * Legacy method for backward compatibility
     * @param entity The entity to calculate sword tip position for
     * @return The world position of the sword tip, or null if it cannot be calculated
     */
    @Nullable
    public static Vec3 getSwordTipPosition(LivingEntity entity) {
        return getSwordTipPosition(entity, "sword_to_right"); // Default to basic attack
    }

    /**
     * Validates that a calculated position is reasonable for particle spawning
     * @param entityPos The entity's position
     * @param calculatedPos The calculated sword tip position
     * @return true if the position is valid for spawning particles
     */
    public static boolean isValidParticlePosition(Vec3 entityPos, Vec3 calculatedPos) {
        if (entityPos == null || calculatedPos == null) {
            return false;
        }

        // Check that the sword tip isn't too far from the entity
        double distance = entityPos.distanceTo(calculatedPos);
        return distance <= 3.0; // Maximum 3 blocks from entity
    }

    /**
     * Gets a debug string describing the bone position calculation
     * @param entity The entity
     * @return Debug information string
     */
    public static String getDebugInfo(LivingEntity entity) {
        Vec3 swordTip = getSwordTipPosition(entity);
        if (swordTip == null) {
            return "Sword tip position: null";
        }

        return String.format("Sword tip position: (%.2f, %.2f, %.2f)", swordTip.x, swordTip.y, swordTip.z);
    }
}