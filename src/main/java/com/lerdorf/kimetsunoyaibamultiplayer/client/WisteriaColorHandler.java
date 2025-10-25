package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side color tinting for Wisteria blocks.
 * Applies purple tint to grayscale textures.
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
                        bus = Mod.EventBusSubscriber.Bus.MOD,
                        value = Dist.CLIENT)
public class WisteriaColorHandler {

    // Color tint definitions for Wisteria variants (MUCH BRIGHTER!)
    // Pink variant - Very bright pink
    private static final int WISTERIA_PINK = 0xE7A2F2;

    // Cyan variant - Very bright cyan 
    private static final int WISTERIA_CYAN = 0xA2D2F2;

    // Lavender variant - Very bright lavender 
    private static final int WISTERIA_LAVENDER = 0xB9A3F2; // #b9a3f2

    // Cream variant - Very bright cream/white
    private static final int WISTERIA_CREAM = 0xF2F0A2;

    // Petal color variants (EVEN BRIGHTER!)
    // Pink petals - Ultra bright pink (RGB: 255, 20, 147)
    private static final int PETAL_PINK = 0xE7A2F2;

    // Cyan petals - Ultra bright cyan (RGB: 0, 255, 255)
    private static final int PETAL_CYAN = 0xA2D2F2;

    // Lavender petals - Ultra bright lavender (RGB: 238, 130, 238)
    private static final int PETAL_LAVENDER = 0xB9A3F2;

    // Cream petals - Ultra bright ivory (RGB: 255, 255, 240)
    private static final int PETAL_CREAM = 0xF2F0A2;

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        // Register color handlers for Wisteria Leaves (4 variants)
        event.register((state, level, pos, tintIndex) -> WISTERIA_PINK,
            ModBlocks.WISTERIA_LEAVES_PINK.get());

        event.register((state, level, pos, tintIndex) -> WISTERIA_CYAN,
            ModBlocks.WISTERIA_LEAVES_CYAN.get());

        event.register((state, level, pos, tintIndex) -> WISTERIA_LAVENDER,
            ModBlocks.WISTERIA_LEAVES_LAVENDER.get());

        event.register((state, level, pos, tintIndex) -> WISTERIA_CREAM,
            ModBlocks.WISTERIA_LEAVES_CREAM.get());
        
        event.register((state, level, pos, tintIndex) -> WISTERIA_PINK,
                ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get());

            event.register((state, level, pos, tintIndex) -> WISTERIA_CYAN,
                ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get());

            event.register((state, level, pos, tintIndex) -> WISTERIA_LAVENDER,
                ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get());

            event.register((state, level, pos, tintIndex) -> WISTERIA_CREAM,
                ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get());

        // Register color handlers for Wisteria Petals (4 variants)
        event.register((state, level, pos, tintIndex) -> PETAL_PINK,
            ModBlocks.WISTERIA_PETALS_PINK.get());

        event.register((state, level, pos, tintIndex) -> PETAL_CYAN,
            ModBlocks.WISTERIA_PETALS_CYAN.get());

        event.register((state, level, pos, tintIndex) -> PETAL_LAVENDER,
            ModBlocks.WISTERIA_PETALS_LAVENDER.get());

        event.register((state, level, pos, tintIndex) -> PETAL_CREAM,
            ModBlocks.WISTERIA_PETALS_CREAM.get());
        
        // Glowing petals
        event.register((state, level, pos, tintIndex) -> PETAL_PINK,
                ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get());

            event.register((state, level, pos, tintIndex) -> PETAL_CYAN,
                ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get());

            event.register((state, level, pos, tintIndex) -> PETAL_LAVENDER,
                ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get());

            event.register((state, level, pos, tintIndex) -> PETAL_CREAM,
                ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Register color handlers for Wisteria Leaves items (4 variants)
        event.register((stack, tintIndex) -> WISTERIA_PINK,
            ModBlocks.WISTERIA_LEAVES_PINK.get());

        event.register((stack, tintIndex) -> WISTERIA_CYAN,
            ModBlocks.WISTERIA_LEAVES_CYAN.get());

        event.register((stack, tintIndex) -> WISTERIA_LAVENDER,
            ModBlocks.WISTERIA_LEAVES_LAVENDER.get());

        event.register((stack, tintIndex) -> WISTERIA_CREAM,
            ModBlocks.WISTERIA_LEAVES_CREAM.get());

        // Register color handlers for Glowing Wisteria Leaves items (4 variants)
        event.register((stack, tintIndex) -> WISTERIA_PINK,
            ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get());

        event.register((stack, tintIndex) -> WISTERIA_CYAN,
            ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get());

        event.register((stack, tintIndex) -> WISTERIA_LAVENDER,
            ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get());

        event.register((stack, tintIndex) -> WISTERIA_CREAM,
            ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get());

        // Register color handlers for Wisteria Petals items (4 variants)
        event.register((stack, tintIndex) -> PETAL_PINK,
            ModBlocks.WISTERIA_PETALS_PINK.get());

        event.register((stack, tintIndex) -> PETAL_CYAN,
            ModBlocks.WISTERIA_PETALS_CYAN.get());

        event.register((stack, tintIndex) -> PETAL_LAVENDER,
            ModBlocks.WISTERIA_PETALS_LAVENDER.get());

        event.register((stack, tintIndex) -> PETAL_CREAM,
            ModBlocks.WISTERIA_PETALS_CREAM.get());

        // Register color handlers for Glowing Wisteria Petals items (4 variants)
        event.register((stack, tintIndex) -> PETAL_PINK,
            ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get());

        event.register((stack, tintIndex) -> PETAL_CYAN,
            ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get());

        event.register((stack, tintIndex) -> PETAL_LAVENDER,
            ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get());

        event.register((stack, tintIndex) -> PETAL_CREAM,
            ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get());
    }
}
