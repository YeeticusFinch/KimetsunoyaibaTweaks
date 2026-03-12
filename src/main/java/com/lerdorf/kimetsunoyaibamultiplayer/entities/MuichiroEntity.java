package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
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
    private enum MarkState {
        NORMAL,         // No mark
        TRANSFORMING,   // Currently transforming (5 seconds)
        TRANSFORMED     // Mark activated
    }

    private int breathingFormUsageCounter = 0;      // Track when to use breathing forms
    private MarkState markState = MarkState.NORMAL; // Demon slayer mark state
    private int transformationTimer = 0;            // Timer for transformation (100 ticks = 5 seconds)
    private int lastDamageTick = -1000;             // Server tick when last damaged
    private static final int TRANSFORMATION_DURATION = 100; // 5 seconds in ticks

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

            // Set sprinting flag for animation (unless transforming)
            if (markState != MarkState.TRANSFORMING) {
                this.setSprinting(hasTarget);
            } else {
                this.setSprinting(false); // No sprinting during transformation
            }

            // Activate Demon Slayer Mark when below 50% HP
            if (markState == MarkState.NORMAL && this.getHealth() <= 67.5f) { 
                activateDemonSlayerMark();
            }

            // Handle transformation timer
            if (markState == MarkState.TRANSFORMING) {
                transformationTimer++;

                // Spawn particle effects during transformation
                spawnTransformationParticles();

                // Play guardian laser charging sound every 20 ticks during transformation
                if (transformationTimer % 20 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.5F);
                }

                // Complete transformation after 5 seconds
                if (transformationTimer >= TRANSFORMATION_DURATION) {
                    completeDemonSlayerMark();
                }
            }

            // Let BreathingFormAttackGoal handle ability usage; only manage sprint toggle here
            if (!hasTarget) {
                breathingFormUsageCounter = 0;
            }
        }
    }

    /**
     * Starts the demon slayer mark transformation.
     * Muichiro kneels for 5 seconds with particle effects, then gains the mark.
     */
    private void activateDemonSlayerMark() {
        markState = MarkState.TRANSFORMING;
        transformationTimer = 0;

        Log.debug("[Muichiro] Starting demon slayer mark transformation at " + this.getHealth() + " HP");

        // Force kneel animation for the full transformation duration
        this.playGeckoAnimation("kneel", TRANSFORMATION_DURATION);

        // Apply super resistance (level 100) for 5 seconds
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TRANSFORMATION_DURATION, 99, false, false));

        // Apply immovable effect (kimetsunoyaiba:immovable) for 5 seconds
        net.minecraft.world.effect.MobEffect immovableEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "immovable"));
        if (immovableEffect != null) {
            this.addEffect(new MobEffectInstance(immovableEffect, TRANSFORMATION_DURATION, 0, false, false));
        }

        // Stop all current goals/movement
        this.getNavigation().stop();
        if (this.getTarget() != null) {
            this.setTarget(null); // Temporarily clear target to prevent attacks
        }
    }

    /**
     * Completes the demon slayer mark transformation.
     * Swaps helmet to marked version and applies permanent buffs.
     */
    private void completeDemonSlayerMark() {
        markState = MarkState.TRANSFORMED;

        Log.debug("[Muichiro] Demon slayer mark transformation complete!");

        // Play dramatic wither spawn sound to signify transformation completion
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);

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

        // Upgrade Strength from 11 to 12 (amplifier 10 -> 11)
        this.removeEffect(MobEffects.DAMAGE_BOOST);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 11, true, false));

        // Restore base Resistance 4 (was temporarily boosted to 100 during transformation)
        // Don't upgrade resistance permanently - mark already makes him strong enough
        this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, true, false));

        // Animation will naturally transition back to idle when the kneel animation expires
    }

    /**
     * Spawns particle effects during the transformation:
     * - Spiral of cloud particles circling around Muichiro
     * - Waves of mist particles traveling across the ground from his feet
     */
    private void spawnTransformationParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = this.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // Spawn spiral cloud particles every tick
        double angle = (transformationTimer * 0.3) % (2 * Math.PI); // Rotate over time
        double radius = 3;
        double height = (transformationTimer % 40) * 0.1; // Spiral upward

        for (int i = 0; i < 3; i++) {
            double spiralAngle = angle + (i * 2 * Math.PI / 3);
            double offsetX = Math.cos(spiralAngle) * radius;
            double offsetZ = Math.sin(spiralAngle) * radius;

            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                x + offsetX, y + height, z + offsetZ,
                1, 0.1, 0.1, 0.1, 0.01
            );
        }

        // Spawn ground wave mist particles every 10 ticks (multiple shockwaves)
        if (transformationTimer % 10 == 0) {
            double waveRadius = (transformationTimer % 40) * 0.15; // Expand from feet

            for (int i = 0; i < 20; i++) {
                double waveAngle = (i / 20.0) * 2 * Math.PI;
                double waveX = x + Math.cos(waveAngle) * waveRadius;
                double waveZ = z + Math.sin(waveAngle) * waveRadius;

                // Use custom mist particles
                serverLevel.sendParticles(
                    ModParticles.MIST_PARTICLE.get(),
                    waveX, y + 0.1, waveZ,
                    3, 0.1, 0.05, 0.1, 0.02
                );

                // Add some small mist particles for detail
                serverLevel.sendParticles(
                    ModParticles.SMALL_MIST_PARTICLE.get(),
                    waveX, y + 0.1, waveZ,
                    2, 0.15, 0.05, 0.15, 0.01
                );
            }
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
            .add(Attributes.ATTACK_DAMAGE, 1.2D)     // Base damage (Strength effect adds the rest)
            .add(Attributes.MOVEMENT_SPEED, 0.24D)   // Moderate base movement (Speed effect multiplies this)
            .add(Attributes.ATTACK_SPEED, 14.0D)     // Extremely fast attack speed baseline
            .add(Attributes.ARMOR, 7.0D)             // From armor equipment
            .add(Attributes.ARMOR_TOUGHNESS, 3.0D)   // From armor equipment
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
            this.setDropChance(slot, 0.2F);
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

    /**
     * Checks if Muichiro is currently transforming (cannot move or use abilities)
     */
    public boolean isTransforming() {
        return markState == MarkState.TRANSFORMING;
    }

    /**
     * Checks if Muichiro has his demon slayer mark active
     */
    public boolean hasMarkActivated() {
        return markState == MarkState.TRANSFORMED;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Mark this as Muichiro so we can restore effects on load
        tag.putBoolean("IsMuichiro", true);
        tag.putString("MarkState", markState.name());
        tag.putInt("TransformationTimer", transformationTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Restore Hashira-level effects if this is Muichiro
        if (tag.getBoolean("IsMuichiro")) {
            this.setHealth(135.0F);

            // Restore mark state
            String markStateName = tag.getString("MarkState");
            try {
                this.markState = MarkState.valueOf(markStateName);
            } catch (IllegalArgumentException e) {
                this.markState = MarkState.NORMAL;
            }
            this.transformationTimer = tag.getInt("TransformationTimer");

            // Apply appropriate effects based on mark state
            if (markState == MarkState.TRANSFORMED) {
                // Apply upgraded stats with mark (only Strength upgrades, Resistance stays at base)
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 11, true, false));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, true, false));
            } else {
                // Apply base stats without mark
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 10, true, false));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, true, false));
            }
        }
    }

    /**
     * Override animation controller to use sprint animation when in combat
     * and kneel animation during transformation
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main controller - handles ALL animations (walk, idle, sprint, attacks, abilities, transformation)
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            // Death animation (highest priority)
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            // Transformation kneel animation (second highest priority)
            if (markState == MarkState.TRANSFORMING) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("kneel"));
            }

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

    private int lastDodgeTick = -1000; // Track last dodge to prevent spam
    private static final int DODGE_COOLDOWN = 60; // 3 seconds between dodges
    private static final float DODGE_CHANCE = 0.35f; // 35% chance to dodge

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Don't dodge during transformation
        if (markState == MarkState.TRANSFORMING) {
            boolean result = super.hurt(source, amount);
            if (result && !this.level().isClientSide) {
                this.lastDamageTick = this.tickCount;
            }
            return result;
        }

        // Try to dodge attacks from living entities
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            // Check dodge conditions
            boolean canDodge = this.tickCount - this.lastDodgeTick >= DODGE_COOLDOWN;
            boolean shouldDodge = canDodge && this.getRandom().nextFloat() < DODGE_CHANCE;

            if (shouldDodge) {
                // Perform dodge animation and particles
                performDodge(attacker);
                this.lastDamageTick = this.tickCount;

                // 50% chance to completely avoid damage, 50% chance to take half damage
                boolean fullDodge = this.getRandom().nextBoolean();
                if (fullDodge) {
                    // Perfect dodge - no damage
                    return false;
                } else {
                    // Partial dodge - take half damage
                    boolean result = super.hurt(source, amount * 0.5f);
                    return result;
                }
            }
        }

        // Normal damage processing if dodge didn't trigger
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            this.lastDamageTick = this.tickCount;
        }
        return result;
    }

    /**
     * Performs a dodge maneuver - jumps away and spawns mist particles.
     */
    private void performDodge(LivingEntity attacker) {
        this.lastDodgeTick = this.tickCount;

        // Store position before dodge for particle spawn
        Vec3 dodgePosition = this.position();

        // Compute backward direction away from attacker
        Vec3 away = this.position().subtract(attacker.position()).normalize();
        if (away.lengthSqr() < 1.0e-4) {
            // Fallback to opposite of look direction
            away = this.getLookAngle().scale(-1.0);
        }

        // Propel backwards and upward (stronger dodge than backstep)
        double backSpeed = 1.5; // Strong evasion
        double upBoost = 0.6;   // Jump up
        Vec3 motion = new Vec3(away.x * backSpeed, upBoost, away.z * backSpeed);
        this.setDeltaMovement(motion);
        this.hurtMarked = true; // Ensure motion sync

        // Play dodge animation
        this.playGeckoAnimation("backstep", 10);

        // Spawn mist particle puff where Muichiro was standing
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnDodgeMistParticles(serverLevel, dodgePosition);
        }

        Log.debug("[Muichiro] Dodged attack from {} at position {}", attacker.getName().getString(), dodgePosition);
    }

    /**
     * Spawns a puff of mist particles at the dodge location.
     */
    private void spawnDodgeMistParticles(ServerLevel level, Vec3 position) {
        double x = position.x;
        double y = position.y;
        double z = position.z;

        // Large puff of mist particles in a sphere
        for (int i = 0; i < 30; i++) {
            double offsetX = (this.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = this.getRandom().nextDouble() * 1.5;
            double offsetZ = (this.getRandom().nextDouble() - 0.5) * 2.0;

            // Mix of regular and small mist particles
            if (i % 2 == 0) {
                level.sendParticles(
                    ModParticles.MIST_PARTICLE.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    3, 0.2, 0.2, 0.2, 0.05
                );
            } else {
                level.sendParticles(
                    ModParticles.SMALL_MIST_PARTICLE.get(),
                    x + offsetX, y + offsetY, z + offsetZ,
                    2, 0.15, 0.15, 0.15, 0.03
                );
            }
        }

        // Add some cloud particles for extra effect
        for (int i = 0; i < 10; i++) {
            double offsetX = (this.getRandom().nextDouble() - 0.5) * 1.5;
            double offsetY = this.getRandom().nextDouble() * 1.0;
            double offsetZ = (this.getRandom().nextDouble() - 0.5) * 1.5;

            level.sendParticles(
                ParticleTypes.CLOUD,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0.1, 0.1, 0.1, 0.02
            );
        }
    }

    public boolean wasRecentlyDamaged(int withinTicks) {
        return this.tickCount - this.lastDamageTick <= withinTicks;
    }
}
