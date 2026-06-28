package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public class OrochiElixirItem extends AlchemyItem {
    private static final int SEARCH_RADIUS = 4;

    public OrochiElixirItem(Properties properties, int tintColor) {
        super(properties, true, tintColor);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        BlockPos spawnPos = findSafeSpawnPos(serverLevel, origin);
        if (spawnPos == null) {
            spawnPos = origin;
        }

        OrochiEntity.discardOwnedOrochi(serverPlayer);

        OrochiEntity orochi = ModEntities.OROCHI.get().create(serverLevel);
        if (orochi == null) {
            return InteractionResult.FAIL;
        }

        orochi.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
        orochi.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, serverPlayer.getYRot(), 0.0F);
        orochi.tame(serverPlayer);
        orochi.setOrderedToSit(false);
        orochi.setPersistenceRequired();
        orochi.setHealth(orochi.getMaxHealth());

        if (!serverLevel.addFreshEntity(orochi)) {
            return InteractionResult.FAIL;
        }

        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.55F, 0.9F);

        ItemStack stack = context.getItemInHand();
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                serverPlayer.setItemInHand(context.getHand(), new ItemStack(ModAlchemyItems.EMPTY_VIAL.get()));
            }
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos origin) {
        if (isSafeSpawnPos(level, origin)) {
            return origin;
        }

        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (BlockPos.MutableBlockPos mutable : BlockPos.spiralAround(origin, radius, Direction.EAST, Direction.SOUTH)) {
                BlockPos candidate = mutable.immutable();
                if (isSafeSpawnPos(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isSafeSpawnPos(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty()
            && !floor.getCollisionShape(level, pos.below()).isEmpty()
            && !level.getFluidState(pos).isSource();
    }
}
