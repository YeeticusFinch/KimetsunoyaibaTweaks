package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles cleanup hooks for Kanroji sword animation data.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KanrojiSwordAnimationCleanup {

    /**
     * No-op: animation state is stored on ItemStacks now.
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // Intentionally empty.
    }

    /**
     * No-op: animation state is stored on ItemStacks now.
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        // Intentionally empty.
    }
}
