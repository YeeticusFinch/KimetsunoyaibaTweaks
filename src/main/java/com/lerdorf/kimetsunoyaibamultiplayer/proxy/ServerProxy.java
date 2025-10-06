package com.lerdorf.kimetsunoyaibamultiplayer.proxy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Server-side proxy implementation
 * All methods are empty since server doesn't handle client rendering
 */
public class ServerProxy implements IClientProxy {

    @Override
    public void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword) {
        // Server doesn't handle client-side display
    }

    @Override
    public void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                    int animationLength, boolean isLooping, boolean stopAnimation,
                                    ItemStack swordItem, ResourceLocation particleType,
                                    float speed, int layerPriority) {
        // Server doesn't handle client-side animations
    }

    @Override
    public void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw) {
        // Server doesn't handle client-side camera rotation
    }

    @Override
    public void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                                   ParticleOptions particleType) {
        // Server doesn't spawn particles
    }
}
