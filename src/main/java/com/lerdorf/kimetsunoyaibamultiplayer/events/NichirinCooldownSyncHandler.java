package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.util.NichirinCooldownHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mirrors the base mod's breathing cooldown effects onto all nichirin swords.
 * This keeps cooldown behavior consistent when players swap swords mid-cooldown.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class NichirinCooldownSyncHandler {

    private NichirinCooldownSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player == null || player.level().isClientSide()) {
            return;
        }

        int remainingCooldownTicks = KnYEffects.getRemainingBaseModCooldownTicks(player);
        if (remainingCooldownTicks > 0) {
            NichirinCooldownHelper.applyCooldownToAllNichirinSwords(player, remainingCooldownTicks);
        }
    }
}
