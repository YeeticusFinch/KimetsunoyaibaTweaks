package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<BlockEntityType<ChestOfDrawersBlockEntity>> CHEST_OF_DRAWERS =
        BLOCK_ENTITIES.register("chest_of_drawers",
            () -> BlockEntityType.Builder.of(ChestOfDrawersBlockEntity::new, ModBlocks.CHEST_OF_DRAWERS.get()).build(null));

    public static final RegistryObject<BlockEntityType<AlchemyTableBlockEntity>> ALCHEMY_TABLE =
        BLOCK_ENTITIES.register("alchemy_table",
            () -> BlockEntityType.Builder.of(AlchemyTableBlockEntity::new, ModAlchemyBlocks.ALCHEMY_TABLE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
