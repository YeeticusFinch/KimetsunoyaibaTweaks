package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies the wool color to the stool's grayscale cushion texture. */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
                        bus = Mod.EventBusSubscriber.Bus.MOD,
                        value = Dist.CLIENT)
public final class StoolColorHandler {
    private StoolColorHandler() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.WHITE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.ORANGE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_ORANGE.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.MAGENTA.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_MAGENTA.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.LIGHT_BLUE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIGHT_BLUE.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.YELLOW.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_YELLOW.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.LIME.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIME.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.PINK.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_PINK.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.GRAY.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_GRAY.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.LIGHT_GRAY.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIGHT_GRAY.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.CYAN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_CYAN.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.PURPLE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_PURPLE.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.BLUE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BLUE.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.BROWN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BROWN.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.GREEN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_GREEN.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.RED.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_RED.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? DyeColor.BLACK.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BLACK.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.WHITE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.ORANGE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_ORANGE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.MAGENTA.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_MAGENTA.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.LIGHT_BLUE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIGHT_BLUE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.YELLOW.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_YELLOW.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.LIME.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIME.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.PINK.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_PINK.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.GRAY.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_GRAY.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.LIGHT_GRAY.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_LIGHT_GRAY.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.CYAN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_CYAN.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.PURPLE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_PURPLE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.BLUE.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BLUE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.BROWN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BROWN.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.GREEN.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_GREEN.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.RED.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_RED.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? DyeColor.BLACK.getFireworkColor() : 0xFFFFFF,
            ModBlocks.STOOL_BLACK.get());
    }
}
