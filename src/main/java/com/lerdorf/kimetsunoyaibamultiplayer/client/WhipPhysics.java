package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.animations.MitsuriWhipAnimations;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Physics simulation for Mitsuri's whip sword with keyframe animation blending.
 *
 * Features:
 * - Spring chain physics for realistic floppy motion
 * - Keyframe animation playback with Catmull-Rom interpolation
 * - Smooth blending between physics and keyframes
 * - Per-player state tracking (positions, velocities, animation state)
 * - Dynamic extension/retraction
 */
public class WhipPhysics {

    /**
     * State data for a single whip instance.
     */
    private static class WhipState {
        List<Vec3> positions = new ArrayList<>();
        List<Vec3> velocities = new ArrayList<>();
        String currentAnimation = "idle";
        int animationTick = 0;
        double extensionLevel = 0.0; // 0.0 = idle length, 1.0 = extended length
        double targetExtension = 0.0;
        int extensionTransitionTicks = 0;
        int extensionTransitionProgress = 0;
        long lastUpdateTime = 0;
    }

    private static final Map<UUID, WhipState> whipStates = new HashMap<>();

    /**
     * Get whip points for rendering.
     */
    public static List<Vec3> getPoints(Player player) {
        WhipState state = whipStates.computeIfAbsent(player.getUUID(), id -> {
            return initState(player);
        });
        Log.debug("[WhipPhysics] Returning {} positions", state.positions.size());
        return state.positions;
    }

    /**
     * Initialize a new whip state with proper visible length.
     */
    private static WhipState initState(Player player) {
        Log.debug("[WhipPhysics] initState() starting...");
        WhipState state = new WhipState();

        // Start from hand position, not body
        Vec3 handPos = getHandPosition(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 down = new Vec3(0, -1, 0);

        int segments = EnhancedBreathingConfig.whipSegmentCount;
        double idleLength = EnhancedBreathingConfig.whipIdleLength;
        double segmentLength = idleLength / segments;

        Log.debug("[WhipPhysics] Initializing with {} segments, idleLength={}", segments, idleLength);

        // Initialize whip hanging down from hand with proper length
        for (int i = 0; i < segments; i++) {
            // Blend between look direction (near hand) and straight down (far from hand)
            double blend = (double) i / segments;
            Vec3 direction = lerpVec(lookVec.scale(0.3).add(down.scale(0.7)), down, blend).normalize();
            Vec3 pos = handPos.add(direction.scale(i * segmentLength));

            state.positions.add(pos);
            state.velocities.add(Vec3.ZERO);
        }

        // Start with idle animation
        state.currentAnimation = "idle";
        state.animationTick = 0;
        state.extensionLevel = 0.0;
        state.targetExtension = 0.0;
        state.lastUpdateTime = System.currentTimeMillis();

        return state;
    }

    /**
     * Play an animation on a player's whip.
     */
    public static void playAnimation(Player player, String animationName, double targetExtension, int transitionTicks) {
        WhipState state = whipStates.computeIfAbsent(player.getUUID(), id -> initState(player));

        // Only restart if it's a different animation or a non-looping animation finished
        MitsuriWhipAnimations.WhipAnimation anim = MitsuriWhipAnimations.getAnimation(animationName);
        if (!state.currentAnimation.equals(animationName) ||
            (!anim.loops && state.animationTick >= anim.totalTicks)) {
            state.currentAnimation = animationName;
            state.animationTick = 0;
        }

        state.targetExtension = targetExtension;
        state.extensionTransitionTicks = transitionTicks;
        state.extensionTransitionProgress = 0;
    }

    /**
     * Play an animation with default extension (idle length).
     */
    public static void playAnimation(Player player, String animationName) {
        playAnimation(player, animationName, 0.0, EnhancedBreathingConfig.whipExtensionTicks);
    }

    /**
     * Set extension level immediately (for special cases).
     */
    public static void setExtension(Player player, double extension) {
        WhipState state = whipStates.computeIfAbsent(player.getUUID(), id -> initState(player));
        state.extensionLevel = Mth.clamp(extension, 0.0, 1.0);
        state.targetExtension = state.extensionLevel;
        state.extensionTransitionProgress = state.extensionTransitionTicks;
    }

    /**
     * Get current animation name for a player.
     */
    public static String getCurrentAnimation(Player player) {
        WhipState state = whipStates.get(player.getUUID());
        return state != null ? state.currentAnimation : "idle";
    }

    /**
     * Get current animation tick for a player.
     */
    public static int getAnimationTick(Player player) {
        WhipState state = whipStates.get(player.getUUID());
        return state != null ? state.animationTick : 0;
    }

    /**
     * Main physics update tick.
     */
    public static void tick(Player player) {
        WhipState state = whipStates.computeIfAbsent(player.getUUID(), id -> initState(player));

        // Handle extension transition
        if (state.extensionTransitionProgress < state.extensionTransitionTicks) {
            state.extensionTransitionProgress++;
            double t = (double) state.extensionTransitionProgress / state.extensionTransitionTicks;
            // Smooth ease-in-out
            t = t * t * (3.0 - 2.0 * t);
            state.extensionLevel = Mth.lerp(t, state.extensionLevel, state.targetExtension);
        }

        // Get current animation
        MitsuriWhipAnimations.WhipAnimation anim = MitsuriWhipAnimations.getAnimation(state.currentAnimation);

        // Update animation tick
        state.animationTick++;
        if (state.animationTick >= anim.totalTicks) {
            if (anim.loops) {
                state.animationTick = 0;
            } else {
                // Non-looping animation finished, return to idle
                if (!state.currentAnimation.equals("idle")) {
                    state.currentAnimation = "idle";
                    state.animationTick = 0;
                    anim = MitsuriWhipAnimations.getAnimation("idle");
                }
            }
        }

        // Get hand position (attachment point)
        Vec3 handPos = getHandPosition(player);

        // Get keyframe targets
        List<Vec3> keyframeTargets = getKeyframeTargets(player, anim, state.animationTick, handPos);

        // Get current whip length based on extension
        double currentLength = Mth.lerp(state.extensionLevel,
                                        EnhancedBreathingConfig.whipIdleLength,
                                        EnhancedBreathingConfig.whipExtendedLength);
        double segmentLength = currentLength / state.positions.size();

        // Apply physics with keyframe blending
        updatePhysics(state, handPos, keyframeTargets, segmentLength, player);

        state.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Get player's hand position (whip attachment point).
     * Works for both first-person and third-person view.
     */
    private static Vec3 getHandPosition(Player player) {
        // Get eye position and look vector
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();

        // Check if player is local (first person) or remote (third person)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean isLocalPlayer = (mc.player != null && mc.player.getUUID().equals(player.getUUID()));
        boolean isFirstPerson = isLocalPlayer && mc.options.getCameraType().isFirstPerson();

        if (isFirstPerson) {
            // First person: whip starts slightly in front and to the right
            return eyePos.add(right.scale(0.15)).add(lookVec.scale(0.5)).add(0, -0.2, 0);
        } else {
            // Third person: attach to visible hand position
            // This accounts for arm swing and positioning
            Vec3 bodyPos = player.position().add(0, player.getEyeHeight() * 0.85, 0);
            Vec3 armOffset = right.scale(0.35); // Right arm offset
            Vec3 forwardOffset = lookVec.scale(0.35); // Slight forward from body

            return bodyPos.add(armOffset).add(forwardOffset);
        }
    }

    /**
     * Get keyframe target positions for current animation frame.
     */
    private static List<Vec3> getKeyframeTargets(Player player, MitsuriWhipAnimations.WhipAnimation anim,
                                                   int tick, Vec3 handPos) {
        // Find current keyframe
        int accumulatedTicks = 0;
        MitsuriWhipAnimations.WhipPose currentPose = anim.keyframes.get(0);
        MitsuriWhipAnimations.WhipPose nextPose = anim.keyframes.get(0);
        double t = 0.0;

        for (int i = 0; i < anim.keyframes.size(); i++) {
            MitsuriWhipAnimations.WhipPose pose = anim.keyframes.get(i);
            if (tick < accumulatedTicks + pose.durationTicks()) {
                currentPose = pose;
                nextPose = anim.keyframes.get((i + 1) % anim.keyframes.size());
                t = (double) (tick - accumulatedTicks) / pose.durationTicks();
                break;
            }
            accumulatedTicks += pose.durationTicks();
        }

        // Interpolate between current and next pose
        Vec3 c1 = lerpVec(currentPose.c1(), nextPose.c1(), t);
        Vec3 c2 = lerpVec(currentPose.c2(), nextPose.c2(), t);
        Vec3 c3 = lerpVec(currentPose.c3(), nextPose.c3(), t);
        Vec3 tip = lerpVec(currentPose.tip(), nextPose.tip(), t);

        // Transform to world space (rotate with player)
        Vec3 lookVec = player.getLookAngle();
        Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(lookVec);

        c1 = transformToWorld(c1, handPos, lookVec, right, up);
        c2 = transformToWorld(c2, handPos, lookVec, right, up);
        c3 = transformToWorld(c3, handPos, lookVec, right, up);
        tip = transformToWorld(tip, handPos, lookVec, right, up);

        // Sample points along Catmull-Rom curve
        List<Vec3> targets = new ArrayList<>();
        int segments = EnhancedBreathingConfig.whipSegmentCount;

        // Use Catmull-Rom interpolation
        Vec3 p0 = handPos; // Start at hand
        Vec3 p1 = c1;
        Vec3 p2 = c2;
        Vec3 p3 = c3;
        Vec3 p4 = tip;

        for (int i = 0; i < segments; i++) {
            double segT = (double) i / (segments - 1);

            Vec3 point;
            if (segT < 0.333) {
                // First third: handPos -> c1 -> c2
                double localT = segT / 0.333;
                point = catmullRom(p0, p1, p2, p3, localT);
            } else if (segT < 0.666) {
                // Second third: c1 -> c2 -> c3
                double localT = (segT - 0.333) / 0.333;
                point = catmullRom(p1, p2, p3, p4, localT);
            } else {
                // Final third: c2 -> c3 -> tip
                double localT = (segT - 0.666) / 0.334;
                point = catmullRom(p2, p3, p4, p4.add(p4.subtract(p3)), localT);
            }

            targets.add(point);
        }

        return targets;
    }

    /**
     * Transform a local position to world space.
     */
    private static Vec3 transformToWorld(Vec3 local, Vec3 origin, Vec3 forward, Vec3 right, Vec3 up) {
        return origin.add(
            right.scale(local.x)
                .add(up.scale(local.y))
                .add(forward.scale(local.z))
        );
    }

    /**
     * Catmull-Rom spline interpolation.
     */
    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double x = 0.5 * (
            (2 * p1.x) +
            (-p0.x + p2.x) * t +
            (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
            (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3
        );

        double y = 0.5 * (
            (2 * p1.y) +
            (-p0.y + p2.y) * t +
            (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
            (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3
        );

        double z = 0.5 * (
            (2 * p1.z) +
            (-p0.z + p2.z) * t +
            (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 +
            (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3
        );

        return new Vec3(x, y, z);
    }

    /**
     * Linear interpolation between vectors.
     */
    private static Vec3 lerpVec(Vec3 a, Vec3 b, double t) {
        return new Vec3(
            Mth.lerp(t, a.x, b.x),
            Mth.lerp(t, a.y, b.y),
            Mth.lerp(t, a.z, b.z)
        );
    }

    /**
     * Update physics simulation with keyframe blending.
     */
    private static void updatePhysics(WhipState state, Vec3 handPos, List<Vec3> keyframeTargets,
                                       double restLength, Player player) {
        double stiffness = EnhancedBreathingConfig.whipStiffness;
        double damping = EnhancedBreathingConfig.whipDamping;
        double gravity = EnhancedBreathingConfig.whipGravity;
        double keyframeBlend = EnhancedBreathingConfig.whipKeyframeBlend;

        // Apply velocities
        for (int i = 0; i < state.positions.size(); i++) {
            Vec3 pos = state.positions.get(i);
            Vec3 vel = state.velocities.get(i);

            // Apply gravity
            vel = vel.add(0, -gravity, 0);

            // Apply velocity
            pos = pos.add(vel);

            state.positions.set(i, pos);
            state.velocities.set(i, vel);
        }

        // Spring constraints
        Vec3 prevTarget = handPos;

        for (int i = 0; i < state.positions.size(); i++) {
            Vec3 pos = state.positions.get(i);
            Vec3 vel = state.velocities.get(i);

            // Get keyframe target (if available)
            Vec3 keyframeTarget = i < keyframeTargets.size() ? keyframeTargets.get(i) : pos;

            // Blend between physics and keyframe
            Vec3 blendedTarget = lerpVec(prevTarget, keyframeTarget, keyframeBlend);

            // Spring force toward blended target
            Vec3 diff = blendedTarget.subtract(pos);
            double dist = diff.length();

            if (dist > 0.001) {
                Vec3 springForce = diff.normalize().scale((dist - restLength) * stiffness);
                vel = vel.add(springForce);
            }

            // Damping
            vel = vel.scale(damping);

            // Update
            state.positions.set(i, pos);
            state.velocities.set(i, vel);

            prevTarget = pos;
        }

        // Constrain first segment to hand
        if (!state.positions.isEmpty()) {
            state.positions.set(0, handPos);
            state.velocities.set(0, Vec3.ZERO);
        }
    }

    /**
     * Clean up state for a player (call when player disconnects).
     */
    public static void cleanup(UUID playerUUID) {
        whipStates.remove(playerUUID);
    }

    /**
     * Clear all states (for world reload).
     */
    public static void clearAll() {
        whipStates.clear();
    }
}
