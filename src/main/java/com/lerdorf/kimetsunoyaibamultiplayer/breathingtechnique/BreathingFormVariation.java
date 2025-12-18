package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a variation of a breathing form.
 *
 * Variations are alternative versions of breathing forms that can be registered
 * for specific swords. They completely replace the base form's effect and can have
 * different cooldowns.
 *
 * Form IDs:
 * - Base mod: 101-1699 (uses breathes value as form ID)
 *   - Water: 101, 102, 103, 104, 105, 106, 107, 111, 112, 113
 *   - Flame: 401, 402, 403, 404, 405, 409
 *   - Mist: 701, 702, 703, 704, 705, 707
 *   - Love: 1501, 1502, 1505, 1506
 * - Our mod: 20000+
 *   - Mist: 20001-20007
 *   - Love: 22001-22006
 *
 * Example: Register two variations for Water Second Form (ID 102):
 *   VariationRegistry.register(102, new BreathingFormVariation(...));
 *   VariationRegistry.register(102, new BreathingFormVariation(...));
 */
public class BreathingFormVariation {
    private final String name;                 // Display name (completely replaces form name)
    private final String description;          // Description of this variation
    private final int cooldownSeconds;         // Cooldown (can differ from base form)
    private final FormEffect effect;           // Effect (completely replaces base form effect)
    private final Set<String> applicableSwords; // Sword IDs this variation applies to

    /**
     * Create a breathing form variation.
     * The variation index is auto-assigned by the registry based on registration order.
     *
     * @param name Display name that completely replaces the base form name
     * @param description Description of this variation
     * @param cooldownSeconds Cooldown in seconds
     * @param effect Effect to execute (replaces base form effect completely)
     * @param applicableSwords Set of sword IDs this variation applies to (empty = all swords)
     */
    public BreathingFormVariation(
        String name,
        String description,
        int cooldownSeconds,
        FormEffect effect,
        Set<String> applicableSwords
    ) {
        this.name = name;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.effect = effect;
        this.applicableSwords = new HashSet<>(applicableSwords);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public FormEffect getEffect() {
        return effect;
    }

    public Set<String> getApplicableSwords() {
        return Collections.unmodifiableSet(applicableSwords);
    }

    /**
     * Check if this variation applies to a specific sword.
     *
     * @param swordId The sword ID to check
     * @return true if this variation applies to the sword (empty set = applies to all)
     */
    public boolean appliesTo(String swordId) {
        return applicableSwords.isEmpty() || applicableSwords.contains(swordId);
    }

    /**
     * Functional interface for variation effects.
     * Same signature as BreathingForm.FormEffect.
     *
     * The formId parameter is the ID of the base form this variation is replacing.
     * Use it for GuardStateHelper.setGuardState() - it will be automatically injected.
     */
    public interface FormEffect {
        /**
         * Execute this variation's effect.
         *
         * @param entity The entity executing the form (player or mob)
         * @param level The level/world
         * @param formId The form ID (automatically injected, use for GuardStateHelper)
         */
        void execute(LivingEntity entity, Level level, int formId);
    }
}
