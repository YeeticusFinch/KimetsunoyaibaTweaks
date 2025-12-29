package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles cleanup of Kanroji sword animation tracking when entities are removed
 * or levels are unloaded.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KanrojiSwordAnimationCleanup {

    /**
     * Clean up animation tracking when an entity leaves the level.
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
            KanrojiSwordEntityAnimationTracker.clearAnimation(event.getEntity().getUUID());
        }
    }

    /**
     * Clean up all animation tracking when a level is unloaded.
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        KanrojiSwordEntityAnimationTracker.clearAll();
    }
}
