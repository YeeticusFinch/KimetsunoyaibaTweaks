package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Hanging wisteria petals that generate beneath wisteria leaves.
 * Similar to weeping vines or glow berries - hangs down and is passable.
 * Can be grown with bonemeal.
 */
public class WisteriaPetalsBlock extends Block implements BonemealableBlock {
    // Age property to control how long the vine hangs (0-4 for more variety)
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;

    // Shape - very thin so player can walk through
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public WisteriaPetalsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // No collision - player can walk through
        return Shapes.empty();
    }

    /**
     * Check if this block can survive at the given position
     * Must have a solid block above (wisteria leaves or another petal block)
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);

        // Can attach to ANY wisteria leaves (all colors), any petal block, or any sturdy down face
        return aboveState.is(ModBlocks.WISTERIA_LEAVES.get()) ||
               aboveState.is(ModBlocks.WISTERIA_LEAVES_PINK.get()) ||
               aboveState.is(ModBlocks.WISTERIA_LEAVES_CYAN.get()) ||
               aboveState.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get()) ||
               aboveState.is(ModBlocks.WISTERIA_LEAVES_CREAM.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get()) ||
               aboveState.is(ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get()) ||
               aboveState.getBlock() instanceof WisteriaPetalsBlock ||
               (!aboveState.isAir() && aboveState.isFaceSturdy(level, above, Direction.DOWN));
    }

    /**
     * Update block when neighbor changes
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // If the block above is removed, break this block
        if (direction == Direction.UP && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Can this block be placed at the given position?
     */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true; // Can be replaced by other blocks
    }

    /**
     * Called after the block is placed by a player
     * If placed below another petal, set the petal above to max age (4)
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Check if there's a petal block directly above
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);

        // If the block above is also a wisteria petal, set it to max age
        if (aboveState.getBlock() instanceof WisteriaPetalsBlock) {
            level.setBlock(above, aboveState.setValue(AGE, 4), 3);
        }
    }

    /**
     * Random tick for growth mechanics - DISABLED to prevent natural growth
     * Petals should only grow with bonemeal or player placement
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Natural growth disabled - petals only grow via bonemeal or player placement
    }

    /**
     * Animate falling particles
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Occasionally drop purple particles
        if (random.nextFloat() < 0.3f) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double y = pos.getY() - 0.05;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;

            // Use cherry leaves particles for purple petals
            level.addParticle(
                ParticleTypes.CHERRY_LEAVES,
                x, y, z,
                0, -0.02, 0  // Slow falling
            );
        }
    }

    /**
     * This block can be replaced by other blocks (like tall grass)
     */
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return true;
    }

    /**
     * Damage and repel monsters that touch the petals
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        // If a demon touches the petals, damage and debuff them (includes players turned into demons)
        if (entity instanceof LivingEntity living && com.lerdorf.kimetsunoyaibamultiplayer.Damager.isDemon(living)) {
            // Deal 1.5 hearts of damage (3.0) every second (20 ticks)
            if (level.getGameTime() % 20 == 0) {
                living.hurt(level.damageSources().magic(), 3.0f);
            }

            // Get wisteria poison effect from KnY mod
            net.minecraft.world.effect.MobEffect wisteriaPoison =
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.getWisteriaPoisonEffect();

            // Apply fear effects (8 seconds)
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, false));
            if (wisteriaPoison != null) {
                living.addEffect(new MobEffectInstance(wisteriaPoison, 160, 1, false, false));
            }

            // Push demon away from petals
            double dx = living.getX() - (pos.getX() + 0.5);
            double dz = living.getZ() - (pos.getZ() + 0.5);
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 0 && distance < 2) {
                double pushStrength = 0.25;
                double normalizedX = dx / distance;
                double normalizedZ = dz / distance;

                living.push(normalizedX * pushStrength, 0.1, normalizedZ * pushStrength);
            }
        }
    }

    // ========== BonemealableBlock Implementation ==========

    /**
     * Check if bonemeal can be applied to this block
     * Can only bonemeal if there's empty space below to grow
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClientSide) {
        return level.isEmptyBlock(pos.below());
    }

    /**
     * Check if bonemeal application will succeed (random chance)
     */
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true; // Always succeeds if there's space below
    }

    /**
     * Perform the bonemeal growth - grow the vine downward
     */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);

        if (age >= 4 && level.isEmptyBlock(pos.below())) {
            // At max age, grow a new petal downward
            level.setBlock(pos.below(), this.defaultBlockState().setValue(AGE, 0), 3);
        } else if (age < 4) {
            // Not at max age yet, just age in place
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        }
    }
}
