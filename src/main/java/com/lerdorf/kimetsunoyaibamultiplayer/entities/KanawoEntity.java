package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedFlowerForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;

/**
 * Kanao Tsuyuri (Kanawo) - Kamaboko Flower Breathing user.
 */
public class KanawoEntity extends BreathingSlayerEntity {
    private static final float MAX_HP = 105.0F;

    public KanawoEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return EnhancedFlowerForms.createKanawoFlowerBreathing();
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_KANAWO.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        return new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    }

    @Override
    public void setPowerLevel(int level) {
        // Kanawo remains fixed at Kamaboko tier.
    }

    @Override
    public int getPowerLevel() {
        return 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HP)
            .add(Attributes.ATTACK_DAMAGE, 1.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.23D)
            .add(Attributes.ATTACK_SPEED, 12.0D)
            .add(Attributes.ARMOR, 8.0D)
            .add(Attributes.ARMOR_TOUGHNESS, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        // Use fixed Kamaboko setup instead of random power scaling.
        this.setItemSlot(EquipmentSlot.MAINHAND, getEquippedSword());
        ItemStack[] armor = getArmorEquipment();
        this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
        this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
        this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
        this.setItemSlot(EquipmentSlot.FEET, armor[3]);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        this.setHealth(MAX_HP);
        this.setPersistenceRequired();

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 7, true, false));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, true, false));

        return spawnData;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setSprinting(this.getTarget() != null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsKanawo", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("IsKanawo")) {
            this.setHealth(MAX_HP);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 7, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, true, false));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            String anim = getCurrentAnimation();
            int animTicks = getAnimationTicks();

            if (animTicks > 0 && !anim.equals("idle") && !anim.equals("walk")
                && !anim.equals("walk_female") && !anim.equals("sprint")) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            if (state.isMoving()) {
                boolean shouldSprint = this.isSprinting()
                    || (this.getTarget() != null && this.getDeltaMovement().horizontalDistanceSqr() > 0.01);
                if (shouldSprint) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("sprint"));
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk_female"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }
}
