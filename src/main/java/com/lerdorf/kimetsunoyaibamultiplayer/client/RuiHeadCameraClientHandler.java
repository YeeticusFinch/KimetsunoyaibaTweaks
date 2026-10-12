package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiDecapitatedHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the local camera attached to the detached head for Decapitate Self. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class RuiHeadCameraClientHandler {
    private static int pendingHeadId = -1;
    private static boolean active;

    private RuiHeadCameraClientHandler() {
    }

    public static void handle(int headEntityId, boolean attach) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!attach) {
            pendingHeadId = -1;
            active = false;
            if (minecraft.player != null) {
                minecraft.setCameraEntity(minecraft.player);
            }
            return;
        }

        pendingHeadId = headEntityId;
        tryAttach(minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            pendingHeadId = -1;
            active = false;
            return;
        }
        if (pendingHeadId >= 0) {
            tryAttach(minecraft);
        }
        Entity cameraEntity = minecraft.getCameraEntity();
        if (active && (!(cameraEntity instanceof RuiDecapitatedHeadEntity)
            || cameraEntity.isRemoved())) {
            active = false;
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    private static void tryAttach(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || pendingHeadId < 0) {
            return;
        }
        Entity entity = minecraft.level.getEntity(pendingHeadId);
        if (entity instanceof RuiDecapitatedHeadEntity) {
            minecraft.setCameraEntity(entity);
            pendingHeadId = -1;
            active = true;
        }
    }
}
