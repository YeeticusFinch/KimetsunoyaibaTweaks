package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.mojang.logging.LogUtils;

/**
 * Wrapper that provides GeckoLib animation support for kasugai_crow entities
 * Since we can't modify the original entity class, we store animation data separately
 */
public class CrowAnimatableWrapper implements GeoAnimatable {
    private static final Map<UUID, CrowAnimatableWrapper> wrappers = new HashMap<>();
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Entity, CrowAnimatableWrapper> WRAPPER_CACHE = new HashMap<>();
    private static boolean hasLoggedController = false;

    private final Entity entity;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private CrowAnimatableWrapper(Entity entity) {
        this.entity = entity;
    }

    /**
     * Get or create a wrapper for the given crow entity
     */
    public static CrowAnimatableWrapper getOrCreate(Entity entity) {
        return wrappers.computeIfAbsent(entity.getUUID(), uuid -> new CrowAnimatableWrapper(entity));
    }

    /**
     * Remove wrapper when entity is removed
     */
    public static void remove(UUID entityId) {
        wrappers.remove(entityId);
    }

    /**
     * Clear all wrappers (on logout/world change)
     */
    public static void clearAll() {
    	WRAPPER_CACHE.clear();
        wrappers.clear();
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (!hasLoggedController && Config.logDebug) {
            LOGGER.info("=== REGISTERING ANIMATION CONTROLLER ===");
            hasLoggedController = true;
        }

        AnimationController<CrowAnimatableWrapper> controller = new AnimationController<>(
            this,
            "crow_controller",
            0, // No transition time for immediate response
            CrowAnimationController::crowAnimationPredicate
        );

        controllers.add(controller);
        
        LOGGER.info("Animation controller 'crow_controller' registered successfully");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return entity.tickCount;
    }
    
    /**
     * Clean up wrapper when entity is removed
     */
    public static void remove(Entity entity) {
        CrowAnimatableWrapper removed = WRAPPER_CACHE.remove(entity);
        if (removed != null && Config.logDebug) {
            LOGGER.debug("Removed wrapper for entity: {}", entity.getUUID());
        }
    }
    
    
}