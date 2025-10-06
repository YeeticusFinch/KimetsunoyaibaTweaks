package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.proxy.IClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Client-side proxy implementation
 * Only loaded on physical client
 */
public class ClientProxy implements IClientProxy {

    @Override
    public void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword) {
        if (Config.logDebug) {
            System.out.println("[DEBUG] Client received sword display sync for player " + playerUUID +
                ": left=" + (leftHipSword.isEmpty() ? "empty" : leftHipSword.getItem().toString()) +
                ", right=" + (rightHipSword.isEmpty() ? "empty" : rightHipSword.getItem().toString()));
        }

        SwordDisplayTracker.updateRemotePlayerDisplay(playerUUID, leftHipSword, rightHipSword);
    }

    @Override
    public void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                    int animationLength, boolean isLooping, boolean stopAnimation,
                                    ItemStack swordItem, ResourceLocation particleType,
                                    float speed, int layerPriority) {
        if (Config.logDebug) {
            System.out.println("[DEBUG] Client received animation sync for player " + playerUUID +
                ": animation=" + animationId + ", tick=" + currentTick + ", stop=" + stopAnimation +
                ", speed=" + speed + ", layer=" + layerPriority);
        }

        AnimationSyncHandler.handleAnimationSync(playerUUID, animationId, currentTick, animationLength,
                                                isLooping, stopAnimation, null, swordItem, particleType,
                                                speed, layerPriority);
    }

    @Override
    public void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerUUID);
            if (player != null) {
                // Always update the entity rotation
                player.setYRot(yaw);
                player.setXRot(pitch);
                player.setYHeadRot(headYaw);
                player.yRotO = yaw;
                player.xRotO = pitch;
                player.yHeadRotO = headYaw;

                // If this is the local client player, update the camera too
                if (player == mc.player) {
                    mc.player.setYRot(yaw);
                    mc.player.setXRot(pitch);
                    mc.player.setYHeadRot(headYaw);

                    mc.player.yRotO = yaw;
                    mc.player.xRotO = pitch;
                    mc.player.yHeadRotO = headYaw;

                    // Update ShoulderSurfing camera if present
                    com.lerdorf.kimetsunoyaibamultiplayer.compat.ShoulderSurfingCompat.setShoulderCameraRotation(yaw, pitch);
                }

                if (Config.logDebug) {
                    System.out.println("[DEBUG] Client received rotation sync for player " +
                        player.getName().getString() + ": yaw=" + yaw + ", pitch=" + pitch + ", headYaw=" + headYaw);
                }
            }
        }
    }

    @Override
    public void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                                   ParticleOptions particleType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            LivingEntity entity = (LivingEntity) mc.level.getEntity(entityUUID.hashCode());
            if (entity != null && particleType != null) {
                com.lerdorf.kimetsunoyaibamultiplayer.particles.BonePositionTracker.spawnRadialRibbonParticles(
                    entity, animationName, animationTick, particleType);
            }
        }
    }
}
