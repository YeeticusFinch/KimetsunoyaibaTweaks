package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;

import net.minecraft.util.RandomSource;

/**
 * Handles weighted form selection for NPC abilities.
 * Lower forms (Form 1, 2, 3) are more likely to be selected.
 */
public class FormSelector {

    /**
     * Select a breathing form using weighted probability.
     * Lower forms are more likely to be selected.
     *
     * @param random RandomSource instance
     * @param totalForms Total number of forms available (e.g., 7 for Mist Breathing)
     * @return Selected form index (0-based, so 0 = Form 1, 1 = Form 2, etc.)
     */
    public static int selectWeightedForm(RandomSource random, int totalForms) {
        if (totalForms <= 0) {
            return 0;
        }

        if (totalForms == 1) {
            return 0;
        }

        // Calculate total weight
        double totalWeight = 0.0;
        for (int i = 0; i < totalForms; i++) {
            totalWeight += CustomNPCConfig.getFormWeight(i, totalForms);
        }

        // Roll random value
        double roll = random.nextDouble() * totalWeight;

        // Find which form was selected
        double cumulative = 0.0;
        for (int i = 0; i < totalForms; i++) {
            cumulative += CustomNPCConfig.getFormWeight(i, totalForms);
            if (roll <= cumulative) {
                return i;
            }
        }

        // Fallback to last form (should never reach here)
        return totalForms - 1;
    }

    /**
     * Select a form with default weights (for testing/fallback).
     * Form 1: 40%, Form 2: 25%, Form 3: 15%, Form 4+: 20% (split evenly)
     *
     * @param random RandomSource instance
     * @param totalForms Total number of forms available
     * @return Selected form index (0-based)
     */
    public static int selectWeightedFormDefault(RandomSource random, int totalForms) {
        if (totalForms <= 0) {
            return 0;
        }

        if (totalForms == 1) {
            return 0;
        }

        double roll = random.nextDouble() * 100.0;

        if (roll < 40.0) {
            // 40% chance for Form 1
            return 0;
        } else if (roll < 65.0) {
            // 25% chance for Form 2
            return Math.min(1, totalForms - 1);
        } else if (roll < 80.0) {
            // 15% chance for Form 3
            return Math.min(2, totalForms - 1);
        } else {
            // 20% chance for Form 4+
            if (totalForms <= 3) {
                // No form 4+, use highest available
                return totalForms - 1;
            } else {
                // Randomly select from forms 4 and up
                int remainingForms = totalForms - 3;
                int selectedOffset = random.nextInt(remainingForms);
                return 3 + selectedOffset;
            }
        }
    }

    /**
     * Get a human-readable form name (e.g., "First Form", "Second Form")
     * @param formIndex 0-based form index
     * @return Form name
     */
    public static String getFormName(int formIndex) {
        String[] names = {
            "First Form",
            "Second Form",
            "Third Form",
            "Fourth Form",
            "Fifth Form",
            "Sixth Form",
            "Seventh Form",
            "Eighth Form",
            "Ninth Form",
            "Tenth Form",
            "Eleventh Form",
            "Twelfth Form",
            "Thirteenth Form"
        };

        if (formIndex >= 0 && formIndex < names.length) {
            return names[formIndex];
        } else {
            return "Form " + (formIndex + 1);
        }
    }

    /**
     * Debug method to print form selection probabilities
     * @param totalForms Total number of forms
     */
    public static void printWeightDistribution(int totalForms) {
        System.out.println("[KnY Custom NPCs] Form Weight Distribution for " + totalForms + " forms:");

        double totalWeight = 0.0;
        for (int i = 0; i < totalForms; i++) {
            totalWeight += CustomNPCConfig.getFormWeight(i, totalForms);
        }

        for (int i = 0; i < totalForms; i++) {
            double weight = CustomNPCConfig.getFormWeight(i, totalForms);
            double percentage = (weight / totalWeight) * 100.0;
            System.out.printf("  %s: %.1f%%\n", getFormName(i), percentage);
        }
    }
}
