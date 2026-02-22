package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.VermilionEyeEffect;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FlowerPetalSlashEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles entity visibility and glowing effects for the Vermilion Eye effect.
 * Makes entities visible through walls with color-coded glowing based on threat level.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public class VermilionEyeEntityRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    // Track entities that should be glowing due to Vermilion Eye
    private static final Set<Integer> vermilionEyeGlowingEntities = new HashSet<>();
    // Short-lived history so we can forcibly clear stale glow right after effect deactivates.
    private static final Set<Integer> recentlyHighlightedEntities = new HashSet<>();
    private static final int DEACTIVATION_CLEAR_FRAMES = 6;
    private static int pendingClearFrames = 0;

    /**
     * Called before rendering an entity.
     * If Vermilion Eye is active and entity is in range, ensure it's set to glow.
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Check if player has Vermilion Eye effect
        if (!player.hasEffect(ModEffects.VERMILION_EYE.get())) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // Don't affect the player themselves
        if (entity == player) return;

        // Check if entity is within range
        double distance = player.distanceTo(entity);
        if (distance > VermilionEyeEffect.VISIBILITY_RANGE) {
            return;
        }

        // Mark this entity as glowing for Vermilion Eye
        vermilionEyeGlowingEntities.add(entity.getId());
        recentlyHighlightedEntities.add(entity.getId());
    }

    /**
     * Called after rendering an entity.
     * Clean up glow tracking.
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        // We don't remove from the set here to keep the glow active
        // The set is cleared periodically in the level stage event
    }

    /**
     * Called at the start of each render frame.
     * Updates the set of glowing entities and clears old entries.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null) {
            vermilionEyeGlowingEntities.clear();
            recentlyHighlightedEntities.clear();
            pendingClearFrames = 0;
            return;
        }

        // If player doesn't have Vermilion Eye, clear all glowing
        if (!player.hasEffect(ModEffects.VERMILION_EYE.get())) {
            vermilionEyeGlowingEntities.clear();

            // Force-clear stale glow for a few frames after effect removal.
            if (!recentlyHighlightedEntities.isEmpty()) {
                pendingClearFrames = DEACTIVATION_CLEAR_FRAMES;
            }
            if (pendingClearFrames > 0) {
                clearRecentlyHighlightedGlowTags();
                pendingClearFrames--;
                if (pendingClearFrames == 0) {
                    recentlyHighlightedEntities.clear();
                }
            }
            return;
        }

        pendingClearFrames = 0;

        // Clear the set - it will be repopulated in the next render cycle
        vermilionEyeGlowingEntities.clear();
    }

    private static void clearRecentlyHighlightedGlowTags() {
        if (mc.level == null || recentlyHighlightedEntities.isEmpty()) {
            return;
        }

        for (Integer entityId : recentlyHighlightedEntities) {
            Entity tracked = mc.level.getEntity(entityId);
            if (tracked instanceof LivingEntity living && !living.hasEffect(MobEffects.GLOWING)) {
                tracked.setGlowingTag(false);
            }
        }
    }

    public static boolean wasRecentlyHighlighted(Entity entity) {
        return entity != null && recentlyHighlightedEntities.contains(entity.getId());
    }

    /**
     * Check if an entity should be glowing due to Vermilion Eye.
     * This is called by the entity's isGlowing() method via mixin or hook.
     */
    public static boolean shouldEntityGlow(Entity entity) {
        try {
            if (mc == null || mc.player == null) return false;

            LocalPlayer player = mc.player;

            // Don't affect the player themselves
            if (entity == null || entity == player) return false;

            // Visual slash entities are not valid Vermilion Eye targets.
            if (entity instanceof FlowerPetalSlashEntity) {
                return false;
            }

            // Check if player has Vermilion Eye effect (with null safety)
            if (ModEffects.VERMILION_EYE == null || ModEffects.VERMILION_EYE.get() == null) {
                return false;
            }

            if (!player.hasEffect(ModEffects.VERMILION_EYE.get())) {
                return false;
            }

            // Check if entity is a living entity
            if (!(entity instanceof LivingEntity)) {
                return false;
            }

            // Check if entity is within range
            double distance = player.distanceTo(entity);
            return distance <= VermilionEyeEffect.VISIBILITY_RANGE;
        } catch (Exception e) {
            // Fail silently during early initialization
            return false;
        }
    }

    /**
     * Get the glow color for an entity affected by Vermilion Eye.
     * Returns the appropriate color based on threat level.
     *
     * Colors:
     * - Red (0xFF0000): Hostile/aggressive entities, players targeting you
     * - Blue (0x0088FF): Non-hostile players
     * - Yellow (0xFFFF00): Neutral mobs (monsters not currently aggro)
     * - Green (0x00FF00): Passive animals
     */
    public static int getGlowColor(Entity entity) {
        try {
            if (mc == null || mc.player == null) return 0xFFFFFF;

            LocalPlayer player = mc.player;
            return VermilionEyeOverlay.getEntityGlowColor(entity, player);
        } catch (Exception e) {
            return 0xFFFFFF; // White as fallback
        }
    }

    /**
     * Check if Vermilion Eye is currently active for the local player.
     */
    public static boolean isActive() {
        LocalPlayer player = mc.player;
        if (player == null) return false;
        return player.hasEffect(ModEffects.VERMILION_EYE.get());
    }
}
