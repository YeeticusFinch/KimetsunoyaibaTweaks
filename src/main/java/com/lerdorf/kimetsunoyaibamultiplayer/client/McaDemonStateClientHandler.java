package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.compat.MinecraftComesAliveCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class McaDemonStateClientHandler {
    private McaDemonStateClientHandler() {
    }

    public static void apply(int entityId, int eyesIndex, int hue) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof LivingEntity living) {
            living.getPersistentData().putBoolean(MinecraftComesAliveCompat.MCA_DEMON_TAG, true);
            living.getPersistentData().putInt(MinecraftComesAliveCompat.MCA_EYE_INDEX_TAG, eyesIndex);
            living.getPersistentData().putInt(MinecraftComesAliveCompat.MCA_EYE_HUE_TAG, hue);
        }
    }
}
