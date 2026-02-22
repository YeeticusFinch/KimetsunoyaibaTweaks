package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for first-person sword swing animations
 */
public class FirstPersonSwordSwingConfig {

    public static ForgeConfigSpec.BooleanValue customSwingEnabled;

    // Counter Vanilla Swing settings
    public static ForgeConfigSpec.DoubleValue counterSwingRotateX;
    public static ForgeConfigSpec.DoubleValue counterSwingRotateY;
    public static ForgeConfigSpec.DoubleValue counterSwingRotateZ;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateX;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateY;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateZ;
    public static ForgeConfigSpec.DoubleValue counterSwingRotateX2;
    public static ForgeConfigSpec.DoubleValue counterSwingRotateY2;
    public static ForgeConfigSpec.DoubleValue counterSwingRotateZ2;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateX2;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateY2;
    public static ForgeConfigSpec.DoubleValue counterSwingTranslateZ2;
    public static ForgeConfigSpec.DoubleValue translateScale;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.comment("First-Person Sword Swing Animation Settings")
               .push("first_person_sword_swing");

        customSwingEnabled = builder
            .comment("Enable custom first-person swing animations for nichirin swords",
                     "Default: true")
            .define("customSwingEnabled", true);

        builder.comment("Counter Vanilla Swing - Adjusts how vanilla swing animation is reversed")
               .push("counter_vanilla_swing");

        counterSwingRotateX = builder
            .comment("X-axis rotation counter (pitch)",
                     "Adjusts how much vanilla's pitch swing is reversed",
                     "Default: 0.0")
            .defineInRange("rotateX", 0.0, -180.0, 180.0);
        
        counterSwingRotateX2 = builder
                .comment("X-axis square root rotation counter (pitch)",
                         "Adjusts how much vanilla's pitch swing is reversed",
                         "Default: 85.0")
                .defineInRange("rotateX2", 85.0, -180.0, 180.0);

        counterSwingRotateY = builder
            .comment("Y-axis rotation counter (yaw)",
                     "Adjusts how much vanilla's yaw swing is reversed",
                     "Default: 0.0")
            .defineInRange("rotateY", 0.0, -180.0, 180.0);
        
        counterSwingRotateY2 = builder
                .comment("Y-axis square root rotation counter (yaw)",
                         "Adjusts how much vanilla's yaw swing is reversed",
                         "Default: -23.0")
                .defineInRange("rotateY2", -23.0, -180.0, 180.0);

        counterSwingRotateZ = builder
            .comment("Z-axis rotation counter (roll)",
                     "Adjusts how much vanilla's roll swing is reversed",
                     "Default: 0.0")
            .defineInRange("rotateZ", 0.0, -180.0, 180.0);
        
        counterSwingRotateZ2 = builder
                .comment("Z-axis square root rotation counter (roll)",
                         "Adjusts how much vanilla's roll swing is reversed",
                         "Default: 25.0")
                .defineInRange("rotateZ2", 25.0, -180.0, 180.0);

        counterSwingTranslateX = builder
            .comment("X-axis translation counter",
                     "Adjusts how much vanilla's X translation is reversed",
                     "Default: 0.0")
            .defineInRange("translateX", 0.0, -2.0, 2.0);
        
        counterSwingTranslateX2 = builder
                .comment("X-axis square root translation counter",
                         "Adjusts how much vanilla's X translation is reversed",
                         "Default: -1.2")
                .defineInRange("translateX2", -1.2, -2.0, 2.0);

        counterSwingTranslateY = builder
            .comment("Y-axis translation counter",
                     "Adjusts how much vanilla's Y translation is reversed",
                     "Default: 0.0")
            .defineInRange("translateY", 0.0, -2.0, 2.0);
        
        counterSwingTranslateY2 = builder
                .comment("Y-axis square root translation counter",
                         "Adjusts how much vanilla's Y translation is reversed",
                         "Default: -1.05")
                .defineInRange("translateY2", -1.05, -2.0, 2.0);

        counterSwingTranslateZ = builder
            .comment("Z-axis translation counter",
                     "Adjusts how much vanilla's Z translation is reversed",
                     "Default: 0.5")
            .defineInRange("translateZ", 0.5, -2.0, 2.0);
        
        counterSwingTranslateZ2 = builder
                .comment("Z-axis square root translation counter",
                         "Adjusts how much vanilla's Z translation is reversed",
                         "Default: 0.0")
                .defineInRange("translateZ2", 0.0, -2.0, 2.0);
        
        translateScale = builder
                .comment("Translate Scale",
                         "Adjusts the translation scale for first person sword swing animations",
                         "Default: 1.3")
                .defineInRange("translateScale", 1.3, -64.0, 64.0);

        builder.pop(); // counter_vanilla_swing
        builder.pop(); // first_person_sword_swing
    }
}
