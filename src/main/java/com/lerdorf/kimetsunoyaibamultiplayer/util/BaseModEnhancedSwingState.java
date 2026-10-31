package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.world.entity.LivingEntity;

/** Tracks whether our combat path owns left-clicks for a base-mod sword. */
public final class BaseModEnhancedSwingState {
    private static final String NBT_ACTIVE = "knymp_base_mod_enhanced_swing";

    private BaseModEnhancedSwingState() {
    }

    public static boolean isActive(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(NBT_ACTIVE);
    }

    public static void setActive(LivingEntity entity, boolean active) {
        if (entity != null) {
            entity.getPersistentData().putBoolean(NBT_ACTIVE, active);
        }
    }
}
