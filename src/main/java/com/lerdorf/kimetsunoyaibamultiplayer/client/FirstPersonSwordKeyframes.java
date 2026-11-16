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

        // guard - exported from Blender
        ANIMATION_KEYFRAMES.put("guard", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.10f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.20f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.30f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.40f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.50f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.60f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.70f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.80f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.90f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(1.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f))
        ));

        // guard_0 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_0", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.10f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.20f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.30f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.40f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.50f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.60f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.70f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.80f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.90f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(1.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f))
        ));

        // guard_1 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_1", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.10f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.20f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.30f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.40f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.50f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.60f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.70f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.80f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(0.90f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f)),
                    new FPKeyframe(1.00f, new Vec3(-0.22146, 0.00000, -0.09659), new Quaternionf(0.012328f, -0.276530f, 0.300701f, 0.912665f))
        ));

        // guard_2 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_2", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.10f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.20f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.30f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.40f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.50f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.60f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.70f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.80f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(0.90f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f)),
                    new FPKeyframe(1.00f, new Vec3(-0.14887, 0.00000, -0.09659), new Quaternionf(-0.025183f, -0.132754f, 0.367028f, 0.920344f))
        ));

        // guard_3 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_3", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.10f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.20f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.30f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.40f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.50f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.60f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.70f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.80f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(0.90f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f)),
                    new FPKeyframe(1.00f, new Vec3(-0.14887, 0.15204, -0.09659), new Quaternionf(0.023513f, -0.133060f, 0.669449f, 0.730466f))
        ));

        // guard_4 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_4", List.of(
                    new FPKeyframe(0.00f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.10f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.20f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.30f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.40f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.50f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.60f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.70f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.80f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(0.90f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f)),
                    new FPKeyframe(1.00f, new Vec3(-0.61484, 0.49914, -0.20870), new Quaternionf(0.147657f, 0.176479f, -0.940190f, 0.251189f))
        ));

        // guard_5 - exported from Blender
        ANIMATION_KEYFRAMES.put("guard_5", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.10f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.20f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.30f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.40f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.50f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.60f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.70f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.80f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(0.90f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f)),
                    new FPKeyframe(1.00f, new Vec3(0.03865, 0.15204, -0.20870), new Quaternionf(0.066889f, -0.085005f, 0.705295f, 0.700613f))
        ));

        // beast10 - exported from Blender
        ANIMATION_KEYFRAMES.put("beast10", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.707080f, 0.006171f, -0.707080f, 0.006171f)),
                    new FPKeyframe(0.20f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.037415f, -0.706116f, 0.037415f, 0.706116f)),
                    new FPKeyframe(0.30f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.730227f, 0.032305f, -0.681641f, 0.033023f)),
                    new FPKeyframe(0.40f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.025931f, -0.730481f, 0.038970f, 0.681327f)),
                    new FPKeyframe(0.50f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.730680f, 0.019556f, -0.680961f, 0.044914f)),
                    new FPKeyframe(0.60f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.013179f, -0.730823f, 0.050855f, 0.680543f)),
                    new FPKeyframe(0.70f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.730910f, 0.006801f, -0.680073f, 0.056792f)),
                    new FPKeyframe(0.80f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.000422f, -0.730941f, 0.062724f, 0.679552f)),
                    new FPKeyframe(0.90f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(-0.730917f, -0.005957f, -0.678978f, 0.068652f)),
                    new FPKeyframe(1.00f, new Vec3(0.19012, 0.00000, -0.31098), new Quaternionf(0.012335f, -0.730837f, 0.074574f, 0.678354f))
        ));

        // love_fourth_form_short - exported from Blender
        ANIMATION_KEYFRAMES.put("love_fourth_form_short", List.of(
            new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
            new FPKeyframe(0.05f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.09f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.14f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.18f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.23f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.27f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.32f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.36f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.41f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.45f, new Vec3(-0.11885, 0.85583, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.50f, new Vec3(-0.11885, 0.61671, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.55f, new Vec3(-0.11885, 0.17264, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.59f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.64f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.68f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.73f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.77f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.82f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.86f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.91f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(0.95f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f)),
            new FPKeyframe(1.00f, new Vec3(-0.11885, -0.06647, -0.24643), new Quaternionf(0.500000f, 0.500000f, -0.500000f, 0.500000f))
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

        // beast10left - exported from Blender
        ANIMATION_KEYFRAMES.put("beast10left", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.10f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.707046f, -0.022287f, 0.706789f, 0.006168f)),
                    new FPKeyframe(0.20f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.016116f, 0.707214f, -0.012336f, 0.706708f)),
                    new FPKeyframe(0.30f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.707328f, -0.009944f, 0.706574f, 0.018502f)),
                    new FPKeyframe(0.40f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.003771f, 0.707387f, -0.024668f, 0.706385f)),
                    new FPKeyframe(0.50f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.707393f, 0.002402f, 0.706143f, 0.030831f)),
                    new FPKeyframe(0.60f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(0.008575f, 0.707345f, -0.036992f, 0.705847f)),
                    new FPKeyframe(0.70f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.707244f, 0.014748f, 0.705498f, 0.043150f)),
                    new FPKeyframe(0.80f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(0.020919f, 0.707088f, -0.049305f, 0.705094f)),
                    new FPKeyframe(0.90f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(-0.706879f, 0.027088f, 0.704637f, 0.055456f)),
                    new FPKeyframe(1.00f, new Vec3(-0.08448, 0.00000, -0.31098), new Quaternionf(0.033256f, 0.706615f, -0.061603f, 0.704126f))
        ));

        // brambles - exported from Blender
        ANIMATION_KEYFRAMES.put("brambles", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.02f, new Vec3(0.22002, -0.06117, -0.00000), new Quaternionf(-0.284419f, -0.238865f, 0.008805f, 0.928424f)),
                    new FPKeyframe(0.03f, new Vec3(0.44005, -0.12235, -0.00000), new Quaternionf(-0.528134f, -0.444585f, 0.016350f, 0.723292f)),
                    new FPKeyframe(0.05f, new Vec3(0.44005, -0.12235, 0.00014), new Quaternionf(-0.528167f, -0.446653f, 0.016324f, 0.721994f)),
                    new FPKeyframe(0.07f, new Vec3(0.44005, -0.12235, 0.00055), new Quaternionf(-0.528195f, -0.448646f, 0.016244f, 0.720738f)),
                    new FPKeyframe(0.08f, new Vec3(0.44005, -0.12235, 0.00122), new Quaternionf(-0.528221f, -0.450564f, 0.016113f, 0.719524f)),
                    new FPKeyframe(0.10f, new Vec3(0.44005, -0.12235, 0.00214), new Quaternionf(-0.528243f, -0.452410f, 0.015932f, 0.718353f)),
                    new FPKeyframe(0.12f, new Vec3(0.44005, -0.12235, 0.00331), new Quaternionf(-0.528263f, -0.454184f, 0.015704f, 0.717223f)),
                    new FPKeyframe(0.13f, new Vec3(0.44005, -0.12235, 0.00470), new Quaternionf(-0.528280f, -0.455889f, 0.015430f, 0.716133f)),
                    new FPKeyframe(0.15f, new Vec3(0.44005, -0.12235, 0.00632), new Quaternionf(-0.528295f, -0.457526f, 0.015112f, 0.715085f)),
                    new FPKeyframe(0.17f, new Vec3(0.44005, -0.12235, 0.00815), new Quaternionf(-0.528308f, -0.459095f, 0.014754f, 0.714076f)),
                    new FPKeyframe(0.18f, new Vec3(0.44005, -0.12235, 0.01017), new Quaternionf(-0.528319f, -0.460600f, 0.014355f, 0.713107f)),
                    new FPKeyframe(0.20f, new Vec3(0.44005, -0.12235, 0.01239), new Quaternionf(-0.528328f, -0.462041f, 0.013919f, 0.712176f)),
                    new FPKeyframe(0.22f, new Vec3(0.44005, -0.12235, 0.01479), new Quaternionf(-0.528335f, -0.463420f, 0.013448f, 0.711283f)),
                    new FPKeyframe(0.23f, new Vec3(0.44005, -0.12235, 0.01736), new Quaternionf(-0.528341f, -0.464737f, 0.012942f, 0.710428f)),
                    new FPKeyframe(0.25f, new Vec3(0.44005, -0.12235, 0.02009), new Quaternionf(-0.528346f, -0.465996f, 0.012406f, 0.709609f)),
                    new FPKeyframe(0.27f, new Vec3(0.44005, -0.12235, 0.02298), new Quaternionf(-0.528350f, -0.467196f, 0.011839f, 0.708826f)),
                    new FPKeyframe(0.28f, new Vec3(0.44005, -0.12235, 0.02600), new Quaternionf(-0.528353f, -0.468340f, 0.011245f, 0.708078f)),
                    new FPKeyframe(0.30f, new Vec3(0.44005, -0.12235, 0.02915), new Quaternionf(-0.528355f, -0.469428f, 0.010625f, 0.707365f)),
                    new FPKeyframe(0.32f, new Vec3(0.44005, -0.12235, 0.03243), new Quaternionf(-0.528355f, -0.470463f, 0.009982f, 0.706686f)),
                    new FPKeyframe(0.33f, new Vec3(0.44005, -0.12235, 0.03581), new Quaternionf(-0.528356f, -0.471446f, 0.009316f, 0.706040f)),
                    new FPKeyframe(0.35f, new Vec3(0.44005, -0.12235, 0.03930), new Quaternionf(-0.528355f, -0.472377f, 0.008631f, 0.705426f)),
                    new FPKeyframe(0.37f, new Vec3(0.44005, -0.12235, 0.04287), new Quaternionf(-0.528354f, -0.473259f, 0.007929f, 0.704844f)),
                    new FPKeyframe(0.38f, new Vec3(0.44005, -0.12235, 0.04653), new Quaternionf(-0.528353f, -0.474093f, 0.007210f, 0.704292f)),
                    new FPKeyframe(0.40f, new Vec3(0.44005, -0.12235, 0.05025), new Quaternionf(-0.528351f, -0.474880f, 0.006478f, 0.703770f)),
                    new FPKeyframe(0.42f, new Vec3(0.44005, -0.12235, 0.05404), new Quaternionf(-0.528349f, -0.475622f, 0.005733f, 0.703277f)),
                    new FPKeyframe(0.43f, new Vec3(0.44005, -0.12235, 0.05788), new Quaternionf(-0.528346f, -0.476319f, 0.004979f, 0.702813f)),
                    new FPKeyframe(0.45f, new Vec3(0.44005, -0.12235, 0.06175), new Quaternionf(-0.528342f, -0.476974f, 0.004217f, 0.702376f)),
                    new FPKeyframe(0.47f, new Vec3(0.44005, -0.12235, 0.06566), new Quaternionf(-0.528339f, -0.477587f, 0.003449f, 0.701966f)),
                    new FPKeyframe(0.48f, new Vec3(0.44005, -0.12235, 0.06959), new Quaternionf(-0.528335f, -0.478160f, 0.002677f, 0.701582f)),
                    new FPKeyframe(0.50f, new Vec3(0.44005, -0.12235, 0.07352), new Quaternionf(-0.528332f, -0.478695f, 0.001904f, 0.701223f)),
                    new FPKeyframe(0.52f, new Vec3(0.44005, -0.12235, 0.07746), new Quaternionf(-0.528328f, -0.479193f, 0.001130f, 0.700887f)),
                    new FPKeyframe(0.53f, new Vec3(0.44005, -0.12235, 0.08139), new Quaternionf(-0.528324f, -0.479654f, 0.000358f, 0.700575f)),
                    new FPKeyframe(0.55f, new Vec3(0.44005, -0.12235, 0.08529), new Quaternionf(-0.528319f, -0.480081f, -0.000410f, 0.700286f)),
                    new FPKeyframe(0.57f, new Vec3(0.44005, -0.12235, 0.08917), new Quaternionf(-0.528315f, -0.480475f, -0.001172f, 0.700018f)),
                    new FPKeyframe(0.58f, new Vec3(0.44005, -0.12235, 0.09301), new Quaternionf(-0.528310f, -0.480837f, -0.001926f, 0.699772f)),
                    new FPKeyframe(0.60f, new Vec3(0.44005, -0.12235, 0.09679), new Quaternionf(-0.528306f, -0.481168f, -0.002670f, 0.699545f)),
                    new FPKeyframe(0.62f, new Vec3(0.44005, -0.12235, 0.10052), new Quaternionf(-0.528301f, -0.481470f, -0.003402f, 0.699337f)),
                    new FPKeyframe(0.63f, new Vec3(0.44005, -0.12235, 0.10418), new Quaternionf(-0.528297f, -0.481745f, -0.004120f, 0.699148f)),
                    new FPKeyframe(0.65f, new Vec3(0.44005, -0.12235, 0.10775), new Quaternionf(-0.528292f, -0.481993f, -0.004823f, 0.698976f)),
                    new FPKeyframe(0.67f, new Vec3(0.44005, -0.12235, 0.11124), new Quaternionf(-0.528287f, -0.482215f, -0.005508f, 0.698821f)),
                    new FPKeyframe(0.68f, new Vec3(0.44005, -0.12235, 0.11462), new Quaternionf(-0.528283f, -0.482414f, -0.006173f, 0.698681f)),
                    new FPKeyframe(0.70f, new Vec3(0.44005, -0.12235, 0.11789), new Quaternionf(-0.528279f, -0.482590f, -0.006816f, 0.698557f)),
                    new FPKeyframe(0.72f, new Vec3(0.44005, -0.12235, 0.12105), new Quaternionf(-0.528274f, -0.482746f, -0.007436f, 0.698446f)),
                    new FPKeyframe(0.73f, new Vec3(0.44005, -0.12235, 0.12407), new Quaternionf(-0.528270f, -0.482881f, -0.008030f, 0.698349f)),
                    new FPKeyframe(0.75f, new Vec3(0.44005, -0.12235, 0.12695), new Quaternionf(-0.528266f, -0.482998f, -0.008596f, 0.698265f)),
                    new FPKeyframe(0.77f, new Vec3(0.44005, -0.12235, 0.12968), new Quaternionf(-0.528262f, -0.483099f, -0.009133f, 0.698191f)),
                    new FPKeyframe(0.78f, new Vec3(0.44005, -0.12235, 0.13225), new Quaternionf(-0.528259f, -0.483183f, -0.009638f, 0.698129f)),
                    new FPKeyframe(0.80f, new Vec3(0.44005, -0.12235, 0.13465), new Quaternionf(-0.528255f, -0.483253f, -0.010109f, 0.698076f)),
                    new FPKeyframe(0.82f, new Vec3(0.44005, -0.12235, 0.13687), new Quaternionf(-0.528252f, -0.483310f, -0.010545f, 0.698033f)),
                    new FPKeyframe(0.83f, new Vec3(0.44005, -0.12235, 0.13890), new Quaternionf(-0.528249f, -0.483355f, -0.010944f, 0.697997f)),
                    new FPKeyframe(0.85f, new Vec3(0.44005, -0.12235, 0.14073), new Quaternionf(-0.528247f, -0.483390f, -0.011303f, 0.697969f)),
                    new FPKeyframe(0.87f, new Vec3(0.44005, -0.12235, 0.14234), new Quaternionf(-0.528245f, -0.483416f, -0.011620f, 0.697948f)),
                    new FPKeyframe(0.88f, new Vec3(0.44005, -0.12235, 0.14374), new Quaternionf(-0.528243f, -0.483434f, -0.011894f, 0.697932f)),
                    new FPKeyframe(0.90f, new Vec3(0.44005, -0.12235, 0.14490), new Quaternionf(-0.528241f, -0.483447f, -0.012123f, 0.697921f)),
                    new FPKeyframe(0.92f, new Vec3(0.44005, -0.12235, 0.14582), new Quaternionf(-0.528240f, -0.483454f, -0.012304f, 0.697914f)),
                    new FPKeyframe(0.93f, new Vec3(0.44005, -0.12235, 0.14650), new Quaternionf(-0.528239f, -0.483457f, -0.012436f, 0.697910f)),
                    new FPKeyframe(0.95f, new Vec3(0.44005, -0.12235, 0.14691), new Quaternionf(-0.528238f, -0.483458f, -0.012517f, 0.697908f)),
                    new FPKeyframe(0.97f, new Vec3(0.44005, -0.12235, 0.14705), new Quaternionf(-0.528238f, -0.483458f, -0.012544f, 0.697908f)),
                    new FPKeyframe(0.98f, new Vec3(0.22002, -0.06117, 0.07352), new Quaternionf(-0.286654f, -0.262353f, -0.006807f, 0.921387f)),
                    new FPKeyframe(1.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f))
        ));

        // left_brambles - exported from Blender
        ANIMATION_KEYFRAMES.put("left_brambles", List.of(
                    new FPKeyframe(0.00f, new Vec3(0.00000, 0.00000, -0.00000), new Quaternionf(0.000000f, 0.000000f, -0.000000f, 1.000000f)),
                    new FPKeyframe(0.02f, new Vec3(-0.08213, 0.04614, -0.05429), new Quaternionf(-0.270029f, 0.000000f, -0.000000f, 0.962852f)),
                    new FPKeyframe(0.03f, new Vec3(-0.19016, 0.09228, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.05f, new Vec3(-0.20743, 0.09228, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.07f, new Vec3(-0.16346, 0.09228, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.08f, new Vec3(-0.19016, 0.14206, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.10f, new Vec3(-0.19016, 0.09228, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.12f, new Vec3(-0.19016, 0.12704, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.13f, new Vec3(-0.20429, 0.07532, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.15f, new Vec3(-0.23724, 0.13426, -0.10858), new Quaternionf(-0.476968f, 0.000000f, -0.000000f, 0.878921f)),
                    new FPKeyframe(0.17f, new Vec3(-0.19016, 0.09228, -0.03018), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.18f, new Vec3(-0.19016, 0.13426, -0.15744), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.20f, new Vec3(-0.20487, 0.07778, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.22f, new Vec3(-0.14529, 0.13426, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.23f, new Vec3(-0.19016, 0.09228, -0.15744), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.25f, new Vec3(-0.19016, 0.13426, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.27f, new Vec3(-0.19016, 0.08302, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.28f, new Vec3(-0.21351, 0.13426, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.30f, new Vec3(-0.19016, 0.09228, -0.10858), new Quaternionf(-0.519996f, 0.000000f, -0.000000f, 0.854169f)),
                    new FPKeyframe(0.32f, new Vec3(-0.10140, 0.09230, -0.08949), new Quaternionf(-0.462025f, -0.319766f, 0.046297f, 0.825917f)),
                    new FPKeyframe(0.33f, new Vec3(0.00385, 0.09245, -0.05383), new Quaternionf(-0.106693f, -0.914417f, 0.133899f, 0.366782f)),
                    new FPKeyframe(0.35f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.37f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.38f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.061997f, -0.985014f, 0.149999f, 0.058339f)),
                    new FPKeyframe(0.40f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.42f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.43f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.45f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.47f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.074315f, -0.976461f, 0.146381f, 0.139908f)),
                    new FPKeyframe(0.48f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.50f, new Vec3(0.08956, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.52f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.53f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.55f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.57f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.58f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.60f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.62f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.63f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.65f, new Vec3(0.03057, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.67f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.68f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.70f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.72f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.73f, new Vec3(0.05140, 0.13086, -0.09473), new Quaternionf(0.026046f, -0.984555f, 0.146197f, 0.092735f)),
                    new FPKeyframe(0.75f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.77f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.78f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.80f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.82f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.83f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.85f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.87f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.88f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.90f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.92f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.93f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.95f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(0.97f, new Vec3(0.05468, 0.13556, -0.03367), new Quaternionf(0.067494f, -0.981869f, 0.149648f, 0.094783f)),
                    new FPKeyframe(0.98f, new Vec3(0.05468, 0.09284, -0.03403), new Quaternionf(0.067515f, -0.982175f, 0.147597f, 0.094812f)),
                    new FPKeyframe(1.00f, new Vec3(0.05468, 0.09470, -0.02912), new Quaternionf(0.064780f, -0.977560f, 0.175580f, 0.096701f))
        ));

        // Left-hand animations reuse the same keyframe curves but render on OFF_HAND.
        ANIMATION_KEYFRAMES.put("left_sword_to_left", ANIMATION_KEYFRAMES.get("sword_to_left"));
        ANIMATION_KEYFRAMES.put("left_sword_to_right", ANIMATION_KEYFRAMES.get("sword_to_right"));
        ANIMATION_KEYFRAMES.put("left_sword_overhead", ANIMATION_KEYFRAMES.get("sword_overhead"));
        ANIMATION_KEYFRAMES.put("double_sword_overhead", ANIMATION_KEYFRAMES.get("sword_overhead"));
        ANIMATION_KEYFRAMES.put("beast2", ANIMATION_KEYFRAMES.get("sword_to_right"));
        ANIMATION_KEYFRAMES.put("breath_beast2", ANIMATION_KEYFRAMES.get("sword_to_left"));
        ANIMATION_KEYFRAMES.put("sprint", ANIMATION_KEYFRAMES.get("sword_none"));
        ANIMATION_KEYFRAMES.put("sprint_senior", ANIMATION_KEYFRAMES.get("sword_none"));

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
