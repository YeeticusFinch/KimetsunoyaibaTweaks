package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.LotusBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.LotusBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.LotusItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class LotusBlockItem extends BlockItem implements GeoItem, Equipable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LotusBlockItem(LotusBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult waterResult = tryPlaceOnWater(level, player, hand);
        if (waterResult.consumesAction()) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
            context.getClickedPos(), context.isInside());
        return tryPlaceOnWater(context.getLevel(), context.getPlayer(), context.getHand(), hit)
            .consumesAction()
            ? InteractionResult.SUCCESS
            : super.useOn(context);
    }

    private InteractionResult tryPlaceOnWater(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        return tryPlaceOnWater(level, player, hand, hit);
    }

    private InteractionResult tryPlaceOnWater(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos waterPos = hit.getBlockPos();
        BlockState waterState = level.getBlockState(waterPos);
        if (!waterState.is(Blocks.WATER)
            || (waterState.hasProperty(BlockStateProperties.FALLING)
                && waterState.getValue(BlockStateProperties.FALLING))
            || level.getFluidState(waterPos).getType() != Fluids.WATER
            || !level.getFluidState(waterPos).isSource()
            || !level.isEmptyBlock(waterPos.above())) {
            return InteractionResult.PASS;
        }

        return place(new LotusPlaceContext(level, player, hand, player.getItemInHand(hand), waterPos.above()));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        boolean onLilypad = !context.replacingClickedOnBlock()
            && context.getClickedFace() == Direction.UP
            && context.getLevel().getBlockState(context.getClickedPos().below()).is(Blocks.LILY_PAD);
        if (onLilypad) {
            context = new LotusPlaceContext(context, context.getClickedPos().below());
        }
        InteractionResult result = super.place(context);
        if (onLilypad && result.consumesAction() && !context.getLevel().isClientSide
            && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LotusBlockEntity blockEntity) {
            blockEntity.setHasLilypad(true);
        }
        return result;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The lotus is a static model.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new LotusItemRenderer();
                }
                return renderer;
            }
        });
    }

    private static final class LotusPlaceContext extends BlockPlaceContext {
        private LotusPlaceContext(Level level, Player player, InteractionHand hand, ItemStack stack, BlockPos position) {
            super(level, player, hand, stack,
                new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false));
            this.replaceClicked = true;
        }

        private LotusPlaceContext(UseOnContext context, BlockPos position) {
            this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), position);
        }

        private LotusPlaceContext(BlockPlaceContext context, BlockPos position) {
            this((UseOnContext) context, position);
        }
    }
}
