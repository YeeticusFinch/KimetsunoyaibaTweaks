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

    /** Clears base-mod ability state that is shared on the player rather than the sword stack. */
    public static void resetTransientState(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        var data = entity.getPersistentData();
        data.putBoolean(NBT_ACTIVE, false);
        data.putBoolean("skill", false);
        data.putDouble("demon_art", 0.0D);
        data.putBoolean("ChangeArt", false);
        data.putBoolean("change_flag", false);
        data.putBoolean("guard", false);
        data.putBoolean("attack", false);
        data.putDouble("Damage", 0.0D);
        data.putDouble("cnt_x", 0.0D);
        for (int i = 1; i <= 10; i++) {
            data.putDouble("cnt" + i, 0.0D);
        }
    }
}
