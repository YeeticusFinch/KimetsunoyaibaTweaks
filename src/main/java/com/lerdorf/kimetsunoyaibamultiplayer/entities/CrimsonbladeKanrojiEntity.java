package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.Set;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Kanroji variant rendered with the Crimsonblade model and animations. */
public class CrimsonbladeKanrojiEntity extends KanrojiEntity {
    private static final String ANIMATION_NAMESPACE = "crimsonblade";
    private static final Set<String> CRIMSONBLADE_ANIMATIONS = Set.of(
        "backstep", "guard", "jump", "hit", "standing"
    );

    public CrimsonbladeKanrojiEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected String getIdleAnimationName() {
        return "standing";
    }

    @Override
    protected String getWalkAnimationName() {
        return "walk";
    }

    @Override
    protected String getSprintAnimationName() {
        return "run";
    }

    @Override
    protected String getAnimationNamespaceForMovement(String animation) {
        return ANIMATION_NAMESPACE;
    }

    @Override
    public void playGeckoAnimation(String animationName, int durationTicks) {
        String name = animationName;
        int separator = name == null ? -1 : name.indexOf(':');
        if (separator >= 0 && separator < name.length() - 1) {
            name = name.substring(separator + 1);
        }

        if (CRIMSONBLADE_ANIMATIONS.contains(name)) {
            super.playGeckoAnimation(ANIMATION_NAMESPACE + ":" + name, durationTicks);
        } else {
            super.playGeckoAnimation(animationName, durationTicks);
        }
    }
}
