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
        SwordSheathRegistry.registerPersistentSheath(
            ModItems.NICHIRINSWORD_LOVE.get(),
            SheathItems.SWORD_SHEATH.get()
        );

        SwordSheathRegistry.registerPersistentSheath(
            ModItems.NICHIRINSWORD_KANAWO.get(),
            SheathItems.SWORD_SHEATH_KANAWO.get()
        );

        SwordSheathRegistry.registerTemporarySheath(
                ModItems.NICHIRINSWORD_BEAST.get(),
                SheathItems.SWORD_SHEATH_INOSUKE.get()
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

        // Register uzui sheath for the base mod's uzui sword (if it exists)
        // Uzui's sheath disappears when sword is drawn (with cloud particle effect)
        net.minecraft.world.item.Item baseModUzuiSword =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "nichirinsword_uzui"));

        if (baseModUzuiSword != null) {
            SwordSheathRegistry.registerTemporarySheath(
                baseModUzuiSword,
                SheathItems.SWORD_SHEATH_UZUI.get()
            );
            if (Config.logDebug) {
                Log.info("Registered uzui sheath for base mod's nichirinsword_uzui (temporary)");
            }
        }

        // Register iguro sheath for the base mod's iguro sword (if it exists)
        net.minecraft.world.item.Item baseModIguroSword =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "nichirinsword_iguro"));

        if (baseModIguroSword != null) {
            SwordSheathRegistry.registerPersistentSheath(
                baseModIguroSword,
                SheathItems.SWORD_SHEATH_IGURO.get()
            );
            if (Config.logDebug) {
                Log.info("Registered iguro sheath for base mod's nichirinsword_iguro");
            }
        }

        // Register inosuke sheath for the base mod's inosuke sword (if it exists)
        // Inosuke's sheath disappears when sword is drawn (with cloud particle effect)
        net.minecraft.world.item.Item baseModInosukeSword =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "nichirinsword_inosuke"));

        if (baseModInosukeSword != null) {
            SwordSheathRegistry.registerTemporarySheath(
                baseModInosukeSword,
                SheathItems.SWORD_SHEATH_INOSUKE.get()
            );
            if (Config.logDebug) {
                Log.info("Registered inosuke sheath for base mod's nichirinsword_inosuke (temporary)");
            }
        }

        // Register inosuke sheath for our enhanced Inosuke sword
        SwordSheathRegistry.registerTemporarySheath(
            ModItems.NICHIRINSWORD_INOSUKE.get(),
            SheathItems.SWORD_SHEATH_INOSUKE.get()
        );

        // Register kokushibo sheaths and display override
        net.minecraft.world.item.Item kokushiboSword1 =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "sword_kokushibo_1"));
        net.minecraft.world.item.Item kokushiboSword2 =
            net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new ResourceLocation("kimetsunoyaiba", "sword_kokushibo_2"));

        if (kokushiboSword1 != null) {
            SwordSheathRegistry.registerPersistentSheath(
                kokushiboSword1,
                SheathItems.SWORD_SHEATH_KOKUSHIBO.get()
            );
            if (Config.logDebug) {
                Log.info("Registered kokushibo sheath for sword_kokushibo_1");
            }
        }

        if (kokushiboSword2 != null) {
            SwordSheathRegistry.registerPersistentSheath(
                kokushiboSword2,
                SheathItems.SWORD_SHEATH_KOKUSHIBO.get()
            );
            if (Config.logDebug) {
                Log.info("Registered kokushibo sheath for sword_kokushibo_2");
            }
        }

        // sword_kokushibo_2 should display as sword_kokushibo_1 when sheathed
        if (kokushiboSword1 != null && kokushiboSword2 != null) {
            SwordSheathRegistry.registerSheathDisplayOverride(kokushiboSword2, kokushiboSword1);
            if (Config.logDebug) {
                Log.info("Registered kokushibo sword_kokushibo_2 -> sword_kokushibo_1 display override");
            }
        }

        // Register uzui-style sheath for our mod's sound sword (temporary - disappears when drawn)
        SwordSheathRegistry.registerTemporarySheath(
            ModItems.NICHIRINSWORD_SOUND.get(),
            SheathItems.SWORD_SHEATH_UZUI.get()
        );

        // Register iguro sheath for our mod's snake sword (persistent)
        SwordSheathRegistry.registerPersistentSheath(
            ModItems.NICHIRINSWORD_SNAKE.get(),
            SheathItems.SWORD_SHEATH_IGURO.get()
        );

        // Register custom sheath scales (legacy API)
        SheathModelRenderer.registerSheathScale(SheathItems.SWORD_SHEATH_IGURO.get(), 1.31f);

        if (Config.logDebug) {
            Log.info("Registered custom sword sheaths");
        }
    } catch (Exception e) {
        System.err.println("Error registering custom sheaths: " + e.getMessage());
        e.printStackTrace();
    }
}
}
