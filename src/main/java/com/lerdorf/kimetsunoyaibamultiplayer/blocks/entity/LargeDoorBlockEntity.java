package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.LargeDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LargeDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int TRANSITION_TICKS = 10;
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("closed");
    private static final RawAnimation OPENING = RawAnimation.begin().thenPlay("opening");
    private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("open");
    private static final RawAnimation CLOSING = RawAnimation.begin().thenPlay("closing");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private DoorPhase phase;
    private int transitionTicks;
    private DoorPhase lastAnimationPhase = DoorPhase.CLOSED;

    public LargeDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_DOOR.get(), pos, state);
        phase = state.getValue(LargeDoorBlock.OPEN) ? DoorPhase.OPEN : DoorPhase.CLOSED;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0D, 2.0D, 2.0D);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeDoorBlockEntity door) {
        door.tick(level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LargeDoorBlockEntity door) {
        door.tick(level);
    }

    public void toggle() {
        if (level == null || phase == DoorPhase.OPENING || phase == DoorPhase.CLOSING) {
            return;
        }

        boolean opening = phase == DoorPhase.CLOSED;
        phase = opening ? DoorPhase.OPENING : DoorPhase.CLOSING;
        transitionTicks = TRANSITION_TICKS;
        LargeDoorBlock.setPhase(level, worldPosition, getBlockState(), opening ? 1 : 3);
        sync();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "door_controller", 0, state -> {
            DoorPhase animationPhase = phaseFromBlockState();
            if (animationPhase != lastAnimationPhase) {
                state.getController().forceAnimationReset();
                lastAnimationPhase = animationPhase;
            }
            return state.setAndContinue(animationFor(animationPhase));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Phase", phase.name());
        tag.putInt("TransitionTicks", transitionTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        try {
            phase = DoorPhase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException exception) {
            phase = getBlockState().getValue(LargeDoorBlock.OPEN) ? DoorPhase.OPEN : DoorPhase.CLOSED;
        }
        transitionTicks = tag.getInt("TransitionTicks");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tick(Level level) {
        if (phase != DoorPhase.OPENING && phase != DoorPhase.CLOSING) {
            return;
        }

        transitionTicks--;
        if (transitionTicks > 0) {
            return;
        }

        boolean opening = phase == DoorPhase.OPENING;
        phase = opening ? DoorPhase.OPEN : DoorPhase.CLOSED;
        LargeDoorBlock.setPhase(level, worldPosition, getBlockState(), opening ? 2 : 0);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static RawAnimation animationFor(DoorPhase phase) {
        return switch (phase) {
            case CLOSED -> CLOSED;
            case OPENING -> OPENING;
            case OPEN -> OPEN;
            case CLOSING -> CLOSING;
        };
    }

    private DoorPhase phaseFromBlockState() {
        return switch (getBlockState().getValue(LargeDoorBlock.ANIMATION)) {
            case 1 -> DoorPhase.OPENING;
            case 2 -> DoorPhase.OPEN;
            case 3 -> DoorPhase.CLOSING;
            default -> DoorPhase.CLOSED;
        };
    }

    private enum DoorPhase {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }
}
