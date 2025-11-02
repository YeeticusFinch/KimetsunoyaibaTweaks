package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// All item classes are in the same package, no imports needed

/**
 * Registry for all mod items
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KimetsunoyaibaMultiplayer.MODID);

    // Nichirin swords with breathing techniques
    public static final RegistryObject<Item> NICHIRINSWORD_MIST = ITEMS.register("nichirinsword_mist",
        () -> new NichirinSwordMist(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_MUICHIRO = ITEMS.register("nichirinsword_muichiro",
        () -> new NichirinSwordMuichiro(new Item.Properties().stacksTo(1).durability(2000)));

    // Creative tab
    public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB = CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_MIST.get()))
            .displayItems((parameters, output) -> {
                // Automatically add all swords registered via the API
                // This allows other mods to have their swords appear in this tab
                for (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.RegisteredSword sword :
                        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getAllSwords()) {
                    // Only add swords that are flagged to be in the creative tab
                    if (sword.shouldRegisterToCreativeTab()) {
                        output.accept(sword.getSwordItem());
                    }
                }

                // Wisteria blocks
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LOG.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_LOG.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_WOOD.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_WOOD.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PLANKS.get());

                // Wisteria Leaves (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CREAM.get());

                // Glowing Wisteria Leaves (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get());

                // Wisteria Saplings (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CREAM.get());

                // Wisteria Petals (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CREAM.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
