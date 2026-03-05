package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.Tag;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Mitsuri Kanroji's Animated Nichirin Sword (Love Breathing)
 *
 * This is a GeckoLib-based animated sword that plays animations based on:
 * - Player/entity state (idle, walk, sprint)
 * - Active animations (sword_to_left, sword_to_right, speed_attack_sword, sword_overhead, sword_rotate)
 * - Special overhead attacks (sword_overhead, kanroji_sword_overhead)
 *
 * The sword uses the kanroji_sword.geo.json model and kanroji_sword.animation.json animations.
 * It can be held by both players (using playeranimator) and entities (using GeckoLib).
 *
 * Features:
 * - Flexible whip-like blade that extends during combat
 * - 6 Love Breathing forms with animations
 * - Faster attack speed but slightly lower damage (whip-style combat)
 * - Special attack animations: 10% sword_overhead, 10% kanroji_sword_overhead (handled by BreathingSwordAnimationHandler)
 *
 * Attack Stats:
 * - Attack Damage: 6.0 (vs normal 7.0) - Compensated by multi-target whip hits
 * - Attack Speed: -1.8 (vs normal -2.4) - Faster whip attacks
 */
public class NichirinSwordKanrojiAnimated extends BreathingSwordItem implements GeoItem {
    private static final BreathingTechnique LOVE_BREATHING = EnhancedLoveForms.createLoveBreathing();

    private static final double ATTACK_DAMAGE = 6.0; // Slightly lower than normal swords
    private static final double ATTACK_SPEED = -1.8; // Faster than normal swords (-2.4)

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String ANIM_NBT_KEY = "KanrojiSwordAnim";
    private static final String DEFAULT_ANIM = "idle";
    private static final AtomicLong CLIENT_ID_ALLOCATOR = new AtomicLong(Long.MIN_VALUE + 1L);

    // Cached animations to prevent recreation every frame
    private static final java.util.Map<String, RawAnimation> CACHED_LOOPING_ANIMATIONS = new java.util.HashMap<>();
    private static final java.util.Map<String, RawAnimation> CACHED_ONESHOT_ANIMATIONS = new java.util.HashMap<>();

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

    public NichirinSwordKanrojiAnimated(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return LOVE_BREATHING;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 5.0 to make total 6.0
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));

            // Attack speed: -1.8 (faster than normal -2.4)
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                    ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

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
        // For players: standard controller that reads from NBT (or cache for displayed swords)
        // For entities: we use a shared entity-based controller from EntitySwordAnimationCache
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);

            // Check if we're rendering for an entity
            net.minecraft.world.entity.LivingEntity currentEntity =
                com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.getCurrentEntity();

            if (currentEntity != null && !(currentEntity instanceof net.minecraft.world.entity.player.Player)) {
                String entityAnim;
                if (!com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker.isInCombat(currentEntity)
                    && !com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker.isInSheathingTransition(currentEntity)) {
                    entityAnim = "sheath";
                } else {
                    // For in-combat entity rendering, read animation from entity cache
                    entityAnim = com.lerdorf.kimetsunoyaibamultiplayer.client.EntitySwordAnimationCache.getAnimation(currentEntity);
                }

                // Determine if it's a looping animation
                boolean isLooping = entityAnim.equals("idle") || entityAnim.equals("walk") ||
                                   entityAnim.equals("sprint") || entityAnim.equals("sheath");

                // Use cached RawAnimation instances to prevent recreation every frame
                // This prevents GeckoLib from seeing a "new" animation and resetting to base pose
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

            // For players, check if this is a displayed sword (on hip/back)
            // If so, read from cache instead of NBT to avoid ItemStack copy issues
            String animName = null;
            if (currentEntity instanceof net.minecraft.world.entity.player.Player) {
                net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) currentEntity;
                java.util.UUID playerUUID = player.getUUID();

                // Check if player is currently holding this sword
                boolean isHeld = player.getMainHandItem() == stack ||
                               (player.getMainHandItem().getItem() == stack.getItem() &&
                                net.minecraft.world.item.ItemStack.isSameItemSameTags(player.getMainHandItem(), stack));

                if (!isHeld) {
                    // Not held - check if it's displayed on hip/back
                    com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.SwordDisplayState displayState =
                        com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.getDisplayState(playerUUID);

                    // Check if this sword is displayed on left or right
                    boolean isLeftDisplay = displayState.hasLeftSword() &&
                        displayState.getLeftHipSword().getItem() instanceof NichirinSwordKanrojiAnimated;
                    boolean isRightDisplay = displayState.hasRightSword() &&
                        displayState.getRightHipSword().getItem() instanceof NichirinSwordKanrojiAnimated;

                    if (isLeftDisplay || isRightDisplay) {
                        // Read from cache for displayed swords
                        String displaySlot = isLeftDisplay ? "left" : "right";
                        animName = com.lerdorf.kimetsunoyaibamultiplayer.client.EntitySwordAnimationCache
                            .getPlayerDisplayedSwordAnimation(playerUUID, displaySlot);
                    }
                }
            }

            // If not from cache, read from NBT (for held swords)
            if (animName == null || animName.isEmpty()) {
                animName = getAnimationFromStack(stack);
            }

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
            private com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.KanrojiSwordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.KanrojiSwordRenderer();
                }
                return this.renderer;
            }
        });
    }
}
