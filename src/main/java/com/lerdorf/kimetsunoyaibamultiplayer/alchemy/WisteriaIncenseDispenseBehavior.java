package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public final class WisteriaIncenseDispenseBehavior {
    private WisteriaIncenseDispenseBehavior() {
    }

    public static void register() {
        DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.getLevel();
                this.setSuccess(true);
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos targetPos = source.getPos().relative(direction);
                BlockState targetState = level.getBlockState(targetPos);

                if (lightIncense(level, targetPos, targetState, stack)) {
                    return stack;
                }

                if (BaseFireBlock.canBePlacedAt(level, targetPos, direction)) {
                    level.setBlockAndUpdate(targetPos, BaseFireBlock.getState(level, targetPos));
                    level.gameEvent(null, GameEvent.BLOCK_PLACE, targetPos);
                } else if (!CampfireBlock.canLight(targetState) && !CandleBlock.canLight(targetState) && !CandleCakeBlock.canLight(targetState)) {
                    Direction fireDirection = source.getBlockState().getValue(DispenserBlock.FACING).getOpposite();
                    if (targetState.isFlammable(level, targetPos, fireDirection)) {
                        targetState.onCaughtFire(level, targetPos, fireDirection, null);
                        if (targetState.getBlock() instanceof TntBlock) {
                            level.removeBlock(targetPos, false);
                        }
                    } else {
                        this.setSuccess(false);
                    }
                } else {
                    level.setBlockAndUpdate(targetPos, targetState.setValue(BlockStateProperties.LIT, true));
                    level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
                }

                if (this.isSuccess() && stack.hurt(1, level.random, (ServerPlayer) null)) {
                    stack.setCount(0);
                }
                return stack;
            }
        });

        DispenserBlock.registerBehavior(Items.FIRE_CHARGE, new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos targetPos = source.getPos().relative(direction);
                BlockState targetState = source.getLevel().getBlockState(targetPos);
                if (lightIncense(source.getLevel(), targetPos, targetState, stack)) {
                    return stack;
                }

                Position position = DispenserBlock.getDispensePosition(source);
                double x = position.x() + (double) ((float) direction.getStepX() * 0.3F);
                double y = position.y() + (double) ((float) direction.getStepY() * 0.3F);
                double z = position.z() + (double) ((float) direction.getStepZ() * 0.3F);
                Level level = source.getLevel();
                RandomSource random = level.random;
                double dx = random.triangle(direction.getStepX(), 0.11485000000000001D);
                double dy = random.triangle(direction.getStepY(), 0.11485000000000001D);
                double dz = random.triangle(direction.getStepZ(), 0.11485000000000001D);
                SmallFireball fireball = new SmallFireball(level, x, y, z, dx, dy, dz);
                level.addFreshEntity(Util.make(fireball, entity -> entity.setItem(stack)));
                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.getLevel().levelEvent(1018, source.getPos(), 0);
            }
        });
    }

    private static boolean lightIncense(Level level, BlockPos targetPos, BlockState targetState, ItemStack stack) {
        if (!(targetState.getBlock() instanceof WisteriaIncenseBlock) || targetState.getValue(WisteriaIncenseBlock.LIT)) {
            return false;
        }

        WisteriaIncenseBlock.light(level, targetPos, targetState);
        level.playSound(null, targetPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.gameEvent((Entity) null, GameEvent.BLOCK_CHANGE, targetPos);
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (stack.hurt(1, level.random, (ServerPlayer) null)) {
                stack.setCount(0);
            }
        } else {
            stack.shrink(1);
        }
        return true;
    }
}
