package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
import net.minecraft.network.chat.Component;

/**
 * Represents a single breathing form with its animations, particles, sounds, and effects
 */
public class BreathingForm {
    private final int formId;           // Unique form ID (20001-20007 for Mist, 22001-22006 for Love)
    private final String name;
    private final String description;
    private final int cooldownSeconds;
    private final FormEffect effect;

    public BreathingForm(int formId, String name, String description, int cooldownSeconds, FormEffect effect) {
        this.formId = formId;
        this.name = name;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.effect = effect;
    }

    public int getFormId() {
        return formId;
    }

    public String getName() {
        return name;
    }

    public String getTranslationKey() {
        return LocalizationHelper.breathingFormKey(formId);
    }

    public Component getDisplayName() {
        return LocalizationHelper.breathingForm(formId);
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

    /**
     * Execute this form's effect with automatic form ID injection.
     * This allows GuardStateHelper to automatically use the correct form ID.
     */
    public void execute(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.level.Level level) {
        effect.execute(entity, level, this.formId);
    }

    /**
     * Interface for form effect execution
     * Works with any LivingEntity (players, mobs, custom entities)
     *
     * The formId parameter is automatically injected - use it for GuardStateHelper.setGuardState()
     * This way you only specify the form ID once in the BreathingForm constructor.
     */
    public interface FormEffect {
        void execute(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.level.Level level, int formId);
    }
}
