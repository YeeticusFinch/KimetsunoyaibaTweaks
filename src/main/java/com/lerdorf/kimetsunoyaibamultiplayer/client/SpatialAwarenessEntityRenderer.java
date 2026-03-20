package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity highlighting behavior for Spatial Awareness.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", value = Dist.CLIENT)
public class SpatialAwarenessEntityRenderer {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final double GLOW_RANGE_BLOCKS = 20.0D;
    private static final double GLOW_RANGE_SQR = GLOW_RANGE_BLOCKS * GLOW_RANGE_BLOCKS;
    private static final int DEACTIVATION_CLEAR_FRAMES = 6;

    private static final Set<Integer> recentlyHighlightedEntities = new HashSet<>();
    private static int pendingClearFrames = 0;

    public static boolean shouldEntityGlow(Entity entity) {
        LocalPlayer player = MC.player;
        if (player == null || entity == null || entity == player) {
            return false;
        }

        if (!player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            return false;
        }

        boolean inRange = entity.distanceToSqr(SpatialAwarenessClientHandler.getFreeCameraPosition()) <= GLOW_RANGE_SQR;
        if (inRange) {
            recentlyHighlightedEntities.add(entity.getId());
        }
        return inRange;
    }

    public static int getGlowColor() {
        return 0xFFFFFF;
    }

    public static void onEffectDisabled() {
        if (!recentlyHighlightedEntities.isEmpty()) {
            pendingClearFrames = DEACTIVATION_CLEAR_FRAMES;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        if (pendingClearFrames <= 0) {
            return;
        }

        clearRecentlyHighlightedGlowTags();
        pendingClearFrames--;
        if (pendingClearFrames == 0) {
            recentlyHighlightedEntities.clear();
        }
    }

    private static void clearRecentlyHighlightedGlowTags() {
        if (MC.level == null || recentlyHighlightedEntities.isEmpty()) {
            return;
        }

        for (Integer entityId : recentlyHighlightedEntities) {
            Entity tracked = MC.level.getEntity(entityId);
            if (tracked instanceof LivingEntity living && !living.hasEffect(MobEffects.GLOWING)) {
                tracked.setGlowingTag(false);
            }
        }
    }
}
