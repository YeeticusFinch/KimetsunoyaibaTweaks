package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;

import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for detecting Custom NPCs entities.
 */
public class CustomNPCHelper {

    private static final String CUSTOM_NPC_PACKAGE = "noppes.npcs";
    private static final String CUSTOM_NPC_NBT_TAG = "CustomNPCsData";

    // Cache for Custom NPCs mod loaded status
    private static Boolean customNPCsLoaded = null;

    /**
     * Check if the Custom NPCs mod is loaded
     * @return true if Custom NPCs is available
     */
    public static boolean isCustomNPCsModLoaded() {
        if (customNPCsLoaded == null) {
            try {
                // Try to load a Custom NPCs class
                Class.forName("noppes.npcs.api.entity.ICustomNpc");
                customNPCsLoaded = true;
            } catch (ClassNotFoundException e) {
                customNPCsLoaded = false;
            }
        }
        return customNPCsLoaded;
    }

    /**
     * Check if an entity is a Custom NPC
     * @param entity The entity to check
     * @return true if the entity is a Custom NPC
     */
    public static boolean isCustomNPC(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        // Check if Custom NPCs mod is loaded
        if (!isCustomNPCsModLoaded()) {
            return false;
        }

        // Check entity class name
        String className = entity.getClass().getName();
        if (className.contains(CUSTOM_NPC_PACKAGE)) {
            return true;
        }

        // Check for Custom NPCs NBT data
        if (entity.getPersistentData().contains(CUSTOM_NPC_NBT_TAG)) {
            return true;
        }

        // Additional check: Try to cast to ICustomNpc interface (if available)
        try {
            Class<?> npcInterface = Class.forName("noppes.npcs.api.entity.ICustomNpc");
            if (npcInterface.isInstance(entity)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Interface not available, skip this check
        }

        return false;
    }

    /**
     * Check if an entity is a Custom NPC and can use abilities
     * @param entity The entity to check
     * @return true if the entity is a Custom NPC and not dead
     */
    public static boolean canUseAbilities(LivingEntity entity) {
        return isCustomNPC(entity) && entity.isAlive() && !entity.isRemoved();
    }

    /**
     * Debug log information about an entity
     * @param entity The entity to log
     * @return Debug string with entity information
     */
    public static String getDebugInfo(LivingEntity entity) {
        if (entity == null) {
            return "Entity: null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Entity Class: ").append(entity.getClass().getName()).append("\n");
        sb.append("Is Custom NPC: ").append(isCustomNPC(entity)).append("\n");
        sb.append("Has CustomNPCsData NBT: ").append(entity.getPersistentData().contains(CUSTOM_NPC_NBT_TAG)).append("\n");
        sb.append("Is Alive: ").append(entity.isAlive()).append("\n");
        sb.append("Is Removed: ").append(entity.isRemoved()).append("\n");

        return sb.toString();
    }
}
