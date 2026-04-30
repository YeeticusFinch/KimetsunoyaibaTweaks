package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class AlchemyFlowerBlock extends FlowerBlock {
    public AlchemyFlowerBlock(Supplier<MobEffect> suspiciousEffect, int effectDuration, Properties properties) {
        super(suspiciousEffect, effectDuration, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.SOUL_SAND);
    }
}
