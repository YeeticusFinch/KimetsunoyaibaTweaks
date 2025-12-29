package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tracks which entity is currently being rendered.
 * This is used to provide entity context to GeoItemRenderer instances,
 * which don't get the entity directly from Minecraft's rendering pipeline.
 */
public class EntityRenderContext {
    private static final ThreadLocal<LivingEntity> currentEntity = new ThreadLocal<>();

    /**
     * Set the entity currently being rendered.
     * Called at the start of entity rendering.
     */
    public static void setCurrentEntity(LivingEntity entity) {
        currentEntity.set(entity);
        //Log.debug("[EntityRenderContext] Set current entity: {}", entity.getName().getString());
    }

    /**
     * Get the entity currently being rendered.
     */
    public static LivingEntity getCurrentEntity() {
        return currentEntity.get();
    }

    /**
     * Clear the current entity.
     * Called at the end of entity rendering.
     */
    public static void clearCurrentEntity() {
        LivingEntity entity = currentEntity.get();
        //if (entity != null) {
        //    Log.debug("[EntityRenderContext] Cleared current entity: {}", entity.getName().getString());
        //}
        currentEntity.remove();
    }
}
