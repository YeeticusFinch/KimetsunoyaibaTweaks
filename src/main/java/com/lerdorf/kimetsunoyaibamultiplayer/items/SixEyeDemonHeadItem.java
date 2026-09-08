package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.SixEyeDemonHeadItemRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SixEyeDemonHeadBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class SixEyeDemonHeadItem extends BlockItem implements GeoItem, Equipable {
    private static final String ANIMATION_KEY = "SixEyeDemonHeadAnimation";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SixEyeDemonHeadItem(SixEyeDemonHeadBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof LivingEntity living) || living.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            stack.getOrCreateTag().putString(ANIMATION_KEY, "stationary");
            return;
        }

        double horizontalSpeed = Math.sqrt(living.getDeltaMovement().horizontalDistanceSqr());
        double normalizedSpeed = Math.min(horizontalSpeed / 0.18D, 2.5D);
        stack.getOrCreateTag().putString(ANIMATION_KEY, normalizedSpeed > 0.02D ? "move" : "stationary");
        stack.getOrCreateTag().putFloat("SixEyeDemonHeadAnimationSpeed",
            (float) Math.max(0.65D, Math.min(2.5D, 0.65D + normalizedSpeed)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "head_item_controller", 0, state -> {
            ItemStack stack = state.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            String animation = stack.getTag() == null ? "stationary" : stack.getTag().getString(ANIMATION_KEY);
            if (!"move".equals(animation)) {
                animation = "stationary";
            }
            float animationSpeed = stack.getTag() == null ? 1.0F
                : stack.getTag().getFloat("SixEyeDemonHeadAnimationSpeed");
            state.getController().setAnimationSpeed(animationSpeed > 0.0F ? animationSpeed : 1.0F);
            return state.setAndContinue(RawAnimation.begin().thenLoop(animation));
        }));
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
                    renderer = new SixEyeDemonHeadItemRenderer();
                }
                return renderer;
            }
        });
    }
}
