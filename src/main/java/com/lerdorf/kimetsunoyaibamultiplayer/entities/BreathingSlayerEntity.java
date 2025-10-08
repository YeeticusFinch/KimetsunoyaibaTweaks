package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Base class for breathing technique slayer entities
 * Uses GeckoLib for animations (biped.geo.json)
 * Uses PlayerAnimator/MobPlayerAnimator for breathing technique animations
 */
public abstract class BreathingSlayerEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Synced data for current breathing form index
    private static final EntityDataAccessor<Integer> CURRENT_FORM_INDEX =
        SynchedEntityData.defineId(BreathingSlayerEntity.class, EntityDataSerializers.INT);

    // Synced data for current animation state
    private static final EntityDataAccessor<String> CURRENT_ANIMATION =
        SynchedEntityData.defineId(BreathingSlayerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> ANIMATION_TICKS =
        SynchedEntityData.defineId(BreathingSlayerEntity.class, EntityDataSerializers.INT);

    // Synced data for power level (1-4)
    private static final EntityDataAccessor<Integer> POWER_LEVEL =
        SynchedEntityData.defineId(BreathingSlayerEntity.class, EntityDataSerializers.INT);

    // Cooldown tracking for breathing forms (in ticks)
    private int breathingFormCooldown = 0;

    // Entity tags for targeting demons
    private static final TagKey<EntityType<?>> DEMON_TAG = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.tryBuild("kimetsunoyaiba", "demon"));
    private static final TagKey<EntityType<?>> TWELVE_KIZUKI_TAG = TagKey.create(Registries.ENTITY_TYPE,
        ResourceLocation.tryBuild("kimetsunoyaiba", "twelve_kizuki"));

    // UUID for attribute modifiers (must be unique per attribute)
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7f3e5c6d-1a2b-4f9e-8d7c-6b5a4e3d2c1b");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("9a8b7c6d-5e4f-3d2c-1b0a-9f8e7d6c5b4a");

    public BreathingSlayerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10; // Same as zombie
        this.setPersistenceRequired(); // Prevent despawning

        // Set equipment immediately (will be re-set in finalizeSpawn)
        if (!level.isClientSide) {
            this.setItemSlot(EquipmentSlot.MAINHAND, getEquippedSword());
            ItemStack[] armor = getArmorEquipment();
            this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
            this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
            this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
            this.setItemSlot(EquipmentSlot.FEET, armor[3]);
        }
    }

    /**
     * Get the breathing technique for this slayer
     */
    public abstract BreathingTechnique getBreathingTechnique();

    /**
     * Get the equipped sword for this slayer
     */
    public abstract ItemStack getEquippedSword();

    /**
     * Get armor equipment for this slayer [head, chest, legs, feet]
     */
    public abstract ItemStack[] getArmorEquipment();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_FORM_INDEX, 0);
        this.entityData.define(CURRENT_ANIMATION, "idle");
        this.entityData.define(ANIMATION_TICKS, 0);
        this.entityData.define(POWER_LEVEL, 1); // Default to power level 1
    }

    public int getCurrentFormIndex() {
        return this.entityData.get(CURRENT_FORM_INDEX);
    }

    public void setCurrentFormIndex(int index) {
        this.entityData.set(CURRENT_FORM_INDEX, index);
    }

    public void cycleForm() {
        BreathingTechnique technique = getBreathingTechnique();
        int currentIndex = getCurrentFormIndex();
        int newIndex = (currentIndex + 1) % technique.getFormCount();
        setCurrentFormIndex(newIndex);
    }

    public int getPowerLevel() {
        return this.entityData.get(POWER_LEVEL);
    }

    public void setPowerLevel(int level) {
        if (level < 1) level = 1;
        if (level > 4) level = 4;
        this.entityData.set(POWER_LEVEL, level);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Breathing form attacks (highest priority)
        this.goalSelector.addGoal(0, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.BreathingFormAttackGoal(this));

        // Priority 1: Float in water
        this.goalSelector.addGoal(1, new FloatGoal(this));

        // Priority 2: Animated melee attack (plays attack animations)
        this.goalSelector.addGoal(2, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.AnimatedMeleeAttackGoal(this, 1.0D, false));

        // Priority 3: Random stroll
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));

        // Priority 4: Look at player
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Priority 5: Random look around
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Target goals
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Target demons from kimetsunoyaiba mod (using entity tags)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            (entity) -> {
                // Target entities with demon or twelve_kizuki tags
                return entity.getType().is(DEMON_TAG) || entity.getType().is(TWELVE_KIZUKI_TAG);
            }));

        // Target players who are demons (check NBT data)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            (player) -> {
                // Check if player has "oni" (demon) NBT tag
                return player.getPersistentData().getBoolean("oni");
            }));
    }

    /**
     * Create default attributes for breathing slayers
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D) // 2x player health
            .add(Attributes.ATTACK_DAMAGE, 7.5D) // Matches sword damage
            .add(Attributes.MOVEMENT_SPEED, 0.3D) // Slightly slower than player
            .add(Attributes.ARMOR, 10.0D) // Some natural protection
            .add(Attributes.FOLLOW_RANGE, 32.0D); // Aggressive pursuit
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Tick down breathing form cooldown
        if (this.breathingFormCooldown > 0) {
            this.breathingFormCooldown--;
        }
    }

    public boolean isBreathingFormOnCooldown() {
        return this.breathingFormCooldown > 0;
    }

    public void setBreathingFormCooldown(int ticks) {
        this.breathingFormCooldown = ticks;
    }

    public int getBreathingFormCooldown() {
        return this.breathingFormCooldown;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag dataTag) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        // Randomly assign power level (1-4)
        int powerLevel = this.random.nextInt(4) + 1;
        setPowerLevel(powerLevel);

        // Apply power level bonuses
        applyPowerLevelBonuses(powerLevel);

        // Apply 2x attack speed to all entities
        apply2xAttackSpeed();

        // Equip sword in main hand
        this.setItemSlot(EquipmentSlot.MAINHAND, getEquippedSword());

        // Equip armor
        ItemStack[] armor = getArmorEquipment();
        this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
        this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
        this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
        this.setItemSlot(EquipmentSlot.FEET, armor[3]);

        // Prevent equipment from dropping and make it permanent
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        return spawnData;
    }

    /**
     * Apply bonuses based on power level
     * Level 1: Speed 2, Resistance 1, 40 HP
     * Level 2: Speed 3, Resistance 2, Strength 1, 60 HP
     * Level 3: Speed 4, Resistance 3, Strength 1, 70 HP
     * Level 4: Speed 6, Resistance 4, Strength 2, 80 HP
     */
    private void applyPowerLevelBonuses(int powerLevel) {
        // Set max health based on power level
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double health = switch (powerLevel) {
                case 1 -> 40.0;
                case 2 -> 60.0;
                case 3 -> 70.0;
                case 4 -> 80.0;
                default -> 40.0;
            };
            maxHealth.setBaseValue(health);
            this.setHealth((float) health);
        }

        // Apply speed effect based on power level
        int speedLevel = switch (powerLevel) {
            case 1 -> 1; // Speed 2 (1 + 1)
            case 2 -> 2; // Speed 3
            case 3 -> 3; // Speed 4
            case 4 -> 5; // Speed 6 (5 + 1)
            default -> 1;
        };
        // ambient=true (no particles), showParticles=false
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, speedLevel, true, false));

        // Apply resistance effect based on power level
        int resistanceLevel = powerLevel - 1; // Level 1 = Resistance 1, etc.
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, resistanceLevel, true, false));

        // Apply strength effect for power levels 2-4
        if (powerLevel >= 2) {
            int strengthLevel = (powerLevel >= 4) ? 1 : 0; // Level 4 = Strength 2, Level 2-3 = Strength 1
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, strengthLevel, true, false));
        }
    }

    /**
     * Apply 2x attack speed to all entities
     */
    private void apply2xAttackSpeed() {
        AttributeInstance attackSpeed = this.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            // Remove existing modifier if present
            attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_UUID);

            // Add 100% attack speed (2x speed)
            AttributeModifier speedModifier = new AttributeModifier(
                ATTACK_SPEED_MODIFIER_UUID,
                "Power level attack speed bonus",
                1.0, // 100% increase = 2x speed
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            attackSpeed.addPermanentModifier(speedModifier);
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        // Override to prevent default equipment from replacing ours
        // Equipment is set in finalizeSpawn instead
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CurrentFormIndex", getCurrentFormIndex());
        tag.putInt("BreathingFormCooldown", this.breathingFormCooldown);
        tag.putInt("PowerLevel", getPowerLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCurrentFormIndex(tag.getInt("CurrentFormIndex"));
        this.breathingFormCooldown = tag.getInt("BreathingFormCooldown");

        // Restore power level and apply bonuses
        int powerLevel = tag.getInt("PowerLevel");
        if (powerLevel > 0) {
            setPowerLevel(powerLevel);
            applyPowerLevelBonuses(powerLevel);
            apply2xAttackSpeed();
        }
    }

    /**
     * Trigger a GeckoLib animation (for attacks and abilities)
     * @param animationName Animation name from biped.animation.json
     * @param durationTicks How long to play the animation (in ticks)
     */
    public void playGeckoAnimation(String animationName, int durationTicks) {
        // Sync to client via entity data
        this.entityData.set(CURRENT_ANIMATION, animationName);
        this.entityData.set(ANIMATION_TICKS, durationTicks);

        // Debug logging
        if (!this.level().isClientSide) {
            System.out.println("[BreathingSlayerEntity] Playing animation: " + animationName + " for " + durationTicks + " ticks");
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Get synced animation data
        int animTicks = this.entityData.get(ANIMATION_TICKS);

        // Tick down animation timer
        if (animTicks > 0) {
            animTicks--;
            this.entityData.set(ANIMATION_TICKS, animTicks);

            if (animTicks == 0) {
                // Animation finished, return to idle/walk
                String newAnim = this.getDeltaMovement().horizontalDistanceSqr() > 0.0001 ? "walk" : "idle";
                this.entityData.set(CURRENT_ANIMATION, newAnim);
            }
        } else {
            // No special animation playing, update based on movement
            String newAnim = this.getDeltaMovement().horizontalDistanceSqr() > 0.0001 ? "walk" : "idle";
            String currentAnim = this.entityData.get(CURRENT_ANIMATION);
            if (!currentAnim.equals(newAnim)) {
                this.entityData.set(CURRENT_ANIMATION, newAnim);
            }
        }
    }

    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    public int getAnimationTicks() {
        return this.entityData.get(ANIMATION_TICKS);
    }

    // GeckoLib animation methods
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main controller - handles ALL animations (walk, idle, attacks, abilities)
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            String anim = getCurrentAnimation();
            int animTicks = getAnimationTicks();

            // Attack and ability animations (play once)
            if (animTicks > 0 && !anim.equals("idle") && !anim.equals("walk")) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            // Movement animations (loop)
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            } else {
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
