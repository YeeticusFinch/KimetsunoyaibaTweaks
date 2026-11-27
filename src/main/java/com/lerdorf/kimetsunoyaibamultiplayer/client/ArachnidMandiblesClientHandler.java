package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidMandiblesItem;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ArachnidMandiblesAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends server requests for arachnid mandibles clicks that have no server click event. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArachnidMandiblesClientHandler {
    private ArachnidMandiblesClientHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ArachnidMandiblesItem.isEquipped(minecraft.player)) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            ModNetworking.sendToServer(new ArachnidMandiblesAttackPacket());
        } else if (!(hitResult instanceof EntityHitResult)
            && hitResult.getType() != HitResult.Type.BLOCK) {
            ModNetworking.sendToServer(new ArachnidMandiblesAttackPacket());
        }
    }
}
