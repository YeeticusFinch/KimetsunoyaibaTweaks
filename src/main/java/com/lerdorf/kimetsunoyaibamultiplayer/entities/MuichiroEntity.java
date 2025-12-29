package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;

/**
 * Muichiro Tokito - Mist Hashira
 * Wields nichirinsword_muichiro, uses Enhanced Mist Breathing (all 7 forms)
 * Wears muichiro uniform armor from the kimetsunoyaiba mod
 * Has 135 HP and enhanced stats based on Hashira-level attributes
 *
 * Stats from NBT data:
 * - HP: 135 (base)
 * - Speed 2 (amplifier 0), Strength 11 (amplifier 10), Resistance 4 (amplifier 3)
 * - Movement speed: 0.32 + 60% from Speed 2
 * - Attack damage: 1.0 + 36.0 from Strength 11
 * - Armor: 6.0, Armor toughness: 2.0
 */
public class MuichiroEntity extends BreathingSlayerEntity {
    private int breathingFormUsageCounter = 0; // Track when to use breathing forms
    private boolean markActivated = false;     // Demon slayer mark state
    private int lastDamageTick = -1000;        // Server tick when last damaged

    public MuichiroEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);

        // Force set max health immediately after parent constructor
        if (!level.isClientSide) {
            AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getBaseValue() != 135.0D) {
                maxHealth.setBaseValue(135.0D);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Safety check: ensure health never drops below intended max
        if (!this.level().isClientSide) {
            AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getBaseValue() != 135.0D) {
                maxHealth.setBaseValue(135.0D);
                this.setHealth(135.0F);
                Log.debug("[Muichiro] WARNING: Max health was changed, forcing back to 135");
            }
        }

        // Server-side AI behaviors
        if (!this.level().isClientSide) {
            boolean hasTarget = this.getTarget() != null;

            // Set sprinting flag for animation
            this.setSprinting(hasTarget);

            // Activate Demon Slayer Mark when below 50% HP
            if (!markActivated && this.getHealth() <= (this.getMaxHealth() * 0.5f)) {
                activateDemonSlayerMark();
            }

            // Let BreathingFormAttackGoal handle ability usage; only manage sprint toggle here
            if (!hasTarget) {
                breathingFormUsageCounter = 0;
            }
        }
    }

    private void activateDemonSlayerMark() {
        markActivated = true;

        // Swap helmet to hair_muichiro_demon_slayer_mark_helmet (client-visible)
        Item markHelmetItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "hair_muichiro_demon_slayer_mark_helmet"));
        if (markHelmetItem != null) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(markHelmetItem));
        }

        // Apply potion_demon_slayer_mark with effectively infinite duration
        net.minecraft.world.effect.MobEffect markEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "potion_demon_slayer_mark"));
        if (markEffect != null) {
            this.addEffect(new MobEffectInstance(markEffect, Integer.MAX_VALUE, 0, true, false));
        }
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return EnhancedMistForms.createMuichiroMistBreathing();
    }

    /**
     * Override to prevent Muichiro from being affected by power level changes
     */
    @Override
    public void setPowerLevel(int level) {
        // Do nothing - Muichiro is always max power (Hashira)
    }

    /**
     * Override to always return max power level
     */
    @Override
    public int getPowerLevel() {
        return 4; // Hashira = max power
    }

    @Override
    public ItemStack getEquippedSword() {
        return new ItemStack(ModItems.NICHIRINSWORD_MUICHIRO.get());
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load muichiro armor from kimetsunoyaiba mod
        Item uniformHelmet = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_muichiro_helmet"));
        Item uniformChest = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_muichiro_chestplate"));
        Item uniformLegs = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_muichiro_leggings"));
        Item uniformBoots = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_muichiro_boots"));

        return new ItemStack[]{
            uniformHelmet != null ? new ItemStack(uniformHelmet) : ItemStack.EMPTY,
            uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY,
            uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY,
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY
        };
    }

    /**
     * Create attributes for Muichiro Tokito (Hashira-level stats)
     * Based on NBT data from the original entity
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 135.0D)      // Hashira health
            .add(Attributes.ATTACK_DAMAGE, 1.0D)     // Base damage (Strength effect adds the rest)
            .add(Attributes.MOVEMENT_SPEED, 0.24D)   // Moderate base movement (Speed effect multiplies this)
            .add(Attributes.ATTACK_SPEED, 14.0D)     // Extremely fast attack speed baseline
            .add(Attributes.ARMOR, 6.0D)             // From armor equipment
            .add(Attributes.ARMOR_TOUGHNESS, 2.0D)   // From armor equipment
            .add(Attributes.FOLLOW_RANGE, 64.0D);    // Same as base slayers
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag dataTag) {
        // DO NOT call super.finalizeSpawn() - it applies random power levels
        // Instead, manually apply equipment and Hashira-level effects

        // Equip sword in main hand
        this.setItemSlot(EquipmentSlot.MAINHAND, getEquippedSword());

        // Equip armor
        ItemStack[] armor = getArmorEquipment();
        this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
        this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
        this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
        this.setItemSlot(EquipmentSlot.FEET, armor[3]);

        // Prevent equipment from dropping
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        // Set to full health (135 HP)
        this.setHealth(135.0F);

        // Apply Hashira-level permanent effects (matching NBT data)
        // Speed 2 (amplifier 0 = Speed I, which is Speed 2 in display)
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));

        // Strength 11 (amplifier 10 = Strength XI)
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 10, true, false));

        // Resistance 4 (amplifier 3 = Resistance IV)
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, true, false));

        // Set persistence required (don't despawn)
        this.setPersistenceRequired();

        // Increase attack speed significantly
        net.minecraft.world.entity.ai.attributes.AttributeInstance atkSpd = this.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpd != null) {
            // Set an even higher base attack speed for rapid swings
            atkSpd.setBaseValue(14.0D);
        }

        return spawnData;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Occasional backstep dodge while in combat
        this.goalSelector.addGoal(2, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.MuichiroBackstepGoal(this));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Mark this as Muichiro so we can restore effects on load
        tag.putBoolean("IsMuichiro", true);
        tag.putBoolean("MuichiroMark", markActivated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Restore Hashira-level effects if this is Muichiro
        if (tag.getBoolean("IsMuichiro")) {
            this.setHealth(135.0F);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 10, true, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, true, false));
        }
        this.markActivated = tag.getBoolean("MuichiroMark");
    }

    /**
     * Override animation controller to use sprint animation when in combat
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main controller - handles ALL animations (walk, idle, sprint, attacks, abilities)
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            String anim = getCurrentAnimation();
            int animTicks = getAnimationTicks();

            // Attack and ability animations (play once)
            if (animTicks > 0 && !anim.equals("idle") && !anim.equals("walk") && !anim.equals("sprint")) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            // Movement animations (loop)
            if (state.isMoving()) {
                // Use sprint animation when sprinting (in combat with target)
                // Check both isSprinting() AND hasTarget to ensure we sprint during combat
                boolean shouldSprint = this.isSprinting() || (this.getTarget() != null && this.getDeltaMovement().horizontalDistanceSqr() > 0.01);

                if (shouldSprint) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("sprint"));
                } else {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                }
            } else {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }));
    }

    /**
     * Increase movement speed when sprinting (in combat)
     */
    @Override
    public double getMyRidingOffset() {
        return super.getMyRidingOffset();
    }

    /**
     * Override to apply sprint speed modifier
     */
    @Override
    public float getSpeed() {
        float baseSpeed = super.getSpeed();

        // Apply 100% speed boost when sprinting (in combat)
        if (this.isSprinting() && this.getTarget() != null) {
            return baseSpeed * 2.0F;
        }

        return baseSpeed;
    }

    // Reduce fall damage taken (80% resistance)
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return super.causeFallDamage(fallDistance, damageMultiplier * 0.2F, damageSource);
    }

    // Custom equipment drops with Looting scaling
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);

        double baseChance = 0.10; // 10%
        double chance = Math.min(1.0, baseChance + (0.10 * Math.max(0, lootingLevel)));

        RandomSource rand = this.getRandom();

        // Sword (main hand)
        if (!this.getMainHandItem().isEmpty() && rand.nextDouble() < chance) {
            this.spawnAtLocation(this.getMainHandItem().copy());
        }

        // Armor pieces — if mark is active, drop the regular helmet instead of mark helmet
        ItemStack head = this.getItemBySlot(EquipmentSlot.HEAD);
        if (rand.nextDouble() < chance) {
            Item regularHelmet = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_muichiro_helmet"));
            if (regularHelmet != null) {
                this.spawnAtLocation(new ItemStack(regularHelmet));
            } else if (!head.isEmpty()) {
                this.spawnAtLocation(head.copy());
            }
        }
        ItemStack chest = this.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && rand.nextDouble() < chance) {
            this.spawnAtLocation(chest.copy());
        }
        ItemStack legs = this.getItemBySlot(EquipmentSlot.LEGS);
        if (!legs.isEmpty() && rand.nextDouble() < chance) {
            this.spawnAtLocation(legs.copy());
        }
        ItemStack feet = this.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.isEmpty() && rand.nextDouble() < chance) {
            this.spawnAtLocation(feet.copy());
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            this.lastDamageTick = this.tickCount;
        }
        return result;
    }

    public boolean wasRecentlyDamaged(int withinTicks) {
        return this.tickCount - this.lastDamageTick <= withinTicks;
    }
}
