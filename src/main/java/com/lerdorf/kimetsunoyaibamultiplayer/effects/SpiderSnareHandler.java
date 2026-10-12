package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enforces the movement lock and line anchors for Spider Snare without changing gravity. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class SpiderSnareHandler {
    private static final String HAD_NO_GRAVITY_KEY = "KnYRuiSpiderSnareHadNoGravity";
    private static final int LINE_SYNC_INTERVAL_TICKS = 20;

    private SpiderSnareHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.hasEffect(ModEffects.SPIDER_SNARE.get())) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(HAD_NO_GRAVITY_KEY)) {
            data.putBoolean(HAD_NO_GRAVITY_KEY, entity.isNoGravity());
        }
        entity.setNoGravity(true);
        restoreLegacyGravityOverride(entity);
        MovementHelper.setVelocity(entity, 0.0D, 0.0D, 0.0D);
        entity.fallDistance = 0.0F;
        if (entity.tickCount % LINE_SYNC_INTERVAL_TICKS == 0) {
            PuppetryHandler.syncLineAnchors(entity);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance() == null
            || event.getEffectInstance().getEffect() != ModEffects.SPIDER_SNARE.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        restoreLegacyGravityOverride(entity);
        PuppetryHandler.syncLineAnchors(entity);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.SPIDER_SNARE.get()) {
            clear(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
            && event.getEffectInstance().getEffect() == ModEffects.SPIDER_SNARE.get()) {
            clear(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        clear(event.getEntity());
    }

    private static void clear(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        restoreLegacyGravityOverride(entity);
        if (!entity.hasEffect(ModEffects.PUPPETRY.get())
            && !entity.hasEffect(ModEffects.WEB_TRAVERSAL.get())) {
            PuppetryHandler.clearLineAnchors(entity);
        }
    }

    /** Migrates entities affected by the previous implementation's gravity override. */
    private static void restoreLegacyGravityOverride(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(HAD_NO_GRAVITY_KEY)) {
            return;
        }
        entity.setNoGravity(data.getBoolean(HAD_NO_GRAVITY_KEY));
        data.remove(HAD_NO_GRAVITY_KEY);
    }
}
