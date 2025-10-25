package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Skip omen effect application if raids are disabled
        if (!RaidConfig.enableRaids.get()) {
            return;
        }

        LivingEntity target = event.getEntity();
        LivingEntity source = null;

        if (event.getSource().getEntity() instanceof LivingEntity) {
            source = (LivingEntity) event.getSource().getEntity();
        }

        // Check if the target was a demon slayer and the source is a demon
        if (source != null && Damager.isDemon(source) && Damager.isDemonSlayer(target)) {
            // Apply omen_of_ubuyashiki effect to the demon who killed the demon slayer
            // ambient=false, visible=true, showIcon=true - no sound effect
            source.addEffect(new MobEffectInstance(ModEffects.OMEN_OF_UBUYASHIKI.get(), 6000, 0, false, true, true));
        }
        // Check if the target was a demon and the source is not a demon (a human)
        else if (source != null && !Damager.isDemon(source) && Damager.isDemon(target)) {
            // Apply omen_of_muzan effect to the human who killed the demon (20% chance)
            if (Math.random() < 0.2) {
                // ambient=false, visible=true, showIcon=true - no sound effect
                source.addEffect(new MobEffectInstance(ModEffects.OMEN_OF_MUZAN.get(), 6000, 0, false, true, true));
            }
        }
    }
}