package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
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

    public static final RegistryObject<Block> WISTERIA_VERTICAL_SLAB = registerBlock("wisteria_vertical_slab",
        () -> new VerticalSlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> SPRUCE_VERTICAL_SLAB = registerBlock("spruce_vertical_slab",
        () -> new VerticalSlabBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_SLAB)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> SPRUCE_SIDEWAYS_STAIRS = registerBlock("spruce_sideways_stairs",
        () -> new SidewaysStairsBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_STAIRS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> TILE_VERTICAL_SLAB = registerBlock("tile_vertical_slab",
        () -> new VerticalSlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    public static final RegistryObject<Block> TILE_SIDEWAYS_STAIRS = registerBlock("tile_sideways_stairs",
        () -> new SidewaysStairsBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICK_STAIRS)
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noOcclusion()));

    public static final RegistryObject<Block> SPRUCE_SIDEWAYS_FENCE = registerBlock("spruce_sideways_fence",
        () -> new SidewaysFenceBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_FENCE)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)
            .noOcclusion()));

    public static final RegistryObject<Block> SIDEWAYS_LANTERN_1 = registerBlock("sideways_lantern_1",
        () -> new SidewaysLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN)
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .sound(SoundType.LANTERN)
            .strength(0.3f)
            .lightLevel(state -> 15)
            .noOcclusion()));

    public static final RegistryObject<Block> SIDEWAYS_LANTERN_2 = registerBlock("sideways_lantern_2",
        () -> new SidewaysLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN)
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.LANTERN)
            .strength(0.3f)
            .lightLevel(state -> 15)
            .noOcclusion()));

    public static final RegistryObject<Block> SIDEWAYS_LANTERN_2RED = registerBlock("sideways_lantern_2red",
        () -> new SidewaysLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN)
            .mapColor(MapColor.COLOR_RED)
            .sound(SoundType.LANTERN)
            .strength(0.3f)
            .lightLevel(state -> 15)
            .noOcclusion()));

    public static final RegistryObject<Block> SIDEWAYS_LANTERN_3 = registerBlock("sideways_lantern_3",
        () -> new SidewaysLanternBlock(BlockBehaviour.Properties.copy(Blocks.LANTERN)
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .sound(SoundType.LANTERN)
            .strength(0.3f)
            .lightLevel(state -> 15)
            .noOcclusion()));

    public static final RegistryObject<Block> WISTERIA_STAIRS = registerBlock("wisteria_stairs",
        () -> new StairBlock(Blocks.OAK_PLANKS.defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)));

    public static final RegistryObject<Block> WISTERIA_SLAB = registerBlock("wisteria_slab",
        () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)));

    public static final RegistryObject<Block> WISTERIA_FENCE = registerBlock("wisteria_fence",
        () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f)));

    public static final RegistryObject<Block> WISTERIA_FENCE_GATE = registerBlock("wisteria_fence_gate",
        () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0f, 3.0f), WoodType.OAK));

    public static final RegistryObject<Block> WISTERIA_PRESSURE_PLATE = registerBlock("wisteria_pressure_plate",
        () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
            BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE)
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(0.5f),
            BlockSetType.OAK));

    public static final RegistryObject<Block> WISTERIA_BUTTON = registerBlock("wisteria_button",
        () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(0.5f), BlockSetType.OAK, 30, true));

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

    public static final RegistryObject<Block> SWORD_RACK = registerBlock("sword_rack",
        () -> new SwordRackBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
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

    public static final RegistryObject<Block> POTTED_WISTERIA_SAPLING_PINK = registerBlockWithoutItem("potted_wisteria_sapling_pink",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WISTERIA_SAPLING_PINK,
            BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WISTERIA_SAPLING_CYAN = registerBlockWithoutItem("potted_wisteria_sapling_cyan",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WISTERIA_SAPLING_CYAN,
            BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WISTERIA_SAPLING_LAVENDER = registerBlockWithoutItem("potted_wisteria_sapling_lavender",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WISTERIA_SAPLING_LAVENDER,
            BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WISTERIA_SAPLING_CREAM = registerBlockWithoutItem("potted_wisteria_sapling_cream",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WISTERIA_SAPLING_CREAM,
            BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION).noOcclusion()));

    // Legacy name - points to pink variant
    public static final RegistryObject<Block> WISTERIA_SAPLING = WISTERIA_SAPLING_PINK;

    public static final RegistryObject<Block> SPIDER_LILY = registerBlock("spider_lily",
        () -> new SpiderLilyBlock(true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WHITE_SPIDER_LILY = registerBlock("white_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.SNOW)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> RED_SPIDER_LILY = registerBlock("red_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_RED)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> PURPLE_SPIDER_LILY = registerBlock("purple_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_PURPLE)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> YELLOW_SPIDER_LILY = registerBlock("yellow_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_YELLOW)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> BLUE_SPIDER_LILY = registerBlock("blue_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_BLUE)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> LIME_SPIDER_LILY = registerBlock("lime_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_LIGHT_GREEN)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> PINK_SPIDER_LILY = registerBlock("pink_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_PINK)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> ORANGE_SPIDER_LILY = registerBlock("orange_spider_lily",
        () -> new SpiderLilyBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_ORANGE)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_SPIDER_LILY = registerBlock("waxed_spider_lily",
        () -> new SpiderLilyBlock(true, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.PLANT)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_WHITE_SPIDER_LILY = registerBlock("waxed_white_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.SNOW)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_RED_SPIDER_LILY = registerBlock("waxed_red_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_RED)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_PURPLE_SPIDER_LILY = registerBlock("waxed_purple_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_PURPLE)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_YELLOW_SPIDER_LILY = registerBlock("waxed_yellow_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_YELLOW)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_BLUE_SPIDER_LILY = registerBlock("waxed_blue_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_BLUE)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_LIME_SPIDER_LILY = registerBlock("waxed_lime_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_LIGHT_GREEN)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_PINK_SPIDER_LILY = registerBlock("waxed_pink_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_PINK)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> WAXED_ORANGE_SPIDER_LILY = registerBlock("waxed_orange_spider_lily",
        () -> new SpiderLilyBlock(false, true, BlockBehaviour.Properties.copy(Blocks.POPPY)
            .mapColor(MapColor.COLOR_ORANGE)
            .noCollission()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final RegistryObject<Block> POTTED_SPIDER_LILY = registerBlockWithoutItem("potted_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WHITE_SPIDER_LILY = registerBlockWithoutItem("potted_white_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WHITE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_RED_SPIDER_LILY = registerBlockWithoutItem("potted_red_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, RED_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_PURPLE_SPIDER_LILY = registerBlockWithoutItem("potted_purple_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, PURPLE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_YELLOW_SPIDER_LILY = registerBlockWithoutItem("potted_yellow_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, YELLOW_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_BLUE_SPIDER_LILY = registerBlockWithoutItem("potted_blue_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BLUE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_LIME_SPIDER_LILY = registerBlockWithoutItem("potted_lime_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, LIME_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_PINK_SPIDER_LILY = registerBlockWithoutItem("potted_pink_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, PINK_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_ORANGE_SPIDER_LILY = registerBlockWithoutItem("potted_orange_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, ORANGE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_WHITE_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_white_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_WHITE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_RED_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_red_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_RED_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_PURPLE_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_purple_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_PURPLE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_YELLOW_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_yellow_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_YELLOW_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_BLUE_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_blue_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_BLUE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_LIME_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_lime_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_LIME_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_PINK_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_pink_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_PINK_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    public static final RegistryObject<Block> POTTED_WAXED_ORANGE_SPIDER_LILY = registerBlockWithoutItem("potted_waxed_orange_spider_lily",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WAXED_ORANGE_SPIDER_LILY,
            BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).noOcclusion()));

    // Toril Gate marker block - invisible, placed inside gate structures to detect players
    public static final RegistryObject<Block> TORIL_GATE_MARKER = registerBlock("toril_gate_marker",
        () -> new TorilGateMarkerBlock(BlockBehaviour.Properties.copy(Blocks.AIR)
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .strength(-1.0f, 3600000.0f)
            .noParticlesOnBreak()));

    public static final RegistryObject<Block> HEMOLITH_ORE = registerBlock("hemolith_ore",
        () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(3.0f, 3.0f)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> GRAVITY_FIELD_PROJECTOR = registerBlock("gravity_field_projector",
        () -> new GravityFieldProjectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(4.0f, 6.0f)
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> GRAVITY_BLOCK = registerBlock("gravity_block",
        () -> new GravityBlock(BlockBehaviour.Properties.copy(Blocks.BARRIER)
            .noCollission()
            .noOcclusion()
            .noLootTable()
            .strength(-1.0f, 3600000.0f)
            .noParticlesOnBreak()));

    public static final RegistryObject<Block> BRIDGER_BLOCK = registerBlock("bridger_block",
        () -> new BridgerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f)
            .noOcclusion()));

    /**
     * Helper method to register a block and its corresponding BlockItem
     */
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    /**
     * Register a block without a corresponding BlockItem.
     */
    private static <T extends Block> RegistryObject<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
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
