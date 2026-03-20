package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidTriggerHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        // Load player's breathing form data from NBT
        PlayerBreathingData.loadFromNBT(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        // Save player's breathing form data to NBT
        PlayerBreathingData.saveToNBT(player);
        // Clean up in-memory cache
        PlayerBreathingData.clear(player.getUUID());
        // Clean up demon slayer initiation tracking
        DemonSlayerInitiationHandler.onPlayerLogout(player.getUUID());
    }

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

        // Don't apply omen effects if target is a raid entity or source is near a raid
        if (RaidTriggerHandler.isRaidEntity(target)) {
            return;
        }
        if (source instanceof Player && RaidTriggerHandler.isPlayerNearRaid((Player) source)) {
            return;
        }

        if (source instanceof net.minecraft.server.level.ServerPlayer serverPlayer &&
            CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
            ResourceLocation targetId = net.minecraft.world.entity.EntityType.getKey(target.getType());
            if (targetId != null && DemonRegistry.isRegistered(targetId)) {
                DemonSlayerInitiationHandler.triggerCustomInitiation(serverPlayer, "killed addon demon " + targetId);
            }
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
