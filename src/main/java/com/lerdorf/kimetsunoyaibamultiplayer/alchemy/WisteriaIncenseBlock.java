package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ModBlockEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.WisteriaIncenseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class WisteriaIncenseBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final IntegerProperty BURN_STAGE = IntegerProperty.create("burn_stage", 0, 7);
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);

    private static final VoxelShape INCENSE_SHAPE = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 11.0D, 9.0D);
    private static final VoxelShape POTTED_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);
    private static final double NORMAL_SMOKE_Y = 10.5D / 16.0D;
    private static final double POTTED_SMOKE_Y = 13.0D / 16.0D;
    private static final IncensePose[][] NORMAL_INCENSE_POSES = {
        {},
        {new IncensePose(8.0D, 8.0D, 0.0F)},
        {
            new IncensePose(7.0D, 8.0D, -12.5F),
            new IncensePose(9.0D, 8.0D, 12.5F)
        },
        {
            new IncensePose(8.0D, 6.9D, 0.0F),
            new IncensePose(7.1D, 8.9D, -18.0F),
            new IncensePose(8.9D, 8.9D, 18.0F)
        },
        {
            new IncensePose(7.0D, 7.0D, -18.0F),
            new IncensePose(9.0D, 7.0D, 18.0F),
            new IncensePose(7.0D, 9.0D, 18.0F),
            new IncensePose(9.0D, 9.0D, -18.0F)
        }
    };
    private static final double[][] POTTED_SMOKE_OFFSETS_1 = {{8.0D / 16.0D, 8.0D / 16.0D}};
    private static final double[][] POTTED_SMOKE_OFFSETS_2 = {
        {6.9D / 16.0D, 8.0D / 16.0D},
        {9.1D / 16.0D, 8.0D / 16.0D}
    };
    private static final double[][] POTTED_SMOKE_OFFSETS_3 = {
        {8.0D / 16.0D, 6.8D / 16.0D},
        {7.1D / 16.0D, 8.9D / 16.0D},
        {8.9D / 16.0D, 8.9D / 16.0D}
    };
    private static final double[][] POTTED_SMOKE_OFFSETS_4 = {
        {7.0D / 16.0D, 7.0D / 16.0D},
        {9.0D / 16.0D, 7.0D / 16.0D},
        {7.0D / 16.0D, 9.0D / 16.0D},
        {9.0D / 16.0D, 9.0D / 16.0D}
    };

    private final boolean potted;

    public WisteriaIncenseBlock(Properties properties, boolean potted) {
        super(properties);
        this.potted = potted;
        registerDefaultState(stateDefinition.any()
            .setValue(LIT, false)
            .setValue(BURN_STAGE, 0)
            .setValue(COUNT, 1));
    }

    public boolean isPotted() {
        return potted;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return potted ? POTTED_SHAPE : INCENSE_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return potted || Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (!potted && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModAlchemyBlocks.WISTERIA_INCENSE.get().asItem()) && state.getValue(COUNT) < 4) {
            if (!level.isClientSide) {
                addIncense(level, pos, state, player, stack);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!state.getValue(LIT) && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (!level.isClientSide) {
                light(level, pos, state);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
                } else if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && !state.getValue(LIT) && projectile instanceof AbstractArrow && projectile.isOnFire()) {
            light(level, hit.getBlockPos(), state);
        }
        super.onProjectileHit(level, state, hit, projectile);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        double[][] offsets = getSmokeOffsets(state);
        double y = pos.getY() + (potted ? POTTED_SMOKE_Y : NORMAL_SMOKE_Y);
        for (double[] offset : offsets) {
            double x = pos.getX() + offset[0];
            double z = pos.getZ() + offset[1];
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.035D, 0.0D);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return potted ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(LIT)) {
            return potted ? List.of(new ItemStack(Items.FLOWER_POT)) : List.of();
        }

        if (potted) {
            return List.of(
                new ItemStack(Items.FLOWER_POT),
                new ItemStack(ModAlchemyBlocks.WISTERIA_INCENSE.get(), state.getValue(COUNT))
            );
        }
        return List.of(new ItemStack(ModAlchemyBlocks.WISTERIA_INCENSE.get(), state.getValue(COUNT)));
    }

    public static void addIncense(Level level, BlockPos pos, BlockState state, @Nullable Player player, ItemStack stack) {
        level.setBlock(pos, state.setValue(COUNT, state.getValue(COUNT) + 1), 3);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public static IncensePose[] getNormalIncensePoses(int count) {
        return NORMAL_INCENSE_POSES[Math.max(1, Math.min(count, 4))];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WisteriaIncenseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.WISTERIA_INCENSE.get(), WisteriaIncenseBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, BURN_STAGE, COUNT);
    }

    public static void light(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof WisteriaIncenseBlock && !state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), 3);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WisteriaIncenseBlockEntity incense) {
                incense.resetBurnTicks();
            }
        }
    }

    public static void burnOut(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof WisteriaIncenseBlock incense && incense.isPotted()) {
            level.setBlock(pos, Blocks.FLOWER_POT.defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    private double[][] getSmokeOffsets(BlockState state) {
        if (!potted) {
            return normalSmokeOffsets(state.getValue(COUNT));
        }
        return switch (state.getValue(COUNT)) {
            case 2 -> POTTED_SMOKE_OFFSETS_2;
            case 3 -> POTTED_SMOKE_OFFSETS_3;
            case 4 -> POTTED_SMOKE_OFFSETS_4;
            default -> POTTED_SMOKE_OFFSETS_1;
        };
    }

    private static double[][] normalSmokeOffsets(int count) {
        IncensePose[] poses = getNormalIncensePoses(count);
        double[][] offsets = new double[poses.length][2];
        for (int index = 0; index < poses.length; index++) {
            offsets[index][0] = poses[index].x() / 16.0D;
            offsets[index][1] = poses[index].z() / 16.0D;
        }
        return offsets;
    }

    public record IncensePose(double x, double z, float yRotation) {
    }
}
