package com.lerdorf.kimetsunoyaibamultiplayer.client.animations;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyframe animation definitions for Mitsuri's whip sword.
 *
 * Each animation is a sequence of WhipPose keyframes that define:
 * - Control points for Catmull-Rom curve interpolation (c1, c2, c3)
 * - Tip position
 * - Physics influence factor (0.0 = pure keyframe, 1.0 = full physics)
 *
 * Animations are interpolated smoothly between keyframes using Catmull-Rom splines.
 */
public class MitsuriWhipAnimations {

    /**
     * Represents a single keyframe pose for the whip.
     *
     * @param c1 First control point (relative to player hand position)
     * @param c2 Second control point
     * @param c3 Third control point
     * @param tip Final tip position
     * @param physicsFactor How much physics influences this pose (0.0-1.0)
     * @param durationTicks How long to hold/transition to this pose
     */
    public record WhipPose(Vec3 c1, Vec3 c2, Vec3 c3, Vec3 tip, double physicsFactor, int durationTicks) {
        public WhipPose(Vec3 c1, Vec3 c2, Vec3 c3, Vec3 tip, double physicsFactor) {
            this(c1, c2, c3, tip, physicsFactor, 5); // Default 5 ticks per keyframe
        }
    }

    /**
     * Animation definition with metadata.
     */
    public static class WhipAnimation {
        public final String name;
        public final List<WhipPose> keyframes;
        public final boolean loops;
        public final int totalTicks;

        public WhipAnimation(String name, List<WhipPose> keyframes, boolean loops) {
            this.name = name;
            this.keyframes = keyframes;
            this.loops = loops;
            this.totalTicks = keyframes.stream().mapToInt(WhipPose::durationTicks).sum();
        }
    }

    private static final Map<String, WhipAnimation> ANIMATIONS = new HashMap<>();

    static {
        // IDLE - Gentle sway
        ANIMATIONS.put("idle", new WhipAnimation("idle", List.of(
            new WhipPose(
                new Vec3(0.2, -0.1, 0.3),
                new Vec3(0.3, -0.2, 0.8),
                new Vec3(0.2, -0.4, 1.3),
                new Vec3(0.1, -0.6, 1.8),
                0.9, 20  // High physics, slow sway
            ),
            new WhipPose(
                new Vec3(-0.2, -0.1, 0.3),
                new Vec3(-0.3, -0.2, 0.8),
                new Vec3(-0.2, -0.4, 1.3),
                new Vec3(-0.1, -0.6, 1.8),
                0.9, 20
            )
        ), true));

        // WALK - Flowing S-curve behind player
        ANIMATIONS.put("walk", new WhipAnimation("walk", List.of(
            new WhipPose(
                new Vec3(0.3, -0.2, 0.2),
                new Vec3(0.4, -0.3, 0.6),
                new Vec3(0.2, -0.5, 1.1),
                new Vec3(-0.1, -0.7, 1.6),
                0.8, 6
            ),
            new WhipPose(
                new Vec3(-0.3, -0.2, 0.2),
                new Vec3(-0.4, -0.3, 0.6),
                new Vec3(-0.2, -0.5, 1.1),
                new Vec3(0.1, -0.7, 1.6),
                0.8, 6
            )
        ), true));

        // SPRINT - Stretched out trailing
        ANIMATIONS.put("sprint", new WhipAnimation("sprint", List.of(
            new WhipPose(
                new Vec3(0.5, 0.0, 0.5),
                new Vec3(0.4, -0.2, 1.2),
                new Vec3(0.2, -0.4, 2.0),
                new Vec3(0.0, -0.6, 2.8),
                0.7, 4
            ),
            new WhipPose(
                new Vec3(-0.5, 0.0, 0.5),
                new Vec3(-0.4, -0.2, 1.2),
                new Vec3(-0.2, -0.4, 2.0),
                new Vec3(0.0, -0.6, 2.8),
                0.7, 4
            )
        ), true));

        // SWORD_TO_LEFT - Wide horizontal arc to the left
        ANIMATIONS.put("sword_to_left", new WhipAnimation("sword_to_left", List.of(
            // Wind up (right)
            new WhipPose(
                new Vec3(0.5, 0.2, 0.0),
                new Vec3(1.0, 0.1, -0.3),
                new Vec3(1.5, 0.0, -0.5),
                new Vec3(2.0, -0.1, -0.7),
                0.4, 3
            ),
            // Peak extension (forward-right)
            new WhipPose(
                new Vec3(1.5, 0.3, 0.5),
                new Vec3(3.0, 0.2, 0.3),
                new Vec3(4.5, 0.1, 0.0),
                new Vec3(6.0, 0.0, -0.3),
                0.3, 2
            ),
            // Swing through (forward)
            new WhipPose(
                new Vec3(0.0, 0.2, 1.5),
                new Vec3(-0.5, 0.1, 3.0),
                new Vec3(-1.0, 0.0, 4.5),
                new Vec3(-1.5, -0.1, 6.0),
                0.3, 3
            ),
            // Follow through (left)
            new WhipPose(
                new Vec3(-2.0, 0.0, 0.5),
                new Vec3(-3.5, -0.1, 0.3),
                new Vec3(-5.0, -0.2, 0.0),
                new Vec3(-6.5, -0.3, -0.3),
                0.5, 4
            ),
            // Recovery
            new WhipPose(
                new Vec3(-1.0, -0.2, 0.2),
                new Vec3(-1.5, -0.3, 0.5),
                new Vec3(-1.2, -0.5, 0.9),
                new Vec3(-0.5, -0.7, 1.3),
                0.8, 6
            )
        ), false));

        // SWORD_TO_RIGHT - Wide horizontal arc to the right
        ANIMATIONS.put("sword_to_right", new WhipAnimation("sword_to_right", List.of(
            // Wind up (left)
            new WhipPose(
                new Vec3(-0.5, 0.2, 0.0),
                new Vec3(-1.0, 0.1, -0.3),
                new Vec3(-1.5, 0.0, -0.5),
                new Vec3(-2.0, -0.1, -0.7),
                0.4, 3
            ),
            // Peak extension (forward-left)
            new WhipPose(
                new Vec3(-1.5, 0.3, 0.5),
                new Vec3(-3.0, 0.2, 0.3),
                new Vec3(-4.5, 0.1, 0.0),
                new Vec3(-6.0, 0.0, -0.3),
                0.3, 2
            ),
            // Swing through (forward)
            new WhipPose(
                new Vec3(0.0, 0.2, 1.5),
                new Vec3(0.5, 0.1, 3.0),
                new Vec3(1.0, 0.0, 4.5),
                new Vec3(1.5, -0.1, 6.0),
                0.3, 3
            ),
            // Follow through (right)
            new WhipPose(
                new Vec3(2.0, 0.0, 0.5),
                new Vec3(3.5, -0.1, 0.3),
                new Vec3(5.0, -0.2, 0.0),
                new Vec3(6.5, -0.3, -0.3),
                0.5, 4
            ),
            // Recovery
            new WhipPose(
                new Vec3(1.0, -0.2, 0.2),
                new Vec3(1.5, -0.3, 0.5),
                new Vec3(1.2, -0.5, 0.9),
                new Vec3(0.5, -0.7, 1.3),
                0.8, 6
            )
        ), false));

        // SWORD_OVERHEAD - Vertical overhead slash
        ANIMATIONS.put("sword_overhead", new WhipAnimation("sword_overhead", List.of(
            // Wind up (back and high)
            new WhipPose(
                new Vec3(0.0, 0.5, -0.5),
                new Vec3(0.0, 1.0, -1.0),
                new Vec3(0.0, 1.5, -1.5),
                new Vec3(0.0, 2.0, -2.0),
                0.3, 4
            ),
            // Peak (overhead)
            new WhipPose(
                new Vec3(0.0, 1.5, 0.5),
                new Vec3(0.0, 2.5, 1.0),
                new Vec3(0.0, 3.0, 2.0),
                new Vec3(0.0, 3.5, 3.0),
                0.2, 2
            ),
            // Slam down (forward)
            new WhipPose(
                new Vec3(0.0, 1.0, 2.0),
                new Vec3(0.0, 0.5, 3.5),
                new Vec3(0.0, 0.0, 5.0),
                new Vec3(0.0, -0.2, 6.5),
                0.3, 3
            ),
            // Impact
            new WhipPose(
                new Vec3(0.0, 0.2, 2.5),
                new Vec3(0.0, -0.1, 4.0),
                new Vec3(0.0, -0.3, 5.5),
                new Vec3(0.0, -0.5, 7.0),
                0.4, 3
            ),
            // Recovery
            new WhipPose(
                new Vec3(0.1, -0.1, 0.4),
                new Vec3(0.2, -0.3, 0.9),
                new Vec3(0.1, -0.5, 1.4),
                new Vec3(0.0, -0.7, 1.9),
                0.8, 8
            )
        ), false));

        // LOVE_FORM_1 - Shivers of First Love (multi-directional)
        ANIMATIONS.put("love_form_1", new WhipAnimation("love_form_1", List.of(
            // Rapid right
            new WhipPose(
                new Vec3(1.5, 0.3, 0.5),
                new Vec3(3.0, 0.2, 0.0),
                new Vec3(4.5, 0.1, -0.5),
                new Vec3(6.0, 0.0, -1.0),
                0.2, 2
            ),
            // Rapid left
            new WhipPose(
                new Vec3(-1.5, 0.3, 0.5),
                new Vec3(-3.0, 0.2, 0.0),
                new Vec3(-4.5, 0.1, -0.5),
                new Vec3(-6.0, 0.0, -1.0),
                0.2, 2
            ),
            // Rapid forward
            new WhipPose(
                new Vec3(0.0, 0.5, 1.5),
                new Vec3(0.0, 0.3, 3.5),
                new Vec3(0.0, 0.1, 5.5),
                new Vec3(0.0, 0.0, 7.5),
                0.2, 2
            ),
            // Spiral finish
            new WhipPose(
                new Vec3(2.0, 0.3, 1.0),
                new Vec3(1.0, 0.1, 3.0),
                new Vec3(-1.0, -0.1, 5.0),
                new Vec3(-2.0, -0.3, 7.0),
                0.4, 6
            )
        ), false));

        // LOVE_FORM_2 - Love Pangs (rapid extension strikes)
        ANIMATIONS.put("love_form_2", new WhipAnimation("love_form_2", List.of(
            // Coil
            new WhipPose(
                new Vec3(0.3, 0.1, 0.2),
                new Vec3(0.4, 0.0, 0.5),
                new Vec3(0.3, -0.1, 0.8),
                new Vec3(0.2, -0.2, 1.1),
                0.5, 3
            ),
            // Strike 1
            new WhipPose(
                new Vec3(1.0, 0.4, 1.0),
                new Vec3(2.0, 0.3, 2.5),
                new Vec3(3.0, 0.2, 4.0),
                new Vec3(4.0, 0.1, 5.5),
                0.2, 2
            ),
            // Retract
            new WhipPose(
                new Vec3(0.5, 0.1, 0.5),
                new Vec3(0.6, 0.0, 1.0),
                new Vec3(0.4, -0.1, 1.5),
                new Vec3(0.2, -0.2, 2.0),
                0.5, 2
            ),
            // Strike 2 (different angle)
            new WhipPose(
                new Vec3(-1.0, 0.5, 0.8),
                new Vec3(-2.0, 0.4, 2.3),
                new Vec3(-3.0, 0.3, 3.8),
                new Vec3(-4.0, 0.2, 5.3),
                0.2, 2
            ),
            // Retract
            new WhipPose(
                new Vec3(-0.3, 0.0, 0.4),
                new Vec3(-0.4, -0.1, 0.9),
                new Vec3(-0.2, -0.3, 1.4),
                new Vec3(0.0, -0.5, 1.9),
                0.6, 4
            )
        ), false));

        // LOVE_FORM_3 - Catlove Shower (overhead rain)
        ANIMATIONS.put("love_form_3", new WhipAnimation("love_form_3", List.of(
            // High wind-up
            new WhipPose(
                new Vec3(-0.5, 1.0, -0.5),
                new Vec3(-0.3, 2.0, -1.0),
                new Vec3(0.0, 2.5, -1.5),
                new Vec3(0.3, 3.0, -2.0),
                0.3, 4
            ),
            // Arc over (high peak)
            new WhipPose(
                new Vec3(0.0, 2.0, 1.0),
                new Vec3(0.0, 3.0, 2.0),
                new Vec3(0.0, 3.5, 3.5),
                new Vec3(0.0, 4.0, 5.0),
                0.2, 3
            ),
            // Rain down (wave pattern)
            new WhipPose(
                new Vec3(1.0, 1.5, 2.5),
                new Vec3(0.5, 0.8, 4.0),
                new Vec3(-0.5, 0.3, 5.5),
                new Vec3(-1.0, -0.1, 7.0),
                0.3, 3
            ),
            new WhipPose(
                new Vec3(-1.5, 0.5, 3.0),
                new Vec3(-1.0, 0.0, 4.5),
                new Vec3(0.0, -0.3, 6.0),
                new Vec3(1.0, -0.5, 7.5),
                0.3, 3
            ),
            // Recovery
            new WhipPose(
                new Vec3(0.2, -0.2, 0.5),
                new Vec3(0.3, -0.4, 1.0),
                new Vec3(0.1, -0.6, 1.5),
                new Vec3(-0.1, -0.8, 2.0),
                0.7, 6
            )
        ), false));

        // LOVE_FORM_5 - Swaying Love, Wildclaw (wide sweeping)
        ANIMATIONS.put("love_form_5", new WhipAnimation("love_form_5", List.of(
            // Start right
            new WhipPose(
                new Vec3(2.0, 0.5, -0.5),
                new Vec3(3.0, 0.3, -1.0),
                new Vec3(4.0, 0.1, -1.5),
                new Vec3(5.0, 0.0, -2.0),
                0.3, 3
            ),
            // Sweep to left-forward
            new WhipPose(
                new Vec3(1.0, 0.4, 2.0),
                new Vec3(-0.5, 0.3, 3.5),
                new Vec3(-2.0, 0.2, 5.0),
                new Vec3(-3.5, 0.1, 6.5),
                0.2, 4
            ),
            // Continue left
            new WhipPose(
                new Vec3(-3.0, 0.3, 1.5),
                new Vec3(-4.5, 0.2, 1.0),
                new Vec3(-6.0, 0.1, 0.5),
                new Vec3(-7.5, 0.0, 0.0),
                0.3, 4
            ),
            // Reverse sweep
            new WhipPose(
                new Vec3(-2.0, 0.2, 3.0),
                new Vec3(0.0, 0.1, 4.5),
                new Vec3(2.0, 0.0, 6.0),
                new Vec3(4.0, -0.1, 7.5),
                0.3, 5
            ),
            // Recovery
            new WhipPose(
                new Vec3(0.5, -0.2, 0.8),
                new Vec3(0.4, -0.4, 1.3),
                new Vec3(0.2, -0.6, 1.8),
                new Vec3(0.0, -0.8, 2.3),
                0.7, 8
            )
        ), false));

        // LOVE_FORM_6 - Cat-Legged Winds of Love (spinning tornado)
        ANIMATIONS.put("love_form_6", new WhipAnimation("love_form_6", List.of(
            // Spin 1 (right)
            new WhipPose(
                new Vec3(2.5, 0.5, 0.0),
                new Vec3(4.0, 0.3, 0.5),
                new Vec3(5.5, 0.1, 1.0),
                new Vec3(7.0, 0.0, 1.5),
                0.2, 2
            ),
            // Spin 2 (forward)
            new WhipPose(
                new Vec3(1.5, 0.6, 2.5),
                new Vec3(2.0, 0.4, 4.0),
                new Vec3(2.5, 0.2, 5.5),
                new Vec3(3.0, 0.0, 7.0),
                0.2, 2
            ),
            // Spin 3 (left)
            new WhipPose(
                new Vec3(-2.5, 0.5, 2.0),
                new Vec3(-4.0, 0.3, 2.0),
                new Vec3(-5.5, 0.1, 2.0),
                new Vec3(-7.0, 0.0, 2.0),
                0.2, 2
            ),
            // Spin 4 (back-left)
            new WhipPose(
                new Vec3(-3.0, 0.4, -1.0),
                new Vec3(-4.0, 0.2, -2.0),
                new Vec3(-5.0, 0.1, -3.0),
                new Vec3(-6.0, 0.0, -4.0),
                0.2, 2
            ),
            // Spin 5 (back-right, rising)
            new WhipPose(
                new Vec3(2.0, 0.8, -2.0),
                new Vec3(3.5, 0.6, -2.5),
                new Vec3(5.0, 0.4, -3.0),
                new Vec3(6.5, 0.2, -3.5),
                0.2, 2
            ),
            // Final spin (overhead)
            new WhipPose(
                new Vec3(0.0, 2.0, 1.0),
                new Vec3(0.0, 2.5, 2.5),
                new Vec3(0.0, 3.0, 4.0),
                new Vec3(0.0, 3.5, 5.5),
                0.3, 3
            ),
            // Slam finish
            new WhipPose(
                new Vec3(0.0, 1.0, 3.0),
                new Vec3(0.0, 0.3, 5.0),
                new Vec3(0.0, 0.0, 7.0),
                new Vec3(0.0, -0.3, 9.0),
                0.4, 4
            ),
            // Recovery
            new WhipPose(
                new Vec3(0.0, -0.2, 1.0),
                new Vec3(0.0, -0.4, 1.5),
                new Vec3(0.0, -0.6, 2.0),
                new Vec3(0.0, -0.8, 2.5),
                0.8, 10
            )
        ), false));
    }

    /**
     * Get an animation by name.
     */
    public static WhipAnimation getAnimation(String name) {
        return ANIMATIONS.getOrDefault(name, ANIMATIONS.get("idle"));
    }

    /**
     * Check if an animation exists.
     */
    public static boolean hasAnimation(String name) {
        return ANIMATIONS.containsKey(name);
    }

    /**
     * Get all animation names.
     */
    public static java.util.Set<String> getAllAnimationNames() {
        return ANIMATIONS.keySet();
    }
}
