package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.WisteriaPetalsBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Tree decorator that places Lavender Wisteria Petals hanging from the bottom of leaves
 */
public class WisteriaPetalsLavenderDecorator extends TreeDecorator {
    public static final Codec<WisteriaPetalsLavenderDecorator> CODEC = Codec.unit(WisteriaPetalsLavenderDecorator::new);

    public WisteriaPetalsLavenderDecorator() {
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecorators.WISTERIA_PETALS_LAVENDER.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        LevelSimulatedReader level = context.level();

        context.leaves().forEach(blockPos -> {
            BlockPos below = blockPos.below();

            boolean hasLeafAbove = level.isStateAtPosition(blockPos, state ->
                state.is(ModBlocks.WISTERIA_LEAVES.get()) ||
                state.is(ModBlocks.WISTERIA_LEAVES_PINK.get()) ||
                state.is(ModBlocks.WISTERIA_LEAVES_CYAN.get()) ||
                state.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get()) ||
                state.is(ModBlocks.WISTERIA_LEAVES_CREAM.get()) ||
                state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get()) ||
                state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get()) ||
                state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get()) ||
                state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get())
            );

            if (!hasLeafAbove) {
                return;
            }

            if (context.isAir(below) && random.nextFloat() < 0.7f) {
                if (level.isStateAtPosition(below, s -> s.isAir())) {
                    // 15% chance for glowing petals
                    boolean isGlowing = random.nextFloat() < 0.15f;
                    var petalBlock = isGlowing ?
                        ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get() :
                        ModBlocks.WISTERIA_PETALS_LAVENDER.get();

                    BlockState firstPetalState = petalBlock
                            .defaultBlockState()
                            .setValue(WisteriaPetalsBlock.AGE, 4);

                    context.setBlock(below, firstPetalState);

                    int additionalLength = random.nextInt(7);

                    for (int i = 0; i < additionalLength; i++) {
                        BlockPos pos = below.below(i + 1);
                        if (!level.isStateAtPosition(pos, s -> s.isAir())) break;

                        int age = (i == additionalLength - 1) ? random.nextInt(5) : 4;
                        BlockState state = petalBlock
                                .defaultBlockState()
                                .setValue(WisteriaPetalsBlock.AGE, age);

                        context.setBlock(pos, state);
                    }

                    validateAndFixPetalChain(context, level, below, isGlowing);
                }
            }
        });
    }

    private void validateAndFixPetalChain(Context context, LevelSimulatedReader level, BlockPos chainStart, boolean isGlowing) {
        var petalBlock = isGlowing ?
            ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get() :
            ModBlocks.WISTERIA_PETALS_LAVENDER.get();
        // STEP 1: Check if the block directly above the chain start is valid
        BlockPos above = chainStart.above();

        boolean isConnectedToLeaf = level.isStateAtPosition(above, state ->
            state.is(ModBlocks.WISTERIA_LEAVES.get()) ||
            state.is(ModBlocks.WISTERIA_LEAVES_PINK.get()) ||
            state.is(ModBlocks.WISTERIA_LEAVES_CYAN.get()) ||
            state.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get()) ||
            state.is(ModBlocks.WISTERIA_LEAVES_CREAM.get()) ||
            state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get()) ||
            state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get()) ||
            state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get()) ||
            state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get()) ||
            state.getBlock() instanceof WisteriaPetalsBlock
        );

        if (!isConnectedToLeaf) {
            // Try to extend upward to find a valid support (up to 10 blocks)
            boolean foundSupport = false;

            for (int i = 1; i <= 10; i++) {
                BlockPos checkPos = chainStart.above(i);

                boolean hasSupport = level.isStateAtPosition(checkPos, state ->
                    state.is(ModBlocks.WISTERIA_LEAVES.get()) ||
                    state.is(ModBlocks.WISTERIA_LEAVES_PINK.get()) ||
                    state.is(ModBlocks.WISTERIA_LEAVES_CYAN.get()) ||
                    state.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get()) ||
                    state.is(ModBlocks.WISTERIA_LEAVES_CREAM.get()) ||
                    state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get()) ||
                    state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get()) ||
                    state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get()) ||
                    state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get()) ||
                    state.getBlock() instanceof WisteriaPetalsBlock
                );

                if (hasSupport) {
                    // Found support! Fill the gap with petals
                    for (int j = 1; j < i; j++) {
                        BlockPos fillPos = chainStart.above(j);
                        BlockState petalState = petalBlock
                                .defaultBlockState()
                                .setValue(WisteriaPetalsBlock.AGE, 4);
                        context.setBlock(fillPos, petalState);
                    }
                    foundSupport = true;
                    break;
                }

                // Stop if we hit a non-air, non-petal block
                if (!level.isStateAtPosition(checkPos, s -> s.isAir() || s.getBlock() instanceof WisteriaPetalsBlock)) {
                    break;
                }
            }

            // If we didn't find support within 10 blocks, remove the entire chain
            if (!foundSupport) {
                removeEntirePetalChain(context, level, chainStart);
                return;
            }
        }

        // STEP 2: Scan downward and fill any air gaps in the middle of the chain
        BlockPos current = chainStart.below();
        for (int depth = 0; depth < 15; depth++) {
            boolean isPetal = level.isStateAtPosition(current, state ->
                state.getBlock() instanceof WisteriaPetalsBlock
            );

            if (isPetal) {
                // This is a petal, continue checking
                current = current.below();
                continue;
            }

            boolean isAir = level.isStateAtPosition(current, s -> s.isAir());
            if (!isAir) {
                // Hit a solid block, end of chain
                break;
            }

            // Found an air gap! Check if there are more petals below
            boolean hasPetalsBelow = false;
            for (int checkDepth = 1; checkDepth <= 3; checkDepth++) {
                BlockPos checkPos = current.below(checkDepth);
                if (level.isStateAtPosition(checkPos, state -> state.getBlock() instanceof WisteriaPetalsBlock)) {
                    hasPetalsBelow = true;
                    break;
                }
                if (!level.isStateAtPosition(checkPos, s -> s.isAir())) {
                    break;
                }
            }

            if (hasPetalsBelow) {
                BlockState petalState = petalBlock
                        .defaultBlockState()
                        .setValue(WisteriaPetalsBlock.AGE, 4);
                context.setBlock(current, petalState);
            }

            current = current.below();
        }
    }

    private void removeEntirePetalChain(Context context, LevelSimulatedReader level, BlockPos start) {
        BlockPos current = start;

        for (int i = 0; i < 20; i++) {
            boolean isPetal = level.isStateAtPosition(current, state ->
                state.is(ModBlocks.WISTERIA_PETALS.get()) ||
                state.is(ModBlocks.WISTERIA_PETALS_PINK.get()) ||
                state.is(ModBlocks.WISTERIA_PETALS_CYAN.get()) ||
                state.is(ModBlocks.WISTERIA_PETALS_LAVENDER.get()) ||
                state.is(ModBlocks.WISTERIA_PETALS_CREAM.get())
            );

            if (!isPetal) {
                break;
            }

            context.setBlock(current, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            current = current.below();
        }
    }
}
