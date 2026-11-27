package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidStrengthItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Maintains the armor-bound Arachnid Strength effect on the server. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArachnidStrengthHandler {
    private static final int EFFECT_REFRESH_TICKS = 40;
    private static final int REFRESH_THRESHOLD_TICKS = 20;

    private ArachnidStrengthHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        try {
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            boolean equipped = ArachnidStrengthItem.isEquipped(event.getEntity());
            MobEffectInstance current = event.getEntity().getEffect(ModEffects.ARACHNID_STRENGTH.get());

            if (!equipped) {
                if (current != null) {
                    event.getEntity().removeEffect(ModEffects.ARACHNID_STRENGTH.get());
                }
                return;
            }

            if (current == null || current.getDuration() < REFRESH_THRESHOLD_TICKS
                || current.getAmplifier() != 0) {
                event.getEntity().addEffect(new MobEffectInstance(
                    ModEffects.ARACHNID_STRENGTH.get(), EFFECT_REFRESH_TICKS, 0,
                    false, false, true));
            }
        } catch (Exception exception) {
            System.err.println("Failed to maintain Arachnid Strength effect: " + exception.getMessage());
        }
    }
}
