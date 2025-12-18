package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for breathing form variations.
 *
 * Variations are ENABLED by default.
 * Add variation IDs to the blacklist to disable specific variations.
 *
 * Config file location: config/kimetsunoyaibamultiplayer/variations.toml
 */
public class VariationConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("Breathing Form Variations Configuration",
                       "Controls which breathing form variations are enabled")
               .push("variations");
    }

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VARIATION_BLACKLIST = BUILDER
            .comment("Blacklist of disabled variation IDs",
                    "Variations are ENABLED by default. Add IDs here to DISABLE specific variations.",
                    "",
                    "Variation ID format: \"styleId:formIndex:variationIndex\"",
                    "Examples:",
                    "  - \"mist_breathing:0:1\" = First variation of Mist Breathing First Form",
                    "  - \"mist_breathing:1:2\" = Second variation of Mist Breathing Second Form",
                    "",
                    "To disable all variations for a form, blacklist all indices:",
                    "  [\"mist_breathing:0:1\", \"mist_breathing:0:2\", \"mist_breathing:0:3\"]",
                    "",
                    "Default: [] (all variations enabled)")
            .defineListAllowEmpty(Arrays.asList("variationBlacklist"),
                                 () -> Arrays.asList(),
                                 obj -> obj instanceof String);

    static {
        BUILDER.pop(); // variations
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Cached blacklist set for fast lookups
    private static Set<String> blacklistedVariations = new HashSet<>();

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        List<? extends String> blacklist = VARIATION_BLACKLIST.get();
        blacklistedVariations = new HashSet<>(blacklist);

        Log.info("[Variation Config] Loaded:");
        Log.info("  - Blacklisted variations: " + blacklistedVariations.size());
        if (!blacklistedVariations.isEmpty()) {
            for (String varId : blacklistedVariations) {
                Log.info("    - " + varId);
            }
        }
    }

    /**
     * Check if a variation is blacklisted (disabled).
     *
     * @param variationId The variation ID to check
     * @return true if blacklisted (disabled), false if enabled
     */
    public static boolean isBlacklisted(String variationId) {
        return blacklistedVariations.contains(variationId);
    }

    /**
     * Check if a variation is enabled.
     *
     * @param variationId The variation ID to check
     * @return true if enabled, false if blacklisted
     */
    public static boolean isEnabled(String variationId) {
        return !isBlacklisted(variationId);
    }
}
