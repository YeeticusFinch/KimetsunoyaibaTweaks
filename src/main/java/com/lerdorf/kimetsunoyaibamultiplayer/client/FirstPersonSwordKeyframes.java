package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;

/**
 * Keyframe definitions for first-person sword swing animations
 */
public class FirstPersonSwordKeyframes {

    /**
     * Represents a single keyframe in a first-person animation
     * @param progress Animation progress (0.0 to 1.0)
     * @param translation Offset translation (x, y, z) in block units
     * @param rotation Rotation (pitch, yaw, roll) in degrees
     */
	public record FPKeyframe(
		    float progress,
		    Vec3 translation,
		    Quaternionf rotation
		) {}


    // Animation keyframe registry
    public static final Map<String, List<FPKeyframe>> ANIMATION_KEYFRAMES = new HashMap<>();

    static {
    	// sword_to_left - exported from Blender
    	ANIMATION_KEYFRAMES.put("sword_to_left", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(0.53249, 0.55914, -0.28072), new Quaternionf(0.090265f, -0.033582f, -0.614437f, 0.783065f)),
    	            new FPKeyframe(0.20f, new Vec3(-0.23376, 0.36206, -0.88560), new Quaternionf(-0.486240f, 0.420532f, -0.411318f, 0.646174f)),
    	            new FPKeyframe(0.30f, new Vec3(-0.89767, 0.16084, -0.89622), new Quaternionf(-0.624629f, 0.570294f, -0.234764f, 0.479051f)),
    	            new FPKeyframe(0.40f, new Vec3(-1.29195, 0.01829, -0.83739), new Quaternionf(-0.667937f, 0.618534f, -0.183027f, 0.371184f)),
    	            new FPKeyframe(0.50f, new Vec3(-1.48405, -0.04167, -0.75982), new Quaternionf(-0.681327f, 0.632224f, -0.176607f, 0.323877f)),
    	            new FPKeyframe(0.60f, new Vec3(-1.55576, -0.04221, -0.68635), new Quaternionf(-0.681438f, 0.632329f, -0.176629f, 0.323426f)),
    	            new FPKeyframe(0.70f, new Vec3(-1.57473, -0.04221, -0.63516), new Quaternionf(-0.681438f, 0.632329f, -0.176629f, 0.323426f)),
    	            new FPKeyframe(0.80f, new Vec3(-1.46312, -0.31312, -0.61270), new Quaternionf(-0.692267f, 0.585289f, -0.161088f, 0.390198f)),
    	            new FPKeyframe(0.90f, new Vec3(-0.66667, -0.53662, -0.37679), new Quaternionf(-0.415699f, 0.248399f, -0.063609f, 0.872609f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
    	));
    	
    	// sword_to_right - exported from Blender
    	ANIMATION_KEYFRAMES.put("sword_to_right", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(-1.02901, -0.00803, -0.60758), new Quaternionf(-0.261334f, -0.380741f, 0.713635f, 0.526750f)),
    	            new FPKeyframe(0.20f, new Vec3(-0.63018, 0.07587, -0.73025), new Quaternionf(-0.371529f, -0.507900f, 0.626137f, 0.460387f)),
    	            new FPKeyframe(0.30f, new Vec3(-0.15029, 0.18232, -0.69913), new Quaternionf(-0.468403f, -0.624830f, 0.472087f, 0.409047f)),
    	            new FPKeyframe(0.40f, new Vec3(0.25569, 0.27374, -0.51171), new Quaternionf(-0.542865f, -0.702151f, 0.303970f, 0.346242f)),
    	            new FPKeyframe(0.50f, new Vec3(0.47952, 0.31256, -0.39043), new Quaternionf(-0.569297f, -0.725598f, 0.225181f, 0.314168f)),
    	            new FPKeyframe(0.60f, new Vec3(0.53631, 0.31256, -0.39043), new Quaternionf(-0.569297f, -0.725598f, 0.225181f, 0.314168f)),
    	            new FPKeyframe(0.70f, new Vec3(0.54442, 0.31256, -0.39043), new Quaternionf(-0.569297f, -0.725598f, 0.225181f, 0.314168f)),
    	            new FPKeyframe(0.80f, new Vec3(0.35520, -0.22473, -0.28921), new Quaternionf(-0.491355f, -0.626257f, 0.194352f, 0.573237f)),
    	            new FPKeyframe(0.90f, new Vec3(0.10810, -0.11237, -0.10122), new Quaternionf(-0.171974f, -0.219190f, 0.068023f, 0.957995f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
    	));
    	
    	// sword_overhead - exported from Blender
    	ANIMATION_KEYFRAMES.put("sword_overhead", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(-0.30529, 0.53027, -0.63846), new Quaternionf(-0.432148f, 0.139736f, -0.036146f, 0.890177f)),
    	            new FPKeyframe(0.20f, new Vec3(-0.62871, -0.31558, -0.83365), new Quaternionf(-0.780927f, 0.166788f, -0.052781f, 0.599624f)),
    	            new FPKeyframe(0.30f, new Vec3(-0.64012, -0.44076, -0.83365), new Quaternionf(-0.799291f, 0.166360f, -0.060847f, 0.574243f)),
    	            new FPKeyframe(0.40f, new Vec3(-0.64656, -0.49967, -0.83365), new Quaternionf(-0.809437f, 0.165022f, -0.065069f, 0.559773f)),
    	            new FPKeyframe(0.50f, new Vec3(-0.64945, -0.52835, -0.83365), new Quaternionf(-0.814195f, 0.162265f, -0.067040f, 0.553410f)),
    	            new FPKeyframe(0.60f, new Vec3(-0.65022, -0.56286, -0.83365), new Quaternionf(-0.815947f, 0.157485f, -0.068313f, 0.552054f)),
    	            new FPKeyframe(0.70f, new Vec3(-0.64806, -0.63701, -0.83102), new Quaternionf(-0.815198f, 0.149231f, -0.070459f, 0.555173f)),
    	            new FPKeyframe(0.80f, new Vec3(-0.59110, -0.73288, -0.76278), new Quaternionf(-0.767602f, 0.128233f, -0.075615f, 0.623399f)),
    	            new FPKeyframe(0.90f, new Vec3(-0.37640, -0.78081, -0.50554), new Quaternionf(-0.507313f, 0.070262f, -0.081661f, 0.855002f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
    	));
    	
    	// sword_to_upper - exported from Blender
    	ANIMATION_KEYFRAMES.put("sword_to_upper", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(-0.71904, -0.68868, -0.99152), new Quaternionf(-0.109726f, -0.535391f, 0.825303f, 0.142096f)),
    	            new FPKeyframe(0.20f, new Vec3(-0.48858, -0.25634, -0.99152), new Quaternionf(-0.110118f, -0.571160f, 0.801254f, 0.140151f)),
    	            new FPKeyframe(0.30f, new Vec3(-0.20478, 0.27603, -0.99152), new Quaternionf(-0.121867f, -0.677879f, 0.713163f, 0.130486f)),
    	            new FPKeyframe(0.40f, new Vec3(0.01690, 0.69188, -0.99152), new Quaternionf(-0.160201f, -0.806340f, 0.562045f, 0.090868f)),
    	            new FPKeyframe(0.50f, new Vec3(0.10490, 0.85695, -0.99152), new Quaternionf(-0.183981f, -0.838205f, 0.508575f, 0.070113f)),
    	            new FPKeyframe(0.60f, new Vec3(0.10490, 0.85695, -0.99152), new Quaternionf(-0.183981f, -0.838205f, 0.508575f, 0.070113f)),
    	            new FPKeyframe(0.70f, new Vec3(0.10490, 0.85695, -0.99152), new Quaternionf(-0.183981f, -0.838205f, 0.508575f, 0.070113f)),
    	            new FPKeyframe(0.80f, new Vec3(0.10490, 0.85695, -0.99152), new Quaternionf(-0.183981f, -0.838205f, 0.508575f, 0.070113f)),
    	            new FPKeyframe(0.90f, new Vec3(0.53617, 0.56290, -0.52283), new Quaternionf(-0.125760f, -0.572955f, 0.347636f, 0.731475f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
    	));
    	
    	// ragnaraku1 - exported from Blender
    	ANIMATION_KEYFRAMES.put("ragnaraku1", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(-0.16663, 0.22761, -0.00068), new Quaternionf(-0.233536f, -0.233536f, 0.235028f, 0.914158f)),
    	            new FPKeyframe(0.20f, new Vec3(-0.36180, 0.54577, -0.00640), new Quaternionf(-0.500000f, -0.500000f, 0.500000f, 0.500000f)),
    	            new FPKeyframe(0.30f, new Vec3(-0.36180, 0.61930, -0.08260), new Quaternionf(0.695016f, 0.695017f, -0.009765f, 0.183872f)),
    	            new FPKeyframe(0.40f, new Vec3(-0.36180, 0.64502, -0.20999), new Quaternionf(0.504344f, 0.504344f, 0.495618f, 0.495618f)),
    	            new FPKeyframe(0.50f, new Vec3(-0.36180, 0.64807, -0.26660), new Quaternionf(0.020518f, 0.020518f, 0.706809f, 0.706809f)),
    	            new FPKeyframe(0.60f, new Vec3(-0.36039, 0.64598, -0.13574), new Quaternionf(-0.491198f, -0.491198f, 0.508650f, 0.508650f)),
    	            new FPKeyframe(0.70f, new Vec3(-0.35200, 0.63352, 0.07826), new Quaternionf(-0.691768f, -0.691768f, 0.014854f, 0.206622f)),
    	            new FPKeyframe(0.80f, new Vec3(-0.33027, 0.60126, 0.19065), new Quaternionf(0.512917f, 0.512917f, 0.486740f, 0.486740f)),
    	            new FPKeyframe(0.90f, new Vec3(-0.15750, 0.28931, 0.09532), new Quaternionf(0.259905f, 0.259905f, 0.246641f, 0.896698f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
    	));
        
    	// ragnaraku2 - exported from Blender
    	ANIMATION_KEYFRAMES.put("ragnaraku2", List.of(
    	            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
    	            new FPKeyframe(0.10f, new Vec3(0.06095, 0.04219, -0.28709), new Quaternionf(-0.125479f, 0.000000f, -0.000000f, 0.992096f)),
    	            new FPKeyframe(0.20f, new Vec3(0.13235, 0.19253, -0.62334), new Quaternionf(-0.277959f, 0.000000f, -0.000000f, 0.960593f)),
    	            new FPKeyframe(0.30f, new Vec3(0.09992, 0.43809, -0.46605), new Quaternionf(-0.083636f, 0.000000f, -0.000000f, 0.996496f)),
    	            new FPKeyframe(0.40f, new Vec3(0.03817, 0.68173, -0.16654), new Quaternionf(0.324147f, 0.000000f, -0.000000f, 0.946007f)),
    	            new FPKeyframe(0.50f, new Vec3(0.00040, 0.80544, 0.01662), new Quaternionf(0.521850f, 0.000000f, -0.000000f, 0.853037f)),
    	            new FPKeyframe(0.60f, new Vec3(0.00000, 0.80667, 0.01858), new Quaternionf(0.523645f, 0.000000f, -0.000000f, 0.851937f)),
    	            new FPKeyframe(0.70f, new Vec3(0.00000, 0.80667, 0.01858), new Quaternionf(0.523645f, 0.000000f, -0.000000f, 0.851937f)),
    	            new FPKeyframe(0.80f, new Vec3(0.00000, 0.80667, 0.01858), new Quaternionf(0.523645f, 0.000000f, -0.000000f, 0.851937f)),
    	            new FPKeyframe(0.90f, new Vec3(0.00000, 0.80667, 0.01858), new Quaternionf(0.523645f, 0.000000f, -0.000000f, 0.851937f)),
    	            new FPKeyframe(1.00f, new Vec3(0.00000, 0.80667, 0.01858), new Quaternionf(0.523645f, 0.000000f, -0.000000f, 0.851937f))
    	));
        
    	// ragnaraku3 - exported from Blender
        ANIMATION_KEYFRAMES.put("ragnaraku3", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(-0.03068, 0.67769, -0.15025), new Quaternionf(0.508784f, 0.000000f, -0.000000f, 0.860895f)),
                    new FPKeyframe(0.20f, new Vec3(-0.12964, 0.26167, -0.63779), new Quaternionf(-0.088686f, 0.000000f, -0.000000f, 0.996060f)),
                    new FPKeyframe(0.30f, new Vec3(-0.25162, -0.25113, -1.09190), new Quaternionf(-0.657488f, 0.000000f, -0.000000f, 0.753465f)),
                    new FPKeyframe(0.40f, new Vec3(-0.35263, -0.67576, -1.30910), new Quaternionf(-0.782371f, 0.000000f, -0.000000f, 0.622813f)),
                    new FPKeyframe(0.50f, new Vec3(-0.39462, -0.85231, -1.35992), new Quaternionf(-0.800919f, 0.000000f, -0.000000f, 0.598773f)),
                    new FPKeyframe(0.60f, new Vec3(-0.39462, -0.85231, -1.35992), new Quaternionf(-0.800919f, 0.000000f, -0.000000f, 0.598773f)),
                    new FPKeyframe(0.70f, new Vec3(-0.39462, -0.85231, -1.35992), new Quaternionf(-0.800919f, 0.000000f, -0.000000f, 0.598773f)),
                    new FPKeyframe(0.80f, new Vec3(-0.39462, -0.85231, -1.35992), new Quaternionf(-0.800919f, 0.000000f, -0.000000f, 0.598773f)),
                    new FPKeyframe(0.90f, new Vec3(-0.39462, -0.85231, -1.35992), new Quaternionf(-0.800919f, 0.000000f, -0.000000f, 0.598773f)),
                    new FPKeyframe(1.00f, new Vec3(-0.00182, -0.23033, -0.10074), new Quaternionf(-0.061031f, 0.000000f, -0.000000f, 0.998136f))
        ));
        
        // speed_attack_sword - exported from Blender
        ANIMATION_KEYFRAMES.put("speed_attack_sword", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(0.00000, -0.21232, 0.11065), new Quaternionf(-0.712352f, 0.000000f, -0.000000f, 0.701823f)),
                    new FPKeyframe(0.20f, new Vec3(0.00000, -0.23946, -0.18785), new Quaternionf(-0.717189f, 0.000000f, -0.000000f, 0.696878f)),
                    new FPKeyframe(0.30f, new Vec3(0.00000, -0.25341, -0.58061), new Quaternionf(-0.719660f, 0.000000f, -0.000000f, 0.694326f)),
                    new FPKeyframe(0.40f, new Vec3(0.00000, -0.25854, -0.92262), new Quaternionf(-0.720568f, 0.000000f, -0.000000f, 0.693384f)),
                    new FPKeyframe(0.50f, new Vec3(0.00000, -0.25927, -1.06885), new Quaternionf(-0.720698f, 0.000000f, -0.000000f, 0.693249f)),
                    new FPKeyframe(0.60f, new Vec3(0.00000, -0.25927, -1.06885), new Quaternionf(-0.720698f, 0.000000f, -0.000000f, 0.693249f)),
                    new FPKeyframe(0.70f, new Vec3(0.00000, -0.25927, -1.06885), new Quaternionf(-0.720698f, 0.000000f, -0.000000f, 0.693249f)),
                    new FPKeyframe(0.80f, new Vec3(0.00000, -0.46053, -0.95769), new Quaternionf(-0.665035f, 0.000000f, -0.000000f, 0.746813f)),
                    new FPKeyframe(0.90f, new Vec3(0.00000, -0.41322, -0.37623), new Quaternionf(-0.273546f, 0.000000f, -0.000000f, 0.961859f)),
                    new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
        ));
    }
    


    /**
     * Get keyframes for a specific animation
     * @param animationName The animation name
     * @return List of keyframes, or null if not found
     */
    public static List<FPKeyframe> getKeyframes(String animationName) {
        return ANIMATION_KEYFRAMES.get(animationName);
    }

    /**
     * Check if keyframes exist for an animation
     */
    public static boolean hasKeyframes(String animationName) {
        return ANIMATION_KEYFRAMES.containsKey(animationName);
    }

    /**
     * Find the two keyframes to interpolate between at a given progress
     * @param keyframes List of keyframes
     * @param progress Animation progress (0.0 to 1.0)
     * @return Array of [previous, next] keyframes
     */
    public static FPKeyframe[] findFramePair(List<FPKeyframe> keyframes, float progress) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }

        FPKeyframe prev = keyframes.get(0);
        for (FPKeyframe next : keyframes) {
            if (next.progress() >= progress) {
                return new FPKeyframe[] {prev, next};
            }
            prev = next;
        }
        // If we're past the last keyframe, return the last frame twice
        return new FPKeyframe[] {prev, prev};
    }

    /**
     * Interpolate between two keyframes
     * @param framePair The [previous, next] keyframe pair
     * @param progress Current animation progress
     * @return Interpolated transform as [translation, rotationQuaternion]
     */
    public static Object[] interpolateKeyframes(FPKeyframe[] framePair, float progress) {
        if (framePair == null || framePair.length != 2) {
            return new Object[] { Vec3.ZERO, new Quaternionf() };
        }

        FPKeyframe prev = framePair[0];
        FPKeyframe next = framePair[1];

        // Local interpolation factor
        float localT;
        if (next.progress() == prev.progress()) {
            localT = 0.0f;
        } else {
            localT = Mth.inverseLerp(progress, prev.progress(), next.progress());
            localT = Mth.clamp(localT, 0.0f, 1.0f);
        }

        // Translation → LERP
        Vec3 translation = prev.translation().lerp(next.translation(), localT);

        // Rotation → SLERP
        Quaternionf rotation = new Quaternionf(prev.rotation())
                .slerp(next.rotation(), localT);

        return new Object[] { translation, rotation };
    }


    /**
     * Register custom keyframes for an animation
     * Allows other mods to add their own animations
     */
    public static void registerKeyframes(String animationName, List<FPKeyframe> keyframes) {
        ANIMATION_KEYFRAMES.put(animationName, keyframes);
    }
}
