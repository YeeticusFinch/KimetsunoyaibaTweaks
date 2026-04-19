package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base class for GeckoLib-animated items (non-sword items like sheaths, cosmetics, etc.).
 *
 * This class provides common functionality for all geckolib items:
 * - Animation management based on NBT
 * - Per-stack animatable IDs
 * - Custom renderer support
 *
 * Subclasses must implement:
 * - getRendererSupplier() to provide their custom renderer
 */
public abstract class GeckolibItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String ANIM_NBT_KEY = "GeckolibItemAnim";
    private static final String DEFAULT_ANIM = "idle";
    private static final AtomicLong CLIENT_ID_ALLOCATOR = new AtomicLong(Long.MIN_VALUE + 1L);

    // Cached animations to prevent recreation every frame
    private static final Map<String, RawAnimation> CACHED_LOOPING_ANIMATIONS = new HashMap<>();
    private static final Map<String, RawAnimation> CACHED_ONESHOT_ANIMATIONS = new HashMap<>();

    /**
     * Get the current animation name from the ItemStack NBT.
     */
    public static String getAnimationFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_ANIM;
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ANIM_NBT_KEY, Tag.TAG_STRING)) {
            String animName = tag.getString(ANIM_NBT_KEY);
            if (!animName.isEmpty()) {
                return animName;
            }
        }

        return DEFAULT_ANIM;
    }

    /**
     * Store the current animation name on the ItemStack NBT.
     */
    public static void setAnimationOnStack(ItemStack stack, String animationName) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (animationName == null || animationName.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(ANIM_NBT_KEY);
            }
            return;
        }

        stack.getOrCreateTag().putString(ANIM_NBT_KEY, animationName);
    }

    /**
     * Ensure each stack has a unique GeckoLib instance id.
     */
    public static void ensureAnimatableId(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || level == null) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(GeoItem.ID_NBT_KEY, Tag.TAG_LONG)) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
            return;
        }

        if (level.isClientSide) {
            long id = CLIENT_ID_ALLOCATOR.getAndIncrement();
            stack.getOrCreateTag().putLong(GeoItem.ID_NBT_KEY, id);
        }
    }

    public GeckolibItem(Properties properties) {
        super(properties);
    }

    /**
     * Get the renderer supplier for this item.
     * Subclasses must implement this to provide their custom renderer.
     *
     * @return Supplier that creates a new instance of the custom renderer
     */
    protected abstract Supplier<BlockEntityWithoutLevelRenderer> getRendererSupplier();

    /**
     * Whether this item should drive a GeckoLib animation controller.
     * Some geo-rendered items are static models and should not attempt to resolve animations.
     */
    protected boolean supportsGeckolibItemAnimations(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        // Ensure per-stack ids so multiple items don't share a single animation timeline.
        ensureAnimatableId(stack, level);
    }

    // ==================== GeckoLib Implementation ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Standard controller that reads from NBT
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);

            if (!supportsGeckolibItemAnimations(stack)) {
                state.getController().forceAnimationReset();
                return software.bernie.geckolib.core.object.PlayState.STOP;
            }

            // For items, use the NBT-based system
            String animName = getAnimationFromStack(stack);

            // Default to idle if no animation specified
            if (animName == null || animName.isEmpty() || animName.equals("empty")) {
                animName = "idle";
            }

            // Only change animation if it's different from current
            if (state.getController().getCurrentAnimation() != null) {
                String currentAnim = state.getController().getCurrentAnimation().animation().name();
                if (currentAnim != null && currentAnim.equals(animName)) {
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                }
            }

            // Determine if it's a looping animation
            boolean isLooping = animName.equals("idle") || animName.equals("walk") ||
                               animName.equals("sprint") || animName.equals("sheath");

            // Return the animation state
            if (isLooping) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(animName));
            } else {
                return state.setAndContinue(RawAnimation.begin().thenPlay(animName).thenLoop("idle"));
            }
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
                if (this.renderer == null) {
                    this.renderer = getRendererSupplier().get();
                }
                return this.renderer;
            }
        });
    }
}
