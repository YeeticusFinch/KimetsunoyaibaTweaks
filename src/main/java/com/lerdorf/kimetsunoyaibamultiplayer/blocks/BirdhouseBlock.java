package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public class BirdhouseBlock extends Block {
    public static final BooleanProperty FULL = BooleanProperty.create("full");
    private static final int TICKS_PER_DAY = 24000;
    private static final VoxelShape SHAPE = Block.box(2.96D, 0.0D, 2.96D, 13.04D, 32.0D, 13.04D);
    private static final ResourceLocation KASUGAI_CROW =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kasugai_crow");

    public BirdhouseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (state.getValue(FULL) || !isSeed(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(FULL, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 1 + level.random.nextInt(TICKS_PER_DAY));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BARREL_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FULL)) {
            return;
        }

        EntityType<?> crowType = ForgeRegistries.ENTITY_TYPES.getValue(KASUGAI_CROW);
        if (crowType == null) {
            return;
        }

        Entity crow = crowType.create(level);
        if (crow == null) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 2.0D;
        double z = pos.getZ() + 0.5D;
        if (crow instanceof Mob mob) {
            mob.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
        } else {
            crow.setPos(x, y, z);
        }
        if (level.addFreshEntity(crow)) {
            level.setBlock(pos, state.setValue(FULL, false), Block.UPDATE_ALL);
        }
    }

    private static boolean isSeed(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS)
            || stack.is(Items.MELON_SEEDS)
            || stack.is(Items.PUMPKIN_SEEDS)
            || stack.is(Items.BEETROOT_SEEDS);
    }
}
