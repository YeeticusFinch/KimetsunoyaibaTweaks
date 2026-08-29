package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps spawned demon slayer mobs from receiving permanent Resistance above III.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SlayerResistanceCapHandler {
    private static final int MAX_RESISTANCE_AMPLIFIER = 2; // Resistance III
    private static final int BASELINE_EFFECT_MIN_DURATION_TICKS = 20 * 60;

    private SlayerResistanceCapHandler() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        clampSpawnResistance(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            clampSpawnResistance(living);
        }
    }

    private static void clampSpawnResistance(LivingEntity entity) {
        if (entity instanceof Player || !Damager.isDemonSlayer(entity)) {
            return;
        }

        MobEffectInstance resistance = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (resistance == null || resistance.getAmplifier() <= MAX_RESISTANCE_AMPLIFIER) {
            return;
        }

        int duration = resistance.getDuration();
        if (duration != Integer.MAX_VALUE && duration < BASELINE_EFFECT_MIN_DURATION_TICKS) {
            return;
        }

        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        entity.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            duration,
            MAX_RESISTANCE_AMPLIFIER,
            resistance.isAmbient(),
            resistance.isVisible(),
            resistance.showIcon()));
    }
}
