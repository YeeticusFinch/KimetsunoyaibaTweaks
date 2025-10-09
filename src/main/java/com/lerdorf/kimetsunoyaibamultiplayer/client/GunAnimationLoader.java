package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Gun animation system info Log
 * The animations are auto-loaded from biped.animation.json by PlayerAnimator
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GunAnimationLoader {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Log.info("Gun animation system initialized");
            Log.info("Animations will be auto-loaded from biped.animation.json:");
            Log.info("  - Rifle: idle_rifle, walk_rifle, shoot_rifle");
            Log.info("  - Pistol: idle_pistol, walk_pistol, shoot_pistol");
            Log.info("  - Minigun: idle_minigun, walk_minigun, shoot_minigun");
            Log.info("Animation layers:");
            Log.info("  - Layer 3000: Idle/Walk animations (base gun animations)");
            Log.info("  - Layer 4000: Shoot animations (plays on top, allows walking while shooting)");
        });
    }
}