package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shared (logical client + server) gameplay enforcement for Spatial Awareness.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class SpatialAwarenessEffectHandler {

    private static final String LOCKED_KEY = "knymp_spatial_awareness_locked";
    private static final String LOCK_X_KEY = "knymp_spatial_awareness_lock_x";
    private static final String LOCK_Y_KEY = "knymp_spatial_awareness_lock_y";
    private static final String LOCK_Z_KEY = "knymp_spatial_awareness_lock_z";
    private static final String KNEEL_PLAYED_KEY = "knymp_spatial_awareness_kneel_played";

    private static final int KNEEL_LAYER_PRIORITY = 6500;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Log.startupProbeOnce("SpatialAwarenessEffectHandler.onPlayerTick");

        Player player = event.player;
        if (player == null || !player.isAlive()) {
            return;
        }

        boolean active = player.hasEffect(ModEffects.SPATIAL_AWARENESS.get());
        if (!active) {
            clearLock(player);
            return;
        }

        enforceLockedPosition(player);
        enforceKneelAnimation(player);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteractionIfSpatial(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteractionIfSpatial(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelInteractionIfSpatial(event.getEntity(), event);
    }

    private static void cancelInteractionIfSpatial(Player player, net.minecraftforge.eventbus.api.Event event) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModEffects.SPATIAL_AWARENESS.get())) {
            event.setCanceled(true);
        }
    }

    private static void enforceLockedPosition(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(LOCKED_KEY)) {
            data.putBoolean(LOCKED_KEY, true);
            data.putDouble(LOCK_X_KEY, player.getX());
            data.putDouble(LOCK_Y_KEY, player.getY());
            data.putDouble(LOCK_Z_KEY, player.getZ());
        }

        double lockX = data.getDouble(LOCK_X_KEY);
        double lockY = data.getDouble(LOCK_Y_KEY);
        double lockZ = data.getDouble(LOCK_Z_KEY);

        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.setSprinting(false);

        if (player.level().isClientSide()) {
            return;
        }

        if (player.distanceToSqr(lockX, lockY, lockZ) > 0.0004D) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(lockX, lockY, lockZ, player.getYRot(), player.getXRot());
            } else {
                player.setPos(lockX, lockY, lockZ);
            }
        }
    }

    private static void enforceKneelAnimation(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(KNEEL_PLAYED_KEY)) {
            return;
        }
        data.putBoolean(KNEEL_PLAYED_KEY, true);

        int duration = 80;
        MobEffectInstance inst = player.getEffect(ModEffects.SPATIAL_AWARENESS.get());
        if (inst != null) {
            duration = Math.max(20, inst.getDuration());
        }

        // One-shot kneel when Spatial Awareness begins.
        AnimationHelper.playAnimationOnLayer(player, "kimetsunoyaibamultiplayer:kneel", duration, 1.0f, KNEEL_LAYER_PRIORITY);
    }

    private static void clearLock(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(LOCKED_KEY)) {
            data.remove(KNEEL_PLAYED_KEY);
            return;
        }

        data.remove(LOCKED_KEY);
        data.remove(LOCK_X_KEY);
        data.remove(LOCK_Y_KEY);
        data.remove(LOCK_Z_KEY);
        data.remove(KNEEL_PLAYED_KEY);
    }
}
