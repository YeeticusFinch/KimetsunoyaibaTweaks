package com.lerdorf.kimetsunoyaibamultiplayer.config;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration for Custom NPCs mod compatibility.
 * Allows NPCs holding nichirin swords or blood demon arts to use those abilities when attacking.
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomNPCConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Enable/Disable
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_NPC_COMPAT;

    // Cooldown Management
    public static final ForgeConfigSpec.DoubleValue NPC_COOLDOWN_MULTIPLIER;

    // Trigger Chance
    public static final ForgeConfigSpec.DoubleValue NPC_TRIGGER_CHANCE;

    // Debug Logging
    public static final ForgeConfigSpec.BooleanValue DEBUG_NPC_ABILITIES;

    // Ranged Abilities
    public static final ForgeConfigSpec.BooleanValue ENABLE_RANGED_ABILITIES;
    public static final ForgeConfigSpec.DoubleValue RANGED_ABILITY_RANGE;
    public static final ForgeConfigSpec.IntValue RANGED_ABILITY_CHECK_INTERVAL;

    // Attack Animations
    public static final ForgeConfigSpec.BooleanValue ENABLE_ATTACK_ANIMATIONS;

    // Maximum Cooldown Auto-Clear
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAX_COOLDOWN_CLEAR;
    public static final ForgeConfigSpec.IntValue MAX_COOLDOWN_SECONDS;

    // Form Selection Weights
    public static final ForgeConfigSpec.DoubleValue FORM_1_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue FORM_2_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue FORM_3_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue FORM_4_PLUS_WEIGHT;

    static {
        BUILDER.push("Custom NPCs Compatibility");

        ENABLE_CUSTOM_NPC_COMPAT = BUILDER
            .comment("Enable Custom NPCs mod compatibility.",
                    "When enabled, NPCs holding nichirin swords or blood demon arts will use those abilities when attacking.")
            .define("enableCustomNPCCompat", true);

        NPC_COOLDOWN_MULTIPLIER = BUILDER
            .comment("Cooldown multiplier for NPCs.",
                    "Multiplies the base cooldown of breathing forms and demon arts for NPCs.",
                    "1.0 = same as players, 2.0 = twice as long, 0.5 = half as long")
            .defineInRange("npcCooldownMultiplier", 1.0, 0.1, 10.0);

        NPC_TRIGGER_CHANCE = BUILDER
            .comment("Probability that an NPC will use an ability when attacking (if not on cooldown).",
                    "0.0 = never use abilities, 1.0 = always use abilities, 0.8 = 90% chance")
            .defineInRange("npcTriggerChance", 0.9, 0.0, 1.0);

        DEBUG_NPC_ABILITIES = BUILDER
            .comment("Enable debug logging for NPC ability usage.",
                    "Prints detailed information to console when NPCs trigger abilities.")
            .define("debugNPCAbilities", false);

        ENABLE_RANGED_ABILITIES = BUILDER
            .comment("Enable NPCs to use abilities from range (not just melee attacks).",
                    "When enabled, NPCs will use breathing forms when they have a target within range.")
            .define("enableRangedAbilities", true);

        RANGED_ABILITY_RANGE = BUILDER
            .comment("Maximum range for NPCs to trigger abilities (in blocks).",
                    "NPCs will use abilities when their attack target is within this range.")
            .defineInRange("rangedAbilityRange", 15.0, 0.0, 500.0);

        RANGED_ABILITY_CHECK_INTERVAL = BUILDER
            .comment("How often to check for ranged ability triggering (in ticks).",
                    "Lower values = more frequent checks but more processing.",
                    "20 ticks = 1 second, 40 ticks = 2 seconds")
            .defineInRange("rangedAbilityCheckInterval", 20, 5, 1000);

        ENABLE_ATTACK_ANIMATIONS = BUILDER
            .comment("Enable basic attack animations for NPCs (sword_to_left, sword_to_right, sword_overhead).",
                    "When enabled, NPCs will play random sword swing animations on normal attacks.")
            .define("enableAttackAnimations", true);

        ENABLE_MAX_COOLDOWN_CLEAR = BUILDER
            .comment("Enable automatic cooldown clearing after maximum time.",
                    "If enabled, all NPC cooldowns will be cleared after the max cooldown time passes.",
                    "This prevents cooldowns from getting stuck indefinitely.")
            .define("enableMaxCooldownClear", true);

        MAX_COOLDOWN_SECONDS = BUILDER
            .comment("Maximum cooldown time in seconds before auto-clearing all cooldowns.",
                    "After an NPC uses any breathing form, if this many seconds pass, all cooldowns are cleared.",
                    "This timer resets every time the NPC uses a breathing form.")
            .defineInRange("maxCooldownSeconds", 10, 1, 300);

        BUILDER.push("Form Selection Weights");
        BUILDER.comment("Weighted probability for form selection. Lower forms are used more often.",
                       "These weights determine how often each form tier is selected.",
                       "Higher values = more likely to be selected.");

        FORM_1_WEIGHT = BUILDER
            .comment("Weight for Form 1 (First Form)")
            .defineInRange("form1Weight", 40.0, 0.0, 100.0);

        FORM_2_WEIGHT = BUILDER
            .comment("Weight for Form 2 (Second Form)")
            .defineInRange("form2Weight", 25.0, 0.0, 100.0);

        FORM_3_WEIGHT = BUILDER
            .comment("Weight for Form 3 (Third Form)")
            .defineInRange("form3Weight", 15.0, 0.0, 100.0);

        FORM_4_PLUS_WEIGHT = BUILDER
            .comment("Weight for Form 4+ (Fourth Form and higher)",
                    "This weight is divided equally among all remaining forms")
            .defineInRange("form4PlusWeight", 20.0, 0.0, 100.0);

        BUILDER.pop();
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Check if Custom NPCs compatibility is enabled
     */
    public static boolean isEnabled() {
        return ENABLE_CUSTOM_NPC_COMPAT.get();
    }

    /**
     * Get the cooldown multiplier for NPCs
     */
    public static double getCooldownMultiplier() {
        return NPC_COOLDOWN_MULTIPLIER.get();
    }

    /**
     * Get the trigger chance (0.0 to 1.0)
     */
    public static double getTriggerChance() {
        return NPC_TRIGGER_CHANCE.get();
    }

    /**
     * Check if debug logging is enabled
     */
    public static boolean isDebugEnabled() {
        return DEBUG_NPC_ABILITIES.get();
    }

    /**
     * Check if ranged abilities are enabled
     */
    public static boolean isRangedAbilitiesEnabled() {
        return ENABLE_RANGED_ABILITIES.get();
    }

    /**
     * Get the range for triggering abilities (in blocks)
     */
    public static double getRangedAbilityRange() {
        return RANGED_ABILITY_RANGE.get();
    }

    /**
     * Get the interval between ranged ability checks (in ticks)
     */
    public static int getRangedAbilityCheckInterval() {
        return RANGED_ABILITY_CHECK_INTERVAL.get();
    }

    /**
     * Check if attack animations are enabled
     */
    public static boolean isAttackAnimationsEnabled() {
        return ENABLE_ATTACK_ANIMATIONS.get();
    }

    /**
     * Check if max cooldown auto-clear is enabled
     */
    public static boolean isMaxCooldownClearEnabled() {
        return ENABLE_MAX_COOLDOWN_CLEAR.get();
    }

    /**
     * Get the max cooldown time in seconds
     */
    public static int getMaxCooldownSeconds() {
        return MAX_COOLDOWN_SECONDS.get();
    }

    /**
     * Get form selection weight for the given form index (0-based)
     * @param formIndex The form index (0 = First Form, 1 = Second Form, etc.)
     * @param totalForms Total number of forms available
     * @return The weight for this form
     */
    public static double getFormWeight(int formIndex, int totalForms) {
        if (formIndex == 0) {
            return FORM_1_WEIGHT.get();
        } else if (formIndex == 1) {
            return FORM_2_WEIGHT.get();
        } else if (formIndex == 2) {
            return FORM_3_WEIGHT.get();
        } else {
            // Form 4+ weight is divided equally among remaining forms
            int remainingForms = Math.max(1, totalForms - 3);
            return FORM_4_PLUS_WEIGHT.get() / remainingForms;
        }
    }

    /**
     * Event handler for config loading/reloading
     */
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (isDebugEnabled()) {
            Log.debug("[KnY Custom NPCs] Config loaded!");
            Log.debug("  Enabled: " + isEnabled());
            Log.debug("  Cooldown Multiplier: " + getCooldownMultiplier());
            Log.debug("  Trigger Chance: " + (getTriggerChance() * 100) + "%");
            Log.debug("  Debug Logging: " + isDebugEnabled());
        }
    }
}
