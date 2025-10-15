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
    public static final RegistryObject<Item> NICHIRINSWORD_FROST = ITEMS.register("nichirinsword_frost",
        () -> new NichirinSwordFrost(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_ICE = ITEMS.register("nichirinsword_ice",
        () -> new NichirinSwordIce(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_SHIMIZU = ITEMS.register("nichirinsword_shimizu",
        () -> new NichirinSwordShimizu(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_KOMOREBI = ITEMS.register("nichirinsword_komorebi",
        () -> new NichirinSwordKomorebi(new Item.Properties().stacksTo(1).durability(2000)));

    // Golden sword - used for Seventh Form: Golden Senses (not in creative menu)
    public static final RegistryObject<Item> NICHIRINSWORD_GOLDEN = ITEMS.register("nichirinsword_golden",
        () -> new NichirinSwordGolden(new Item.Properties().stacksTo(1).durability(2000)));

    // Komorebi's cosmetic armor
    public static final RegistryObject<Item> KOMOREBI_HAIR = ITEMS.register("komorebi_hair",
        () -> new KomorebiHairItem());

    public static final RegistryObject<Item> KOMOREBI_HAORI = ITEMS.register("komorebi_haori",
        () -> new KomorebiHaoriItem());

    public static final RegistryObject<Item> KOMOREBI_HAKAMA = ITEMS.register("komorebi_hakama",
        () -> new KomorebiHakamaItem());

    // Shimizu's cosmetic armor
    public static final RegistryObject<Item> SHIMIZU_HAIR = ITEMS.register("shimizu_hair",
        () -> new ShimizuHairItem());

    public static final RegistryObject<Item> SHIMIZU_HAORI = ITEMS.register("shimizu_haori",
        () -> new ShimizuHaoriItem());

    // Spawn eggs for breathing slayer entities
    public static final RegistryObject<Item> ICE_SLAYER_SPAWN_EGG = ITEMS.register("ice_slayer_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.ICE_SLAYER,
            0x5DBCD2, 0xFFFFFF, // Light blue body, white spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FROST_SLAYER_SPAWN_EGG = ITEMS.register("frost_slayer_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.FROST_SLAYER,
            0xB0E0E6, 0xF0F8FF, // Powder blue body, alice blue spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KOMOREBI_SPAWN_EGG = ITEMS.register("komorebi_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KOMOREBI,
            0x8B4513, 0xFFD700, // Saddle brown body, gold spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> SHIMIZU_SPAWN_EGG = ITEMS.register("shimizu_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SHIMIZU,
            0xE6E6FA, 0x9370DB, // Lavender body, medium purple spots
            new Item.Properties().stacksTo(64)));

    // Creative tab
    public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB = CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_ICE.get()))
            .displayItems((parameters, output) -> {
                // Automatically add all swords registered via the API
                // This allows other mods to have their swords appear in this tab
                for (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.RegisteredSword sword :
                        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getAllSwords()) {
                    output.accept(sword.getSwordItem());
                }

                // Cosmetic armor
                output.accept(KOMOREBI_HAIR.get());
                output.accept(KOMOREBI_HAORI.get());
                output.accept(KOMOREBI_HAKAMA.get());
                output.accept(SHIMIZU_HAIR.get());
                output.accept(SHIMIZU_HAORI.get());

                // Spawn eggs
                output.accept(ICE_SLAYER_SPAWN_EGG.get());
                output.accept(FROST_SLAYER_SPAWN_EGG.get());
                output.accept(KOMOREBI_SPAWN_EGG.get());
                output.accept(SHIMIZU_SPAWN_EGG.get());

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
