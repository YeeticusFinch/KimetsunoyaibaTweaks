package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Registry for demon slayer mark definitions.
 * Each mark type represents a different breathing style's mark.
 */
public class DemonSlayerMarkRegistry {

    /**
     * Love Breathing Demon Slayer Mark (Mitsuri Kanroji)
     * Attaches to the torso with a pink heart-shaped mark pattern.
     */
    public static final DemonSlayerMark LOVE_MARK = new DemonSlayerMark(
        "body",  // Attach to body/torso bone
        ResourceLocation.tryBuild("kimetsunoyaibamultiplayer", "demonslayermark_love"),
        new Vec3(0.0, 0.6, -0.1),   // Position offset (slightly up from center, forward)
        new Vec3(0, 0, 0),           // No rotation needed
        new Vec3(0.5, 0.5, 0.01)     // Scale down and make very flat (like a tattoo)
    ) {
        @Override
        public boolean shouldRender(LivingEntity entity) {
            // Only render if the entity has the mark activated
            // This will be checked via NBT or entity state
            if (entity.getPersistentData().contains("KanrojiMark")) {
                return entity.getPersistentData().getBoolean("KanrojiMark");
            }
            return false;
        }
    };

    /**
     * Mist Breathing Demon Slayer Mark (Muichiro Tokito)
     * This would attach to the face/head area.
     */
    public static final DemonSlayerMark MIST_MARK = new DemonSlayerMark(
        "head",  // Attach to head bone
        ResourceLocation.tryBuild("kimetsunoyaiba", "demonslayermark_mist"),  // From base mod
        new Vec3(0.0, 0.2, 0.1),     // Position on forehead area
        new Vec3(0, 0, 0),            // No rotation
        new Vec3(0.4, 0.4, 0.01)      // Small and flat
    ) {
        @Override
        public boolean shouldRender(LivingEntity entity) {
            // Check if entity has mist mark via NBT
            String markState = entity.getPersistentData().getString("MarkState");
            return "TRANSFORMED".equals(markState);
        }
    };

    // Add more marks as needed for other breathing styles...

    private DemonSlayerMarkRegistry() {
        // Private constructor to prevent instantiation
    }
}
