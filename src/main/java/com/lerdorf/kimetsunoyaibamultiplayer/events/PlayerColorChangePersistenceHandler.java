package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.util.PlayerColorChangeStyleHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class PlayerColorChangePersistenceHandler {
    private PlayerColorChangePersistenceHandler() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerColorChangeStyleHelper.copyPersistentStyleData(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Log.startupProbeOnce("PlayerColorChangePersistenceHandler.onPlayerLogin.start");
        PlayerColorChangeStyleHelper.restoreAssignedColorChangeStyle(event.getEntity());
        Log.startupProbeOnce("PlayerColorChangePersistenceHandler.onPlayerLogin.end");
    }
}
