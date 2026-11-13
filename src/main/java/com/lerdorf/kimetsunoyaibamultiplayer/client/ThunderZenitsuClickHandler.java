package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordZenitsu;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderZenitsuClickPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.HitResult;

/** Sends empty-space and block M1 clicks to the server for Zenitsu chains. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ThunderZenitsuClickHandler {
    private ThunderZenitsuClickHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
            && minecraft.player.getMainHandItem().getItem() instanceof NichirinSwordZenitsu
            && (minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS)) {
            ModNetworking.sendToServer(new ThunderZenitsuClickPacket());
        }
    }
}
