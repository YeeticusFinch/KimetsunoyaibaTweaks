package com.lerdorf.kimetsunoyaibamultiplayer.proxy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Proxy interface for client-only operations
 * Server implementation will have empty methods
 */
public interface IClientProxy {

    /**
     * Handle sword display sync on client
     */
    void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword);

    /**
     * Handle animation sync on client
     */
    void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                            int animationLength, boolean isLooping, boolean stopAnimation,
                            ItemStack swordItem, ResourceLocation particleType,
                            float speed, int layerPriority);

    /**
     * Handle player rotation sync on client
     */
    void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw);

    /**
     * Spawn sword particles on client
     */
    void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                            ParticleOptions particleType);
}
