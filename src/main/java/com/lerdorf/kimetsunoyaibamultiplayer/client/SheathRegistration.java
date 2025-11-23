package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.SheathItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers custom sheaths for specific swords during client initialization
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SheathRegistration {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerCustomSheaths();
        });
    }

    /**
     * Registers custom sheaths for specific swords.
     * Runs on the client setup thread to ensure proper initialization order.
     */
    private static void registerCustomSheaths() {
    try {
        // Set the default sheath for all swords
        SwordSheathRegistry.setDefaultSheath(SheathItems.SWORD_SHEATH.get());

        // Register kanroji sheath for our mod's kanroji sword
        SwordSheathRegistry.registerPersistentSheath(
            ModItems.NICHIRINSWORD_KANROJI.get(),
            SheathItems.SWORD_SHEATH_KANROJI.get()
        );

        // Register kanroji sheath for the base mod's kanroji sword (if it exists)
        net.minecraft.world.item.Item baseModKanrojiSword =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "nichirinsword_kanroji"));

        if (baseModKanrojiSword != null) {
            SwordSheathRegistry.registerPersistentSheath(
                baseModKanrojiSword,
                SheathItems.SWORD_SHEATH_KANROJI.get()
            );
            if (Config.logDebug) {
                Log.info("Registered kanroji sheath for base mod's nichirinsword_kanroji");
            }
        }

        // Register rengoku sheath for the base mod's rengoku sword (if it exists)
        net.minecraft.world.item.Item baseModRengokuSword =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "nichirinsword_rengoku"));

        if (baseModRengokuSword != null) {
            SwordSheathRegistry.registerPersistentSheath(
                baseModRengokuSword,
                SheathItems.SWORD_SHEATH_RENGOKU.get()
            );
            if (Config.logDebug) {
                Log.info("Registered rengoku sheath for base mod's nichirinsword_rengoku");
            }
        }

        if (Config.logDebug) {
            Log.info("Registered custom sword sheaths");
        }
    } catch (Exception e) {
        System.err.println("Error registering custom sheaths: " + e.getMessage());
        e.printStackTrace();
    }
}
}
