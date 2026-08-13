package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.AlchemyTableRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.PetriDishBlockRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.VialRackRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AlchemyClientHandler {
    private AlchemyClientHandler() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModAlchemyMenus.MICROSCOPE.get(), MicroscopeScreen::new);
            MenuScreens.register(ModAlchemyMenus.ALCHEMY_TABLE.get(), AlchemyTableScreen::new);
            MenuScreens.register(ModAlchemyMenus.VIAL_RACK.get(), VialRackScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.ALCHEMY_TABLE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.VIAL_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.WISTERIA_INCENSE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.POTTED_WISTERIA_INCENSE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.FERMENTED_ORCHID.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.POTTED_FERMENTED_ORCHID.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.IMMORTAL_DAISY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModAlchemyBlocks.POTTED_IMMORTAL_DAISY.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ALCHEMY_TABLE.get(), AlchemyTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VIAL_RACK.get(), VialRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PETRI_DISH.get(), PetriDishBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (RegistryObject<net.minecraft.world.item.Item> item : ModAlchemyItems.ITEMS.getEntries()) {
            event.register((stack, tintIndex) -> {
                if ((stack.is(ModAlchemyItems.DARK_STAR_CATALYST.get()) || stack.is(ModAlchemyItems.DARK_STAR.get())) && tintIndex == 0) {
                    if (stack.hasTag() && stack.getTag().contains("TintColor")) {
                        return stack.getTag().getInt("TintColor") & 0xFFFFFF;
                    }
                    return BloodDemonArtAlchemyCatalog.tintFor(stack);
                }
                if (stack.is(ModAlchemyItems.DARK_STAR_CATALYST.get()) && tintIndex == 1) {
                    return 0x000000;
                }
                if (tintIndex == 1) {
                    return BloodDemonArtAlchemyCatalog.tintFor(stack);
                }
                if (tintIndex == 2) {
                    return BloodDemonArtAlchemyCatalog.ringTintFor(stack);
                }
                return 0xFFFFFF;
            }, item.get());
        }
    }
}
