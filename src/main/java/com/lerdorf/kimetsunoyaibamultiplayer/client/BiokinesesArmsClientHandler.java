package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BiokinesesArmsSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client-only application of synchronized Biokineses arm state. */
public final class BiokinesesArmsClientHandler {
    private BiokinesesArmsClientHandler() {
    }

    public static void apply(BiokinesesArmsSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(packet.getEntityId());
        if (entity instanceof LivingEntity living) {
            living.getCapability(KimetsunoyaibaMultiplayer.BIOKINESES_ARMS)
                .ifPresent(data -> {
                    packet.applyTo(data);
                    if (living instanceof AbstractClientPlayer player) {
                        BiokinesesFloatAnimation.update(player, data.isClimbing());
                    }
                });
        }
    }
}
