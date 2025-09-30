package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * Gun animation system info logger
 * The animations are auto-loaded from biped.animation.json by PlayerAnimator
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GunAnimationLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Gun animation system initialized");
            LOGGER.info("Animations will be auto-loaded from biped.animation.json:");
            LOGGER.info("  - Rifle: idle_rifle, walk_rifle, shoot_rifle");
            LOGGER.info("  - Minigun: idle_minigun, walk_minigun, shoot_minigun");
            LOGGER.info("Note: Pistol animations removed - pistol uses bow-style mechanics");
        });
    }
}