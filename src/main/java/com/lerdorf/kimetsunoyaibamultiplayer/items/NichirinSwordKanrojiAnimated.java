package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicLong;

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

    // Track which entity is currently being rendered (for animation lookup)
    private static final ThreadLocal<net.minecraft.world.entity.LivingEntity> currentRenderingEntity = new ThreadLocal<>();

    /**
     * Set the entity currently being rendered. Called by the renderer.
     */
    public static void setCurrentRenderingEntity(net.minecraft.world.entity.LivingEntity entity) {
        currentRenderingEntity.set(entity);
    }

    /**
     * Get the entity currently being rendered.
     */
    private static net.minecraft.world.entity.LivingEntity getCurrentRenderingEntity() {
        return currentRenderingEntity.get();
    }

    /**
     * Clear the current rendering entity. Called after rendering completes.
     */
    public static void clearCurrentRenderingEntity() {
        currentRenderingEntity.remove();
    }

    public NichirinSwordKanrojiAnimated(Properties properties) {
        super(properties);
        Log.info("[NichirinSwordKanrojiAnimated] Constructor called - GeckoLib item ready");
        // NOTE: We DO NOT use SingletonGeoAnimatable because it makes ALL swords share animations
        // Instead, we use per-entity animation tracking via KanrojiSwordEntityAnimationTracker
        Log.info("[NichirinSwordKanrojiAnimated] Per-entity animation system initialized");
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

    // ==================== GeckoLib Implementation ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        Log.info("[NichirinSwordKanrojiAnimated] registerControllers() called");
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            // Try to get the entity holding this sword from the animation state
            // GeckoLib provides this when rendering items held by entities
            net.minecraft.world.entity.LivingEntity entity = getCurrentRenderingEntity();

            Log.debug("[NichirinSwordKanrojiAnimated] Animation controller tick - entity: {}",
                      entity != null ? entity.getName().getString() : "null");

            if (entity != null) {
                // Look up this specific entity's animation from our tracker
                String animName = com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordEntityAnimationTracker
                    .getAnimation(entity.getUUID());

                Log.debug("[NichirinSwordKanrojiAnimated] Entity {} UUID {} has animation: {}",
                          entity.getName().getString(), entity.getUUID(), animName);

                if (animName != null && !animName.isEmpty()) {
                    Log.debug("[NichirinSwordKanrojiAnimated] Playing animation: {}", animName);

                    // Determine if it's a looping animation or one-shot
                    boolean isLooping = animName.equals("idle") || animName.equals("walk") ||
                                       animName.equals("sprint") || animName.equals("sheath");

                    if (isLooping) {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop(animName));
                    } else {
                        state.getController().setAnimation(RawAnimation.begin().thenPlay(animName));
                    }
                    return software.bernie.geckolib.core.object.PlayState.CONTINUE;
                }
            }

            // Default: sheath animation for GUI/inventory rendering or when no entity
            Log.debug("[NichirinSwordKanrojiAnimated] Using default sheath animation (entity={}, has anim={})",
                      entity != null ? entity.getName().getString() : "null",
                      entity != null ? com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordEntityAnimationTracker.getAnimation(entity.getUUID()) : "null");
            state.getController().setAnimation(RawAnimation.begin().thenLoop("sheath"));
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        })
        // Register triggerable animations
        .triggerableAnim("idle", RawAnimation.begin().thenLoop("idle"))
        .triggerableAnim("walk", RawAnimation.begin().thenLoop("walk"))
        .triggerableAnim("sprint", RawAnimation.begin().thenLoop("sprint"))
        .triggerableAnim("sheath", RawAnimation.begin().thenLoop("sheath"))
        .triggerableAnim("sword_to_left", RawAnimation.begin().thenPlay("sword_to_left"))
        .triggerableAnim("sword_to_right", RawAnimation.begin().thenPlay("sword_to_right"))
        .triggerableAnim("speed_attack_sword", RawAnimation.begin().thenPlay("speed_attack_sword"))
        .triggerableAnim("sword_overhead", RawAnimation.begin().thenPlay("sword_overhead"))
        .triggerableAnim("kanroji_sword_overhead", RawAnimation.begin().thenPlay("kanroji_sword_overhead"))
        .triggerableAnim("sword_rotate", RawAnimation.begin().thenPlay("sword_rotate"))
        .triggerableAnim("random", RawAnimation.begin().thenPlay("random"))
        .triggerableAnim("random2", RawAnimation.begin().thenPlay("random2"))
        .triggerableAnim("love_first_form", RawAnimation.begin().thenPlay("love_first_form"))
        .triggerableAnim("love_second_form", RawAnimation.begin().thenPlay("love_second_form"))
        .triggerableAnim("love_third_form", RawAnimation.begin().thenPlay("love_third_form"))
        .triggerableAnim("love_fourth_form", RawAnimation.begin().thenPlay("love_fourth_form"))
        .triggerableAnim("love_fifth_form", RawAnimation.begin().thenPlay("love_fifth_form"))
        .triggerableAnim("love_sixth_form", RawAnimation.begin().thenPlay("love_sixth_form"))
        );
        Log.info("[NichirinSwordKanrojiAnimated] Animation controller registered with triggerable animations");
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
