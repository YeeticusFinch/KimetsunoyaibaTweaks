package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * Base class for GeckoLib-animated breathing swords.
 *
 * This class provides common functionality for all geckolib breathing swords:
 * - Animation management based on player/entity state
 * - NBT-based animation storage
 * - Per-stack animatable IDs
 * - Custom attribute modifiers
 *
 * Subclasses must:
 * - Implement getBreathingTechnique() to provide their breathing style
 * - Implement getRendererSupplier() to provide their custom renderer
 * - Override getAttackDamage() and getAttackSpeed() if custom values are needed
 */
public abstract class GeckolibBreathingSwordItem extends BreathingSwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String ANIM_NBT_KEY = "GeckolibSwordAnim";
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

    public GeckolibBreathingSwordItem(Properties properties) {
        super(properties);
    }

    /**
     * Get the attack damage for this sword.
     * Override to customize per sword type.
     *
     * @return Attack damage (default: 7.0)
     */
    protected double getAttackDamage() {
        return 7.0;
    }

    /**
     * Get the attack speed for this sword.
     * Override to customize per sword type.
     *
     * @return Attack speed (default: -2.4)
     */
    protected double getAttackSpeed() {
        return -2.4;
    }

    /**
     * Get the renderer supplier for this sword.
     * Subclasses must implement this to provide their custom renderer.
     *
     * @return Supplier that creates a new instance of the custom renderer
     */
    protected abstract Supplier<BlockEntityWithoutLevelRenderer> getRendererSupplier();

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add (damage - 1) to make total = damage
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    getAttackDamage() - 1.0, AttributeModifier.Operation.ADDITION));

            // Attack speed
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    getAttackSpeed(), AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        // Ensure per-stack ids so multiple swords don't share a single animation timeline.
        ensureAnimatableId(stack, level);
    }

    // ==================== GeckoLib Implementation ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Standard controller that reads from NBT for players
        // For entities, uses EntitySwordAnimationCache
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);

            // Check if we're rendering for an entity
            net.minecraft.world.entity.LivingEntity currentEntity =
                com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.getCurrentEntity();

            if (currentEntity != null && !(currentEntity instanceof net.minecraft.world.entity.player.Player)) {
                // For entities, read the animation directly from the cache
                String entityAnim = com.lerdorf.kimetsunoyaibamultiplayer.client.EntitySwordAnimationCache.getAnimation(currentEntity);

                // Determine if it's a looping animation
                boolean isLooping = entityAnim.equals("idle") || entityAnim.equals("walk") ||
                                   entityAnim.equals("sprint") || entityAnim.equals("sheath");

                // Use cached RawAnimation instances to prevent recreation every frame
                if (isLooping) {
                    RawAnimation anim = CACHED_LOOPING_ANIMATIONS.computeIfAbsent(entityAnim,
                        key -> RawAnimation.begin().thenLoop(key));
                    return state.setAndContinue(anim);
                } else {
                    RawAnimation anim = CACHED_ONESHOT_ANIMATIONS.computeIfAbsent(entityAnim,
                        key -> RawAnimation.begin().thenPlay(key).thenLoop("sheath"));
                    return state.setAndContinue(anim);
                }
            }

            // For players, use the normal NBT-based system
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
