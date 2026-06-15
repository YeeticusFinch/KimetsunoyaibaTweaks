package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class KazumiSpawnGuardHandler {
    private static final double DUPLICATE_RADIUS = 50.0D;

    private KazumiSpawnGuardHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKazumiJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof KazumiEntity kazumi)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean duplicateNearby = serverLevel.getEntitiesOfClass(
            KazumiEntity.class,
            new AABB(kazumi.blockPosition()).inflate(DUPLICATE_RADIUS),
            existing -> existing != kazumi && existing.isAlive() && !existing.isRemoved()
        ).stream().findAny().isPresent();

        if (duplicateNearby) {
            event.setCanceled(true);
        }
    }
}
