package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwordDisplayConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Sword Display Configuration")
                .push("sword_display");
    }

    // Enable/disable sword display feature
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable displaying swords on player model when not actively held")
            .define("enabled", true);

    // Position preference (HIP or BACK) - this is now the default fallback
    private static final ForgeConfigSpec.EnumValue<SwordDisplayPosition> POSITION = BUILDER
            .comment("Default position for swords on the player model (HIP or BACK). Per-sword overrides below take precedence.")
            .defineEnum("default_position", SwordDisplayPosition.HIP);

    // Per-sword position overrides
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SWORD_POSITIONS = BUILDER
            .comment("Per-sword position overrides. Format: 'modid:itemname=POSITION' (e.g., 'kimetsunoyaiba:nichirinsword_uzui=BACK')")
            .defineList("sword_position_overrides",
                () -> {
                    List<String> defaults = new ArrayList<>();
                    defaults.add("kimetsunoyaiba:nichirinsword_uzui=BACK");
                    defaults.add("kimetsunoyaiba:nichirinsword_inosuke=HIP");
                    return defaults;
                },
                obj -> obj instanceof String && ((String) obj).contains("="));

    // Scale of displayed swords
    private static final ForgeConfigSpec.DoubleValue SCALE = BUILDER
            .comment("Scale of displayed swords (0.5 = half size, 1.0 = normal size, 2.0 = double size)")
            .defineInRange("scale", 1.0, 0.1, 5.0);

    // Additional scale multiplier for sheaths
    private static final ForgeConfigSpec.DoubleValue SHEATH_SCALE = BUILDER
            .comment("Additional scale multiplier for sheaths (applied on top of sword scale)",
                     "Example: sword scale=0.5, sheath scale=1.2 → sheath displays at 0.6 (0.5 * 1.2)")
            .defineInRange("sheath_scale", 1.0, 0.1, 5.0);

    // Enable/disable sheath rendering
    private static final ForgeConfigSpec.BooleanValue RENDER_SHEATHS = BUILDER
            .comment("Enable rendering sword sheaths on the hip/back")
            .define("render_sheaths", true);

    // Hip position settings
    static {
        BUILDER.comment("Hip Display Position Configuration")
                .push("hip_position");
    }

    // Left hip
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_X = BUILDER
            .comment("Left hip X translation")
            .defineInRange("left_translate_x", 0.3, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_Y = BUILDER
            .comment("Left hip Y translation")
            .defineInRange("left_translate_y", 0.55, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_TRANSLATE_Z = BUILDER
            .comment("Left hip Z translation")
            .defineInRange("left_translate_z", -0.1, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_Z = BUILDER
            .comment("Left hip Z rotation (degrees)")
            .defineInRange("left_rotate_z", 0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_Y = BUILDER
            .comment("Left hip Y rotation (degrees)")
            .defineInRange("left_rotate_y", 180, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_LEFT_ROTATE_X = BUILDER
            .comment("Left hip X rotation (degrees)")
            .defineInRange("left_rotate_x", -65.0, -360.0, 360.0);

    // Right hip
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_X = BUILDER
            .comment("Right hip X translation")
            .defineInRange("right_translate_x", -0.3, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_Y = BUILDER
            .comment("Right hip Y translation")
            .defineInRange("right_translate_y", 0.55, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_TRANSLATE_Z = BUILDER
            .comment("Right hip Z translation")
            .defineInRange("right_translate_z", -0.1, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_Z = BUILDER
            .comment("Right hip Z rotation (degrees)")
            .defineInRange("right_rotate_z", 0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_Y = BUILDER
            .comment("Right hip Y rotation (degrees)")
            .defineInRange("right_rotate_y", 180, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue HIP_RIGHT_ROTATE_X = BUILDER
            .comment("Right hip Z rotation (degrees)")
            .defineInRange("right_rotate_x", -65.0, -360.0, 360.0);

    static {
        BUILDER.pop(); // hip_position
    }

    // Back position settings
    static {
        BUILDER.comment("Back Display Position Configuration")
                .push("back_position");
    }

    // Left back
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_X = BUILDER
            .comment("Left back X translation")
            .defineInRange("left_translate_x", 0.3, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_Y = BUILDER
            .comment("Left back Y translation")
            .defineInRange("left_translate_y", -0.1, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_TRANSLATE_Z = BUILDER
            .comment("Left back Z translation")
            .defineInRange("left_translate_z", 0.2, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_Z = BUILDER
            .comment("Left back Z rotation (degrees)")
            .defineInRange("left_rotate_z", 35, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_Y = BUILDER
            .comment("Left back Y rotation (degrees)")
            .defineInRange("left_rotate_y", 90.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_LEFT_ROTATE_X = BUILDER
            .comment("Left back X rotation (degrees)")
            .defineInRange("left_rotate_x", 0.0, -360.0, 360.0);

    // Right back
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_X = BUILDER
            .comment("Right back X translation")
            .defineInRange("right_translate_x", -0.3, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_Y = BUILDER
            .comment("Right back Y translation")
            .defineInRange("right_translate_y", -0.1, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_TRANSLATE_Z = BUILDER
            .comment("Right back Z translation")
            .defineInRange("right_translate_z", 0.2, -2.0, 2.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_Z = BUILDER
            .comment("Right back Z rotation (degrees)")
            .defineInRange("right_rotate_z", -35.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_Y = BUILDER
            .comment("Right back Y rotation (degrees)")
            .defineInRange("right_rotate_y", -90.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue BACK_RIGHT_ROTATE_X = BUILDER
            .comment("Right back X rotation (degrees)")
            .defineInRange("right_rotate_x", 0.0, -360.0, 360.0);

    static {
        BUILDER.pop(); // back_position
        BUILDER.pop(); // sword_display
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static SwordDisplayPosition position;  // Default position
    public static double scale;
    public static double sheathScale;
    public static boolean renderSheaths;

    // Per-sword position overrides (parsed from config)
    public static Map<String, SwordDisplayPosition> swordPositionOverrides = new HashMap<>();

    // Hip position values
    public static double hipLeftTranslateX;
    public static double hipLeftTranslateY;
    public static double hipLeftTranslateZ;
    public static double hipLeftRotateZ;
    public static double hipLeftRotateY;
    public static double hipLeftRotateX;
    public static double hipRightTranslateX;
    public static double hipRightTranslateY;
    public static double hipRightTranslateZ;
    public static double hipRightRotateZ;
    public static double hipRightRotateY;
    public static double hipRightRotateX;

    // Back position values
    public static double backLeftTranslateX;
    public static double backLeftTranslateY;
    public static double backLeftTranslateZ;
    public static double backLeftRotateZ;
    public static double backLeftRotateY;
    public static double backLeftRotateX;
    public static double backRightTranslateX;
    public static double backRightTranslateY;
    public static double backRightTranslateZ;
    public static double backRightRotateZ;
    public static double backRightRotateY;
    public static double backRightRotateX;

    public enum SwordDisplayPosition {
        HIP,
        BACK
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        Log.debug("SWORD DISPLAY CONFIG LOADING...");
        enabled = ENABLED.get();
        position = POSITION.get();
        scale = SCALE.get();
        sheathScale = SHEATH_SCALE.get();
        renderSheaths = RENDER_SHEATHS.get();

        // Load hip position values
        hipLeftTranslateX = HIP_LEFT_TRANSLATE_X.get();
        hipLeftTranslateY = HIP_LEFT_TRANSLATE_Y.get();
        hipLeftTranslateZ = HIP_LEFT_TRANSLATE_Z.get();
        hipLeftRotateZ = HIP_LEFT_ROTATE_Z.get();
        hipLeftRotateY = HIP_LEFT_ROTATE_Y.get();
        hipLeftRotateX = HIP_LEFT_ROTATE_X.get();
        hipRightTranslateX = HIP_RIGHT_TRANSLATE_X.get();
        hipRightTranslateY = HIP_RIGHT_TRANSLATE_Y.get();
        hipRightTranslateZ = HIP_RIGHT_TRANSLATE_Z.get();
        hipRightRotateZ = HIP_RIGHT_ROTATE_Z.get();
        hipRightRotateY = HIP_RIGHT_ROTATE_Y.get();
        hipRightRotateX = HIP_RIGHT_ROTATE_X.get();

        // Load back position values
        backLeftTranslateX = BACK_LEFT_TRANSLATE_X.get();
        backLeftTranslateY = BACK_LEFT_TRANSLATE_Y.get();
        backLeftTranslateZ = BACK_LEFT_TRANSLATE_Z.get();
        backLeftRotateZ = BACK_LEFT_ROTATE_Z.get();
        backLeftRotateY = BACK_LEFT_ROTATE_Y.get();
        backLeftRotateX = BACK_LEFT_ROTATE_X.get();
        backRightTranslateX = BACK_RIGHT_TRANSLATE_X.get();
        backRightTranslateY = BACK_RIGHT_TRANSLATE_Y.get();
        backRightTranslateZ = BACK_RIGHT_TRANSLATE_Z.get();
        backRightRotateZ = BACK_RIGHT_ROTATE_Z.get();
        backRightRotateY = BACK_RIGHT_ROTATE_Y.get();
        backRightRotateX = BACK_RIGHT_ROTATE_X.get();

        // Parse per-sword position overrides
        swordPositionOverrides.clear();
        List<? extends String> overrides = SWORD_POSITIONS.get();
        if (overrides != null) {
            for (String override : overrides) {
                String[] parts = override.split("=");
                if (parts.length == 2) {
                    String itemId = parts[0].trim();
                    String posStr = parts[1].trim().toUpperCase();
                    try {
                        SwordDisplayPosition pos = SwordDisplayPosition.valueOf(posStr);
                        swordPositionOverrides.put(itemId, pos);
                        Log.debug("Loaded sword position override: {} = {}", itemId, pos);
                    } catch (IllegalArgumentException e) {
                        Log.warn("Invalid position '{}' for sword '{}', ignoring", posStr, itemId);
                    }
                }
            }
        }

        Log.debug("Sword display config loaded: enabled=" + enabled +
                         ", defaultPosition=" + position + ", scale=" + scale +
                         ", sheathScale=" + sheathScale + ", renderSheaths=" + renderSheaths +
                         ", overrides=" + swordPositionOverrides.size());
    }

    /**
     * Gets the display position for a specific sword item
     * @param itemId The full item ID (e.g., "kimetsunoyaiba:nichirinsword_uzui")
     * @return The position for this sword (from override or default)
     */
    public static SwordDisplayPosition getPositionForSword(String itemId) {
        SwordDisplayPosition override = swordPositionOverrides.get(itemId);
        return override != null ? override : position;
    }
}
