package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModAlchemyBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<Block> ALCHEMY_TABLE = registerBlock("alchemy_table",
        () -> new AlchemyTableBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.5F)
            .lightLevel(state -> state.getValue(AlchemyTableBlock.LIT) ? 10 : 0)
            .noOcclusion()));

    public static final RegistryObject<Block> VIAL_RACK = registerBlockWithItem("vial_rack",
        () -> new VialRackBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(1.5F, 2.0F)
            .noOcclusion()),
        block -> new VialRackBlockItem(block.get(), new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Block> PETRI_DISH = registerBlockWithoutItem("petri_dish",
        () -> new PetriDishBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
            .mapColor(MapColor.NONE)
            .sound(SoundType.GLASS)
            .instabreak()
            .noOcclusion()));

    public static final RegistryObject<Block> WISTERIA_INCENSE = registerBlockWithItem("wisteria_incense",
        () -> new WisteriaIncenseBlock(BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH)
            .mapColor(MapColor.PLANT)
            .sound(SoundType.WOOD)
            .instabreak()
            .noCollission()
            .noOcclusion()
            .lightLevel(state -> state.getValue(WisteriaIncenseBlock.LIT) ? 3 : 0), false),
        block -> new WisteriaIncenseBlockItem(block.get(), new Item.Properties()));

    public static final RegistryObject<Block> IMMORTAL_DAISY = registerBlock("immortal_daisy",
        () -> new AlchemyFlowerBlock(() -> MobEffects.REGENERATION, 5,
            BlockBehaviour.Properties.copy(Blocks.DANDELION)
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> FERMENTED_ORCHID = registerBlock("fermented_orchid",
        () -> new AlchemyFlowerBlock(() -> MobEffects.REGENERATION, 5,
            BlockBehaviour.Properties.copy(Blocks.BLUE_ORCHID)
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> POTTED_IMMORTAL_DAISY = registerBlockWithoutItem("potted_immortal_daisy",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, IMMORTAL_DAISY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    public static final RegistryObject<Block> POTTED_FERMENTED_ORCHID = registerBlockWithoutItem("potted_fermented_orchid",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, FERMENTED_ORCHID,
            BlockBehaviour.Properties.copy(Blocks.POTTED_BLUE_ORCHID).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WISTERIA_INCENSE = registerBlockWithoutItem("potted_wisteria_incense",
        () -> new WisteriaIncenseBlock(BlockBehaviour.Properties.copy(Blocks.POTTED_DEAD_BUSH)
            .mapColor(MapColor.PLANT)
            .sound(SoundType.WOOD)
            .instabreak()
            .noOcclusion()
            .lightLevel(state -> state.getValue(WisteriaIncenseBlock.LIT) ? 3 : 0), true));

    private ModAlchemyBlocks() {
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> blockSupplier) {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        ModAlchemyItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static <T extends Block> RegistryObject<T> registerBlockWithItem(
        String name, Supplier<T> blockSupplier, java.util.function.Function<RegistryObject<T>, Item> itemFactory) {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        ModAlchemyItems.ITEMS.register(name, () -> itemFactory.apply(block));
        return block;
    }

    private static <T extends Block> RegistryObject<T> registerBlockWithoutItem(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
