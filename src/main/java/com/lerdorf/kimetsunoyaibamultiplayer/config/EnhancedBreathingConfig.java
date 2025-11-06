package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration for Enhanced Breathing Forms
 *
 * This config controls whether enhanced versions of breathing techniques should be enabled.
 * When enabled, base mod nichirin swords are automatically replaced with enhanced versions
 * that have improved/additional breathing forms.
 *
 * Config file location: config/kimetsunoyaibamultiplayer/enhanced-breathing.toml
 */
public class EnhancedBreathingConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Enhanced Breathing Forms Configuration",
                       "Controls which breathing styles use enhanced versions with improved forms")
               .push("enhanced_breathing");
    }

    // Mist Breathing Enhancement
    private static final ForgeConfigSpec.BooleanValue ENHANCED_MIST_BREATHING = BUILDER
            .comment("Enable enhanced Mist Breathing forms",
                    "When true, automatically replaces base mod mist swords with enhanced versions:",
                    "  - kimetsunoyaiba:nichirinsword_mist -> kimetsunoyaibamultiplayer:nichirinsword_mist",
                    "  - kimetsunoyaiba:nichirinsword_tokito -> kimetsunoyaibamultiplayer:nichirinsword_muichiro",
                    "",
                    "Enhanced features:",
                    "  - 7th Form: Obscuring Clouds (Muichiro's original technique)",
                    "  - Improved particle effects and animations",
                    "  - Custom sword slash models",
                    "",
                    "Default: true")
            .define("enhancedMistBreathing", true);

    static {
        BUILDER.pop(); // enhanced_breathing
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Public static fields for easy access
    public static boolean enhancedMistBreathing;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enhancedMistBreathing = ENHANCED_MIST_BREATHING.get();

        System.out.println("[Enhanced Breathing Config] Loaded:");
        System.out.println("  - Enhanced Mist Breathing: " + enhancedMistBreathing);
    }
}
