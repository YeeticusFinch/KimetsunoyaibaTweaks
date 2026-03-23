package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Sets proper render types for blocks with transparency
 */
@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
                        bus = Mod.EventBusSubscriber.Bus.MOD,
                        value = Dist.CLIENT)
public class BlockRenderTypeHandler {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHEST_OF_DRAWERS.get(), RenderType.cutout());

            // Set all wisteria petals variants to use cutout rendering (for transparency)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_PETALS_PINK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_PETALS_CYAN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_PETALS_LAVENDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_PETALS_CREAM.get(), RenderType.cutout());

            // Set all wisteria leaves variants to use cutout mipped rendering
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_LEAVES_PINK.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_LEAVES_CYAN.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_LEAVES_LAVENDER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_LEAVES_CREAM.get(), RenderType.cutoutMipped());

            // Set all wisteria saplings to use cutout rendering (for transparency)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_SAPLING_PINK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_SAPLING_CYAN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_SAPLING_LAVENDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WISTERIA_SAPLING_CREAM.get(), RenderType.cutout());
            
            // Set all glowing wisteria petals variants to use cutout rendering (for transparency)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get(), RenderType.cutout());
        });
    }
}
