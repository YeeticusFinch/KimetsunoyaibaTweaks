package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class SwordSwingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Sword Swing Configuration")
                .push("sword_swing");
    }

    // Enable/disable sword swing model
    private static final ForgeConfigSpec.BooleanValue USE_SWORD_SWING_MODEL = BUILDER
            .comment("Use 3D sword slash models instead of particles for sword swings (experimental)")
            .define("use-sword-swing-model", false);
    
    private static final ForgeConfigSpec.DoubleValue BREATHING_DISPLAY_SCALE = BUILDER
            .comment("Scale/size of the sword slash models")
            .defineInRange("breathing-display-scale", 0, 0.5, 5.0);


    static {
        BUILDER.pop(); // sword_swing
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean useSwordSwingModel;
    public static double swordSwingScale;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        System.out.println("SWORD SLASH MODEL CONFIG LOADING...");
        useSwordSwingModel = USE_SWORD_SWING_MODEL.get();
        swordSwingScale = BREATHING_DISPLAY_SCALE.get();

    }
}
