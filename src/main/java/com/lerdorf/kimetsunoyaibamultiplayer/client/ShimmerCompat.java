package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lowdragmc.shimmer.client.light.LightManager;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers colored block lights when the optional Shimmer mod is installed. */
@Mod.EventBusSubscriber(
    modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ShimmerCompat {
    private static final String SHIMMER_MOD_ID = "shimmer";

    private ShimmerCompat() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded(SHIMMER_MOD_ID)) {
            return;
        }

        event.enqueueWork(() -> {
            try {
                registerLights();
            } catch (Throwable throwable) {
                System.err.println("[Kimetsunoyaiba Tweaks] Shimmer light registration failed: "
                    + throwable.getMessage());
            }
        });
    }

    private static void registerLights() {
        // Lamp colors match the existing Shimmer config color references.
        register(ModBlocks.SIDEWAYS_LANTERN_1.get(), 0xDFD7CD, 14.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_2.get(), 0xF3F3ED, 11.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_2RED.get(), 0xFF5955, 12.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_3.get(), 0xDBD0C3, 14.0F);
        register(ModBlocks.LANTERN_TSUZUMI.get(), 0xFF9C51, 18.0F);
        register(ModBlocks.HANGING_LAMP.get(), 0xFFFFFF, 14.0F);
        register(ModBlocks.DIM_ORANGE_LAMP.get(), 0xFF6128, 7.0F);

        // Only the glowing wisteria variants emit colored light.
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get(), 0xE7A2F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get(), 0xA2D2F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get(), 0xB9A3F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get(), 0xF2F0A2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get(), 0xE7A2F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get(), 0xA2D2F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get(), 0xB9A3F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get(), 0xF2F0A2, 10.0F);
    }

    private static void register(Block block, int color, float radius) {
        LightManager.INSTANCE.registerBlockLight(block, color, radius);
    }
}
