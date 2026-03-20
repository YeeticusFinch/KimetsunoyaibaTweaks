package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BleedingFlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BleedingHandler {
    private static final String NEXT_MOVEMENT_BLEED_TICK_TAG = "BleedingNextMovementTick";
    private static final int NORMAL_MOVE_MIN_TICKS = 20;
    private static final int NORMAL_MOVE_MAX_TICKS = 40;
    private static final int FAST_MOVE_MIN_TICKS = 15;
    private static final int FAST_MOVE_MAX_TICKS = 25;
    private static final int BEAST_BLEED_DURATION_TICKS = 100;

    private BleedingHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        MobEffectInstance effect = target.getEffect(ModEffects.BLEEDING.get());
        if (effect == null) {
            return;
        }

        if (!isAttackDamage(event.getSource())) {
            return;
        }

        int bleedingLevel = effect.getAmplifier() + 1;
        event.setAmount(event.getAmount() + bleedingLevel);
        spawnBleedingParticles(target, bleedingLevel);
        sendBleedingFlash(target, bleedingLevel);
    }

    public static void tickBleedingMovement(LivingEntity entity, int bleedingLevel) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }

        long gameTime = entity.level().getGameTime();
        if (!isMoving(entity)) {
            entity.getPersistentData().remove(NEXT_MOVEMENT_BLEED_TICK_TAG);
            return;
        }

        if (!entity.getPersistentData().contains(NEXT_MOVEMENT_BLEED_TICK_TAG)) {
            entity.getPersistentData().putLong(NEXT_MOVEMENT_BLEED_TICK_TAG, gameTime + nextMovementDelay(entity));
            return;
        }

        long nextBleedTick = entity.getPersistentData().getLong(NEXT_MOVEMENT_BLEED_TICK_TAG);
        if (gameTime < nextBleedTick) {
            return;
        }

        applyBleedingDamage(entity, bleedingLevel);
        entity.getPersistentData().putLong(NEXT_MOVEMENT_BLEED_TICK_TAG, gameTime + nextMovementDelay(entity));
    }

    public static void applyBeastBleeding(LivingEntity target, boolean dualWield) {
        if (target == null || !target.isAlive()) {
            return;
        }

        int amplifier = dualWield ? 1 : 0;
        applyOrRefreshBleeding(target, BEAST_BLEED_DURATION_TICKS, amplifier);
    }

    public static void applyOrRefreshBleeding(LivingEntity target, int durationTicks, int amplifier) {
        if (target == null || durationTicks <= 0) {
            return;
        }

        MobEffectInstance current = target.getEffect(ModEffects.BLEEDING.get());
        int finalAmplifier = current == null ? amplifier : Math.max(current.getAmplifier(), amplifier);
        int finalDuration = current == null ? durationTicks : Math.max(current.getDuration(), durationTicks);
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), finalDuration, finalAmplifier, false, false, false));
    }

    private static void applyBleedingDamage(LivingEntity entity, int bleedingLevel) {
        DamageSource bleedSource = entity.damageSources().generic();
        if (entity.hurt(bleedSource, bleedingLevel)) {
            spawnBleedingParticles(entity, bleedingLevel);
            sendBleedingFlash(entity, bleedingLevel);
        }
    }

    private static boolean isAttackDamage(DamageSource source) {
        return source != null && (source.getEntity() != null || source.getDirectEntity() != null);
    }

    private static boolean isMoving(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        return movement.horizontalDistanceSqr() > 1.0E-2D;
    }

    private static int nextMovementDelay(LivingEntity entity) {
        boolean fastMoving = entity.isSprinting() || entity.getDeltaMovement().horizontalDistance() > 0.16D;
        int min = fastMoving ? FAST_MOVE_MIN_TICKS : NORMAL_MOVE_MIN_TICKS;
        int max = fastMoving ? FAST_MOVE_MAX_TICKS : NORMAL_MOVE_MAX_TICKS;
        return min + entity.level().random.nextInt(max - min + 1);
    }

    public static void spawnBleedingParticles(LivingEntity entity, int bleedingLevel) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int count = Mth.clamp(4 + bleedingLevel * 2, 4, 20);
        double width = entity.getBbWidth() * 0.35D;
        double height = entity.getBbHeight() * 0.35D;
        serverLevel.sendParticles(
            ModParticles.BLOOD.get(),
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.5D,
            entity.getZ(),
            count,
            width,
            height,
            width,
            0.12D
        );
    }

    private static void sendBleedingFlash(LivingEntity entity, int bleedingLevel) {
        if (entity instanceof ServerPlayer player) {
            ModNetworking.sendToPlayer(new BleedingFlashPacket(bleedingLevel), player);
        }
    }
}
