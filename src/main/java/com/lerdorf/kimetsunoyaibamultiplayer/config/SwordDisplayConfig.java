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
                    defaults.add("kimetsunoyaibamultiplayer:nichirinsword_sound=BACK");
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

    // Enable/disable draw/sheath animations
    private static final ForgeConfigSpec.BooleanValue DRAW_SHEATH_ANIMATIONS = BUILDER
            .comment("Enable draw and sheath animations when swords are drawn from or returned to sheaths")
            .define("draw_sheath_animations", true);

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
        BUILDER.comment("Entity Display Overrides (non-player)")
                .push("entity_display");
        BUILDER.comment("Back display overrides for entities")
                .push("back");
        BUILDER.comment("Translation overrides for entity sword/sheath display on back")
                .push("translation");
    }

    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_TRANSLATE_OFFSET_X = BUILDER
            .comment("Entity translation X offset (added after optional flip)")
            .defineInRange("offset_x", 0.0, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_TRANSLATE_OFFSET_Y = BUILDER
            .comment("Entity translation Y offset (added after optional flip)")
            .defineInRange("offset_y", 1.5, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_TRANSLATE_OFFSET_Z = BUILDER
            .comment("Entity translation Z offset (added after optional flip)")
            .defineInRange("offset_z", 0.0, -5.0, 5.0);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_TRANSLATE_FLIP_X = BUILDER
            .comment("Flip entity translation X (multiply base by -1)")
            .define("flip_x", false);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_TRANSLATE_FLIP_Y = BUILDER
            .comment("Flip entity translation Y (multiply base by -1)")
            .define("flip_y", true);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_TRANSLATE_FLIP_Z = BUILDER
            .comment("Flip entity translation Z (multiply base by -1)")
            .define("flip_z", false);

    static {
        BUILDER.pop(); // translation
        BUILDER.comment("Rotation overrides for entity sword/sheath display on back")
                .push("rotation");
    }

    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_ROTATE_OFFSET_X = BUILDER
            .comment("Entity rotation X offset in degrees (added after optional flip)")
            .defineInRange("offset_x", 180.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_ROTATE_OFFSET_Y = BUILDER
            .comment("Entity rotation Y offset in degrees (added after optional flip)")
            .defineInRange("offset_y", 0.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_BACK_ROTATE_OFFSET_Z = BUILDER
            .comment("Entity rotation Z offset in degrees (added after optional flip)")
            .defineInRange("offset_z", 0.0, -360.0, 360.0);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_ROTATE_FLIP_X = BUILDER
            .comment("Flip entity rotation X (multiply base by -1)")
            .define("flip_x", false);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_ROTATE_FLIP_Y = BUILDER
            .comment("Flip entity rotation Y (multiply base by -1)")
            .define("flip_y", true);
    private static final ForgeConfigSpec.BooleanValue ENTITY_BACK_ROTATE_FLIP_Z = BUILDER
            .comment("Flip entity rotation Z (multiply base by -1)")
            .define("flip_z", true);

    static {
        BUILDER.pop(); // rotation
        BUILDER.pop(); // back
        BUILDER.comment("Hip display overrides for entities")
                .push("hip");
        BUILDER.comment("Translation overrides for entity sword/sheath display on hip")
                .push("translation");
    }

    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_TRANSLATE_OFFSET_X = BUILDER
            .comment("Entity translation X offset (added after optional flip)")
            .defineInRange("offset_x", 0.0, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_TRANSLATE_OFFSET_Y = BUILDER
            .comment("Entity translation Y offset (added after optional flip)")
            .defineInRange("offset_y", 1.5, -5.0, 5.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_TRANSLATE_OFFSET_Z = BUILDER
            .comment("Entity translation Z offset (added after optional flip)")
            .defineInRange("offset_z", 0.0, -5.0, 5.0);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_TRANSLATE_FLIP_X = BUILDER
            .comment("Flip entity translation X (multiply base by -1)")
            .define("flip_x", false);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_TRANSLATE_FLIP_Y = BUILDER
            .comment("Flip entity translation Y (multiply base by -1)")
            .define("flip_y", true);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_TRANSLATE_FLIP_Z = BUILDER
            .comment("Flip entity translation Z (multiply base by -1)")
            .define("flip_z", false);

    static {
        BUILDER.pop(); // translation
        BUILDER.comment("Rotation overrides for entity sword/sheath display on hip")
                .push("rotation");
    }

    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_ROTATE_OFFSET_X = BUILDER
            .comment("Entity rotation X offset in degrees (added after optional flip)")
            .defineInRange("offset_x", 0.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_ROTATE_OFFSET_Y = BUILDER
            .comment("Entity rotation Y offset in degrees (added after optional flip)")
            .defineInRange("offset_y", 0.0, -360.0, 360.0);
    private static final ForgeConfigSpec.DoubleValue ENTITY_HIP_ROTATE_OFFSET_Z = BUILDER
            .comment("Entity rotation Z offset in degrees (added after optional flip)")
            .defineInRange("offset_z", 180.0, -360.0, 360.0);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_ROTATE_FLIP_X = BUILDER
            .comment("Flip entity rotation X (multiply base by -1)")
            .define("flip_x", false);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_ROTATE_FLIP_Y = BUILDER
            .comment("Flip entity rotation Y (multiply base by -1)")
            .define("flip_y", false);
    private static final ForgeConfigSpec.BooleanValue ENTITY_HIP_ROTATE_FLIP_Z = BUILDER
            .comment("Flip entity rotation Z (multiply base by -1)")
            .define("flip_z", false);

    static {
        BUILDER.pop(); // rotation
        BUILDER.pop(); // hip
        BUILDER.pop(); // entity_display
        BUILDER.pop(); // sword_display
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static SwordDisplayPosition position;  // Default position
    public static double scale;
    public static double sheathScale;
    public static boolean renderSheaths;
    public static boolean drawSheathAnimations;

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

    // Entity display overrides (non-player)
    public static double entityBackTranslateOffsetX;
    public static double entityBackTranslateOffsetY;
    public static double entityBackTranslateOffsetZ;
    public static boolean entityBackTranslateFlipX;
    public static boolean entityBackTranslateFlipY;
    public static boolean entityBackTranslateFlipZ;
    public static double entityBackRotateOffsetX;
    public static double entityBackRotateOffsetY;
    public static double entityBackRotateOffsetZ;
    public static boolean entityBackRotateFlipX;
    public static boolean entityBackRotateFlipY;
    public static boolean entityBackRotateFlipZ;
    public static double entityHipTranslateOffsetX;
    public static double entityHipTranslateOffsetY;
    public static double entityHipTranslateOffsetZ;
    public static boolean entityHipTranslateFlipX;
    public static boolean entityHipTranslateFlipY;
    public static boolean entityHipTranslateFlipZ;
    public static double entityHipRotateOffsetX;
    public static double entityHipRotateOffsetY;
    public static double entityHipRotateOffsetZ;
    public static boolean entityHipRotateFlipX;
    public static boolean entityHipRotateFlipY;
    public static boolean entityHipRotateFlipZ;

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
        drawSheathAnimations = DRAW_SHEATH_ANIMATIONS.get();

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

        // Load entity display overrides
        entityBackTranslateOffsetX = ENTITY_BACK_TRANSLATE_OFFSET_X.get();
        entityBackTranslateOffsetY = ENTITY_BACK_TRANSLATE_OFFSET_Y.get();
        entityBackTranslateOffsetZ = ENTITY_BACK_TRANSLATE_OFFSET_Z.get();
        entityBackTranslateFlipX = ENTITY_BACK_TRANSLATE_FLIP_X.get();
        entityBackTranslateFlipY = ENTITY_BACK_TRANSLATE_FLIP_Y.get();
        entityBackTranslateFlipZ = ENTITY_BACK_TRANSLATE_FLIP_Z.get();
        entityBackRotateOffsetX = ENTITY_BACK_ROTATE_OFFSET_X.get();
        entityBackRotateOffsetY = ENTITY_BACK_ROTATE_OFFSET_Y.get();
        entityBackRotateOffsetZ = ENTITY_BACK_ROTATE_OFFSET_Z.get();
        entityBackRotateFlipX = ENTITY_BACK_ROTATE_FLIP_X.get();
        entityBackRotateFlipY = ENTITY_BACK_ROTATE_FLIP_Y.get();
        entityBackRotateFlipZ = ENTITY_BACK_ROTATE_FLIP_Z.get();
        entityHipTranslateOffsetX = ENTITY_HIP_TRANSLATE_OFFSET_X.get();
        entityHipTranslateOffsetY = ENTITY_HIP_TRANSLATE_OFFSET_Y.get();
        entityHipTranslateOffsetZ = ENTITY_HIP_TRANSLATE_OFFSET_Z.get();
        entityHipTranslateFlipX = ENTITY_HIP_TRANSLATE_FLIP_X.get();
        entityHipTranslateFlipY = ENTITY_HIP_TRANSLATE_FLIP_Y.get();
        entityHipTranslateFlipZ = ENTITY_HIP_TRANSLATE_FLIP_Z.get();
        entityHipRotateOffsetX = ENTITY_HIP_ROTATE_OFFSET_X.get();
        entityHipRotateOffsetY = ENTITY_HIP_ROTATE_OFFSET_Y.get();
        entityHipRotateOffsetZ = ENTITY_HIP_ROTATE_OFFSET_Z.get();
        entityHipRotateFlipX = ENTITY_HIP_ROTATE_FLIP_X.get();
        entityHipRotateFlipY = ENTITY_HIP_ROTATE_FLIP_Y.get();
        entityHipRotateFlipZ = ENTITY_HIP_ROTATE_FLIP_Z.get();

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

    /**
     * Programmatically adds a sword position override
     * @param itemId The full item ID (e.g., "knyextraadditions:nichirinsword_forest")
     * @param position The position (HIP or BACK)
     */
    public static void addSwordPositionOverride(String itemId, SwordDisplayPosition position) {
        swordPositionOverrides.put(itemId, position);
        Log.info("Added sword position override: {} = {}", itemId, position);
    }

    /**
     * Per-sword position and rotation offsets
     * Maps sword item IDs to their custom offset values
     */
    public static class SwordOffsets {
        public double translateX = 0.0;
        public double translateY = 0.0;
        public double translateZ = 0.0;
        public double rotateX = 0.0;
        public double rotateY = 0.0;
        public double rotateZ = 0.0;

        public SwordOffsets() {}

        public SwordOffsets(double tx, double ty, double tz, double rx, double ry, double rz) {
            this.translateX = tx;
            this.translateY = ty;
            this.translateZ = tz;
            this.rotateX = rx;
            this.rotateY = ry;
            this.rotateZ = rz;
        }
    }

    // Per-sword custom offsets (added to base position offsets)
    private static Map<String, SwordOffsets> swordOffsets = new HashMap<>();

    /**
     * Registers custom position/rotation offsets for a specific sword
     * @param itemId The full item ID (e.g., "knyextraadditions:nichirinsword_forest")
     * @param offsets The custom offsets to apply
     */
    public static void registerSwordOffsets(String itemId, SwordOffsets offsets) {
        swordOffsets.put(itemId, offsets);
        Log.info("Registered custom offsets for sword: {}", itemId);
    }

    /**
     * Gets the custom offsets for a specific sword, or null if none are registered
     * @param itemId The full item ID
     * @return The custom offsets, or null if not registered
     */
    public static SwordOffsets getSwordOffsets(String itemId) {
        return swordOffsets.get(itemId);
    }

    /**
     * Checks if a sword has custom offsets registered
     * @param itemId The full item ID
     * @return true if custom offsets exist, false otherwise
     */
    public static boolean hasSwordOffsets(String itemId) {
        return swordOffsets.containsKey(itemId);
    }
}
