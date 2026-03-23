package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registry for all mod blocks
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, KimetsunoyaibaMultiplayer.MODID);

    // Wisteria wood blocks
    public static final RegistryObject<Block> WISTERIA_LOG = registerBlock("wisteria_log",
        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> STRIPPED_WISTERIA_LOG = registerBlock("stripped_wisteria_log",
        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> WISTERIA_WOOD = registerBlock("wisteria_wood",
        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> STRIPPED_WISTERIA_WOOD = registerBlock("stripped_wisteria_wood",
        () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_WOOD)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> WISTERIA_PLANKS = registerBlock("wisteria_planks",
        () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)));

    public static final RegistryObject<Block> DARK_BAMBOO_FENCE = registerBlock("dark_bamboo_fence",
        () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BARS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.BAMBOO_WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> FUSUMA_BARS = registerBlock("fusuma_bars",
        () -> new FusumaBarsBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(1.0f, 10.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> DARK_BAMBOO_FUSUMA = registerBlock("dark_bamboo_fusuma",
        () -> new DarkBambooFusumaBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(1.0f, 10.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> DARK_OAK_WALL = registerBlock("dark_oak_wall",
        () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_LOG)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> STRIPPED_DARK_OAK_WALL = registerBlock("stripped_dark_oak_wall",
        () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_DARK_OAK_LOG)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f)));

    public static final RegistryObject<Block> CHEST_OF_DRAWERS = registerBlock("chest_of_drawers",
        () -> new ChestOfDrawersBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)
            .noOcclusion()));

    // Wisteria Leaves - 4 color variants (with moderate glow)
    public static final RegistryObject<Block> WISTERIA_LEAVES_PINK = registerBlock("wisteria_leaves_pink",
        () -> new WisteriaLeavesPinkBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_PINK)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_LEAVES_CYAN = registerBlock("wisteria_leaves_cyan",
        () -> new WisteriaLeavesCyanBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_CYAN)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_LEAVES_LAVENDER = registerBlock("wisteria_leaves_lavender",
        () -> new WisteriaLeavesLavenderBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_LEAVES_CREAM = registerBlock("wisteria_leaves_cream",
        () -> new WisteriaLeavesCreamBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    // Glowing Wisteria Leaves - 4 color variants (dim glow)
    public static final RegistryObject<Block> GLOWING_WISTERIA_LEAVES_PINK = registerBlock("glowing_wisteria_leaves_pink",
        () -> new GlowingWisteriaLeavesPinkBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_PINK)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .lightLevel((state) -> 4)
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_LEAVES_CYAN = registerBlock("glowing_wisteria_leaves_cyan",
        () -> new GlowingWisteriaLeavesCyanBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_CYAN)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .lightLevel((state) -> 4)
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_LEAVES_LAVENDER = registerBlock("glowing_wisteria_leaves_lavender",
        () -> new GlowingWisteriaLeavesLavenderBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .lightLevel((state) -> 4)
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_LEAVES_CREAM = registerBlock("glowing_wisteria_leaves_cream",
        () -> new GlowingWisteriaLeavesCreamBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.GRASS)
            .strength(0.2f)
            .randomTicks()
            .noOcclusion()
            .lightLevel((state) -> 4)
            .isValidSpawn((state, level, pos, type) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)));

    // Legacy name - points to pink variant
    public static final RegistryObject<Block> WISTERIA_LEAVES = WISTERIA_LEAVES_PINK;

    // Wisteria Petals - 4 color variants (no randomTicks - growth disabled)
    public static final RegistryObject<Block> WISTERIA_PETALS_PINK = registerBlock("wisteria_petals_pink",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_PINK)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 2)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_PETALS_CYAN = registerBlock("wisteria_petals_cyan",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_CYAN)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 2)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_PETALS_LAVENDER = registerBlock("wisteria_petals_lavender",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 2)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> WISTERIA_PETALS_CREAM = registerBlock("wisteria_petals_cream",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 2)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    // Legacy name - points to pink variant
    public static final RegistryObject<Block> WISTERIA_PETALS = WISTERIA_PETALS_PINK;

    // Glowing Wisteria Petals - 4 color variants (brighter glow)
    public static final RegistryObject<Block> GLOWING_WISTERIA_PETALS_PINK = registerBlock("glowing_wisteria_petals_pink",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_PINK)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 10)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_PETALS_CYAN = registerBlock("glowing_wisteria_petals_cyan",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_CYAN)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 10)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_PETALS_LAVENDER = registerBlock("glowing_wisteria_petals_lavender",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 10)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    public static final RegistryObject<Block> GLOWING_WISTERIA_PETALS_CREAM = registerBlock("glowing_wisteria_petals_cream",
        () -> new WisteriaPetalsBlock(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES)
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.CAVE_VINES)
            .strength(0.0f)
            .noCollission()
            .noOcclusion()
            .lightLevel((state) -> 10)
            .isViewBlocking((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)));

    // Wisteria Saplings - 4 color variants
    public static final RegistryObject<Block> WISTERIA_SAPLING_PINK = registerBlock("wisteria_sapling_pink",
        () -> new WisteriaSaplingBlock(com.lerdorf.kimetsunoyaibamultiplayer.worldgen.WisteriaTreeGrowers.PINK,
            BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                .mapColor(MapColor.COLOR_PINK)
                .noCollission()
                .randomTicks()
                .strength(0.0f)
                .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> WISTERIA_SAPLING_CYAN = registerBlock("wisteria_sapling_cyan",
        () -> new WisteriaSaplingBlock(com.lerdorf.kimetsunoyaibamultiplayer.worldgen.WisteriaTreeGrowers.CYAN,
            BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                .mapColor(MapColor.COLOR_CYAN)
                .noCollission()
                .randomTicks()
                .strength(0.0f)
                .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> WISTERIA_SAPLING_LAVENDER = registerBlock("wisteria_sapling_lavender",
        () -> new WisteriaSaplingBlock(com.lerdorf.kimetsunoyaibamultiplayer.worldgen.WisteriaTreeGrowers.LAVENDER,
            BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                .mapColor(MapColor.COLOR_PURPLE)
                .noCollission()
                .randomTicks()
                .strength(0.0f)
                .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> WISTERIA_SAPLING_CREAM = registerBlock("wisteria_sapling_cream",
        () -> new WisteriaSaplingBlock(com.lerdorf.kimetsunoyaibamultiplayer.worldgen.WisteriaTreeGrowers.CREAM,
            BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .noCollission()
                .randomTicks()
                .strength(0.0f)
                .sound(SoundType.GRASS)));

    // Legacy name - points to pink variant
    public static final RegistryObject<Block> WISTERIA_SAPLING = WISTERIA_SAPLING_PINK;

    // Toril Gate marker block - invisible, placed inside gate structures to detect players
    public static final RegistryObject<Block> TORIL_GATE_MARKER = registerBlock("toril_gate_marker",
        () -> new TorilGateMarkerBlock(BlockBehaviour.Properties.copy(Blocks.AIR)
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .strength(-1.0f, 3600000.0f)
            .noParticlesOnBreak()));

    /**
     * Helper method to register a block and its corresponding BlockItem
     */
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    /**
     * Register a BlockItem for the given block
     */
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems.ITEMS.register(name,
            () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
