package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import java.util.Set;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Nezuko variant rendered with the Crimsonblade models and animations. */
public class CrimsonbladeNezukoEntity extends NezukoEntity {
    private static final Set<String> CRIMSONBLADE_ANIMATIONS = Set.of(
        "kick_left", "backstep", "guard", "kick_right", "right_attack", "left_attack",
        "jump", "hit", "standing"
    );

    public CrimsonbladeNezukoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected String resolveIdleAnimation() {
        return "standing";
    }

    @Override
    protected String resolveWalkAnimation() {
        return "walk";
    }

    @Override
    protected String resolveSprintAnimation() {
        return "run";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return "run".equals(animation);
    }

    public boolean hasCrimsonbladeAnimation(String animation) {
        return CRIMSONBLADE_ANIMATIONS.contains(animation);
    }
}
