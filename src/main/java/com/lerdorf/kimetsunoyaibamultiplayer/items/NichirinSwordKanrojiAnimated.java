package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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

    public NichirinSwordKanrojiAnimated(Properties properties) {
        super(properties);
        Log.info("[NichirinSwordKanrojiAnimated] Constructor called - registering GeckoLib item");
        // Register this item for synced animations (required for GeckoLib items)
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        Log.info("[NichirinSwordKanrojiAnimated] GeckoLib registration complete");
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
            // Set default sheath animation for GUI/inventory rendering
            // This will be overridden by triggered animations when held in hand
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
