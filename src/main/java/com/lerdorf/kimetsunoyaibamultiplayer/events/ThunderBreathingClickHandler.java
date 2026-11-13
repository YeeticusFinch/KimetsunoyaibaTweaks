package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordZenitsu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Prevents a click during a Zenitsu chain from becoming a normal sword attack. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class ThunderBreathingClickHandler {
    private ThunderBreathingClickHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !(player.getMainHandItem().getItem() instanceof NichirinSwordZenitsu)) {
            return;
        }
        if (ThunderBreathingForms.handleZenitsuClick(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !(player.getMainHandItem().getItem() instanceof NichirinSwordZenitsu)) {
            return;
        }
        if (ThunderBreathingForms.handleZenitsuClick(player)) {
            event.setCanceled(true);
        }
    }
}
