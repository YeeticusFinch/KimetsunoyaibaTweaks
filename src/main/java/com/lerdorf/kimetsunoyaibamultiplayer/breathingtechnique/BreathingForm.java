package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

/**
 * Represents a single breathing form with its animations, particles, sounds, and effects
 */
public class BreathingForm {
    private final String name;
    private final String description;
    private final int cooldownSeconds;
    private final FormEffect effect;

    public BreathingForm(String name, String description, int cooldownSeconds, FormEffect effect) {
        this.name = name;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
        this.effect = effect;
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

    /**
     * Interface for form effect execution
     */
    public interface FormEffect {
        void execute(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level);
    }
}
