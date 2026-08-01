package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;

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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Animated Love Breathing Nichirin Sword (GeckoLib).
 * Uses the same animation system as NichirinSwordKanrojiAnimated but with
 * the generic love sword model (nichirinsword_love.geo.json).
 *
 * Only includes base Love Breathing forms (1-4) via createLoveBreathingBase().
 */
public class NichirinSwordLoveAnimated extends BreathingSwordItem implements GeoItem {
    private static final BreathingTechnique LOVE_BREATHING = EnhancedLoveForms.createLoveBreathingBase();

    private static final double ATTACK_DAMAGE = 4.5;
    private static final double ATTACK_SPEED = -2.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String ANIM_NBT_KEY = "LoveSwordAnim";
    private static final String DEFAULT_ANIM = "idle";
    private static final AtomicLong CLIENT_ID_ALLOCATOR = new AtomicLong(Long.MIN_VALUE + 100001L);

    private static final java.util.Map<String, RawAnimation> CACHED_LOOPING_ANIMATIONS = new java.util.HashMap<>();
    private static final java.util.Map<String, RawAnimation> CACHED_ONESHOT_ANIMATIONS = new java.util.HashMap<>();

    public static String getAnimationFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DEFAULT_ANIM;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ANIM_NBT_KEY, Tag.TAG_STRING)) {
            String animName = tag.getString(ANIM_NBT_KEY);
            if (!animName.isEmpty()) return animName;
        }
        return DEFAULT_ANIM;
    }

    public static void setAnimationOnStack(ItemStack stack, String animationName) {
        if (stack == null || stack.isEmpty()) return;
        if (animationName == null || animationName.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) tag.remove(ANIM_NBT_KEY);
            return;
        }
        stack.getOrCreateTag().putString(ANIM_NBT_KEY, animationName);
    }

    public static void ensureAnimatableId(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty() || level == null) return;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(GeoItem.ID_NBT_KEY, Tag.TAG_LONG)) return;
        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
            return;
        }
        if (level.isClientSide) {
            long id = CLIENT_ID_ALLOCATOR.getAndIncrement();
            stack.getOrCreateTag().putLong(GeoItem.ID_NBT_KEY, id);
        }
    }

    public NichirinSwordLoveAnimated(Properties properties) {
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
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                    ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
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
        ensureAnimatableId(stack, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);

            net.minecraft.world.entity.LivingEntity currentEntity =
                com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.getCurrentEntity();

            if (currentEntity != null && !(currentEntity instanceof net.minecraft.world.entity.player.Player)) {
                String entityAnim;
                if (!com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker.isInCombat(currentEntity)) {
                    // Love sword should always stay in sheath pose whenever the entity is sheathed.
                    entityAnim = "sheath";
                } else {
                    entityAnim = com.lerdorf.kimetsunoyaibamultiplayer.client.EntitySwordAnimationCache.getAnimation(currentEntity);
                }
                boolean isLooping = entityAnim.equals("idle") || entityAnim.equals("walk") ||
                                   entityAnim.equals("sprint") || entityAnim.equals("sheath");
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

            String animName = null;
            if (currentEntity instanceof net.minecraft.world.entity.player.Player player) {
                java.util.UUID playerUUID = player.getUUID();

                boolean isHeld = player.getMainHandItem() == stack ||
                               (player.getMainHandItem().getItem() == stack.getItem() &&
                                net.minecraft.world.item.ItemStack.isSameItemSameTags(player.getMainHandItem(), stack));

                if (!isHeld) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.SwordDisplayState displayState =
                        com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.getDisplayState(playerUUID);

                    boolean isLeftDisplay = displayState.hasLeftSword() &&
                        displayState.getLeftHipSword().getItem() instanceof NichirinSwordLoveAnimated;
                    boolean isRightDisplay = displayState.hasRightSword() &&
                        displayState.getRightHipSword().getItem() instanceof NichirinSwordLoveAnimated;

                    if (isLeftDisplay || isRightDisplay) {
                        String displaySlot = isLeftDisplay ? "left" : "right";
                        animName = com.lerdorf.kimetsunoyaibamultiplayer.client.EntitySwordAnimationCache
                            .getPlayerDisplayedSwordAnimation(playerUUID, displaySlot);
                    }
                }
            }

            if (animName == null || animName.isEmpty()) {
                animName = getAnimationFromStack(stack);
            }

            if (animName == null || animName.isEmpty() || animName.equals("empty")) {
                animName = "idle";
            }

            if (state.getController().getCurrentAnimation() != null) {
                String currentAnim = state.getController().getCurrentAnimation().animation().name();
                if (currentAnim != null && currentAnim.equals(animName)) {
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                }
            }

            boolean isLooping = animName.equals("idle") || animName.equals("walk") ||
                               animName.equals("sprint") || animName.equals("sheath");
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
            private com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.LoveSwordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.LoveSwordRenderer();
                }
                return this.renderer;
            }
        });
    }
}
