package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.Biokineses;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BiokinesesM1AttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT)
public final class BiokinesesAttackClientHandler {
    private BiokinesesAttackClientHandler() {
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
            || !Biokineses.isBiokinesesItem(minecraft.player.getMainHandItem())) {
            return;
        }

        java.util.UUID excludedTargetId = minecraft.hitResult instanceof EntityHitResult entityHit
            ? entityHit.getEntity().getUUID() : null;
        ModNetworking.sendToServer(new BiokinesesM1AttackPacket(excludedTargetId));
    }
}
