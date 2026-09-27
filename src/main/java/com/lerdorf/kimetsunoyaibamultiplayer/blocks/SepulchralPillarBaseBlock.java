package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class SepulchralPillarBaseBlock extends DeepslateParticleBlock {
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");

    public SepulchralPillarBaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UPSIDE_DOWN, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(UPSIDE_DOWN, context.getClickedFace() == Direction.DOWN);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UPSIDE_DOWN);
    }
}
