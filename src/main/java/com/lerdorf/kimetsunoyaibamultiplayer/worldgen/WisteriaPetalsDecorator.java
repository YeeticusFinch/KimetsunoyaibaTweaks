package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.WisteriaPetalsBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Tree decorator that places Wisteria Petals hanging from the bottom of leaves
 */
public class WisteriaPetalsDecorator extends TreeDecorator {
    public static final Codec<WisteriaPetalsDecorator> CODEC = Codec.unit(WisteriaPetalsDecorator::new);

    public WisteriaPetalsDecorator() {
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecorators.WISTERIA_PETALS.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        LevelSimulatedReader level = context.level();

        // Iterate through all leaf blocks
        context.leaves().forEach(blockPos -> {
            BlockPos below = blockPos.below();

            // IMPORTANT: Verify the leaf block still exists before placing petals
            // This prevents petals from spawning when leaves were removed or replaced
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

            // Only place petals if there's actually a wisteria leaf above
            if (!hasLeafAbove) {
                return;
            }

            // Check if the block below is air (70% chance to place petals - increased from 50%)
            if (context.isAir(below) && random.nextFloat() < 0.7f) {
                if (level.isStateAtPosition(below, s -> s.isAir())) {
                    // Always place the first petal directly below the leaf (at age 4 for full appearance)
                    BlockState firstPetalState = ModBlocks.WISTERIA_PETALS.get()
                            .defaultBlockState()
                            .setValue(WisteriaPetalsBlock.AGE, 4);

                    context.setBlock(below, firstPetalState);

                    // Then possibly continue the chain downward with random length (0-6 additional blocks - increased from 0-4)
                    int additionalLength = random.nextInt(7);

                    for (int i = 0; i < additionalLength; i++) {
                        BlockPos pos = below.below(i + 1);

                        // Stop if we hit something solid
                        if (!level.isStateAtPosition(pos, s -> s.isAir())) break;

                        int age;
                        if (i == additionalLength - 1) {
                            // Last petal has random age (0-4) for natural variation
                            age = random.nextInt(5);
                        } else {
                            // All other petals are max age (4) so they look full
                            age = 4;
                        }

                        BlockState state = ModBlocks.WISTERIA_PETALS.get()
                                .defaultBlockState()
                                .setValue(WisteriaPetalsBlock.AGE, age);

                        context.setBlock(pos, state);
                    }

                    // POST-VALIDATION: Verify the chain is connected to a leaf at the top
                    // If not, try to extend upward to connect, or remove the chain
                    validateAndFixPetalChain(context, level, below, blockPos);
                }
            }
        });
    }

    /**
     * Validates that a petal chain is properly connected to a leaf.
     * If not connected, tries to extend upward to find a leaf (up to 5 blocks).
     * If still not connected, removes the entire chain.
     */
    private void validateAndFixPetalChain(Context context, LevelSimulatedReader level,
                                          BlockPos chainStart, BlockPos originalLeafPos) {
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

                // Check if this position has a valid support block
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
                        BlockState petalState = ModBlocks.WISTERIA_PETALS.get()
                                .defaultBlockState()
                                .setValue(WisteriaPetalsBlock.AGE, 4);
                        context.setBlock(fillPos, petalState);
                    }
                    foundSupport = true;
                    break;
                }

                // Stop if we hit a non-air, non-petal, non-leaf block
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
            BlockPos belowGap = current.below();
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
                // Fill this gap
                BlockState petalState = ModBlocks.WISTERIA_PETALS.get()
                        .defaultBlockState()
                        .setValue(WisteriaPetalsBlock.AGE, 4);
                context.setBlock(current, petalState);
            }

            current = current.below();
        }
    }

    /**
     * Removes an entire petal chain by scanning downward and removing all connected petals
     */
    private void removeEntirePetalChain(Context context, LevelSimulatedReader level, BlockPos start) {
        BlockPos current = start;

        // Remove petals going downward until we hit a non-petal block
        for (int i = 0; i < 20; i++) { // Max 20 blocks down to prevent infinite loop
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

            // Remove this petal
            context.setBlock(current, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            current = current.below();
        }
    }
}
