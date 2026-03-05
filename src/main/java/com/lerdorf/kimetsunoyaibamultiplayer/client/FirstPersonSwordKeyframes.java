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

        // sword_none - exported from Blender
        ANIMATION_KEYFRAMES.put("sword_none", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.20f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.30f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.40f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.50f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.60f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.70f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.80f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.90f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
        ));

    	// sword_to_left - exported from Blender
        ANIMATION_KEYFRAMES.put("sword_to_left", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(0.02676, 0.43142, -0.74701), new Quaternionf(-0.348336f, 0.306236f, -0.516152f, 0.720047f)),
                    new FPKeyframe(0.20f, new Vec3(-1.08186, 0.09552, -0.87348), new Quaternionf(-0.646784f, 0.595343f, -0.205401f, 0.430171f)),
                    new FPKeyframe(0.30f, new Vec3(-1.49478, -0.04221, -0.75194), new Quaternionf(-0.681438f, 0.632329f, -0.176629f, 0.323426f)),
                    new FPKeyframe(0.40f, new Vec3(-1.57447, -0.04221, -0.65439), new Quaternionf(-0.681438f, 0.632329f, -0.176629f, 0.323426f)),
                    new FPKeyframe(0.50f, new Vec3(-1.05898, -0.66932, -0.53272), new Quaternionf(-0.622809f, 0.418807f, -0.110294f, 0.651571f)),
                    new FPKeyframe(0.60f, new Vec3(-0.54220, -0.59971, -0.38754), new Quaternionf(-0.343195f, 0.208052f, -0.054791f, 0.914292f)),
                    new FPKeyframe(0.70f, new Vec3(-0.22874, -0.43372, -0.24420), new Quaternionf(-0.152929f, 0.079637f, -0.020972f, 0.984800f)),
                    new FPKeyframe(0.80f, new Vec3(-0.06778, -0.23560, -0.12018), new Quaternionf(-0.054474f, 0.022127f, -0.005827f, 0.998253f)),
                    new FPKeyframe(0.90f, new Vec3(-0.00847, -0.06961, -0.03296), new Quaternionf(-0.011018f, 0.002696f, -0.000710f, 0.999935f)),
                    new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
        ));
    	
    	// sword_to_right - exported from Blender
        ANIMATION_KEYFRAMES.put("sword_to_right", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(-0.89961, 0.22056, -0.41600), new Quaternionf(-0.188875f, -0.311717f, 0.690602f, 0.624682f)),
                    new FPKeyframe(0.20f, new Vec3(-0.76362, 0.28783, -0.80604), new Quaternionf(-0.224597f, -0.412749f, 0.667038f, 0.578148f)),
                    new FPKeyframe(0.30f, new Vec3(-0.39844, 0.39560, -0.79449), new Quaternionf(-0.333398f, -0.631805f, 0.525928f, 0.461592f)),
                    new FPKeyframe(0.40f, new Vec3(-0.15568, 0.42763, -0.65744), new Quaternionf(-0.425458f, -0.736859f, 0.411532f, 0.326598f)),
                    new FPKeyframe(0.50f, new Vec3(0.87736, 0.42926, -0.59879), new Quaternionf(-0.560846f, -0.784671f, 0.243435f, 0.102390f)),
                    new FPKeyframe(0.60f, new Vec3(0.84728, 0.41454, -0.57826), new Quaternionf(-0.558468f, -0.781344f, 0.242403f, 0.137316f)),
                    new FPKeyframe(0.70f, new Vec3(0.64990, 0.31797, -0.44355), new Quaternionf(-0.513227f, -0.718048f, 0.222766f, 0.413980f)),
                    new FPKeyframe(0.80f, new Vec3(0.36587, 0.17901, -0.24970), new Quaternionf(-0.311544f, -0.435877f, 0.135226f, 0.833466f)),
                    new FPKeyframe(0.90f, new Vec3(0.11072, 0.05417, -0.07557), new Quaternionf(-0.079033f, -0.110574f, 0.034304f, 0.990127f)),
                    new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
        ));
    	
        // sword_overhead - exported from Blender
        ANIMATION_KEYFRAMES.put("sword_overhead", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(-0.64150, -0.48116, -0.83365), new Quaternionf(-0.801652f, 0.164599f, -0.062499f, 0.571274f)),
                    new FPKeyframe(0.20f, new Vec3(-0.65025, -0.59357, -0.83365), new Quaternionf(-0.816382f, 0.154133f, -0.069168f, 0.552250f)),
                    new FPKeyframe(0.30f, new Vec3(-0.64945, -0.60914, -0.83269), new Quaternionf(-0.816689f, 0.146463f, -0.069694f, 0.553814f)),
                    new FPKeyframe(0.40f, new Vec3(-0.64386, -0.63792, -0.82600), new Quaternionf(-0.812682f, 0.142209f, -0.070739f, 0.560643f)),
                    new FPKeyframe(0.50f, new Vec3(-0.62870, -0.67420, -0.80782), new Quaternionf(-0.800436f, 0.138918f, -0.072389f, 0.578588f)),
                    new FPKeyframe(0.60f, new Vec3(-0.59915, -0.71232, -0.77243), new Quaternionf(-0.774641f, 0.133889f, -0.074682f, 0.613537f)),
                    new FPKeyframe(0.70f, new Vec3(-0.55045, -0.74658, -0.71407), new Quaternionf(-0.726954f, 0.123815f, -0.077504f, 0.670970f)),
                    new FPKeyframe(0.80f, new Vec3(-0.47780, -0.77131, -0.62702), new Quaternionf(-0.643938f, 0.104331f, -0.080311f, 0.753664f)),
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

        // Left-hand animations reuse the same keyframe curves but render on OFF_HAND.
        ANIMATION_KEYFRAMES.put("left_sword_to_left", ANIMATION_KEYFRAMES.get("sword_to_left"));
        ANIMATION_KEYFRAMES.put("left_sword_to_right", ANIMATION_KEYFRAMES.get("sword_to_right"));
        ANIMATION_KEYFRAMES.put("left_sword_overhead", ANIMATION_KEYFRAMES.get("sword_overhead"));
        ANIMATION_KEYFRAMES.put("double_sword_overhead", ANIMATION_KEYFRAMES.get("sword_overhead"));
        ANIMATION_KEYFRAMES.put("beast2", ANIMATION_KEYFRAMES.get("sword_to_right"));
        ANIMATION_KEYFRAMES.put("breath_beast2", ANIMATION_KEYFRAMES.get("sword_to_left"));
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
