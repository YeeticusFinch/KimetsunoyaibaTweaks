package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Represents a single blood demon art form.
 */
public class BloodDemonArtForm {
    private final int formId;
    private final String name;
    private final String description;
    private final int cooldownSeconds;
    private final FormEffect effect;

    public BloodDemonArtForm(int formId, String name, String description, int cooldownSeconds, FormEffect effect) {
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
        return LocalizationHelper.bloodDemonArtFormKey(formId);
    }

    public Component getDisplayName() {
        return LocalizationHelper.bloodDemonArtForm(formId);
    }

    public String getDescription() {
        return description;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void execute(LivingEntity entity, Level level) {
        if (PuppetryHandler.isAbilityUseBlocked(entity)) {
            return;
        }
        effect.execute(entity, level, formId);
    }

    public interface FormEffect {
        void execute(LivingEntity entity, Level level, int formId);
    }
}
