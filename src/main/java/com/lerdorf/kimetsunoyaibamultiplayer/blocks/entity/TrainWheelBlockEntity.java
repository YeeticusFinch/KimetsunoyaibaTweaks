package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

public class TrainWheelBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String SPIN_DIRECTION_TAG = "SpinDirection";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String spinDirection = RIGHT;

    public TrainWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAIN_WHEEL.get(), pos, state);
    }

    public String getSpinDirection() {
        return spinDirection;
    }

    public boolean isLeftSpinning() {
        return LEFT.equals(spinDirection);
    }

    public void toggleSpinDirection() {
        setSpinDirection(LEFT.equals(spinDirection) ? RIGHT : LEFT);
    }

    private void setSpinDirection(String direction) {
        spinDirection = LEFT.equals(direction) ? LEFT : RIGHT;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Wheel timing is driven by the shared level clock in TrainWheelModel.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(SPIN_DIRECTION_TAG, spinDirection);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        spinDirection = LEFT.equals(tag.getString(SPIN_DIRECTION_TAG)) ? LEFT : RIGHT;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
