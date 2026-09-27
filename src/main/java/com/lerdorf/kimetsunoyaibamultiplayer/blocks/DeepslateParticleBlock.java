package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DeepslateParticleBlock extends Block {
    public DeepslateParticleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        spawnDeepslateDestroyParticles(level, player, pos);
    }

    public static void spawnDeepslateDestroyParticles(Level level, Player player, BlockPos pos) {
        level.levelEvent(player, 2001, pos, Block.getId(Blocks.DEEPSLATE.defaultBlockState()));
    }
}
