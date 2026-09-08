package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SixEyeDemonHeadBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SixEyeDemonHeadBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("move");
    private static final RawAnimation STATIONARY = RawAnimation.begin().thenLoop("stationary");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean lastPowered;

    public SixEyeDemonHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIX_EYE_DEMON_HEAD.get(), pos, state);
        lastPowered = state.getValue(SixEyeDemonHeadBlock.POWERED);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "head_controller", 0, state -> {
            boolean powered = getBlockState().getValue(SixEyeDemonHeadBlock.POWERED);
            if (powered != lastPowered) {
                state.getController().forceAnimationReset();
                lastPowered = powered;
            }
            return state.setAndContinue(powered ? MOVE : STATIONARY);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
