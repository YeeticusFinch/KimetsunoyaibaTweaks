package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LotusBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean hasLilypad;

    public LotusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOTUS.get(), pos, state);
    }

    public static LotusBlockEntity create(BlockPos pos, BlockState state) {
        return new LotusBlockEntity(pos, state);
    }

    public boolean hasLilypad() {
        return hasLilypad;
    }

    public void setHasLilypad(boolean hasLilypad) {
        if (this.hasLilypad == hasLilypad) {
            return;
        }

        this.hasLilypad = hasLilypad;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("HasLilypad", hasLilypad);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hasLilypad = tag.getBoolean("HasLilypad");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The lotus is a static model.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
